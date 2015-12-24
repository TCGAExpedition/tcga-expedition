package edu.pitt.tcga.httpclient.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import edu.pitt.tcga.httpclient.exception.QueryException;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.util.MySettings;

public abstract class Storage {
	private  Properties params = null;
	private static SimpleDateFormat df = null ;

	public static boolean UPDATE_IN_REAL_TIME = true;
	public static boolean CHECKED_STORAGE_EXISTS = false;
	
	protected File updateLogFile = null;
	
	public static String formatNowNoTZ(){
	     return getDateTimeFormat().format(new Date()).replace(" ", "T");
	}
	
	public static SimpleDateFormat getDateTimeFormat(){
		if (df == null ){
			df = new SimpleDateFormat(MySettings.TIMESTAMP_MILLI_FORMAT) ;
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
		}
		return df;
	}
	
	public abstract void checkStorageExists();
	
	
	public abstract String getRdfsPredicate();
	public abstract String getRdfPredicate();
	public abstract String nameWithPrefixUUID(String prefix, String value);
	public abstract String nameWithPrefixPorG(String prefix, String value);
	public abstract String literal(String value);
	public abstract String formatPorG(String str); // TEMPO: assuming it's in Virtuoso format initially
	;
	public abstract String formatNowMetaFile(); // preserving the old format for Metadata.tsv
	public abstract String formatTimeInStorage(String dateTime);
	
	public abstract void disconnect();
	
	//UTIL insert/delete
	public abstract void update(String statement);
	public abstract void modify(String oldS, String oldP, String oldO, String newS, String newP, 
			String newO, String graph)  throws QueryException;
	public abstract void delete(String s, String p, String o, String graph) throws QueryException;
	public abstract void insert(String s, String p, String o, String graph) throws QueryException;
	public abstract void uploadNQFromDir(String uploadDir);
	public abstract  String[] splitVQString(String nq);
	
	//UTIL 
	// --RESULTS
	public abstract List<String[]> resultAsList(String query, String[] fieldNames);
	public abstract List<String> resultAsStrList(String query, String fieldName);
	public abstract Map<String, String> getMap(String query, String keyName, String valName);
	public abstract Map<String, Map<String, String>> getMapOfMap(String query, String topKey, String innerKey, String innerValue);
	
		
	// QUERIES
	public abstract String fieldValueByUUID(String uuid, String field);
	
	
	
	public abstract JSONObject getJSONResult(String query) throws QueryException;
	
	
	public  String getStrProperty(String pName){
		try{
		 return params.getProperty(pName).trim();
		} catch (NullPointerException e) {
			String stName = MySettings.getStrProperty("storage.name");
			if(stName.equalsIgnoreCase("virtuoso"))
				stName = "virt";
			String err = " CAN'T find parameter value for '"+pName+"' . Check your "+stName+"_queries.conf settings.";
			System.err.println(err);
			ErrorLog.logFatal(err);
			return null;
		}
	}
	
	/**
	 * Moves successfully uploaded *.nq file  from upload to upload_history directory
	 */
	public void moveFileToHistory(String fName){
		try {
			FileUtils.moveFile(new File(MySettings.NQ_UPLOAD_DIR+fName), new File(MySettings.NQ_UPLOAD_HISTORY_DIR+fName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected File getUpdateLogFile(){
    	if(updateLogFile == null)
    		updateLogFile = 
    			new File(MySettings.NQ_UPLOAD_DIR+MySettings.getDayFormat()+getStrProperty("update.log.file.name"));
    	return updateLogFile;
    }

	
	protected  void load(String confFileName){
		try {
			params = new Properties();
			params.load(new FileInputStream(System.getProperty("user.dir")+
					File.separator+"resources"+File.separator+confFileName));
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	

}
