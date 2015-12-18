package edu.pitt.tcga.httpclient.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

import edu.pitt.tcga.httpclient.exception.QueryException;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.util.QueryHelper;
import edu.pitt.tcga.httpclient.util.MySettings;

public class VirtuosoStorage extends Storage {
	
	private static String CONF_FILENAME = "virt_queries.conf";
	
	private String VIRT_HOST = null, VIRT_PORT = null;
	private String VIRT_USER = null, VIRT_PWD = null;

	private static String[] qHeaderTypes = {"Accept"};
	private static String[] qHeaderValues = {"application/sparql-results+json"};
	
	private static String[] updHeaderTypes = {"Content-Type"};
	private static String[] updHeaderValues = {"application/sparql-query"};
	
	// query and update urls
	private String VIRT_SPARQL_URL = null, VIRT_UPDATE_URL = null;
	
	private static int SLEEP = 5;
	
	private String dateTimeSuff = "^^<http://www.w3.org/2001/XMLSchema#dateTime>";
	
    private static final Storage INSTANCE = new VirtuosoStorage();
    
    private static List<String> graphList = Arrays.asList("<http://purl.org/pgrr/core#center-links>", "<http://purl.org/pgrr/core#dataType-links>",
    		"<http://purl.org/pgrr/core#diseaseStudy-links>", "<http://purl.org/pgrr/core#pgrr-meta>",
    		"<http://purl.org/pgrr/core#sampleType-links>", "<http://purl.org/pgrr/core#tss-links>", "<http://purl.org/pgrr/core#tss-synonyms>",
    		"<http://purl.org/pgrr/core#diseaseDataType-pairs>", "<http://purl.org/pgrr/core#protocol>", "<http://purl.org/pgrr/core#subscription>",
    		"<http://purl.org/pgrr/core#tcga-bc-uuid>","<http://purl.org/pgrr/core#tcga-archive>","<http://purl.org/pgrr/core#tcga-file>");
  

    //to prevent creating another instance of Singleton
    private VirtuosoStorage(){
    	if(MySettings.getStrProperty("storage.name").equalsIgnoreCase("virtuoso")){
	    	try{
	    		
	    
	    	load(CONF_FILENAME);
	    	// set parameters
	    	VIRT_HOST = MySettings.getStrProperty("virt.host");
	    	VIRT_PORT = MySettings.getStrProperty("virt.port");
	
	    	VIRT_USER = MySettings.getStrProperty("virt.user");
	    	VIRT_PWD = MySettings.getStrProperty("virt.pwd");
	    	
	    	VIRT_SPARQL_URL = "http://" + VIRT_HOST+":" + VIRT_PORT+"/" + MySettings.getStrProperty("virt.sparql.endpoint");
	    	VIRT_UPDATE_URL = "http://" + VIRT_USER + 
	    			":" + VIRT_PWD + "@" + VIRT_HOST+":"+ VIRT_PORT+"/" + MySettings.getStrProperty("virt.update.endpoint");
	    	checkStorageExists();
	    	} catch (Exception e) {}
    	}
    }
    	

    public static Storage getInstace(){
        return INSTANCE;
    }
    
    
    @Override
    public void checkStorageExists(){
    	if(!CHECKED_STORAGE_EXISTS){
    		List<String[]> gList = resultAsList("SELECT ?g WHERE { GRAPH ?g { ?s ?p ?o }} GROUP BY ?g", new String[] {"g"});
    		boolean found = false;
    		for(String[] s: gList){
    			if(s[0].indexOf("http://purl.org/pgrr/core#pgrr-meta") != -1){
    				found = true;
    				break;
    			}
    		}
    		if(!found){
    			try {
					initStorage();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.err.print("*************************");
					System.err.print("CAN'T initialize graphs in Virtuoso. Check your Virtuoso settings in "+
					System.getProperty("user.dir")+"/resources/tcgaexplorer.conf files");
					System.err.print("*************************");
					System.exit(0);
				}
    		}
    		CHECKED_STORAGE_EXISTS = true;
    	}
    }
    
    private void initStorage() throws Exception{
    	//1. create graphs
    	String pref = "CREATE GRAPH ";
    	String q = "";
    	for(String gName:graphList){
    		q = pref+gName;
    		update(q);
    		try {
    			q = URLEncoder.encode(q, "UTF-8");
    		} catch (UnsupportedEncodingException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			throw e;
    		}	
    	}
    	//2.  load pre-defined NQs from files
    	insertNQFromFile(System.getProperty("user.dir")+"/setup/virt_dataType-links.nq");
    	insertNQFromFile(System.getProperty("user.dir")+"/setup/virt_tss-synonyms.nq");
    	
    }
    
    @Override
	public  String[] splitVQString(String nqStr){
    	if(nqStr.endsWith(" ."))
    		nqStr = nqStr.substring(0, nqStr.lastIndexOf(" ."));
		return nqStr.split(" ");
	}
    
