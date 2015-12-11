package edu.pitt.tcga.httpclient.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;

import com.google.gson.Gson;

import edu.pitt.tcga.httpclient.exception.QueryException;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.storage.upload.DBUpload;
import edu.pitt.tcga.httpclient.transfer.Transfer;
import edu.pitt.tcga.httpclient.util.QueryHelper;
import edu.pitt.tcga.httpclient.util.MySettings;

public class PostgreStorage extends Storage {
	
	private static final String UPSERT = "UPDATE <graphName> SET <col_name>='<col_val>' WHERE uuid='<uuid_val>'; "+
			"INSERT INTO <graphName> (uuid, <col_name>) "+
			"SELECT '<uuid_val>', '<col_val>' "+
			"WHERE NOT EXISTS (SELECT 1 FROM <graphName> WHERE uuid='<uuid_val>');";
	private static final String DELETE = "UPDATE <graphName> SET <col_name>=NULL WHERE uuid='<uuid_val>';";
	
	private static String CONF_FILENAME = "posrtgres_queries.conf";
	private String DRIVER = null, URL = null, USER = null, PASS = null;
	
	private Connection conn = null;
	
	private static final Storage INSTANCE = new PostgreStorage();
	
	public static Storage getInstace(){
        return INSTANCE;
    }
	
	private PostgreStorage(){
		
		load(CONF_FILENAME);
		DRIVER 	= MySettings.getStrProperty("db.driver");
		URL 	= MySettings.getStrProperty("db.url");
		if(!URL.endsWith("/"))
			URL = URL+"/";
		URL = URL+"pgrr";
		USER 	= MySettings.getStrProperty("db.user");
		PASS 	= MySettings.getStrProperty("db.pwd");
		
		checkStorageExists();
		
	}

	private Connection getConnection() throws Exception {
		try {
			if (conn == null || conn.isClosed()) {
				try {
					Class.forName(DRIVER).newInstance();
					conn = DriverManager.getConnection(URL, USER, PASS);
				} catch (org.postgresql.util.PSQLException e) {
					String mess = e.getMessage();
					if(mess.startsWith("FATAL: database") && mess.endsWith("does not exist"))
						System.err.println("NO DBL "+mess);
					throw new Exception("PostgreStorage getConnection: "+e.getMessage());
				} 
			}
		} catch (SQLException e) {
			ErrorLog.log("PostgreStorage getConnection SQLException: "+e.getMessage());
			throw new Exception("PostgreStorage getConnection: "+e.getMessage());
		}
		return conn;
	}
	
	@Override
	public void disconnect(){
		if(conn == null)
			return;
		try {
			if (conn != null || !conn.isClosed())
				conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ErrorLog.log("PostgreStorage disconnect: "+e.getMessage());
		} catch (NullPointerException ex){
			ex.printStackTrace();
			ErrorLog.log("PostgreStorage disconnect: "+ex.getMessage());
		}
	}
	
	@Override
	public void checkStorageExists(){
    	if(!CHECKED_STORAGE_EXISTS){
    		String dbName = URL.substring(URL.lastIndexOf("/")+1, URL.length()).toLowerCase();
    		String postqrULR = URL.replace(dbName, "postgres");
    		Connection postgrConn = null;
    		try {
    			
    			postgrConn = DriverManager.getConnection(postqrULR, USER, PASS);
    			Statement  stmt = postgrConn.createStatement();
    			ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_catalog.pg_database WHERE lower(datname) = lower('pgrr');");
    			boolean found = rs.next();
    	
    			rs.close();
    			stmt.close();
			
    		if(!found){
    			int dInd = URL.indexOf("//")+2;
			    int qInd = URL.indexOf(":", dInd);
			    String host = URL.substring(dInd, qInd);
			    String port = URL.substring(qInd+1, URL.indexOf("/", qInd));
			    
			    String pgpassPath = System.getProperty("user.home")+"/.pgpass";
    			//make sure: host:5432:*:username:mypass in ~/.ppass
    			addAbsentLine(host+":"+port+":*:"+USER+":"+PASS, new File(pgpassPath));
    			// change its mod to chmod 600 ~/.pgpass
    			Transfer.execBash("chmod 600 "+pgpassPath, false);
    			
    			//replace "<your_role> with username in postgrSchema.dump
    			String dumpFPath = System.getProperty("user.dir")+"/setup/postgrSchema.dump";
    			String comm2 = "sed -i 's/<your_role>/"+USER+"/g' "+dumpFPath;
    			Transfer.execBash(comm2, false);
    			
    			System.out.println("INITIALIZING POSTGRES...");
    			
    			// create db
    			Statement  stmt2 = postgrConn.createStatement();
			    stmt2 .executeUpdate("CREATE DATABASE "+dbName);
			    stmt2.close();
    			
    			// now dump it!
			    String comm3 = "psql -h "+host+" -p "+port+" "+dbName+" -U "+USER+" -f "+dumpFPath;
    			Transfer.execBash(comm3, false);

    			
			    
				postgrConn.close(); 
        	}
    		} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		 		
    		CHECKED_STORAGE_EXISTS = true;
    	}
    	
	}
	
