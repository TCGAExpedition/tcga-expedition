package edu.pitt.tcga.httpclient.module.methyl;

import java.io.BufferedReader;
import java.io.File;
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
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public abstract class MethylModule extends TCGAModule{
	
	protected DataMatrix dataMatrix = null;
	protected DataMatrix adfMatrix = null;
	
	private String[] endings =  {".sdrf.txt", ".txt", ".idat", ".adf.txt"};

	
	@Override
	public  boolean canProcessArchive(LineBean archiveBean){
		String archiveName = archiveBean.getName();
		return (archiveName.indexOf(".Level_") != -1  || archiveName.indexOf(".mage-tab.") != -1);
	}

	public void processData(List<LineBean> levBeans) {
		int sz = levBeans.size();
		int cn = 0;
		String fNameEnding = null;
		for (LineBean lb : levBeans) {
			// skip .aux. archive
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
							
							readDataToMatrix(dataMatrix, lb.getFullURL(), '\t');
							
						}else if (lb.getName().endsWith(".adf.txt") && needADF()){
							if(adfMatrix == null)
								adfMatrix = new DataMatrix("MageTabADF");
							else 
								adfMatrix.clear();
							
							readDataToMatrix(adfMatrix, lb.getFullURL(), '\t');
	
						}else {
							setLevel(lb.getLevel());
							al = constructAliquot(lb);
							if(!needTempoFile()){
								al.setTcgaFileUrl(new URL(lb.getFullURL()));
							}else {
								File tempoFile = new File(MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".txt");
								TEMP_FILE_NUM++;
								if(al != null){
									al.setTempoFile(tempoFile);
									constructFile(lb.getFullURL(), tempoFile, al.getArchiveName());
								}
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
			al.setLevel(getLevel());
			al.setOrigUUID(ModuleUtil.getUUIDByBarcode(al.getBarcode()));
			al.setFileFractionType("aliquot");

			al.setCenterName(CodesUtil.getCenterNameFromArchive(al.getArchiveName()));
			al.setCenterCode(CodesUtil.getCenterAbbFromArchName(al.getCenterName()));
			al.setRefGenome(getRefGenome(rowNum));
			al.setRefGenomeSource(getRefGenomeSource(al.getArchiveName()));
			al.setPlatform(lb.getPlatform());
			al.setFileType(getFileType(lbName));
			al.setAlgorithmName(getAlgorithmName(al.getArchiveName()));
			al.setPortion(getPortion(al.getPortion(), lbName));
			
			return al;
		} else {
			String err = "MethylModule: can't find in mage-tab a row for "+lb.getFullURL()+"\n name: "+lbName;
			System.err.println(err);
			ErrorLog.logFatal(err);
			return null;
		}
	}
	
	@Override
	public String getDataType(){
		return "DNA_Methylation";
	}
	
	
	public abstract String getFileExtension(String lbName);
	public abstract int getFileColNum(DataMatrix dm, String lbName);
	public abstract String getBarcodeColName();
	public abstract String getAlgorithmName(String archiveName); //name
	public abstract String getFileType(String lbName);
	public abstract String getPortion(String defPortion, String lbName);
	public abstract String getRefGenome(int rowNum);
	public abstract String getRefGenomeSource(String archiveName);
	public abstract void setLevel(String level);
	public abstract String getLevel();
	public abstract boolean needADF();
	public abstract boolean needTempoFile();
	public abstract void constructFile(String lbFullURL, File saveAs, String archiveName);
	
	
	public int getDataRowNum(String lbName, int colNum){
		return dataMatrix.getDataRow(lbName, colNum);
	}

	@Override
	public String[] getResourceEndings() {
		return endings;
	}

	@Override
	public String getAnalysisDirName() {
		return "methylation";
	}

	@Override
	public String getResourceKey() {
		return "dna.methyl";
	}
	

}

