package edu.pitt.tcga.httpclient.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CompareJSON {
	
	private static String stJSON = "{\"results\":{\"bindings\":[";
	private static String endJSON = "],\"ordered\":true,\"distinct\":false},\"head\":{\"vars\":"+
	"[\"Query_Date\",\"File_Name\",\"File_Version\",\"Pitt_Path\",\"File_Creation_Date\",\"File_Archived_Date\","+
			"\"Reason_Archived\"],\"link\":[]}}";
	
	public static void compare(String fshort, String flong, String field, String saveTo){
		
		 try {

			Map<String, JSONObject> mapShort = getBingins(new JSONObject(readFile(fshort)), field);
			Map<String, JSONObject> mapLong = getBingins(new JSONObject(readFile(flong)), field);
			
			Map<String, JSONObject> diff = new HashMap<String, JSONObject>();
			
			String key = null;
			for (Map.Entry<String, JSONObject> entry : mapLong.entrySet()) {
				key = entry.getKey();
				if(mapShort.get(key) == null)
					diff.put(key, mapLong.get(key));
			}
			System.out.println("diff sz = "+diff.size()+" mapShort = "+mapShort.size()+" mapLong = "+mapLong.size());
			StringBuilder sb = new StringBuilder(stJSON);
			for(Map.Entry<String, JSONObject> entry : diff.entrySet()){
				sb.append(entry.getValue());
				sb.append(",");
				//System.out.println("key= "+entry.getKey()+"   v="+entry.getValue());
			}
			sb.setLength(sb.length() - 1);
			sb.append(endJSON);
			PrintWriter writer = new PrintWriter(new FileWriter(saveTo, true));
			writer.print(sb.toString());
			writer.close();
			
			// 
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static String readFile( String file ) throws IOException {
	    BufferedReader reader = new BufferedReader( new FileReader (file));
	    String         line = null;
	    StringBuilder  stringBuilder = new StringBuilder();

	    while( ( line = reader.readLine() ) != null ) {
	        stringBuilder.append( line );
	    }

	    return stringBuilder.toString();
	}
	
	private static Map<String, JSONObject> getBingins(JSONObject jobj, String field){
		Map<String,JSONObject> map = new HashMap<String,JSONObject>();
		JSONObject jsonRes;
		try {
			jsonRes = new JSONObject(jobj.getString("results"));
			JSONArray bindings = jsonRes.getJSONArray("bindings");
			String fVal = null;
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject jsonBin = new JSONObject(bindings.getString(i));
				fVal = new JSONObject(jsonBin.getString(field)).getString("value");
				map.put(fVal, jsonBin);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
		
	}

	public static void main(String[] args) {

		String fshort = System.getProperty("user.home")+"/topRepo/tempo/vcf/thca_old.json";
		String flong = System.getProperty("user.home")+"/topRepo/tempo/vcf/thca_new.json";
		String saveTo = System.getProperty("user.home")+"/topRepo/tempo/vcf/thca_Prot-Mut_2014-03-07.json";
		compare(fshort, flong, "File_Name", saveTo);
		System.out.println("done");

	}

}