	private void addAbsentLine(String toAdd, File f){
		BufferedReader br = null;
		PrintWriter writer = null;
		boolean found = false;	 
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(f));
 
			while ((sCurrentLine = br.readLine()) != null) {
				if(sCurrentLine.equals(toAdd)){
					found = true;
					break;
				}
			}
			
			if(!found){
				writer = new PrintWriter(new FileWriter(f, true));
				writer.println(toAdd);
				writer.close();
				writer = null;
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
					br = null;
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	@Override
	public void uploadNQFromDir(String uploadDir){
		DBUpload dbUpload = new DBUpload();
		dbUpload.uploadNQFromDir(uploadDir);
	}
	
	
	
	@Override
	public void update(String statement){
		try {
			Connection conn = getConnection();
			PreparedStatement st = conn
					.prepareStatement(statement);
			st.executeUpdate();
			st.close();
		} catch (Exception ex) {
			//ex.printStackTrace();
			if(ex.getMessage().indexOf("A result was returned when none was expected.") == -1)
				ErrorLog.log("PostgreStatement update: "+ex.toString());

		}	
	}
	
	
	private String toJSON(ResultSet rs) throws SQLException, IOException {
		if(rs == null) return null;
		
	    ResultSetMetaData rsmd = rs.getMetaData();
	    int count = rsmd.getColumnCount();
	    ArrayList<String> record = new ArrayList<String>();
	    ArrayList<ArrayList<String>> recordSet = new ArrayList<ArrayList<String>>();
	    //adding column headers
	    for(int idx=1; idx<=count; idx++) {
	         record.add(rsmd.getColumnLabel(idx)); // write key:value pairs
	    }
	    recordSet.add(record);
	    while(rs.next()) {
	       // loop rs.getResultSetMetadata columns
	       for(int idx=1; idx<=count; idx++) {
	         record.add(rs.getString(idx));
	       }
	       recordSet.add(record);
	    }
	    rs.close();
		
		
	    
	    Gson gson = new Gson();
	    String jsonStr = gson.toJson(recordSet);
	    
	    record.clear();
	    recordSet.clear();
	    record = null;
	    recordSet = null;

	    return jsonStr;
		
	}
	
	/////////////////// QUERIES  /////////////////
	
	@Override
	public String fieldValueByUUID(String uuid, String field) {
		field = formatPorG(field);
		String toret = null;
		try {
			String q = getStrProperty("VALUE_BY_UUID_Q").replace("<s>", uuid);
			
			q = q.replaceAll("<field>",field);

			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(q));
			JSONObject jsonBin = new JSONObject(bindings.getString(0));
			toret = jsonBin.getString("value");
			
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toret;
	}

	@Override
	public String getRdfsPredicate() {
		return "label";
	}

	@Override
	public String getRdfPredicate() {
		return "";
	}

	@Override
	public String formatNowMetaFile() {
		return formatNowNoTZ().replace(" ", "T")+"Z";
	}

	@Override
	public String formatTimeInStorage(String dateTime) {
		String localObj = dateTime;
		int dateTimeInd = localObj.indexOf("\"^^<http://www.w3.org/2001/XMLSchema#dateTime>");
		if(dateTimeInd > -1)
			localObj = localObj.substring(0, dateTimeInd);
		
		localObj = dateTime.replace("T", " ");
		return localObj.replace("Z","");
	}
	
	@Override
	public  String[] splitVQString(String nq){
		String tabStr = DBUpload.nqRecordToString(nq);
		return tabStr.split("\t");
	}


	@Override
	public JSONObject getJSONResult(String query) throws QueryException {
		JSONObject jsonObj = null;	
		List<Map<String, Object>> listOfMaps = null;
        try {
            QueryRunner queryRunner = new QueryRunner();
            listOfMaps = queryRunner.query(getConnection(), query, new MapListHandler());
            String jsonStr = "{\"results\": { \"bindings\":"+new Gson().toJson(listOfMaps)+"}}";
            listOfMaps.clear();
            listOfMaps = null;
            jsonObj = new JSONObject(jsonStr);
        } catch (SQLException se) {
            throw new QueryException("PostgreStorage getJSONResult: Couldn't query the database."+ se.getMessage());
		} catch (JSONException e) {
			throw new QueryException("PostgreStorage getJSONResult: "+e.getMessage());
		}
		 catch (Exception e) {
    		throw new QueryException("PostgreStorage getJSONResult: "+e.getMessage());
		}
        
        return jsonObj; 
	}
	
	@Override 
	public Map<String, String> getClinBarcodeUUID() {
    	try{
			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(getStrProperty("ORIG_CLIN_BARCODE_UUID_Q")));			
			Map<String, String> map = new  HashMap<String, String>();
			
			if(bindings == null) return map;

			for (int i = 0; i < bindings.length(); i++) {
				JSONObject jsonBin = new JSONObject(bindings.getString(i));
				map.put(jsonBin.getString("barcode"),jsonBin.getString("origuuid"));		
			}
			return map;
		} catch (JSONException e) {
			return null;
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String nameWithPrefixUUID(String prefix, String value) {
		if(value.startsWith(prefix))
			value = value.substring(prefix.length());
		return value;
	}



	@Override
	public String nameWithPrefixPorG(String prefix, String value) {
		if(value.startsWith(prefix))
			value = value.substring(prefix.length());
		return formatPorG(value);
	}

	

	@Override
	public String literal(String value) {
		value = value.replaceAll("'", "''");
		value = value.replaceAll("\\\\", "\\\\\\\\");
		return value;
	}
	
	@Override
	public String formatPorG(String value) {
		return value.toLowerCase().replaceAll("-", "_");
	}
	
	@Override
	public List<String[]> resultAsList(String query, String[] fieldNames){
		try{
			List<String[]> toret = new ArrayList<String[]>();
			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(query));			
			
			int len = fieldNames.length;
			if(bindings == null) return toret;
			
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject jsonBin = new JSONObject(bindings.getString(i));
				String[] sArr = new String[len];
				for (int k = 0; k < len; k++){
					try{
						sArr[k] = jsonBin.getString(formatPorG(fieldNames[k]));
					} catch (JSONException e){
						sArr[k] = "";
					}
				}
				toret.add(sArr);
			}
			return toret;
		} catch (JSONException e) {
			return null;
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public List<String> resultAsStrList(String query, String fieldName){
		fieldName = formatPorG(fieldName);
		List<String> toret = new ArrayList<String>();
		try {
			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(query));		
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject jsonBin = new JSONObject(bindings.getString(i));
				toret.add(jsonBin.getString(fieldName));
			}
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e){
			//e.printStackTrace();
		}
		return toret;
	}
	
