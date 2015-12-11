package edu.pitt.tcga.httpclient.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import edu.pitt.tcga.httpclient.exception.QueryException;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.storage.StorageFactory;
import edu.pitt.tcga.httpclient.util.QueryHelper;
import edu.pitt.tcga.httpclient.util.MySettings;

public class NewData {
	
	private static SimpleDateFormat dformat = new SimpleDateFormat(MySettings.REPORT_DATE_FORMAT);
	
	/**
	 * Save as topDir/<dataType>/<date>/disease/<platform>/<level1,2,3>
	 * @param topDir - top directory
	 * @param dataType - analysisType
	 * @param platform - list of platforms
	 * @param level - list of levels
	 */
	public static void dType_Dis_Platf_Level_Report(String topDir, String dataType, String[] platforms, 
			String[] levels, String beforeCreationDate){
		
		Storage storage = StorageFactory.getStorage();
		String query = storage.getStrProperty("NEWDATA_BY_DIS_DATATYPE_PL_LEV");
		
		String dateStr = MySettings.getDayFormat();
		topDir = topDir+"/"+dataType+"/"+dateStr+"/";
		String curDir = null, q = null, saveToFile = null;
		
		
		List<String> diseases = storage.resultAsStrList(storage.getStrProperty("ALL_DISEASES_ABBR_Q"),"studyabbreviation");
		for(String dis:diseases){
			for(String platform:platforms){
				for(String level:levels){
					
					q = query.replace("<diseaseAbbr>", dis);
					q = q.replace("<analysisType>",dataType);
					q = q.replace("<analysisPlatform>",platform);
					q = q.replace("<level>",level);
					q = q.replace("<snapshotDate>",beforeCreationDate);
					try{

						JSONObject res = storage.getJSONResult(q);
						if(QueryHelper.getBindings(res) != null ){
System.out.println("about to get report for: "+dis+" pl: "+platform+" level: "+level);	
							curDir = topDir+dis+"/"+platform+"/level_"+level+"/";
							File f = new File(curDir);
							f.mkdirs();
							saveToFile = dis+"."+dataType+"."+platform+".Level_"+level+"."+dateStr+".json";
							BufferedWriter writer = new BufferedWriter(new FileWriter(curDir+saveToFile));
							writer.write(res.toString());
							writer.close();
						}
					}
					catch (QueryException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					
					
				}
			}
		}
		platforms = null;
		levels = null;
		diseases.clear(); diseases = null;
	}
	
	/**
	 * for some reason now() doesn't work on web-dev01
	 * example dateTimeNow = TCGAHelper.formatNowNoZ()
	 * @param saveTo
	 */
	public static void saveNewDataReport(String q_template, String saveToDir, String saveToFileName, 
			String dataType, String level, String date){
			
			File f = new File(saveToDir);
			f.mkdirs();
		    
			Storage storage = StorageFactory.getStorage();
			List<String> diseases = storage.resultAsStrList(storage.getStrProperty("ALL_DISEASES_ABBR_Q"),"studyabbreviation");
			//String[] diseases = {"coad", "read", "kich", "blca", "hnsc", "ucec", "cesc","kirc", "paad"};
			//String[] diseases = {"acc", "lihc", "ucs","luad","kirc","kirp","lgg","skcm","thca"};
			
			String saveLoc = null, qLoc = null;
			String q = q_template.replaceAll("<snapshotDate>", date);
			q = q.replaceAll("<dataType>", dataType);
			q = q.replaceAll("<level>", level);
			for(String dis:diseases){
				qLoc = q.replace("<diseaseAbbr>", dis);

	System.out.println("Q: "+qLoc);
				try{
					JSONObject res = storage.getJSONResult(qLoc);
					if(QueryHelper.getBindings(res) != null ){
			System.out.println("about to get report for: "+dis);
						saveLoc = saveToFileName.replace("<diseaseAbbr>", dis);
						BufferedWriter writer = new BufferedWriter(new FileWriter(saveToDir+saveLoc));
						writer.write(res.toString());
						writer.close();
					}
				}
				catch (QueryException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
		} 
	}
	
	public static void newFormat(String topDir, String dataType, String beforeCreationDate){
		//get Platforms
		Storage storage = StorageFactory.getStorage();
		String q = storage.getStrProperty("PLATFORMS_BY_DATATYPE").replace("<analysistype>", dataType);
		
		List<String> platforms = storage.resultAsStrList(q, "analysisplatform");
		
		String[] platformsArr = platforms.toArray(new String[platforms.size()]);
		
				
	//System.out.println("platforms: "+platforms);
		String[] levels = {"1","2","3"};
		dType_Dis_Platf_Level_Report(topDir, dataType, platformsArr, levels, beforeCreationDate);
		
	}
	

	public static void main(String[] args) {
		// create dirs: <YYYY-MM-DD>/<dataType>/<diseaseAbbr>_<dataType><level>.json
		String dateStr = MySettings.getDayFormat();
		/*String saveToDir = System.getProperty("user.home")+"/topRepo/reports/expression_gene/level_1/"+dateStr+"/";
				String saveToFileName = "<diseaseAbbr>_RNASeq_level3_"+dateStr+".json";
		saveNewDataReport(VirtuosoQueryHelper.NEW_DATA_ONLY_Q, saveToDir, saveToFileName, "RNASeq", "1", "2014-03-01");*/
		//saveNewDataReport(saveTo, "RNASeq", "3");
		//saveNewDataReport(saveTo, "Clinical", "2");
		//System.out.println("done with report generation savedTo: "+saveToDir+saveToFileName);
		
		String topDir = MySettings.REPORTS_DIR;
		newFormat(topDir, "Protected_Mutations", "2015-10-01");
		
		/*List<String> diseases = VirtuosoQueryHelper.getAllDiseasesAbbr();
		for(String dis:diseases)
			System.out.println("dis:: "+dis);*/
			
		
		System.out.println("Done with newData");
	}
}
