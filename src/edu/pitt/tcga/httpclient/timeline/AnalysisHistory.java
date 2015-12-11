package edu.pitt.tcga.httpclient.timeline;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.tcga.httpclient.util.CSVWriter;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAChangeTracker;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

/**
 * Creates a tsv file with Date, Num_Added, Num_Modified, Num_Deleted files 
 * for a particular analysis.
 * @author Olga Medvedeva
 *
 */

public class AnalysisHistory {
		
	private Map<String,DateRecord> records = null;
	
	private static String[] header = {"DATE", "DATE_AS_DAYS", "ADDED", "MODIFIED", "DELETED", "TOTAL_NUM_FILES"};
	private String headerStr = "DATE\tDATE_AS_DAYS\tADDED\tMODIFIED\tDELETED\tTOTAL_NUM_FILES\n";
	
	
	public AnalysisHistory(String scrapDir, String saveToFileName){	
		records = new HashMap<String,DateRecord>();
		scrapeArchive(scrapDir, saveToFileName);
	}
	
	public void scrapeArchive(String scrapDir, String saveToFileName){
		List<LineBean> lbList = TCGAHelper
				.getPageBeans(scrapDir);
		
		StringBuilder sb = new StringBuilder();
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(saveToFileName, true));
			
			output.write(headerStr);
			String datStr = null;
			List<String[]>changeDCC = null;
			for(LineBean lb:lbList){
				if (lb.getUrl().endsWith("/")){
					datStr = lb.getModifiedDate();
					changeDCC = TCGAChangeTracker.revisionOfDataFilesOnly(TCGAHelper.changeDCC(lb.getFullURL()
							+ MySettings.CHANGES));
					
					for(String[] s:changeDCC)
						addRecord(datStr, s[1], s[0]);
					// add total number of files
					DateRecord cur = records.get(datStr);
					if(cur != null)
						cur.setNumFiles(TCGAHelper.countDataFiles(lb.getFullURL()));
				}
			}
	
			for (Map.Entry<String, DateRecord> entry : records.entrySet()) {
				String out = entry.getValue().integratedString();
		//System.out.println("out: "+out);
				output.write(out);
			}
			
			
			
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
	}
	
	public void addRecord(String date, String status, String fileName){
		DateRecord cur = records.get(date);
		if(cur == null){
			cur = new DateRecord(date);
			records.put(date, cur);
		}
		cur.setFileStatus(status, fileName);
	}
	
	
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(String key:records.keySet())
			sb.append(records.get(key).toString());
		
		return sb.toString();
	}
	
	public String integratedString(){
		StringBuilder sb = new StringBuilder();
		for(String key:records.keySet())
			sb.append(records.get(key).integratedString());
		
		return sb.toString();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*String dir = "brca/bcr/nationwidechildrens.org/bio/clin/";
		String saveTo = "brca_bcr_nationwidechildrens.org_bio_clin_history";*/
		/*String dir = "gbm/bcr/nationwidechildrens.org/bio/clin/";
		String saveTo = "gbm_bcr_nationwidechildrens.org_bio_clin_history";*/
		/*String dir = "ov/bcr/nationwidechildrens.org/bio/clin/";
		String saveTo = "ov_bcr_nationwidechildrens.org_bio_clin_history";*/
		//----
		// PROTECTED
		//----
		/*String dir = "brca/gsc/ucsc.edu/illuminaga_dnaseq_cont/mutations_protected/";
		String saveTo = "brca_gsc_ucsc.edu_illuminaga_dnaseq_cont_mutations_protected_history";*/
		
		String dir = "ov/cgcc/bcgsc.ca/illuminahiseq_rnaseq/rnaseq/";
		String saveTo = "ov_sgcc_bcgsc.ca_illuminahiseq_rnaseq_rnaseq_history";
		
		
		AnalysisHistory ah = new AnalysisHistory(MySettings.getRoot("public")+dir,
				"C:/tcga/roadmap/scrapHistory/"+saveTo+".tsv");
		
		System.out.println("Done");
	}

}
