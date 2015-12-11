package edu.pitt.tcga.httpclient.module.bam;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.storage.StorageFactory;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;


/**
 * 
 * Creates *.tsv file for not yet downloaded BAM files based on the most recent cgHub report
 * from https://cghub.ucsc.edu/reports/SUMMARY_STATS/LATEST_MANIFEST.tsv.
 *
 * @author opm1
 * @version 1
 * @since Dec 11, 2015
 *
 */

public class PreprocessRequest {
	
	private List<String> existingAnalysIDList = null;
	private String[] existingCols = {"study","barcode","disease","disease_name","sample_type","sample_type_name","analyte_type","library_type",
			"center","center_name","platform","platform_name","assembly","filename","files_size","checksum","analysis_id","aliquot_id",
			"participant_id","sample_id","tss_id","sample_accession","published","uploaded","modified","state"};
	
	private String[] newCols = {"side","start_time","end_time","download_attempt_num","status","overall_rate_(MB/s)","pgrr_file_path"};
	private String[] newVals = {"","","","","","",""};
	
	private final String Q_TEMPLATE ="cgquery \"<disieaseAbbr>=OV&library_strategy=<library_type>\"";
	private final String latestManifestURL = "https://cghub.ucsc.edu/reports/SUMMARY_STATS/LATEST_MANIFEST.tsv"; 
	
	
	/**
	 * 
	 * @param studyName - eg. 'TCGA'
	 * @param diseaseAbbr - eg. 'brca'
	 * @param libType - eg. 'WXS'
	 */
	public PreprocessRequest(String studyName, String diseaseAbbr, String libType){
		existingAnalysIDList =StorageFactory.getStorage().resultAsStrList(StorageFactory.getStorage().getStrProperty("EXISTING_BAM_ANALYSI_IDS"), "origuuid");
		
		createTSV(studyName, diseaseAbbr,libType);
		
	}
	
	
	