	private void upsert(String subj, String p, String o, String g){
		String ups = UPSERT.replaceAll("<graphName>", g);
		ups = ups.replaceAll("<col_name>", p);
		ups = ups.replaceAll("<col_val>", o);
		ups = ups.replaceAll("<uuid_val>", subj);
		
		update(ups);
			
	}

	@Override
	public void modify(String oldS, String oldP, String oldO, String newS,
			String newP, String newO, String graph) throws QueryException {
		delete(oldS, oldP, oldO, graph);
		upsert(newS, newP, newO, graph);
		
	}

	@Override
	public void delete(String s, String p, String o, String graph)
			throws QueryException {
		String ups = DELETE.replaceAll("<graphName>", graph);
		ups = ups.replaceAll("<col_name>", p);
		ups = ups.replaceAll("<uuid_val>", s);
		update(ups);
	}

	@Override
	public void insert(String s, String p, String o, String graph)
			throws QueryException {
		upsert(s,p,o,graph);
		
	}
	
	@Override
	public Map<String, String> getMap(String query, String keyName, String valName){
		keyName = formatPorG(keyName);
		valName = formatPorG(valName);
		try{
			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(query));
			Map<String, String> map = new  HashMap<String, String>();	
			if(bindings == null) return map;
			String id = null;
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject jsonBin = new JSONObject(bindings.getString(i));	
				try{
					if(jsonBin.getString(keyName) != null & jsonBin.getString(valName) != null){
						map.put(jsonBin.getString(keyName),	jsonBin.getString(valName));
					}
				}catch (JSONException e) {}
			}
			return map;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args){
		//Storage s = PostgreStorage.getInstace();
		
		
		//System.out.println("valu *"+s.nameWithPrefixUUID("http://purl.org/pgrr/core#", "http://purl.org/pgrr/core#1234567")+"*");
		/*
		Map<String, String> postMap = s.getClinBarcodeUUID();
		for(Map.Entry<String,String> entry:postMap.entrySet())
	System.out.println("Key = "+entry.getKey()+"  VAL = "+entry.getValue());*/
		
		/*String q = "select patientbarcode, patientuuid, analysistype from pgrr_meta limit 1";
		try {
			s.getJSONResult(q);
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		System.out.println("done PSStorage");
	}

}
