package edu.pitt.tcga.httpclient.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.storage.StorageFactory;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public abstract class TCGAModule {
	
	public static final String CONTROLLED = "controlled";
	public static final String PUBLIC = "public";
	private static Properties params;
	
	public abstract String[] getResourceEndings(); // almost the same as file extension (not always: see ProtectedMaf)
	
	public abstract String getDataType();
	public abstract String getAnalysisDirName();
	public abstract String getResourceKey(); //field name in knownPlatforms.conf file
	public abstract String dataAccessType();
	
	public abstract boolean canProcessArchive(LineBean archiveBean);
	
	public static List<String> tcgaArchvePathsList = null;
	
	public abstract void processData(List<LineBean> levBeans);
	
	public static int TEMP_FILE_NUM = 1;
	
	// resume download parameters
	public static boolean CAN_PROCEED = true; // true for any new archive
	public static String START_FROM_ARCHIVE = ""; // TCGA archive
	public static String START_FROM_FILE = "";  // TCGA file name
	public static int START_FROM_RECORD = 0; // default = 0 : start from the first record
	private static String START_FROM_DISEASE = "";
	private static String CURRENT_MAGE_TAB_URL = null;
	private static boolean NEED_RESUME = false;
	
	
	private static Map<String, TCGAModule> availModules = new HashMap<String, TCGAModule>();
	
	private static void load(){
		try {
			params = new Properties();
			params.load(new FileInputStream(System.getProperty("user.dir")+
					File.separator+"resources"+File.separator+"knownPlatforms.conf"));
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	/**
	 * 
	 *  Sets parameters to resume download.
	 *
	 * @param startFromArchName - full URL of the TCGA archive like 
	 *  <b>
	 *  "https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/
	 *  blca/bcr/nationwidechildrens.org/bio/clin/nationwidechildrens.org_BLCA.bio.Level_2.0.43.0/"
	 *  </b>
	 * @param startFromFileName - name of file to be processed
	 * @param startFromRowNumStr - record number to start download from
	 * @return Nothing.
	 */
	public void setResumeParams(String startFromArchName,
			String startFromFileName, String startFromRowNumStr) {
		NEED_RESUME = true;
		CAN_PROCEED = false;
		START_FROM_ARCHIVE = startFromArchName;
		START_FROM_FILE = startFromFileName;
		START_FROM_RECORD = Integer.valueOf(startFromRowNumStr);
		START_FROM_DISEASE = CodesUtil.getDiseaseAbbrFromURL(startFromArchName);
		
	}
	
	public static void archiveFilesInPGRRPath( String diseaseAbbr, String dataType, String pgrrPath, 
			String latestPGRRAnalysisDate, String currAnalysisDate, String reasonArchived){
		
		Storage storage = StorageFactory.getStorage();
		String q = storage.getStrProperty("UUIDS_BY_DIS_ANALYSIS_DATE_PATH");
		q = q.replace("<diseaseabbr>", diseaseAbbr);
		q = q.replace("<analysistype>", dataType);
		q = q.replace("<datecreated>", latestPGRRAnalysisDate);
		q = q.replace("<pgrrpath>", pgrrPath);
		
		
		
		List<String> uuidsList = storage.resultAsStrList(q,"uuid");
		String archivedDate = currAnalysisDate.replaceAll("_", "-")+"T00:00:01Z";
	
		for (String uuid:uuidsList) {
			ModuleUtil.archiveRecord(pgrrPath, "pgrrUUID", uuid, reasonArchived, archivedDate);
		}
		
	}
	

	public void processArchiveLevel(List<LineBean> levBeans) {	
		for (LineBean lb : levBeans) {
			if(NEED_RESUME){
				if(!CAN_PROCEED ){
					if (lb.getName().indexOf(".mage-tab.") != -1 )
						CURRENT_MAGE_TAB_URL = lb.getFullURL();
					else if(lb.getFullURL().equals(START_FROM_ARCHIVE)){
						CAN_PROCEED = true;
						if(CURRENT_MAGE_TAB_URL != null)
							processData(TCGAHelper.getPageBeans(CURRENT_MAGE_TAB_URL));	
					}
				}
			}		
			else if((!NEED_RESUME && 
					//!ModuleUtil.currentArchives.contains(lb.getUrl()) && 
					canProcessArchive(lb)) ||
					(NEED_RESUME && CAN_PROCEED)){
				processData(TCGAHelper.getPageBeans(lb.getFullURL()));	
				if(NEED_RESUME)
					NEED_RESUME = false;
			}
		}

	}
	
	private static Properties getProperties(){
		if(params == null )
			load();
		return params;
	}
	
	public static List<String> getListProperty(String key, String separator){
		try{
			String arrStr = getProperties().getProperty(key).replaceAll(" ", "");
			return Arrays.asList(arrStr.split(separator));
		} catch (NullPointerException e){
			return new ArrayList<String>();
		}
	}
	
	public static String getProperty(String key){
		return getProperties().getProperty(key);
	}
	
	
	public static void clearTempoDir(){
		File dir = new File(MySettings.TEMPO_DIR);
		for(File file: dir.listFiles()) file.delete();
	}
	
	public static void readDataToMatrix(DataMatrix dm, String lbFullURL, char delim){
		HttpClient httpclient = TCGAHelper.getHttpClient();
		InputStream is = TCGAHelper.getGetResponseInputStream(
				httpclient, lbFullURL);

		CSVReader reader = new CSVReader(new BufferedReader(
				new InputStreamReader(is)), delim);
		
		try {
			dm.setData(reader.readAllToList());
			reader.close();
			is.close();	
			httpclient.getConnectionManager().shutdown();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public static List<String> unknownPlatformsByDataTypeFileExt(List<LineBean> lineBeans, String[] ext, String currDis, 
			String analysisType, List<String> unknownPaths, List<String> knownPaths, String centerName) {
		
		boolean isDir = false;
		String centerTypeName = "";
		for (LineBean lb : lineBeans) {
			centerTypeName = lb.getCenterTypeName();
			isDir = lb.isDirectory();
//System.out.println("LB isDir? "+isDir+"  fullurl :"+lb.getUrl());
		
	if(isDir && !lb.getDiseaseStudy().equals(currDis)){
		currDis = lb.getDiseaseStudy();
		System.out.println("Scanning "+currDis);
	}
			if(centerTypeName.equals("") || centerTypeName.equals(centerName)){
				if(isDir && lb.isDataTypeLevel()){
		/*System.out.println("lb.isDataTypeLevel(): "+lb.getUrl());
		System.out.println("lb.getName(): "+lb.getName()+"  analysisType: "+analysisType);
		System.out.println("lb.getModuleIdent(): "+lb.getModuleIdent());*/
					if(lb.getName().equalsIgnoreCase(analysisType) && !unknownPaths.contains(lb.getModuleIdent())){
						unknownPaths = processArchive(TCGAHelper.recentOnly(TCGAHelper.getPageBeans(lb.getFullURL())), ext, unknownPaths, knownPaths);
					}
				}
				else if(isDir) // scan up to datatype
					unknownPlatformsByDataTypeFileExt(TCGAHelper.getPageBeans(lb.getFullURL()), ext, currDis, analysisType, unknownPaths,knownPaths,centerName);
			}
		}
		
		
		return unknownPaths;

	}
	
	public static boolean canProcess(String tcgaArchiveURL){
		return (ModuleUtil.currentArchives.contains(tcgaArchiveURL))?false:true;
	}
	
	
	
	/**
	 * Process only one archive if found
	 * @param archBeans - top dir
	 * @param ext - file extension - archive MUST contain all the extensions
	 * @param unknownPaths - to be determined
	 * @param knownPaths - dataType/center/platform/analysisType/ from knownPlatform.conf
	 * @return unknownPaths
	 */
	public static List<String>  processArchive(List<LineBean> archBeans, String[] extArr, List<String> unknownPaths, List<String> knownPaths){
		
		List<String> defaultList = null;
		for(LineBean lb : archBeans) {
			List<LineBean> fileBeans = TCGAHelper.getPageBeans(lb.getFullURL());
			defaultList = new ArrayList(Arrays.asList(extArr));
			for(LineBean fileBean:fileBeans){
				for(String ext:extArr){
					if(!TCGAHelper.stringContains(fileBean.getName(), MySettings.infoFilesList) && (fileBean.getName()).toLowerCase().endsWith(ext) && defaultList.contains(ext))
						defaultList.remove(ext);
	
				}
				if(defaultList.size() == 0 && !knownPaths.contains(fileBean.getModuleIdent())){
	//System.out.println("ADDING "+Arrays.asList(extArr)+"  AT: "+fileBean.getModuleIdent());	
	//System.out.println("fileBean"+fileBean.getUrl());	
					unknownPaths.add(fileBean.getModuleIdent());
					defaultList.clear();
					return unknownPaths;
				}
			}
		}
		if(defaultList != null)
			defaultList.clear();
		return unknownPaths;
	}
	

	private static boolean doScan = true;
	private static String currDis = "";
	//need to know resourceKey ("clinical"), analysisdirname ("clin")
	public static void scrape(List<LineBean> levBeans, List<String>knownPaths,String resourceKey, String analysisDirName) {
//System.out.println("levBeans: "+levBeans+" knownPaths: "+knownPaths+" resourceKey: "+resourceKey+" analysisDirName: "+analysisDirName);
		//PrepareVirtData.PRE_SERIALIZED = true; //don't serialize anything
		boolean isDir = false;
		//List<String> knownPaths = getListProperty(getResourceKey(), ",");
		for (LineBean lb : levBeans) {
			isDir = lb.isDirectory();
			// skip some disesase levels
			
			if(isDir && !MySettings.skipOnDiseaseLevel_List.contains(lb.getDiseaseStudy())){
				
if(!lb.getDiseaseStudy().equals(currDis)){
	currDis = lb.getDiseaseStudy();
	System.out.println("TCGAModule:   Scanning "+currDis);
}
//System.out.println("url: "+lb.getUrl()+" isDaTy? "+lb.isDataTypeLevel()+ " analysisDirName: "+analysisDirName+" lb.getModuleIdent(): "+lb.getModuleIdent());
//System.out.println("in paths: "+knownPaths.contains(lb.getModuleIdent())+"  knownPaths: "+knownPaths);
			//scan up to datatype
			if((NEED_RESUME && START_FROM_DISEASE.equals(lb.getDiseaseStudy())) || !NEED_RESUME){
				if(lb.isDataTypeLevel()){
					if(doScan && lb.getName().equalsIgnoreCase(analysisDirName) 			
						&& knownPaths.contains(lb.getModuleIdent())){
						TCGAModule  module = availModules.get(resourceKey+"."+lb.getDataTypeToAnalysisType());
						if(module==null){
	//System.out.println("lb.getModuleIdent(): "+lb.getModuleIdent());
	//System.out.println("isDATA = "+lb.getName()+" lookfor: "+getProperty(resourceKey+"."+lb.getDataTypeToAnalysisType()));
							module = initModule(getProperty(resourceKey+"."+lb.getDataTypeToAnalysisType()));
							availModules.put(lb.getUrl(), module);
	//System.out.println("module: "+module);
						}
						if(module  != null)
							module.processArchiveLevel(TCGAHelper.recentOnly(TCGAHelper.getPageBeans(lb.getFullURL())));
					}
				}
				else {
					scrape(TCGAHelper.getPageBeans(lb.getFullURL()), knownPaths, resourceKey, analysisDirName);
					
				}
			}
			}
			
		}
		
	}
	
	public static TCGAModule initModule(String className){

		try {
			Class cl = Class.forName(className);
			return (TCGAModule) cl.newInstance();
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ErrorLog.logFatal("TCGAModule.initModule NO class found for  "+className);
		return null;
	}
	
	public static boolean acceptableEnding(String lbName, String[] endings){
		for(String s:endings){
			if(lbName.toLowerCase().endsWith(s))
				return true;
		}
		return false;
	}
	
	
	
	public static void main(String[] args) {
	
		/*ProtectedMaf pm = new ProtectedMaf();
		List<String> res = unknownPlatformsByDataTypeFileExt(TCGAHelper.getPageBeans(MySettings.getControlledRoot()), 
				pm.getResourceEndings(), "", 
		pm.getAnalysisDirName(), new ArrayList<String>(), pm.getListProperty(pm.getResourceKey(), ","));*/
		
		/*ImageModule pm = new ImageModule();
		List<String> res = unknownPlatformsByDataTypeFileExt(TCGAHelper.getPageBeans(MySettings.PUB_ROOT_URL+"coad/"), 
				pm.getResourceEndings(), "", 
		pm.getAnalysisDirName(), new ArrayList<String>(), pm.getListProperty(pm.getResourceKey(), ","), "bcr");*/
		
		/*Broad_GenWideSnp6 pm = new Broad_GenWideSnp6();
		List<String> res = unknownPlatformsByDataTypeFileExt(TCGAHelper.getPageBeans(MySettings.getControlledRoot()), 
				pm.getResourceEndings(), "", 
		pm.getAnalysisDirName(), new ArrayList<String>(), pm.getListProperty(pm.getResourceKey(), ","), "cgcc");*/
		
		/*SomaticMaf pm = new SomaticMaf();
		List<String> res = unknownPlatformsByDataTypeFileExt(TCGAHelper.getPageBeans(MySettings.PUB_ROOT_URL), 
				pm.getResourceEndings(), "", 
		pm.getAnalysisDirName(), new ArrayList<String>(), pm.getListProperty(pm.getResourceKey(), ","), "gsc");*/
		
		
		/*CNAModule pm = new CNAModule();
		List<String> res = unknownPlatformsByDataTypeFileExt(TCGAHelper.getPageBeans(MySettings.PUB_ROOT_URL), 
				pm.getResourceEndings(), "", 
		pm.getAnalysisDirName(), new ArrayList<String>(),new ArrayList<String>(),"cgcc");*/
		
		
		
		//System.out.println("unknownPlatformsByDataTypeFileExt list.sz = "+res);
		
		 
				
	}
	
	
}