	/**
	 * Creates *.tsv fie 
	 * @param studyName - eg. 'TCGA'
	 * @param diseaseAbbr - eg. 'brca'
	 * @param libType - eg. 'WXS'
	 * Use 'reason' when need to update the metadata (if analysis is not in the latest report
	 * Check by the way if a particular analysis which is already in PGRR has been 'Suppressed' or 'Bad data' or 'Redacted'
	 * and skip others: 'Submitted' or 'Uploading' or 'Validating sample'
	 */
	public void createTSV(String studyName, String diseaseAbbr, String libType){

		String savetToDir = BAMMetadataManager.addSlashAndCreateDirIfNeed("top.bam.tsv.dir", "need_to_download");
		
		String saveToTSV = savetToDir+"bam_status_"+diseaseAbbr.toUpperCase()+"_"+libType+".tsv";


		HttpClient httpclient = TCGAHelper.getHttpClient();
		InputStream is = TCGAHelper.getGetResponseInputStream(
				httpclient, latestManifestURL);

		CSVReader reader = new CSVReader(new BufferedReader(
				new InputStreamReader(is)), '\t');
		String[] readLine = null;
		List<Integer> needColNumbers = new LinkedList<Integer>();
		List<String[]> newData = new LinkedList<String[]>();
		int reason_col = -1, analysisID_col = -1, pl_col = -1, plName_col = -1;
		int study_col = -1, dis_col = -1, libType_col = -1, state_col = -1;
		boolean isHeader = true;
		Map<String, String> deleteID_ReasonMap = new HashMap<String,String>();
		int headSz = existingCols.length; // remove "reason" col
		int len = newCols.length;
		String[] newArr = new String[headSz + len];
		int count = 0;
		try {
			while ((readLine = reader.readNext()) != null){
	if(String.valueOf(count).endsWith("0000"))
		System.out.println("Processed "+count+" lines");
	count++;
				if(isHeader) {
					isHeader = false;
					for(String s:existingCols){
						needColNumbers.add(ArrayUtils.indexOf(readLine, s));
					}
					// set col numbers used for filter
					reason_col = ArrayUtils.indexOf(readLine, "reason");
					analysisID_col = ArrayUtils.indexOf(readLine, "analysis_id");
					pl_col = ArrayUtils.indexOf(readLine, "platform");
					plName_col = ArrayUtils.indexOf(readLine, "platform_name");
					
					study_col = ArrayUtils.indexOf(readLine, "study");
					dis_col = ArrayUtils.indexOf(readLine, "disease");
					libType_col = ArrayUtils.indexOf(readLine, "library_type");
					state_col = ArrayUtils.indexOf(readLine, "state"); // look for "Suppressed"
					
					//add header
					System.arraycopy(existingCols, 0, newArr, 0, headSz);
					System.arraycopy(newCols, 0, newArr, headSz, len);
					newData.add(newArr);
				} 
				else if(readLine[study_col].equalsIgnoreCase(studyName)){
					// check if analysis is still valid
					if(existingAnalysIDList.contains(readLine[analysisID_col].toLowerCase())  && 
							(readLine[state_col].equalsIgnoreCase("Bad data") || 
								readLine[state_col].equalsIgnoreCase("Redacted") ||
								readLine[state_col].equalsIgnoreCase("Suppressed"))) {
						deleteID_ReasonMap.put(readLine[analysisID_col].toLowerCase(), readLine[state_col]+": "+readLine[reason_col]);
						
					}
					else if(readLine[dis_col].equalsIgnoreCase(diseaseAbbr) && readLine[libType_col].equalsIgnoreCase(libType) &&
							readLine[state_col].equalsIgnoreCase("LIVE") && !existingAnalysIDList.contains(readLine[analysisID_col].toLowerCase())){
			
						readLine[pl_col] = normPlatform(readLine[pl_col]);
						readLine[plName_col] = normPlatform(readLine[plName_col]);
						newArr = new String[headSz + len];
						int cc = 0;
						for(Integer i:needColNumbers){
							newArr[cc] = readLine[i];
							cc++;
						}
						System.arraycopy(newVals, 0, newArr, headSz, len);
						newData.add(newArr);
					}
				}

		
			}
			
			List<String[]> copy = new ArrayList<String[]>(newData);
			writeToFile(saveToTSV, newData);
			
	for (Map.Entry<String, String> entry : deleteID_ReasonMap.entrySet()) {
		System.out.println("*** NEED to Delete origuuid: "+entry.getKey()+"    reason: "+entry.getValue());
	}			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				reader.close();
				if(is != null)
					is.close();
				if(httpclient != null)
					httpclient.getConnectionManager().shutdown();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
	}
	
	/**
	 * Normalizes 'platformname' from cgHub's LATEST_MANIFEST.tsv. 
	 * Replaces '_' or ' ' with dash.
	 * @param name
	 * @return
	 */
	private String normPlatform(String name){
		name = name.replaceAll("_","-");
		return name.replaceAll(" ","-");
	}
	
	/**
	 * Writes to *.tsv file
	 * @param filePath
	 * @param list
	 */
	public static void writeToFile(String filePath, List<String[]> list){
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(filePath), false));			
			output.write(ModuleUtil.listArrayToString(list, "\t"));
			
			list.clear();
			list = null;

			output.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		//test
		/*
		 * args = new String[3]; args[0] = "TCGA"; args[1] = "luad"; args[2] =
		 * "Bisulfite-Seq";
		 */
		
		
		if(args.length != 4){
			System.out.println("edu.pitt.tcga.httpclient.module.bam.PreprocessRequest requires 4 arguments:\n" +
					"(1) study name ('TCGA')\n" +
					"(2) disease abbreviation ('brca')\n" +
					"(3) library type ('WGS')."); 
			System.exit(0);
		}
		else {
			PreprocessRequest pp = new PreprocessRequest(args[0], args[1],args[2]);
		}
		
		System.out.println("DONE createTSV");
	}

}
