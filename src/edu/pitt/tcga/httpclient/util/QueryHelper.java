package edu.pitt.tcga.httpclient.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.pitt.tcga.httpclient.exception.QueryException;
import edu.pitt.tcga.httpclient.log.ErrorLog;

public class QueryHelper {


	// query strings
	public static String KNOWN_ENTITIES = null;
	public static String DISEASE_FILES_PRE = "query=prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "prefix tcga:<"+MySettings.TCGA_PRE+">\n"
			+ "select distinct ?url ?id where {?id tcga:url ?url . ?id tcga:diseaseStudy ?d . ?d rdfs:label \"";
	public static String DISEASE_FILES_SUF = "\" . }";
	
	public static String[] qHeaderTypes = {"Content-Type","Accept"};
	public static String[] qHeaderValues = {"application/x-www-form-urlencoded", "application/sparql-results+json"};
	

	public static JSONArray getBindings(JSONObject res){
		JSONObject jsonRes;
		try {
			jsonRes = new JSONObject(res.getString("results"));
			JSONArray bindings = jsonRes.getJSONArray("bindings");
			if (bindings.length() == 0) return null;

			return bindings;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ErrorLog.logFatal("QueryHelper getBindings: "+e.toString());
			return null;
		}
		
	}
	/*
	public static List<String> getCodes(String graph, String field) throws QueryException{
		String query = "query=prefix tcga:<"+MySettings.TCGA_PRE+">" + "\n"+
				"select distinct ?code from tcga:"+graph+" where { ?s tcga:"+field+" ?code .}";
		
		//test q:
		//select distinct ?s ?p ?o  from tcga:linkedct-links where { ?s ?p ?o .}
		// list graphs:
		// select distinct ?g  where { graph ?g { ?s ?p ?o .}}
		
		List<String> res = new ArrayList<String>();
		try {
		HttpResponse response = TCGAHelper.getPostResponse(MySettings.getSparqlURL(), qHeaderTypes, 
				qHeaderValues, query);
		int statusCode = response.getStatusLine().getStatusCode();
		
		if (statusCode == HttpStatus.SC_NO_CONTENT) {
			System.out.println("Found no known entities.");
			throw new QueryException("QueryException: HTTP Status Code: "+statusCode);
		}

		if (statusCode != HttpStatus.SC_OK) {
			System.out.println("Unable to query SPARQL endpoint");
			throw new QueryException("QueryException: HTTP Status Code: "+statusCode);
		}
		
		
		if (statusCode == HttpStatus.SC_OK) {
			JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
			
			//System.out.println("json: " + json);
			JSONObject jsonRes = new JSONObject(json.getString("results"));
			JSONArray bindings = jsonRes.getJSONArray("bindings");

			// Build the return string.
			for (int i = 0; i < bindings.length(); i++) {
				//System.out.println("EACH: " + bindings.getString(i));
				JSONObject jsonBin = new JSONObject(bindings.getString(i));
				res.add(new JSONObject(jsonBin.getString(field)).getString("value"));
			}
		}
		return res;
			
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new QueryException("QueryException: ParseException");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new QueryException("QueryException: JSONException");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new QueryException("QueryException: IOException");
			}
		
	}
	*/
	
	/**
	 * returns map <url,id>
	 * @param SPARQL_URL
	 * @param query
	 * @return
	 * @throws QueryException
	 */

