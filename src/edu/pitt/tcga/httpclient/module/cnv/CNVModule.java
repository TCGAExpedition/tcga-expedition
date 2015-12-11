package edu.pitt.tcga.httpclient.module.cnv;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
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
import edu.pitt.tcga.httpclient.util.ReferenceGenomeUtil;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public abstract class CNVModule extends TCGAModule{
	
	// platforms :Genome_Wide_SNP_6,  	Human1MDuo (ov, lusc),  	HumanHap550 (gbm)
	// exclude cgcc/stanford.edu/humanhap550/snp - too old
	
	protected static DataMatrix dataMatrix = null;
	protected String level = null;
	

	protected abstract String[] getAcceptableEndings();
	protected abstract int getFileColNum(DataMatrix dm, String lbName);
	protected abstract String getAlgorithmName(String lbName); //name
	protected abstract String getFileType(String lbName);
	protected abstract String getPortion(String defPortion, String lbName);
	protected abstract String getRefGenome(String lbName);
	protected abstract void processMultiSampleFile(LineBean lb);


	@Override
	public String[] getResourceEndings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDataType() {
		return "CNV_(SNP_Array)";
	}

	@Override
	public  boolean canProcessArchive(LineBean archiveBean){
		String archiveName = archiveBean.getName();
		return (archiveName.indexOf(".Level_") != -1  || archiveName.indexOf(".mage-tab.") != -1);
	}

	
	protected void setLevel(String l){
		level = l;
	}
	
	public String getLevel(){
		return level;
	}
	
	@Override
	public void processData(List<LineBean> levBeans) {

		int sz = levBeans.size();
		int cn = 0;
		
		for (LineBean lb : levBeans) {

				if (acceptableEnding(lb.getName(), getAcceptableEndings()) && !TCGAHelper.stringContains(lb.getName(), MySettings.infoFilesList)) {
	
				cn++;
	
		// clear tempo dir
				clearTempoDir();
				System.gc();
							
				System.out.println(lb.getDiseaseStudy()+" : "+cn+" out of "+sz+"  lb.n = " + lb.getUrl());
				
				
				Aliquot al = null;
				try{
									
					if(lb.getName().endsWith(".sdrf.txt")){
						readInDataMatrix("MageTabSDRF", lb.getFullURL());
						
					} else {
						setLevel(lb.getLevel());
						al = constructAliquot(lb);	
						File tempoFile = new File(MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".txt");
						TEMP_FILE_NUM++;
						if(al != null){
							al.setTempoFile(tempoFile);
			
							TCGAHelper.copyURLToFile(new URL(lb.getFullURL()), tempoFile, dataAccessType().equals("controlled"));
							
						} else 
							processMultiSampleFile(lb);
					}
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	
				if(al != null){
					ModuleUtil.transferNew(new Aliquot[]{al});	
				}
					
			}

		} // for(LineBean lb:levBeans)
	}
	
	public void readInDataMatrix( String dmName, String urlStr){
		if(dataMatrix == null)
			dataMatrix = new DataMatrix(dmName);
		else 
			dataMatrix.clear();
		try{
			HttpClient httpclient = TCGAHelper.getHttpClient();
			InputStream is = TCGAHelper.getGetResponseInputStream(
					httpclient, urlStr);
	
			CSVReader reader = new CSVReader(new BufferedReader(
					new InputStreamReader(is)), '\t');
			
			dataMatrix.setData(reader.readAllToList());
			reader.close();
			is.close();	
			httpclient.getConnectionManager().shutdown();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public String getFileExtension(String lbName) {
		int ind = lbName.lastIndexOf(".");
		// TODO Auto-generated method stub
		return (lbName.substring(ind+1)).toLowerCase();
	}
	

	public String getBarcodeColName() {
		return "Comment [TCGA Barcode]";
	}
	
	public  Aliquot constructAliquot(LineBean lb, String aliquotBarcode){
		String lbName = lb.getName();
		
		int rowNum = getDataRowNum(lbName, getFileColNum(dataMatrix, lbName));
		if(aliquotBarcode == null && rowNum == -1 )
			return null;
		
		Aliquot al = new Aliquot(lb.getUrl(), getDataType(),
				getFileExtension(lb.getName()));
		if (rowNum > -1) 
			al.setBarcode(dataMatrix.getRowColValue(getBarcodeColName(), rowNum));
		else
			al.setBarcode(aliquotBarcode);
			
		al.setOrigUUID(ModuleUtil.getUUIDByBarcode(al.getBarcode()));
		al.setFileFractionType("aliquot");

		al.setCenterName(CodesUtil.getCenterNameFromArchive(al.getArchiveName()));
		al.setCenterCode(CodesUtil.getCenterAbbFromArchName(al.getCenterName()));
		al.setRefGenome(getRefGenome(lbName));
		
		String genURL = ReferenceGenomeUtil.getGenomeURL(al.getRefGenome());
		if(genURL != null)
			al.setRefGenomeSource(genURL);
		al.setPlatform(lb.getPlatform());
		al.setFileType(getFileType(lbName));
		al.setAlgorithmName(getAlgorithmName(lbName));
		al.setPortion(getPortion(al.getPortion(), lbName));
		al.setDataAccessType(dataAccessType());
	
		return al;
		
	}
	
	public  Aliquot constructAliquot(LineBean lb){
		return constructAliquot(lb, null);
	
	}
	
	public int getDataRowNum(String lbName, int colNum){
		try{
			return dataMatrix.getDataRow(lbName, colNum);
		} catch (NullPointerException e){
			return -1;
		}
	}
	
	

}
