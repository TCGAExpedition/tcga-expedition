package edu.pitt.tcga.httpclient.module.eg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.module.rnaseq.level3.RNASeqLevel3;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public abstract class ExpGeneModule extends TCGAModule{
	
	protected DataMatrix dataMatrix = null;
	
	private String[] endings =  {".sdrf.txt", ".txt", ".cel"};
	

	protected String level = null;
	
	@Override
	public  boolean canProcessArchive(LineBean archiveBean){
		String archiveName = archiveBean.getName();
		return (archiveName.indexOf(".Level_") != -1  || archiveName.indexOf(".mage-tab.") != -1);
	}

	@Override
	public void processData(List<LineBean> levBeans) {
		int sz = levBeans.size();
		int cn = 0;
		for (LineBean lb : levBeans) {
				if (acceptableEnding(lb.getName(), getResourceEndings()) && !TCGAHelper.stringContains(lb.getName(), MySettings.infoFilesList)) {
				
					cn++;
		
			// clear tempo dir
					clearTempoDir();
					System.gc();
								
		System.out.println(lb.getDiseaseStudy()+" : "+cn+" out of "+sz+"  lb.n = " + lb.getUrl());
					
					
					Aliquot al = null;
					try{
										
						if(lb.getName().endsWith(".sdrf.txt")){
							if(dataMatrix == null)
								dataMatrix = new DataMatrix("MageTabSDRF");
							else 
								dataMatrix.clear();
							
							HttpClient httpclient = TCGAHelper.getHttpClient();
							InputStream is = TCGAHelper.getGetResponseInputStream(
									httpclient, lb.getFullURL());
				
							CSVReader reader = new CSVReader(new BufferedReader(
									new InputStreamReader(is)), '\t');
							
							dataMatrix.setData(reader.readAllToList());
							reader.close();
							is.close();	
							httpclient.getConnectionManager().shutdown();
						}else {
							setLevel(lb.getLevel());
							al = constructAliquot(lb);	
							File tempoFile = new File(MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".txt");
							TEMP_FILE_NUM++;
							if(al != null){
								al.setTempoFile(tempoFile);
								FileUtils.copyURLToFile(new URL(lb.getFullURL()), tempoFile);
							}
						}
						
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
	
					if(al != null){
						ModuleUtil.transferNew(new Aliquot[]{al});	
					}

				} //fNameEnding != null
	
		} // for(LineBean lb:levBeans)
	}
	
	public void setLevel(String l){
		level = l;
	}
	
	public String getLevel(){
		return level;
	}

	@Override
	public String dataAccessType() {
		return TCGAModule.PUBLIC;
	}
	
	public  Aliquot constructAliquot(LineBean lb){
		String lbName = lb.getName();
	
		int rowNum = getDataRowNum(lbName, getFileColNum(dataMatrix, lbName));
		if (rowNum > -1) {
			Aliquot al = new Aliquot(lb.getUrl(), getDataType(),
					getFileExtension(lb.getName()));
			al.setBarcode(dataMatrix.getRowColValue(getBarcodeColName(), rowNum));
			
			al.setOrigUUID(ModuleUtil.getUUIDByBarcode(al.getBarcode()));
			al.setFileFractionType("aliquot");

			al.setCenterName(CodesUtil.getCenterNameFromArchive(al.getArchiveName()));
			al.setCenterCode(CodesUtil.getCenterAbbFromArchName(al.getCenterName()));
			al.setRefGenome("unknown");
			al.setRefGenomeSource("unknown");
			al.setPlatform(lb.getPlatform());
			al.setFileType(getFileType(lbName));
			al.setAlgorithmName(getAlgorithmName());
			al.setPortion(getPortion(al.getPortion(), lbName));
			return al;
		} else {
			String err = "ExpGeneModule: can't find in mage-tab a row for "+lb.getFullURL()+"\n name: "+lbName;
			System.err.println(err);
			ErrorLog.logFatal(err);
			return null;
		}
	}
	
	public String getDataType(){
		return "Expression_Gene";
	}
	
	
	public abstract String getFileExtension(String lbName);
	public abstract int getFileColNum(DataMatrix dm, String lbName);
	public abstract String getBarcodeColName();
	public abstract String getAlgorithmName(); //name
	public abstract String getFileType(String lbName);
	
	public String getPortion(String defPortion, String lbName){
		return defPortion;
	}
	
	
	public int getDataRowNum(String lbName, int colNum){
		return dataMatrix.getDataRow(lbName, colNum);
	}

	@Override
	public String[] getResourceEndings() {
		return endings;
	}

	@Override
	public String getAnalysisDirName() {
		return "transcriptome";
	}

	@Override
	public String getResourceKey() {
		return "exp_gene";
	}
	

}