	public static Map<String, String> getJSONResult(String SPARQL_URL, String query)
			throws QueryException {

		Map<String,String> map = new HashMap<String,String>();

		try {
			
			HttpResponse response = TCGAHelper.getPostResponse(SPARQL_URL, qHeaderTypes, 
					qHeaderValues, query);
//System.out.println("Q: "+query);


			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == HttpStatus.SC_NO_CONTENT) {
				System.out.println("Found no known entities.");
				throw new QueryException("QueryException: HTTP Status Code: "+statusCode);
			}

			if (statusCode != HttpStatus.SC_OK) {
				System.out.println("Unable to query SPARQL endpoint");
				throw new QueryException("QueryException: HTTP Status Code: "+statusCode);
			}

			if (statusCode == HttpStatus.SC_OK) {

				JSONObject json = new JSONObject(EntityUtils.toString(response
						.getEntity()));
				//System.out.println("json: " + json);
				JSONObject jsonRes = new JSONObject(json.getString("results"));
				JSONArray bindings = jsonRes.getJSONArray("bindings");

				// Build the return string.
				for (int i = 0; i < bindings.length(); i++) {
					// returnString += "\n\t" + bindings.getString(i);
					//System.out.println("EACH: " + bindings.getString(i));
					JSONObject jsonBin = new JSONObject(bindings.getString(i));
					map.put(new JSONObject(jsonBin.getString("url")).getString("value"),
							new JSONObject(jsonBin.getString("id")).getString("value"));
					

					// for
					// {"dateF":{"value":"2023-10-17T00:14:26.321Z","type":"literal"}}
					/*
					 * JSONObject jsonURL = new
					 * JSONObject(jsonBin.getString("dateF"));
					 * System.out.println("VAL: "+jsonURL.getString("value"));
					 */

				}
			}

			return map;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new QueryException("QueryException: IOException");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new QueryException("QueryException: JSONException");
		}

	}
	
	public static void parseJSON_Test(String jsonStr){
		try{
		JSONObject json = new JSONObject(jsonStr);
		System.out.println("json: " + json);
		JSONObject jsonRes = new JSONObject(json.getString("results"));
		JSONArray bindings = jsonRes.getJSONArray("bindings");

		// Build the return string.
		for (int i = 0; i < bindings.length(); i++) {
			// returnString += "\n\t" + bindings.getString(i);
			//System.out.println("EACH: " + bindings.getString(i));
			JSONObject jsonBin = new JSONObject(bindings.getString(i));
			//JSONObject jsonURL = new JSONObject(jsonBin.getString("url"));
			System.out.println("url value: "+( new JSONObject(jsonBin.getString("url")).getString("value")));
			
			System.out.println("id value: "+( new JSONObject(jsonBin.getString("id")).getString("value")));
			//list.add(jsonURL.getString("value"));

			// for
			// {"dateF":{"value":"2023-10-17T00:14:26.321Z","type":"literal"}}
			/*
			 * JSONObject jsonURL = new
			 * JSONObject(jsonBin.getString("dateF"));
			 * System.out.println("VAL: "+jsonURL.getString("value"));
			 */
		} 
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static JSONObject getGetAsJSONResult(String url)
			throws QueryException {
		JSONObject jsonObj = null;
		HttpResponse response = null;
		try {
			
			HttpClient httpclient = TCGAHelper.getHttpClient();
			
			HttpGet request = new HttpGet(url);
			response = httpclient.execute(request);


			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == HttpStatus.SC_NO_CONTENT) {
				System.out.println("Found no known entities.");
				throw new QueryException("QueryException: HTTP Status Code: "+statusCode);
			}

			if (statusCode != HttpStatus.SC_OK) {
				System.out.println("Unable to query SPARQL endpoint");
				throw new QueryException("QueryException: HTTP Status Code: "+statusCode);
			}

			if (statusCode == HttpStatus.SC_OK) 
				jsonObj =  new JSONObject(EntityUtils.toString(response.getEntity()));
			

			return jsonObj;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new QueryException("QueryException: IOException");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new QueryException("QueryException: JSONException");
		}

	}

	@Deprecated
	public static String getKnownEntitiesQStr() {
		if (KNOWN_ENTITIES != null)
			return KNOWN_ENTITIES;
		StringBuilder sb = new StringBuilder();
		sb.append("query=prefix tcga:<http://purl.org/tcga/core#>" + "\n");

		sb.append("select distinct ?url ?id where {");
		sb.append("?id tcga:url ?url .");
		sb.append("{ ?id a tcga:DiseaseStudy . }");
		sb.append(" UNION ");
		sb.append("{ ?id a tcga:CenterType . }");
		sb.append(" UNION ");
		sb.append("{ ?id a tcga:CenterDomain . }");
		sb.append(" UNION ");
		sb.append("{ ?id a tcga:Platform . }");
		sb.append(" UNION ");
		sb.append("{ ?id a tcga:DataType . }");
		sb.append(" UNION ");
		sb.append("{ ?id a tcga:Archive . }");
		sb.append("}");

		// sb.append("select ?dateF{ ?file tcga:firstSeen ?dateF .}");

		KNOWN_ENTITIES = sb.toString();

		return KNOWN_ENTITIES;

	}

	public static String getDiseaseFilesQStr(String disease) {
		StringBuilder sb = new StringBuilder(DISEASE_FILES_PRE);
		sb.append(disease);
		sb.append(DISEASE_FILES_SUF);
		return sb.toString();
	}
	
	public static void main(String[] args){
		
		
		//System.out.println(getKnownEntitiesQStr());
		//try {
			//System.out.println(getDiseaseFilesQStr("brca"));
			//getJSONResult("http://agalpha.mathbiol.org/repositories/tcga?query=",getDiseaseFilesQStr("brca"));
			/*System.out.println(getCodes("center-links", "code"));
			List<String[]> report = TCGAHelper.getReport("centerCode");
			for(String[]ss:report)
				System.out.println("REP: "+Arrays.asList(ss));*/
		//} catch (QueryException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//} 
		
		
		/*String tt = "{"+
				 " \"head\" : {"+
				    "\"vars\" : [\"url\", \"id\"]"+
				  "},"+
				  "\"results\" : {"+
				    "\"bindings\" : [ "+
				      "{"+
				        " \"url\":{\"type\":\"literal\",\"value\":\"https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/brca/bcr/nationwidechildrens.org/tissue_images/slide_images/nationwidechildrens.org_BRCA.tissue_images.Level_1.120.2.0/TCGA-E2-A1LH-11A-01-TSA.701ACE4F-60B1-4C21-A6DB-37766BA1E881.svs\"},"+
				         "\"id\":{\"type\":\"uri\",\"value\":\"http://purl.org/tcga/core#8cb34109-2e16-42de-b517-e49605aa1fbf\"}"+
				      "},"+
				      "{"+
				        "\"url\":{\"type\":\"literal\",\"value\":\"https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/brca/bcr/nationwidechildrens.org/tissue_images/slide_images/nationwidechildrens.org_BRCA.tissue_images.Level_1.120.2.0/TCGA-E2-A1IU-11A-06-TSF.96609ca3-27d5-47b7-a3b2-9f658b24a26b.svs\"},"+
				         "\"id\":{\"type\":\"uri\",\"value\":\"http://purl.org/tcga/core#2103183f-3cd0-420c-832d-fc2e4f2f39ed\"}"+
				      "},"+
				      "{"+
				         "\"url\":{\"type\":\"literal\",\"value\":\"https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/brca/bcr/nationwidechildrens.org/tissue_images/slide_images/nationwidechildrens.org_BRCA.tissue_images.Level_1.117.1.0/TCGA-E9-A1N6-11A-03-TSC.7fc99944-9421-4376-b64c-c5f3536a02ff.svs\"},"+
				        " \"id\":{\"type\":\"uri\",\"value\":\"http://purl.org/tcga/core#c1375596-3e4c-4b68-9e73-e572a8420092\"}"+
				      "}"+
				    "]"+
				  "}"+
				"}";
		
		parseJSON_Test(tt);*/
	}

}
