package edu.pitt.tcga.httpclient.module.methyl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class IllumMethylOMA  extends MethylModule {
	private String level = null;
	

	@Override
	public String getFileExtension(String lbName) {
		return "txt";
	}

	@Override
	public int getFileColNum(DataMatrix dm, String lbName) {
		if("2".equals(getLevel())){
			return dm.getColumnNum("Derived Array Data Matrix File",1); 
		}
			
		else return dm.getColumnNum("Derived Array Data Matrix File",2); 
	}

	@Override
	public String getBarcodeColName() {
		return "Comment [TCGA Barcode]";
	}

	@Override
	public String getAlgorithmName(String archiveName) {
		return "Illumina Golden Gate BeadArray";
	}

	@Override
	public String getFileType(String lbName) {
		return "methyl_Level_"+getLevel();
	}

	@Override
	public String getPortion(String defPortion, String lbName) {
		return defPortion;
	}

	@Override
	public String getRefGenome(int rowNum) {
		return "36.1";	
	}

	@Override
	public String getRefGenomeSource(String archiveName) {
		if(archiveName.toLowerCase().indexOf("oma002") != -1)
			return "https://tcga-data.nci.nih.gov/docs/integration/fasta/jhu-usc.edu_TCGA_IlluminaDNAMethylation_OMA002_CPI.fa.zip";
		else
			return "https://tcga-data.nci.nih.gov/docs/integration/adfs/tcga/jhu-usc.edu_TCGA_IlluminaDNAMethylation_OMA003_CPI.adf.txt.zip";
	}

	@Override
	public boolean needADF() {
		return true;
	}
	
	/**
	 * Level_3 (original level_2) needs to be modified
	 */
	@Override
	public boolean needTempoFile(){
		return ("3".equals(getLevel())) ? true : false;
	}
	
	
	/**
	 * since this level_1 corresponds to usual level_2
	 */
	@Override
	public void setLevel(String l){
		try{
			int intL = Integer.parseInt(l);
			level = String.valueOf(intL+1);
		} catch (NumberFormatException e ){
			e.printStackTrace();
			level = l;
		}
	}
	
	@Override
	public String getLevel(){
		return level;
	}
	
	/**
	 * level 3:
	 * OLD header line 1:  ["Hybridization REF", <aliquot_barcode>]
	 * NEW header line 1:  ["Hybridization REF", {<aliquot_barcode> 5 times}]
	 * OLD header line 2: ["Composite Element REF", "Beta Value"]
	 * NEW header line 2: ["Composite Element REF", "Beta_Value", Gene_Symbol", "Chromosome", "Genomic_Coordinate", "Input_Sequence"]
	 */
	@Override
	public void constructFile(String lbFullURL, File saveAs, String archiveName){
		
		HttpClient httpclient = null;
		InputStream is = null;
		CSVReader reader = null;
		BufferedWriter writer = null;
		try{
			httpclient = TCGAHelper.getHttpClient();
			is = TCGAHelper.getGetResponseInputStream(httpclient, lbFullURL);
	
			reader = new CSVReader(new BufferedReader(
					new InputStreamReader(is)), '\t');
			writer = new BufferedWriter(new FileWriter(saveAs, true));
			String[] readLine = null;
			String[] newLine = null;
			String[] HEADER_Line2 = {"Composite Element REF", "Beta_Value", "Gene_Symbol", "Chromosome", "Genomic_Coordinate", "Input_Sequence"};

			int count = 1; 
			
			while ((readLine = reader.readNext()) != null) {
				if(count == 1){
					count++;
					newLine = new String[HEADER_Line2.length];
					newLine[0] = readLine[0];
					for(int i=1; i<HEADER_Line2.length; i++)
						newLine[i] = readLine[1];
					
				}
				else if(count == 2) {
					newLine = HEADER_Line2;
					count++;
				} else{
					newLine = new String[6];
					
					int lookupRow = adfMatrix.getDataRow(readLine[0], 0);
					newLine[0] = readLine[0];
					newLine[1] = readLine[1];
					newLine[2] = adfMatrix.getRowColValue(2, lookupRow); 	// Gene_Symbol
					if(archiveName.toLowerCase().indexOf("oma002") != -1)	// Chromosome
						newLine[3] = adfMatrix.getRowColValue(4, lookupRow);   
					else
						newLine[3] = adfMatrix.getRowColValue(3, lookupRow);
					if(archiveName.toLowerCase().indexOf("oma002") != -1)	// Genomic_Coordinate
						newLine[4] = "";
					else
						newLine[4] = adfMatrix.getRowColValue(6, lookupRow);	
					if(archiveName.toLowerCase().indexOf("oma002") != -1)	// Input_Sequence
						newLine[5] = adfMatrix.getRowColValue(6, lookupRow);	
					else
						newLine[5] = adfMatrix.getRowColValue(5, lookupRow);		
				}
				writer.write(ModuleUtil.copyArrayToStr(newLine, MySettings.TAB));
			}
				
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				is.close();
				reader.close();
				writer.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			httpclient.getConnectionManager().shutdown();
		}
				
		
	}
	
	public static void main(String[] args){
		TCGAModule.clearTempoDir();
		
		IllumMethylOMA m = new IllumMethylOMA();
		String[] urls = {
				//MySettings.PUB_ROOT_URL+"gbm/cgcc/jhu-usc.edu/illuminadnamethylation_oma002_cpi/methylation/"
				MySettings.PUB_ROOT_URL+"gbm/cgcc/jhu-usc.edu/illuminadnamethylation_oma003_cpi/methylation/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		
		System.out.println("Done IllumMethylOMA");

	}

}
