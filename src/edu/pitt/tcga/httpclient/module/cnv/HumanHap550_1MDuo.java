package edu.pitt.tcga.httpclient.module.cnv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class HumanHap550_1MDuo  extends CNVModule{

	
	
	private String[] acceptableEndingArr = {".idat", "sdrf.txt", ".txt"}; //using lower case for comparison
	
	private DataMatrix dMatrix = null;


	public String getBarcodeColName() {
		return "Normalization Name";
	}

	
	@Override
	protected String[] getAcceptableEndings() {
		return acceptableEndingArr;
	}

	@Override
	protected int getFileColNum(DataMatrix dm, String lbName) {
		
		if("1".equals(getLevel()) && lbName.endsWith(".idat"))
			return dm.getColumnNum("Array Data File");
		else
			return -1;
	}
	
	@Override
	protected void processMultiSampleFile(LineBean lb){ 
		if(level == null)
			setLevel(lb.getLevel());
		Aliquot[] als = null;
		String lbNameLower = lb.getName().toLowerCase();
		if(lbNameLower.endsWith(".xandyintensity.txt"))
			als = processMultiSampleFile(lb,3,2);
		else if("2".equals(level))
			als = processMultiSampleFile(lb,3,1);
		else if ("3".equals(level))
			als = processMultiSampleLevel3(lb);
		
		if(als != null)
			ModuleUtil.transferNew(als);	
	}


	// See https://tcga-data-secure.nci.nih.gov/tcgafiles/tcga4yeo/tumor/gbm/cgcc/hudsonalpha.org/humanhap550/snp/hudsonalpha.org_GBM.HumanHap550.mage-tab.10.4.0/README.txt
	@Override
	protected String getAlgorithmName(String lbName) {
		String toret = "N/A";
		if("2".equals(getLevel())){
			if(lbName.toLowerCase().indexOf("_logr.") != -1)
				toret = "Log R Ratio";
			else if(lbName.toLowerCase().indexOf("delta_b_allele_freq") != -1)
				toret = "Delta B Frequency";
			else if(lbName.toLowerCase().indexOf(".genotypes.") != -1)
				toret = "Genotype calls at each SNP in A/B format";
			else if(lbName.toLowerCase().indexOf(".b_allele_freq.") != -1)
				toret = "B allele frequency";
			else {
				String errStr = "HumanHap550.getAlgorithmName: NO Algorithm for "+lbName;
				ErrorLog.log(errStr);
				System.err.println(errStr);
				System.out.println(errStr);
			}
		}
		else if("3".equals(getLevel())){
			if(lbName.toLowerCase().indexOf(".loh.") != -1)
				toret = "Delta B Segmentation";
			else
				toret = "Segmentation";	
		}
		
		return toret;
	}

	@Override
	protected String getFileType(String lbName) {
		String toret = null;
		if("1".equals(getLevel())){
			if(lbName.endsWith(".idat")){
				if(lbName.toLowerCase().endsWith("_red.idat"))
					toret =  "red-idat";
				else if (lbName.toLowerCase().endsWith("_grn.idat"))
					toret =  "grn-idat";
			}
			else if(lbName.toLowerCase().endsWith(".xandyintensity.txt"))
				toret = "intensities";
		} else if("2".equals(getLevel())){
			if(lbName.toLowerCase().indexOf(".genotypes.") != -1)
				toret = "genotype";
			else if(lbName.toLowerCase().indexOf(".b_allele_freq.") != -1)
				toret = "byallele-cn";
			else if(lbName.toLowerCase().indexOf("delta_b_allele_freq") != -1)
				toret = "delta-byallele-cn";
			else if(lbName.toLowerCase().indexOf(".paired_") != -1)
				toret = "paired-cn";
			else if(lbName.toLowerCase().indexOf(".npaired_") != -1)
				toret = "unpaired-cn";
			else if(lbName.toLowerCase().indexOf(".normal_") != -1)
				toret = "normal-cn";
		} else if ("3".equals(getLevel())){ // level3
			if(lbName.toLowerCase().indexOf(".loh.") != -1)
				toret = "loh";
			else if(lbName.toLowerCase().indexOf(".segnormal.") != -1)
				toret = "seg-normal";
			else
				toret = "seg";
		}
		
		return toret;
	}

	@Override
	protected String getPortion(String defPortion, String lbName) {
		
		String toret = null;
		if("1".equals(getLevel()) ){
			if(lbName.endsWith(".idat")){
				if(lbName.toLowerCase().endsWith("_red.idat"))
					toret =  "red";
				else if (lbName.toLowerCase().endsWith("_grn.idat"))
					toret =  "grn";
			}
			else if(lbName.toLowerCase().endsWith(".xandyintensity.txt"))
				toret = "intensities";
		} else if("2".equals(getLevel())){
			if(lbName.toLowerCase().indexOf(".genotypes.") != -1)
				toret = "genotype";
			else if(lbName.toLowerCase().indexOf(".b_allele_freq.") != -1)
				toret = "byallele-cn";
			else if(lbName.toLowerCase().indexOf("delta_b_allele_freq") != -1)
				toret = "delta-byallele-cn";
			else if(lbName.toLowerCase().indexOf(".paired_") != -1)
				toret = "paired-cn";
			else if(lbName.toLowerCase().indexOf(".unpaired_") != -1)
				toret = "unpaired-cn";
			else if(lbName.toLowerCase().indexOf(".normal_") != -1)
				toret = "normal-cn";
		} else if ("3".equals(getLevel())){ // level3
			if(lbName.toLowerCase().indexOf(".loh.") != -1)
				toret = "loh";
			else if(lbName.toLowerCase().indexOf(".segnormal.") != -1)
				toret = "seg-normal";
			else
				toret = "seg";
		}
		
		return toret;
	}

	@Override
	protected String getRefGenome(String lbName) {
		return "unknown";
	}

	@Override
	public String getAnalysisDirName() {
		return "snp";
	}

	@Override
	public String getResourceKey() {
		return "cnv_snp";
	}

	@Override
	public String dataAccessType() {
		return (level.equals("3"))?TCGAModule.PUBLIC:TCGAModule.CONTROLLED;
	}

	/**
	 * 
	 * @param lb
	 * @param firstUncommonCol - 3 for intensities - count from 0
	 * @param countBy - 2 for intensities, 1 for level2
	 * @param newDataNumCols  - 5 for inten
	 * @return
	 */
	protected Aliquot[] processMultiSampleFile(LineBean lb, int firstUncommonCol, int countBy){
		Aliquot[] als = null;
		//try{
			HttpClient httpclient = TCGAHelper.getHttpClient();
			InputStream is = TCGAHelper.getGetResponseInputStream(
					httpclient, lb.getFullURL());
			
			
			HashMap<String, String> aliquotTempFileMap = toAliquotTempFileMap(is, firstUncommonCol, countBy);
	
			httpclient.getConnectionManager().shutdown();
			
			als = new Aliquot[aliquotTempFileMap.size()];
			int count = 0;

			for(Map.Entry<String, String> entry : aliquotTempFileMap.entrySet()){

				Aliquot al = constructAliquot(lb, entry.getKey());
				al.setTempoFile(new File(entry.getValue()));
				als[count] = al;
System.out.println("created AL num "+count+" for "+al.getBarcode());
				count++;
			}
			
			aliquotTempFileMap.clear();
			aliquotTempFileMap = null;
		
		/*} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
		return als;
		
	}
	
	protected HashMap<String, String> toAliquotTempFileMap(InputStream is, int firstUncommonCol, int countBy){
		HashMap<String, String> aliquotTempFileMap = new LinkedHashMap<String, String> ();
		try{	
			CSVReader reader = new CSVReader(new BufferedReader(
					new InputStreamReader(is)), '\t');
			

			String[] readLine = null;
			int newDataNumCols = firstUncommonCol+countBy;
			int stTT = TEMP_FILE_NUM;
			boolean isHeader = true;
			int numCols = 0;
			String[] newLine = null;
			String tempFileName = null;
			
			HashMap<Integer, BufferedWriter> wMap = null;

			while ((readLine = reader.readNext()) != null){
				
					if(numCols == 0) 
						numCols = readLine.length;
					
					
					for(int i=firstUncommonCol; i<numCols; i+=countBy){
						newLine = new String[newDataNumCols];
						System.arraycopy(readLine, 0, newLine, 0, firstUncommonCol);
						System.arraycopy(readLine, i, newLine, firstUncommonCol, countBy);
						
						tempFileName = MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".txt";

						wMap = appendToFile(newLine, wMap, stTT+(i-firstUncommonCol)/countBy, tempFileName);
						TEMP_FILE_NUM++;
						
						if(isHeader)
							aliquotTempFileMap.put(readLine[i], tempFileName);
						
					}
					if(isHeader)
						isHeader = false;
			}
			
			is.close();	
			
			closeOutputs(wMap);
			
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return aliquotTempFileMap;
		
	}
	
	protected Aliquot[] processMultiSampleLevel3(LineBean lb){
		Aliquot[] als = null;
		try{
			HttpClient httpclient = TCGAHelper.getHttpClient();
			InputStream is = TCGAHelper.getGetResponseInputStream(
					httpclient, lb.getFullURL());
	
			CSVReader reader = new CSVReader(new BufferedReader(
					new InputStreamReader(is)), '\t');
			
			String[] commonHeader = null;
			String[] readLine = null;
			
			Integer sampleTT = -1;
			
			boolean isHeader = true;

			String[] newLine = null;
			String tempFileName = null;
			String currBarcode = "";
			
			HashMap<Integer, BufferedWriter> wMap = null; //<currTT, writer>
			HashMap<String, Integer> aliquotTempFileNumMap = new HashMap<String, Integer> (); // <AliquotBc, TempoFile name>

			while ((readLine = reader.readNext()) != null){
				
				if(isHeader){
					commonHeader = readLine; //{"Normalization Name", "chrom", "loc.start", "loc.end", "mean"};
					isHeader = false;
				}
				else{
					if(!currBarcode.equals(readLine[0])){
						currBarcode = readLine[0];
						sampleTT = aliquotTempFileNumMap.get(currBarcode);
	//System.out.println("***** sampleTT= "+sampleTT+"  for "+currBarcode);
						if(sampleTT == null){
							sampleTT = TEMP_FILE_NUM;
							aliquotTempFileNumMap.put(readLine[0], sampleTT);
							wMap = appendToFile(commonHeader, wMap, sampleTT, MySettings.TEMPO_DIR + "f"+String.valueOf(sampleTT)+".txt");
							TEMP_FILE_NUM++;
						}						
					}
					
					wMap = appendToFile(readLine, wMap, sampleTT, MySettings.TEMPO_DIR + "f"+String.valueOf(sampleTT)+".txt", true);
				}
					
			}
			
			is.close();	
			httpclient.getConnectionManager().shutdown();
			
			closeOutputs(wMap);
			
			als = new Aliquot[aliquotTempFileNumMap.size()];
			int count = 0;

			for(Map.Entry<String, Integer> entry : aliquotTempFileNumMap.entrySet()){

				Aliquot al = constructAliquot(lb, entry.getKey());
System.out.println("created AL num "+count+" for "+al.getBarcode());
				al.setTempoFile(new File(MySettings.TEMPO_DIR + "f"+String.valueOf(entry.getValue())+".txt"));
				als[count] = al;

				count++;
			}
			
			aliquotTempFileNumMap.clear();
			aliquotTempFileNumMap = null;
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return als;
	}
	
		
	private void closeOutputs(HashMap<Integer, BufferedWriter> wMap){
		if(wMap == null) return;
		for (Map.Entry<Integer, BufferedWriter> entry : wMap.entrySet()) {
			
			try {
				entry.getValue().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		wMap.clear();
		wMap = null;

	}
	
	private HashMap<Integer, BufferedWriter> appendToFile(String[] line, HashMap<Integer, BufferedWriter> wMap, int currTT, String filePath){
		
		return appendToFile(line, wMap, currTT, filePath, false);
		
	}
	
	private HashMap<Integer, BufferedWriter> appendToFile(String[] line, HashMap<Integer, BufferedWriter> wMap, int currTT, String filePath, boolean doAppend){
		if(wMap == null)
			wMap = new HashMap<Integer, BufferedWriter> ();
		try {

			BufferedWriter output = wMap.get(currTT);
		
			if(output == null){
				output = new BufferedWriter(new FileWriter(new File(filePath), doAppend));	
				wMap.put(currTT, output);
			}
			output.write(ModuleUtil.copyArrayToStr(line, "\t"));

			line = null;


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return wMap;
	}
	
	public static void main(String[] args) {
		
		//test below
		MySettings.PGRR_META_NQ_FILE = "_BRCA_BROAD_gwSNP6_TEST.nq";
		
		HumanHap550_1MDuo pmf = new HumanHap550_1MDuo();
		// level 1
		/*pmf.processMultiSampleFile(new LineBean(MySettings.getControlledRoot()+
				"gbm/cgcc/hudsonalpha.org/humanhap550/snp/hudsonalpha.org_GBM.HumanHap550.Level_1.11.0.0/hudsonalpha.org_GBM.HumanHap550.11.0.0.XandYintensity.txt"));*/
		// level 2
		/*pmf.processMultiSampleFile(new LineBean(MySettings.getControlledRoot()+
				"gbm/cgcc/hudsonalpha.org/humanhap550/snp/hudsonalpha.org_GBM.HumanHap550.Level_2.12.0.0/hudsonalpha.org_GBM.HumanHap550.12.0.0.B_Allele_Freq.txt"));*/

		// level 3
		/*pmf.processMultiSampleFile(new LineBean(MySettings.PUB_ROOT_URL+
				"gbm/cgcc/hudsonalpha.org/humanhap550/snp//hudsonalpha.org_GBM.HumanHap550.Level_3.8.2.0/hudsonalpha.org_GBM.HumanHap550.8.2.0.segnormal.txt"));*/
		

		

		System.out.println("done HumanHap550");
	}



}