    @Override
	public void uploadNQFromDir(String uploadDir){
    	File folder = new File(uploadDir);
		  File[] listOfFiles = folder.listFiles(); 
		  int sz = listOfFiles.length;
		  for (int i = 0; i< sz; i++) 
		  {
			   if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".nq")) {
				   
				   insertNQFromFile(listOfFiles[i].getAbsolutePath());
				   moveFileToHistory(listOfFiles[i].getName());
			   }
		  }
	}
    
    public static void insertNQFromFile(String fileName){
        try {
            
            //virtuoso gena
        	VirtGraph set = new VirtGraph ("jdbc:virtuoso://"+MySettings.getStrProperty("virt.host")+":1111",
        			MySettings.getStrProperty("virt.user"),MySettings.getStrProperty("virt.pwd"));
        	BufferedReader br = null;
        	try{
        	br = new BufferedReader(new FileReader(fileName));
        	String  sCurrentLine = null;
        	StringBuilder sb = new StringBuilder();
        	int pos = -1;
			while ((sCurrentLine = br.readLine()) != null) {
				sb = new StringBuilder("INSERT INTO GRAPH ");
				//have to convert nq into triples
				pos = sCurrentLine.lastIndexOf("<");
				sb.append(sCurrentLine.substring(pos, sCurrentLine.lastIndexOf(">"+1)));
				sb.append(" { ");
				sb.append(sCurrentLine.substring(0,pos));
				sb.append(" . }");
		//System.out.println("s: "+sb.toString());

				VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(sb.toString(),set);
				vur.exec();
			}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    @Override
    public JSONObject getJSONResult(String query)
			throws QueryException {
		
		JSONObject json = null;

		try {	
			String respStr = getPostResponse(buildQueryURL(VIRT_SPARQL_URL, query), qHeaderTypes, 
					qHeaderValues, null);

				json = new JSONObject(respStr);
			return json;

		} catch (IOException e) {
			e.printStackTrace();
			throw new QueryException("QueryException: in VirtuosoHelper.getJSONResult: "+e);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new QueryException("QueryException: in VirtuosoHelper.getJSONResult: "+e);
		}

	}
    
    @Override
    public  String fieldValueByUUID(String uuid, String field){
		String toret = null;
		try {
			String q = getStrProperty("VALUE_BY_UUID_Q").replace("<s>", uuid);
			
			q = q.replaceAll("<field>",field);

			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(q));	
			JSONObject jsonBin = new JSONObject(bindings.getString(0));
			toret = new  JSONObject(jsonBin.getString("value")).getString("value");
			
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
    public List<String[]> resultAsList(String query, String[] fieldNames){
    	List<String[]> toret = new ArrayList<String[]>();
		try{
			
			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(query));			
			
			int len = fieldNames.length;
			if(bindings == null) return toret;
			
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject jsonBin = new JSONObject(bindings.getString(i));
				String[] sArr = new String[len];
				for (int k = 0; k < len; k++){
					try{
						sArr[k] = new JSONObject(jsonBin.getString(fieldNames[k])).getString("value");
					} catch (JSONException e){
						sArr[k] = "";
					}
				}
				toret.add(sArr);
			}
		} catch (JSONException e) {
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toret;
	}
    
    @Override
    public  List<String> resultAsStrList(String query, String fieldName){
    	List<String> toret = new ArrayList<String>();
    	try {
			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(query));		
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject jsonBin = new JSONObject(bindings.getString(i));
				toret.add(new JSONObject(jsonBin.getString(fieldName)).getString("value"));
				
			}
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
    public void modify(String oldS, String oldP, String oldO, String newS, String newP, 
			String newO, String graph)  throws QueryException{
    	try {
			delete(oldS, oldP, oldO, graph);
			//Thread.sleep(SLEEP);
			insert(newS, newP, newO, graph);
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
    }
    
    @Override
    public void update(String statement){
		try{
			getPostResponse(VIRT_UPDATE_URL, updHeaderTypes,updHeaderValues,
					statement);

			ErrorLog.log(statement, getUpdateLogFile());	// write to file
			Thread.sleep(SLEEP);
		} catch (InterruptedException e) {
			e.printStackTrace();
		
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    @Override
    public void delete(String s, String p, String o, String graph) throws QueryException{
    	upsert(s,p,o,graph,true);
    }
    
    public void insert(String s, String p, String o, String graph) throws QueryException{
    	upsert(s,p,o,graph,false);
    }
    
    
    private void upsert(String s, String p, String o, String graph, boolean doDelete) throws QueryException{
		StringBuilder sb = new StringBuilder();
		if(doDelete)
			sb.append("DELETE DATA FROM ");
		else sb.append("INSERT IN GRAPH ");
		
		sb.append(graph+" { ");
		sb.append(s+" ");
		sb.append(p+" ");
		sb.append(o+" .}");
		
		update(sb.toString());
		
	}
    
    
    
    private String getPostResponse(String qUrl, String[] hTypes, 
			String[] hValues, String body)throws QueryException {

		HttpClient httpclient = new DefaultHttpClient();
		
		HttpPost httppost = new HttpPost(qUrl);
		for(int i=0; i<hTypes.length; i++){
			httppost.setHeader(hTypes[i], hValues[i]);
		}
		
		try {
			if(body != null)
				httppost.setEntity(new StringEntity(body));
			
			HttpResponse response = httpclient.execute(httppost);
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED) {
				System.out.println("statusCode: "+statusCode);
				throw new QueryException("QueryException: in VirtuosoHelper.getPostResponse: HTTP Status Code: "+statusCode);
			}
			
			if (statusCode == HttpStatus.SC_OK) {
				String toret = EntityUtils.toString(response
					.getEntity());
			
				return toret;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			ErrorLog.log(e.getMessage());
			throw new QueryException("QueryException: in VirtuosoHelper.getPostResponse: " + e.getMessage());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			ErrorLog.log(e.getMessage());
			throw new QueryException("QueryException: in VirtuosoHelper.getPostResponse: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			ErrorLog.log(e.getMessage());
			throw new QueryException("QueryException: in VirtuosoHelper.getPostResponse: " + e.getMessage());
		}finally {   /// this was the problem!!!
			httpclient.getConnectionManager().shutdown();
		}	
		
		return null;
	}
	
    
    private  String buildQueryURL(String SPARQL_URL, String query) throws UnsupportedEncodingException{
		StringBuilder sb = new StringBuilder(SPARQL_URL+"?query=");
		try {
			sb.append(URLEncoder.encode(query, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		return sb.toString();
	}


	@Override
	public String getRdfsPredicate() {
		return "<http://www.w3.org/2000/01/rdf-schema#label>";
	}
    
	@Override
	public String getRdfPredicate() {
		return "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
	}
	
	
	@Override
	public String formatNowMetaFile() {
		return formatNowNoTZ().replace(" ", "T")+"Z" ;
	}
	
	
	@Override
	public String formatTimeInStorage(String dateTime) {
		return  dateTime+dateTimeSuff;
	}


	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String nameWithPrefixUUID(String prefix, String value){
		if(!value.startsWith(prefix))
			return "<"+prefix+value+">";
		else
			return "<"+value+">";
	}


	@Override
	public String nameWithPrefixPorG(String prefix, String value) {
		if(!value.startsWith(prefix))
			return "<"+prefix+value+">";
		else
			return "<"+value+">";
	}


	@Override
	public String literal(String value) {
		if(!value.startsWith("\"") && !value.endsWith("\""))
			return "\""+value+"\"";
		else return value;
			
	}
	
	@Override
	public String formatPorG(String value) {
		return value;
	}
	
	@Override
	public Map<String, String> getMap(String query, String keyName, String valName){
		Map<String, String> map = new  HashMap<String, String>();
		try{
			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(query));			
			
			
			if(bindings == null) return map;
			String id = null;
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject jsonBin = new JSONObject(bindings.getString(i));
				//id = new JSONObject(jsonBin.getString("keyName")).getString("value");
				
				map.put(new JSONObject(jsonBin.getString(keyName)).getString("value"),
						new JSONObject(jsonBin.getString(valName)).getString("value"));
			}
		} catch (JSONException e) {
		} catch (QueryException e) {
			e.printStackTrace();
		}
		return map;
	}
	
	@Override
	public Map<String, Map<String, String>> getMapOfMap(String query, String topKey, String innerKey, String innerValue){
		Map<String, Map<String, String>> topMap = new  HashMap<String, Map<String, String>>();
		try{
			JSONArray bindings =  QueryHelper.getBindings(getJSONResult(query));			
			if(bindings == null) return topMap;
			String id = null;
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject jsonBin = new JSONObject(bindings.getString(i));
				Map<String, String> innerMap = new  HashMap<String,String>();
				innerMap.put(new JSONObject(jsonBin.getString(innerKey)).getString("value"),
						new JSONObject(jsonBin.getString(innerValue)).getString("value"));
				topMap.put(new JSONObject(jsonBin.getString(topKey)).getString("value"), innerMap);
			}
		} catch (JSONException e) {
		} catch (QueryException e) {
			e.printStackTrace();
		}
		return topMap;
	}
	
	
	public static void main(String[] args){
		Storage s = VirtuosoStorage.getInstace();
		//s.getClinBarcodeUUID();
		System.out.println("VIRT formatNowMetaFile: "+s.formatNowMetaFile());
		
	}
	
	
}
