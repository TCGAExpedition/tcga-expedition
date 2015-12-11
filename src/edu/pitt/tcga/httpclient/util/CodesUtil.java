package edu.pitt.tcga.httpclient.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.storage.StorageFactory;


/**
 * For any analysis, but NOT for clinical 
 * @author Olga Medvedeva
 *
 */

public class CodesUtil {
	public static final String TCGAmappingURL = "https://tcga-data.nci.nih.gov/uuid/uuidws/mapping/json/";
	public static final String TCGAmetadataURL = "https://tcga-data.nci.nih.gov/uuid/uuidws/metadata/json/";
	public static final String BARCODE_STR = "barcode";
	public static final String UUID_STR = "uuid";
	
	
	public static int REST_TCGA_SLEEP = 250; //in milliseconds; only 330 calls are allowed per minute (~ 5 per sec)
	
	public static String mapping(String givenFieldName, String givenFieldValue){
//System.out.println("CodesUtil.mapping givenFieldName = "+givenFieldName+ " givenFieldValue: "+givenFieldValue);
		//check if it's exists
		String exists = null;
		if(givenFieldName.equals(BARCODE_STR)){
			exists = ModuleUtil.origBarcodeUUIDMap.get(givenFieldValue);
			
		} else{
			exists = ModuleUtil.getBarcodeByUUID(givenFieldValue);
		}
		if(exists != null)
			return exists;
		
		String fieldOut = (givenFieldName.equals(BARCODE_STR)) ? UUID_STR:BARCODE_STR;
		HttpClient httpclient = new DefaultHttpClient();
		String res = null;
		InputStream in = null;
		BufferedReader reader = null;
		try{
			//Thread.sleep(REST_TCGA_SLEEP);
			in = TCGAHelper.getGetResponseInputStream(httpclient, TCGAmappingURL+givenFieldName+"/"+givenFieldValue);
			
			if(in == null) return null;
			
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) 
            	sb.append(line+"\n");
        
            String sbStr = sb.toString();
            if(!sbStr.startsWith("{\"validationError\":")){
            	if(givenFieldName.equals(BARCODE_STR))
            		res = new JSONObject(new JSONObject(sbStr).getString("uuidMapping")).getString(fieldOut);	
            	else
            		res = new JSONObject(sbStr).getString(fieldOut);	
            	
            	if(fieldOut.equals(UUID_STR))
            		res = res.toLowerCase();
    // System.out.println("CodesUtil.mapping res = "+res);
            }
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        
	        } 
		/*catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} */
		catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} finally {
	            try {
	            	if(in != null)
	            		in.close();
	            	if(reader != null)
	            		reader.close();
	                httpclient.getConnectionManager().shutdown();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	if(res != null && !res.equals("")){
		if(givenFieldName.equals(BARCODE_STR))
			ModuleUtil.addToOrigBarcodeUUIDMap(givenFieldValue, res);
		 else
			ModuleUtil.addToOrigBarcodeUUIDMap(res, givenFieldValue);
		
	}
		return res;
	}
	
	private static int MAX_ATTEMPTS = 3;
	private static int CURR_ATTEMPT = 0;
	
	public static String uuidFromMetadata(String givenFieldName, String givenFieldValue, String[] returnFieldHier){
		String fieldOut = (givenFieldName.equals(BARCODE_STR)) ? UUID_STR:BARCODE_STR;
		HttpClient httpclient = new DefaultHttpClient();
		String res = null;
		InputStream in = null;
		BufferedReader reader = null;
		try{
			//Thread.sleep(REST_TCGA_SLEEP);
			in = TCGAHelper.getGetResponseInputStream(httpclient, TCGAmetadataURL+givenFieldName+"/"+givenFieldValue);
			
			if(in == null) return null;
			
			reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) 
            	sb.append(line+"\n");
        
            String sbStr = sb.toString();
            if(!sbStr.startsWith("{\"validationError\":")){
            	try{
            	res = new JSONObject(sbStr).getString("tcgaElement");
            	} catch (Exception e){
            		if(CURR_ATTEMPT > MAX_ATTEMPTS){
            			CURR_ATTEMPT++;
            			Thread.sleep(REST_TCGA_SLEEP);
            			return uuidFromMetadata(givenFieldName, givenFieldValue, returnFieldHier);
            		} else{
            			CURR_ATTEMPT = 0;
            			ErrorLog.logFatal("CodesUtil.uuidFromMetadata: "+e.getMessage()+" givenFieldName: "+givenFieldName+" givenFieldValue: "+givenFieldValue);
            			return res;
            		}
            		
            	}
            	for(String rf:returnFieldHier)
            		res = new JSONObject(res).getString(rf);
          	
            	res = res.substring(res.lastIndexOf("/")+1);
            	if(fieldOut.equals(UUID_STR))
            		res = res.toLowerCase();
            	
            }
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        
	        } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
	            try {
	            	if(in != null)
	            		in.close();
	            	if(reader != null)
	            		reader.close();
	                httpclient.getConnectionManager().shutdown();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }

		returnFieldHier = null;
		CURR_ATTEMPT = 0;
		return res;
	}

	// ////////////////////////////
	// / From Barcode
	// ///////////////////////////

	/**
	 * 
	 * @param barcode
	 * @return TCGA-XX-XXXX == first 12 symbols
	 */
	public static String getPatientBarcode(String barcode) {
		return (barcode.length() < 12) ? "" : barcode.substring(0, 12);
	}

	/**
	 * 
	 * @param barcode
	 * @return TCGA-XX-XXXX-XXX == first 16 symbols
	 */
	public static String getFullSampleBarcode(String barcode) {
		return (barcode.length() < 16) ? "" : barcode.substring(0, 16);
	}
	

	/**
	 * 
	 * @param barcode
	 * @return TCGA-XX-XXXX-XX == first 15 symbols - no vial
	 */
	public static String getSampleBarcode(String barcode) {
		String toret = "";
		int len = barcode.length();
		if(len < 15)
			return toret;
		if(len <= 16 ) 
			return barcode.substring(0, 15);
		
		String sampleBarcode = barcode.substring(0, 15);
		//to speed the things up before lookup in metadata, try to find in PGRR first
/*System.out.println(" ***CodesUtil.getSampleBarcode origBarcodeUUIDMap.size "+ModuleUtil.origBarcodeUUIDMap.size());
for (Map.Entry<String, String> entry : ModuleUtil.origBarcodeUUIDMap.entrySet()) {
	System.out.println("key: "+entry.getKey()+"    VAL: "+entry.getValue());
}*/
		if(ModuleUtil.origBarcodeUUIDMap.get(sampleBarcode) != null)
			return sampleBarcode;
		
		//sample barcode might not be the same as analyte/aliquoyt/portion/...
		//so look up the sample field uuid in metadata
		//String sampleUUID = uuidFromMetadata(BARCODE_STR,barcode, new String[] {"sample","@href"});
		
		//ATT: not working from September 4, 2015
		//String sampleUUID = uuidFromMetadata(BARCODE_STR,barcode.substring(0, 16), new String[] {"sample","@href"});
		
		String sampleUUID = uuidFromMetadata(BARCODE_STR,barcode.substring(0, 16), new String[] {"uuid"});

		//even if uuid not found, should return barcode to put it in correct location (otherwise it's going to /disease/patient dir
		if(sampleUUID == null) 
			return barcode.substring(0, 15);
		else{
			ModuleUtil.addToOrigBarcodeUUIDMap(barcode.substring(0, 16), sampleUUID);
			//get barcode through mapping
			toret = mapping(UUID_STR,sampleUUID).substring(0, 15);
		}
		return toret;
	}
	
	
	
	/**
	 * 
	 * @param barcode
	 * @return TCGA-XX-XXXX-XX == first 15 symbols - no vial
	 * Do not look it up
	 */
	public static String justSampleBarcode(String barcode) {
		String toret = "";
		int len = barcode.length();
		if(len < 15)
			return toret;
		else
			return barcode.substring(0, 15);
	}
	
	/**
	 * 
	 * @param barcode
	 * @return TCGA-XX-XXXX-XXX-XX == first 19 symbols - no analyte
	 */
	public static String getPortionBarcode(String barcode) {
		return (barcode.length() < 19) ? "" : barcode.substring(0, 19);
	}
	
	/**
	 * 
	 * @param barcode
	 * @return TCGA-XX-XXXX-XXX-XXX == first 20 symbols 
	 */
	public static String getAnalyteBarcode(String barcode) {
		return (barcode.length() < 20) ? "" : barcode.substring(0, 20);
	}
	
	/**
	 * 
	 * @param barcode
	 * @return TCGA-XX-XXXX-XXX-XXX-XXXX-XX == first 28 symbols 
	 * 
	 */
	public static String getAliquotBarcode(String barcode) {
		return (barcode.length() < 28) ? "" : barcode.substring(0, 28);
	}
	
	
	

	public static String getSampleTypeAbb(String barcode) {
		return (barcode.length() < 15) ? "" : barcode.substring(13, 15);
	}

	public static String getSampleVial(String barcode) {
		int len = barcode.length();
		if(len < 15)
			return "";
		if(len == 16 )
			return barcode.substring(15,16).toUpperCase();
		
		String sampleBarcode = barcode.substring(0, 15);
		//to speed the things up before lookup in metadata, try to find in PGRR first
		if(ModuleUtil.origBarcodeUUIDMap.get(sampleBarcode) != null)
			return barcode.substring(15,16).toUpperCase();
		
		//sample barcode might not be the same as analyte/aliquoyt/portion/...
		//so look up the sample field uuid in metadata
		
		//ATT: not working frpm September 4, 2015
		//String sampleUUID = uuidFromMetadata(BARCODE_STR,barcode.substring(0, 16), new String[] {"sample","@href"});
		String sampleUUID = uuidFromMetadata(BARCODE_STR,barcode.substring(0, 16), new String[] {"uuid"});
		
		if(sampleUUID == null) 
			return barcode.substring(15,16).toUpperCase();
		else
			ModuleUtil.origBarcodeUUIDMap.put(barcode.substring(0, 16), sampleUUID);
		
		//get barcode through mapping
		String toret = mapping(UUID_STR,sampleUUID);
		return toret.substring(15, 16).toUpperCase();
	}

	public static String getSamplePortionCode(String barcode) {
		return (barcode.length() < 19) ? "" : barcode.substring(17, 19);
	}

	public static String getSampleAnalyteAbb(String barcode) {
		String toret = (barcode.length() < 20) ? "" : barcode.substring(19, 20);
		return (toret.equals("-")) ? "" : toret;
	}

	public static String getPlateCode(String barcode) {
		return (barcode.length() < 25) ? "" : barcode.substring(21, 25);
	}

	public static String getCenterAbb(String barcode) {
		return (barcode.length() < 28) ? "" : barcode.substring(26, 28);
	}
	

	// ////////////////////////////
	// / From Code Tables
	// ///////////////////////////

	/**
	 * 
	 * @param barcode
	 *            any barcode (patient or sample) in form: TCGA-<AA>-<...>
	 * @return AA
	 */
	public static String getTSSAbb(String barcode) {
		return (barcode.substring(5, 7)).trim();
	}

	
	/**
	 * use ModuleUtil.tssNamePrimary and ModuleUtil.tssAbbrPrimary to get correct name
	 * @param barcode
	 * @return
	 */
	public static String getTSSName(String barcode) {
		String toret = ModuleUtil.tssAbbrPrimary.get(getTSSAbb(barcode));
		if (toret == null){
			File tssLog = new File(MySettings.NEW_TSS_DIR+MySettings.getDayFormat()+"_NOT_FOUND_Tss.txt");
			ErrorLog.log("No TSS abbr for "+getTSSAbb(barcode),tssLog);
			System.out.println("		***** NO  TSS Abbr found in  RDF for: "+barcode);
			
		}
		
		return toret;
		/*return TCGAHelper.lookUp(TCGAHelper.getCodeTable("tssCodes"),
				new int[] { 0 }, new String[] { getTSSAbb(barcode) },
				new int[] { 1 })[0].trim();*/

	}
	
	
	
	

	public static String getDiseaseNameFromBarcode(String barcode) {
		return TCGAHelper.lookUp(TCGAHelper.getCodeTable("tssCodes"),
				new int[] { 0 }, new String[] { getTSSAbb(barcode) },
				new int[] { 2 })[0];
	}

	public static String getDiseaseAbbFromBarcode(String barcode) {	
		String dName = getDiseaseNameFromBarcode(barcode);
		if(dName.equalsIgnoreCase("Cell Line Control"))
			dName = "Controls";
		return TCGAHelper.lookUp(TCGAHelper.getCodeTable("diseaseStudyCodes"),
				new int[] { 1 },
				new String[] { dName },
				new int[] { 0 })[0].trim();
	}

	public static String getSampleTypeDesc(String barcode) {
		try{
			return TCGAHelper.lookUp(TCGAHelper.getCodeTable("sampleTypeCodes"),
					new int[] { 0 }, new String[] { getSampleTypeAbb(barcode) },
					new int[] { 1 })[0].trim();
		} catch (NullPointerException e){
			return null;
		}
	}

	public static String getAnalyteDesc(String barcode) {
		try{
			return TCGAHelper.lookUp(TCGAHelper.getCodeTable("portionAnalyte"),
					new int[] { 0 }, new String[] { getSampleAnalyteAbb(barcode) },
					new int[] { 1 })[0];
		} catch (NullPointerException e){
			return null;
		}
	}

	public static String getCenterName(String barcode) {
		return TCGAHelper.lookUp(TCGAHelper.getCodeTable("centerCodes"),
				new int[] { 0 }, new String[] { getCenterAbb(barcode) },
				new int[] { 1 })[0].trim();
	}

	public static String getCenterShortName(String barcode) {
		return TCGAHelper.lookUp(TCGAHelper.getCodeTable("centerCodes"),
				new int[] { 0 }, new String[] { getCenterAbb(barcode) },
				new int[] { 4 })[0].trim();
	}
	
	public static String getCenterCodeFromCenterName(String centerName) {
		return TCGAHelper.lookUp(TCGAHelper.getCodeTable("centerCodes"),
				new int[] { 1}, new String[] { centerName },
				new int[] { 4 })[0].trim();
	}
	
	
	public static String getCenterAbbFromArchName(String archName) {
		return TCGAHelper.lookUp(TCGAHelper.getCodeTable("centerCodes"),
				new int[] { 1 }, new String[] { archName },
				new int[] { 4 })[0].trim();
	}
	
	
	
	// ////////////////////////////
	// / From Archive Name
	// ///////////////////////////
	
	// see https://wiki.nci.nih.gov/display/TCGA/Data+archive
	//<center.Name>_<disease study>.<platform>.<archive type>.<serial index>.<revision>.<series>
	// broad.mit.edu_BRCA.Genome_Wide_SNP_6.Level_3.103.2001.0/  
	// unc.edu_BRCA.IlluminaHiSeq_RNASeq.Level_3.1.2.0/  
	
	public static String getCenterNameFromArchive(String archName){
		return (archName.substring(0,archName.indexOf("_"))).replaceAll("_", "-");
	}

	
	/**
	 * see https://wiki.nci.nih.gov/display/TCGA/Data+archive
	 * <center.Name>_<disease study>.<platform>.<archive type>.<serial index>.<revision>.<series>
	 * @param archName
	 * @return
	 */
	public static String getPlatform(String archName){
		int stPos = archName.indexOf(".", archName.indexOf("_")+1);
		int endPos = archName.indexOf(".", stPos+1);
		return (archName.substring(stPos+1, endPos)).replaceAll("_", "-").toLowerCase();
				
	}
	
	public static String getLevel(String archName){
		int stPos = archName.toLowerCase().indexOf("level_")+6;
		int endPos = archName.indexOf(".",stPos+1);
		//return "Level-"+archName.substring(stPos, endPos);
		return archName.substring(stPos, endPos);
	}
	
	public static String getArchiveName(String tcgaPath) {
		//String toret = tcgaPath.substring(0,tcgaPath.lastIndexOf("/"));
		//return toret.substring(toret.lastIndexOf("/")+1);
		String toret = "";
		int ind = tcgaPath.lastIndexOf("/");
		if(ind != -1){
			toret = tcgaPath.substring(0,ind);
			toret = toret.substring(toret.lastIndexOf("/")+1);
		} 
		return toret;	
	}
	
	/**
	 * 
	 * @param tcgaPathToFile
	 * @return with last "/"
	 */
	public static String getArchivePathFromFile(String tcgaPathToFile) {
		//String toret = tcgaPath.substring(0,tcgaPath.lastIndexOf("/"));
		//return toret.substring(toret.lastIndexOf("/")+1);
		String toret = "";
		int ind = tcgaPathToFile.lastIndexOf("/");
		if(ind != -1){
			toret = tcgaPathToFile.substring(0,ind+1);
		} 
		return toret;	
	}
	
	public static String getFileNameFromPath(String tcgaPathToFile){
		String toret = "";
		int ind = tcgaPathToFile.lastIndexOf("/");
		if(ind != -1){
			toret = tcgaPathToFile.substring(ind+1);
		} 
		return toret;	
	}
	
	

	// ////////////////////////////
	// / From URL
	// ///////////////////////////
	
	public static String getDiseaseAbbrFromURL(String url){
		int stPos = url.indexOf("/tumor/")+7;
		return url.substring(stPos, url.indexOf("/",stPos+1));
	}
	

	public static void main(String[] args) {
		/*
		 * List<String[]> res = TCGAHelper.getCodeTable("diseaseStudyCodes");
		 * for(String[]s :res) System.out.println(Arrays.asList(s));
		 */

		//System.out.println("*"+getSampleTypeAbb("TCGA-A1-A0SD-01A-01D-A110-09")+"*");
		//System.out.println("*"+getLevel("unc.edu_BRCA.IlluminaHiSeq_RNASeq.Level_8.1.2.0/")+"*");
		
		/*String url = "https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/brca/bcr/nationwidechildrens.org/bio/clin/nationwidechildrens.org_BRCA.bio.Level_1.72.50.0/nationwidechildrens.org_biospecimen.TCGA-A1-A0SE.xml";
		System.out.println("disAbbFromURL: *"+getDiseaseAbbrFromURL(url)+"*");*/
		
		//out.println("uuid: "+mapping(BARCODE_STR,"TCGA-A1-A0SD-01A"));
		//System.out.println("barcode: "+mapping(UUID_STR,"81f193d9-ad19-43c2-b89f-54003a8d133e"));
		
		//System.out.println("uuid: "+uuidFromMetadata(BARCODE_STR,"TCGA-01-0629-11A", new String[] {"sample","@href"}));
		//System.out.println("getSampleBarcode: "+getSampleBarcode("TCGA-01-0629-11A-01D"));
		
		String uuid = "94ea3682-28b6-429a-82d9-e6a8ff8a916d".toLowerCase();	
		String fullBC = mapping(UUID_STR,uuid);
		System.out.println("Barcode by uuid: "+fullBC);
		
		//checkTSSNames();
		
		//System.out.println("getDiseaseAbbFromBarcode: "+getDiseaseAbbFromBarcode("TCGA-A7-AODC"));
		
		System.out.println("done with CodesUtil");
		

	}

}
