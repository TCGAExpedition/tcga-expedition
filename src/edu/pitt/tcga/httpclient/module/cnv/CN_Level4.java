package edu.pitt.tcga.httpclient.module.cnv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.exception.QueryException;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.storage.StorageFactory;
import edu.pitt.tcga.httpclient.transfer.exec.SystemCommandExecutor;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.QueryHelper;
import edu.pitt.tcga.httpclient.util.ReferenceGenomeUtil;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

/**
 * CN Level4 data from http://firebrowse.org/.
 * 
 * 
 * These data are PUBLIC on FIREHOSE, but CONTROLLED at tcga - we use CONTROLLED access type
 * 
 * Level 0 - in /<disAbbr>/Exp_Protein foler
 * 
 * To maintain the versions of the common files:
 * (1) rename common files: "<analysisDate>_"+origFileName
 * (2) TODO :ARCHIVE the record HERE if has previous version
 * @author opm1
 * @version 1
 * @since Dec 11, 2015
 *
 */
public class CN_Level4 extends HumanHap550_1MDuo{
	
	
	private String[] filesToProcess = {"all_data_by_genes.txt", "all_thresholded.by_genes.txt", "broad_values_by_arm.txt", "focal_data_by_genes.txt"};
	
	private static final String FIREHOSE_URL_TEMPLATE = "firehose_get -b -tasks cnv gistic2 analyses <analysisDate> <diseaseAbbr>";
	
	private String currFirehoseURL = "";
	private String currAnalysisDate = null;
	private String currDiseaseAbbr = null;
	private String newVersion = "1"; //TODO 
	
	private String OTHER_ROOT = "/other/gdacs/gdacbroad/";
	private String OTHER_DIS_PAGE = MySettings.getControlledRoot().replace("/tumor/", OTHER_ROOT);
	
	private String destinationTopDir = MySettings.SUPERCELL_HOME+File.separator+"other"+File.separator+"CN_Level4";
	
	private Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");

	
	
	public  List<String> getCohorts(){
		// get all available cohorts, extract only those that are in TCGA
		List<String> fireList = null;
		try {
			JSONObject fRes =QueryHelper.getGetAsJSONResult("http://firebrowse.org/api/v1/Metadata/Cohorts?format=json");
			// firehose cohort in upperCase
			fireList = jsonResAsArray(fRes, "Cohorts", "cohort");
			// do not remove any disease like {coadread, fppp, gbmlgg, kipan, stes}
			//Just check for real disease in fppp

			
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return fireList;
	}

	@Override
	public String dataAccessType() {
		return TCGAModule.CONTROLLED;
	}
	
	/**
	 * Directory names are in format YYYYMMDD00 ("2013092300")
	 * @param diseaseAbbr
	 * @return in format "YYYY_MM_DD"
	 */
	public String getLatestAnalysisDate(String disAbbr){
		String analiDatesPage = OTHER_DIS_PAGE+disAbbr.toLowerCase()+"/analyses/";
		List<LineBean> disDatesBeans = TCGAHelper.getPageBeans(analiDatesPage);
		Integer maxDate = 0, currData = 0;
		for(LineBean disBean:disDatesBeans){
			currData = Integer.valueOf(disBean.getName());
			if(maxDate < currData)
				maxDate = currData;
		}
		String toret = String.valueOf(maxDate);
		toret = toret.substring(0,4)+"_"+toret.substring(4,6)+"_"+toret.substring(6,8);
		return toret;
	}
	
	/**
	 * 
	 * @param diseaseAbbr
	 * @return in YYYY_MM_DD format
	 */
	public String getLatestPGRRAnalysisDate(String diseaseAbbr){
		String q = StorageFactory.getStorage().getStrProperty("LATEST_DATE_CREATED_VERSION");
		q = q.replace("<diseaseabbr>", diseaseAbbr);
		q = q.replace("<analysistype>","CN_Level4");	

		Map<String, String> resMap = StorageFactory.getStorage().getMap(q, "datecreated", "version");
		if(resMap.size() == 0)
			return null;
		
		String toret = (String)resMap.keySet().toArray()[0];
		toret = toret.replace("T", " ");
		toret = toret.substring(0, toret.indexOf(" "));
		return toret.replaceAll("-", "_");
	}
	
	/**
	 * 
	 * @param analysisDate
	 * @param diseaseAbbr
	 * @return
	 */
	public boolean downloadRawData(String diseaseAbbr){
		currAnalysisDate = getLatestAnalysisDate(diseaseAbbr);
		String latestPGRRAnalysisDate = getLatestPGRRAnalysisDate(diseaseAbbr);

		boolean success = false;
		if(latestPGRRAnalysisDate == null){
			return doDownloadRawData(currAnalysisDate, diseaseAbbr);

		} else{ // if there is a latest date
			//if the same => do nothing, otherwise => archive +do download
			if(currAnalysisDate.equalsIgnoreCase(latestPGRRAnalysisDate))
				return false;
			else {
			
				success = doDownloadRawData(currAnalysisDate, diseaseAbbr);

				if(success)
					doArchive(latestPGRRAnalysisDate, diseaseAbbr);
				
				return success;
			}	
		}	
	}
	
	/**
	 * 
	 * @param latestPGRRAnalysisDate 
	 * @param diseaseAbbr
	 */
	public void doArchive(String latestPGRRAnalysisDate, String diseaseAbbr){
		if(latestPGRRAnalysisDate == null)
			return;
		if(!latestPGRRAnalysisDate.equals(currAnalysisDate)){
			Storage storage = StorageFactory.getStorage();
			latestPGRRAnalysisDate = latestPGRRAnalysisDate.replaceAll("_","-");
			String q = storage.getStrProperty("UUIDS_PATH_BY_DIS_ANALYSIS_DATE");
			q = q.replace("<diseaseabbr>", diseaseAbbr);
			q = q.replace("<analysistype>", getDataType());
			q = q.replace("<datecreated>", latestPGRRAnalysisDate);
			
			Map<String, String> toArchiveMap = 
					storage.getMap(q,"uuid","pgrrpath");
			String archivedDate = currAnalysisDate.replaceAll("_", "-")+"T00:00:01Z";
			
			for (Map.Entry<String, String> entry : toArchiveMap.entrySet()) {
				ModuleUtil.archiveRecord(entry.getValue(), "pgrrUUID", entry.getKey(), "modified", archivedDate);
			}
			
			//set current version 
			String sampleUUID = (String)toArchiveMap.keySet().toArray()[0];
			String prevVersion = storage.fieldValueByUUID(storage.nameWithPrefixUUID(MySettings.PGRR_PREFIX_URI, sampleUUID), "version");
			newVersion= String .valueOf(Integer.valueOf(prevVersion)+1);
					
			toArchiveMap.clear();
			toArchiveMap = null;
		}
		
	}
	
	/**
	 * firehose_get does not always work
	 * @param analysisDate
	 * @param diseaseAbbr
	 * @return
	 */
	public boolean doDownloadRawData(String analysisDate, String diseaseAbbr){
		currFirehoseURL = FIREHOSE_URL_TEMPLATE.replace("<analysisDate>", analysisDate);
		currFirehoseURL = currFirehoseURL.replace("<diseaseAbbr>", diseaseAbbr);
		currAnalysisDate = analysisDate;
		currDiseaseAbbr = diseaseAbbr;
		
		String comm = "cd "+MySettings.TEMPO_DIR+"; "+MySettings.FIREHOSE_GET+" -b -tasks cnv gistic2 analyses "+analysisDate+" "+diseaseAbbr;

		 String output = SystemCommandExecutor.execBash(comm, true).trim();
		 if(output.endsWith("Now performing post-processing on retrieved files ...")) return true;
		 
		 // try another date
		 if(output.equalsIgnoreCase("ERROR exitVal NOT 0")){
System.out.println("CN_Level4 no data for analysisDate: "+analysisDate+" dis: "+diseaseAbbr);			 
		 }
		 return false;
	}
	
	public List<File> decomress(String analysisDate, String diseaseAbbr){
		String filesDir = MySettings.TEMPO_DIR+"analyses__"+analysisDate+File.separator+diseaseAbbr.toUpperCase()+File.separator+analysisDate.replaceAll("_","");
		File destination = new File(destinationTopDir+File.separator+diseaseAbbr.toLowerCase()+File.separator+analysisDate+File.separator);
		destination.mkdirs();
System.out.println("currAnalysisDate: "+currAnalysisDate+"  destination: "+destination);
		List<File> createdFolders = new ArrayList<File>();
		File fDir = new File(filesDir);
		String fName = "";
		if(fDir.isDirectory()){
			File[] fList = fDir.listFiles();
			for(File archive:fList ){
				fName = archive.getName();
	System.out.println("fName: "+fName);
				if(fName.indexOf(".CopyNumber_Gistic2.") == -1 || fName.endsWith(".tar.gz.md5"))
					archive.delete();
				else{
					createdFolders.add(new File(destination+File.separator+fName.substring(0,fName.indexOf(".tar.gz"))));

System.out.println("in decompress archive: "+archive.getAbsolutePath());
System.out.println("in decompress destination: "+destination.getAbsolutePath());
					try {

						archiver.extract(archive, destination);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			fList = null;
		}
		// remove files from tempo dir
		File dir = new File(filesDir);
		System.out.println("CN_Level4 clearTempoDir: "+dir.getAbsolutePath());

		for(File file: dir.listFiles()) file.delete();				
		clearTempoDir();
		
		System.gc();
		
		return createdFolders;
	}
	
	
	public void processDirData(File[] createdDirs){
		
		// (I) process ONLY level4 DATA directory FIRST (to get aliquotList)
		File dataDir = getFile(createdDirs, ".level_4.");
		File[] dataFileList = dataDir.listFiles();
		//Aliquot[] als = null;
		
System.out.println("			*** in CN_Level4.processDirData");
System.out.println("currAnalysisDate="+currAnalysisDate);
System.out.println("currDiseaseAbbr="+currDiseaseAbbr);
System.out.println("newVersion="+newVersion);
System.out.println("createdDirs.size="+createdDirs.length);
Arrays.sort(createdDirs);
Arrays.sort(createdDirs);
for(File ff:dataFileList)
	System.out.println("createdDirs.dir="+ff.getAbsolutePath());
System.out.println("dataDir="+dataDir);
System.out.println("dataFileList.size="+dataFileList.length);
for(File ff:dataFileList)
	System.out.println("dataDir.file="+ff.getAbsolutePath());

		step_1(dataFileList, createdDirs, "NULL", "1");
		
	}
	
	public void step_1(File[] dataFileList, File[] createdDirs, String currFileOrDirName, String startFromAlquotStr){		
		int startFromAlquotNum = Integer.valueOf(startFromAlquotStr).intValue();

		boolean found = (currFileOrDirName.equalsIgnoreCase("NULL"))?true:false;
		for(String s:filesToProcess){
			if(!found && currFileOrDirName.equalsIgnoreCase(s))	
				found = true;
			if(found){
System.out.println("CN_Level4.step_1 currFileOrDirName = "+currFileOrDirName);
				if(s.equals("broad_values_by_arm.txt"))
					processMultiSampleFile(getFile(dataFileList, "broad_values_by_arm.txt"), 1, 1, startFromAlquotNum);
				else
					processMultiSampleFile(getFile(dataFileList, s), 3, 1, startFromAlquotNum);
				// reset to initial value
				if(!currFileOrDirName.equalsIgnoreCase("NULL")){
					currFileOrDirName = "NULL";
					startFromAlquotNum = 1;
				}
			}
			
		}
	
		
		step_2(dataFileList,createdDirs,currFileOrDirName,startFromAlquotStr);
	}
	
	// Create Meatadada ONLY for the common files
	// For every entry in aliquotList create metadata for DATA file in all data dir
	public void step_2(File[] dataFileList, File[] createdDirs, String currFileOrDirName, String startFromAlquotStr){
		System.out.println("CN_Level4.step_2");
		
		List<String> aliquotList = getAliquotList(dataFileList, createdDirs, 1);
		for(File ddDir:createdDirs){
			if(!currFileOrDirName.equalsIgnoreCase("NULL")){
				List<String> partAliquotList = getAliquotList(dataFileList, createdDirs, Integer.valueOf(startFromAlquotStr).intValue());
				// check if dir contains file
				File file = new File(ddDir + File.separator+currFileOrDirName);
                boolean exists = file.exists();
                 
                if (file.exists()) {
                	createCommonFilesMetadata(ddDir, partAliquotList,currFileOrDirName, createdDirs);
                	currFileOrDirName = "NULL";
                	startFromAlquotStr = "1";
                }
			}
			else
				createCommonFilesMetadata(ddDir, aliquotList,"NULL", createdDirs);
			
		}

		createdDirs = null;
		dataFileList = null;
		aliquotList.clear();
		aliquotList = null;
	}
	
	public List<String> getAliquotList(File[] dataFileList, File[] createdDirs, int startFromAliquotNum){
		File aliqoutListF = getFile(dataFileList, "arraylistfile.txt");
		List<String> aliquotList = new LinkedList<String>();
		if(aliqoutListF != null){
			try {
				CSVReader reader = new CSVReader(new BufferedReader(
						new FileReader(aliqoutListF)), '\t');
				List<String[]> rows = reader.readAllToList();
				//remove head
				rows.remove(0);
				int k = 1;
				boolean found = false;
				for(String[] sArr:rows){
					if(k == startFromAliquotNum)
						found = true;
					if(found)
						aliquotList.add(sArr[0]);
					k++;
				}
				rows.clear();
				rows = null;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			File dataDir = getFile(createdDirs, ".level_4.");
			ErrorLog.log("CN_Level4.processDirData: no 'arraylistfile.txt' in "+dataDir.getAbsolutePath());
			System.out.println("CN_Level4.processDirData: no 'arraylistfile.txt' in "+dataDir.getAbsolutePath());
		}
		
		Collections.sort(aliquotList);
		return aliquotList;
	}
	

	public void createCommonFilesMetadata(File dirFile, List<String> aliquotList, String currFileOrDirName, File[] createdDirs){
		
		File[] dataFileList = dirFile.listFiles();
		Arrays.sort(dataFileList);
		
		Aliquot al = null;
		String origFileName = null, newFileName = null;
		String fSize = null, checksum = null,pittPath = null;
		String metadataPath = null;
		File newF = null;
		File metadataFile = null;
		int prefixSz = MySettings.SUPERCELL_HOME.length();
		boolean found = (currFileOrDirName.equalsIgnoreCase("NULL"))?true:false;
		
		
		for(File dataF:dataFileList){
			origFileName = dataF.getName();
			if(!found && origFileName.equals(currFileOrDirName))
				found = true;
			if(found && !origFileName.endsWith("MANIFEST.txt")	&&
			   !origFileName.endsWith(".mat")){
				
				if(!origFileName.startsWith(currAnalysisDate)){
					newFileName = currAnalysisDate+"_"+origFileName;
					//dataF.moveTo(dest)
					// rename file!
					newF = new File(dataF.getParentFile().getAbsolutePath()+File.separator+newFileName);
					dataF.renameTo(newF);
					dataF = newF;
				}
				else {
					newFileName = new String(origFileName);
					origFileName = origFileName.substring(11);
				}
				if (!dataF.isDirectory()){
		
					fSize = String.valueOf(dataF.length());
					try {
						checksum = ModuleUtil.calcCheckSum(new FileInputStream(dataF));
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						ErrorLog.log("CN_Level4 : "+dataF.getAbsolutePath()+"  ERR: "+e.getMessage());
					}
					pittPath = dataF.getAbsolutePath().substring(prefixSz, dataF.getAbsolutePath().length());
					pittPath = pittPath.substring(0,pittPath.lastIndexOf("/")+1);
							
			System.out.println("pittPath: "+pittPath);
					int sz = aliquotList.size();
					int num = 1;
					for(String bc:aliquotList){
	System.out.println("num "+num+" out of " +sz);
	
						al = constructAliquot(bc, newFileName, dataF.getParentFile().getName(), true);
						al.setFileSize(fSize);
						al.setChecksum(checksum);
						al.setPittPath(pittPath);
						metadataPath = MySettings.SUPERCELL_HOME + al.constractPittPath(true);
						// make dir if not exist
						metadataFile = new File(metadataPath);
						metadataFile.mkdirs();						
						ModuleUtil.saveMetaData(al, metadataPath);						
						num++;
					}
					if(!currFileOrDirName.equalsIgnoreCase("NULL")){
						currFileOrDirName = "NULL";
						aliquotList = getAliquotList(dataFileList, createdDirs, 1);
					}
				}
			} //if(!origFileName.endsWith("arraylistfile.txt") ...
		}
		
		dataFileList = null;
	
	}
	
	protected Aliquot[] processMultiSampleFile(File f, int firstUncommonCol, int countBy, int startFromAlquotNum){
	System.out.println("CN_Level4.processMultiSampleFile file = "+f.getAbsolutePath()+" firstUncommonCol: "+firstUncommonCol+" startFromAlquotNum="+startFromAlquotNum);
		Aliquot[] als = null;
		try{
			HttpClient httpclient = TCGAHelper.getHttpClient();
			InputStream is = new FileInputStream(f);
			
			
			HashMap<String, String> aliquotTempFileMap = toAliquotTempFileMap(is, firstUncommonCol, countBy);
			
			int c = 0;			
			for(Map.Entry<String, String> entry : aliquotTempFileMap.entrySet()){	
				System.out.println("num:  "+c+" aliquotTempFileMap.key: "+entry.getKey()+" aliquotTempFileMap.val: "+entry.getValue());
				c++;
			}

	
			httpclient.getConnectionManager().shutdown();
System.out.println("currDiseaseAbbr="+currDiseaseAbbr+"  aliquotTempFileMap.size()="+aliquotTempFileMap.size());			
			als = new Aliquot[aliquotTempFileMap.size() - startFromAlquotNum + 1];
			int count = 1;
			int ind = 0;
			boolean found = false;
			for(Map.Entry<String, String> entry : aliquotTempFileMap.entrySet()){
				if(!found && count == startFromAlquotNum)
					found = true;
				if(found){
					Aliquot al = constructAliquot(entry.getKey(), f.getName(), f.getParentFile().getName(), false);
					al.setTempoFile(new File(entry.getValue()));
					als[ind] = al;
					ind++;
System.out.println("created AL num "+count+" for "+al.getBarcode()+" fileName: "+f.getName());

			
				}
				count++;
			}
			
			aliquotTempFileMap.clear();
			aliquotTempFileMap = null;
			
			if(als != null){
				System.out.println("CN_Level4.processMultiSampleFile start transfer for file="+f.getAbsolutePath());
				ModuleUtil.transferNew(als);	
			}
			clearTempoDir();
			System.gc();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return als;
		
	}
	
	@Override
	public String getDataType() {
		return "CN_Level4";
	}
	
	
	protected String getFileType(String fileName, boolean asWholeFile) {
		String partStr = fileName.substring(0, fileName.lastIndexOf("."));
		String localAnalysisDate = currAnalysisDate.replaceAll("_","-")+"-";
		partStr = partStr.replaceAll("_","-");
		partStr = partStr.replace(localAnalysisDate,"");
		if(!asWholeFile){
			if(fileName.endsWith("all_data_by_genes.txt"))
				return "single-data-by-genes";
			else if(fileName.endsWith("all_thresholded.by_genes.txt"))
				return "single-thresholded-by-genes";
			else if(fileName.endsWith("broad_values_by_arm.txt"))
				return "single-values-by-arm";
			else if(fileName.endsWith("focal_data_by_genes.txt"))
				return "single-focal-data-by-genes";
			else return partStr;
		}
		
		else{
			if(partStr.indexOf("broad-values") != -1 || partStr.indexOf("focal-data)") != -1)
					partStr = "all-"+partStr;
			return partStr;
		}
		
	}
	

	protected String getPortion(String defPortion, String fileName, boolean asWholeFile) {
		if(!asWholeFile){
			if(fileName.endsWith("all_data_by_genes.txt"))
				return defPortion+"single-data-by-genes";
			else if(fileName.endsWith("all_thresholded.by_genes.txt"))
				return defPortion+"single-thresholded-by-genes";
			else if(fileName.endsWith("broad_values_by_arm.txt"))
				return defPortion+"single-values-by-arm";
			else if(fileName.endsWith("focal_data_by_genes.txt"))
				return defPortion+"single-focal-data-by-genes";
			else return defPortion;
		}
		
		else{
			String partStr = fileName.substring(0, fileName.lastIndexOf(".")).replaceAll("_","-");
			if(partStr.startsWith("broad-values")|| partStr.startsWith("focal-data)"))
					partStr = "all-"+partStr;
			return defPortion+partStr;
		}
		
	}
	
	public  Aliquot constructAliquot(String aliquotBarcode, String analysisFileName, String fileDir, boolean asWholeFile){
		String exten = "";
		int lastInd = analysisFileName.lastIndexOf(".");
		if(lastInd != -1)
			exten = analysisFileName.substring(lastInd+1, analysisFileName.length());
		Aliquot al = new Aliquot(currFirehoseURL, getDataType(),exten);
		al.setLevel("4");
		al.setBarcode(aliquotBarcode);

		if(currDiseaseAbbr.equalsIgnoreCase("FPPP"))
			al.setDiseaseStudyAbb(CodesUtil.getDiseaseAbbFromBarcode(al.getBarcode()));	
		else
			al.setDiseaseStudyAbb(currDiseaseAbbr);

		al.setOrigUUID(ModuleUtil.getUUIDByBarcode(al.getBarcode()));

		al.setFileFractionType("aliquot");
		al.setDateCreated(currAnalysisDate.replaceAll("_","-")+"T00:00:01Z");
		al.setDataAccessType(TCGAModule.CONTROLLED);

		al.setCenterName("broad.mit.edu");
		al.setCenterCode("BI");
		
		al.setRefGenome("hg19");
		
		String genURL = ReferenceGenomeUtil.getGenomeURL(al.getRefGenome());
		if(genURL != null)
			al.setRefGenomeSource(genURL);
		al.setPlatform("Gistic2");
		al.setFileType(getFileType(analysisFileName, asWholeFile));
		al.setAlgorithmName("Gistic2");

		al.setPortion(getPortion("", analysisFileName, asWholeFile));
		al.setTcgaArchivePath(OTHER_ROOT+"analyses/"+currDiseaseAbbr.toLowerCase()+"/"+currAnalysisDate.replaceAll("_","")+"/"+fileDir+".tar.gz/");
		String tcgaFileName = (analysisFileName.startsWith(currAnalysisDate)?analysisFileName.substring(11):analysisFileName);
		al.setTCGAFileName(tcgaFileName);
		
		if(asWholeFile)
			al.setOwnFileName(analysisFileName);
		else
			al.setOwnFileName(al.fileNameUpToPortion()+al.getPortion()+"_V"+newVersion+"."+al.getFileExtension());	
		al.setVersion(newVersion);
System.out.println("DIS:"+currDiseaseAbbr+"  aliquotBarcode:"+aliquotBarcode+"  analysisFileName:"+analysisFileName+"  OwnFileName:"+al.getFileName()+" analysisDate:"+currAnalysisDate);

		return al;
		
	}
	
	
	public File getFile(File[] fList, String fileName){
		for(File f: fList){
			if(f.getName().toLowerCase().indexOf(fileName) != -1)
				return f;
		}
		return null;
	}
	
	public List<String> jsonResAsArray(JSONObject res, String jArr,
			String fieldName) {
		try {
			JSONArray bindings = res.getJSONArray(jArr);
			String userID = null;
			List<String> rr = new ArrayList<String>();
			for (int i = 0; i < bindings.length(); i++) {
				JSONObject c = bindings.getJSONObject(i);
				rr.add(c.getString(fieldName));
			}
			Collections.sort(rr, String.CASE_INSENSITIVE_ORDER);

			return rr;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}
	
	
	public void downloadForDisease(String diseaseAbbr){
		newVersion = "1";
		if(downloadRawData(diseaseAbbr)){
		 	doArchive(currAnalysisDate, diseaseAbbr);
			List<File>createdFolders = decomress(currAnalysisDate, diseaseAbbr);		
			processDirData(createdFolders.toArray(new File[createdFolders.size()]));
		}
		
	}
	
	/**
	 * Download for disAbbr from TCGA ONLY
	 */
	public void doDownload(){
		TCGAExpedition.setUniqueDirs();
		MySettings.PGRR_META_NQ_FILE = "_CN_Level4"+MySettings.PGRR_META_NQ_FILE;
		List<String> cohortList = getCohorts();
		List<String> tcgaList = ModuleUtil.getTCGADiseaseAbbrList();
		for(String diseaseAbbr:cohortList){
			diseaseAbbr = diseaseAbbr.toLowerCase();
			// do it for the diseases in TCGA only!
			if(tcgaList.contains(diseaseAbbr)){
				downloadForDisease(diseaseAbbr);
			}
		}
	}
	
	
	
	/**
	 * Resume from
	 * disease, analisysDate in format YYYY_MM_DD, methodName 
	 */
	public void resumeFrom(String startFromDiseaseAbbr, String analysisDate, String methodName, String currFileOrDirName, String startFromAlquot){
System.out.println("**** CN_Level4 resumeFrom: startFromDiseaseAbbr="+startFromDiseaseAbbr+" analysisDate="+analysisDate+" methodName="+methodName+
		" currFileOrDirName="+currFileOrDirName+" startFromAlquot="+startFromAlquot);
		TCGAExpedition.setUniqueDirs();
		MySettings.PGRR_META_NQ_FILE = "_CN_Level4"+MySettings.PGRR_META_NQ_FILE;
		List<String> cohortList = getCohorts();
		List<String> tcgaList = ModuleUtil.getTCGADiseaseAbbrList();
		boolean doIt = false;
		for(String diseaseAbbr:cohortList){
			diseaseAbbr = diseaseAbbr.toLowerCase();
			
			// do it for the diseases in TCGA only!
			if(tcgaList.contains(diseaseAbbr) && doIt){
				downloadForDisease(diseaseAbbr);
			}
			if(diseaseAbbr.equalsIgnoreCase(startFromDiseaseAbbr)){

				File destination = new File(destinationTopDir+File.separator+diseaseAbbr.toLowerCase()+File.separator+analysisDate+File.separator);
				File[] listOfDirs = destination.listFiles();
				if(listOfDirs == null || listOfDirs.length == 0){
					// download first
					downloadForDisease(startFromDiseaseAbbr);
					doIt = true;
				}
				else{
				
					File dataDir = getFile(listOfDirs, ".level_4.");
					File[] dataFileList = dataDir.listFiles();
					
					//sort
					Arrays.sort(listOfDirs);
					Arrays.sort(dataFileList);
					
					Class[] clArg = new Class[4];
					clArg[0] = dataFileList.getClass();
					clArg[1] = listOfDirs.getClass();
					clArg[2] = currFileOrDirName.getClass();
					clArg[3] = startFromAlquot.getClass();
					
					try {
						currDiseaseAbbr = startFromDiseaseAbbr;
						this.currAnalysisDate = analysisDate;
						Method m = this.getClass().getDeclaredMethod(methodName, clArg);
						m.invoke(this, new Object[]{dataFileList, listOfDirs, currFileOrDirName, startFromAlquot});
	
						
					} catch (NoSuchMethodException | SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					doIt = true;
				}
				
			}
		}
		
	}
		
	
	public static void main(String[] args){
	
		CN_Level4 lev4 = new CN_Level4();
		//test
		/*args = new String[5];
		args[0] = "meso";
		args[1] = "2015_04_02";
		args[2] = "step_1";
		args[3] = "null";
		args[4] = "1";*/
				
		
		
		if(args.length == 0){
			lev4.doDownload();
boolean b = lev4.doDownloadRawData("2015_04_02", "esca");
System.out.println("b: "+b);
		}
		else if (args.length == 5){
			lev4.resumeFrom(args[0], args[1], args[2], args[3], args[4]);
		}
		else{
			System.out.println("CN_Level4: no agrs - start fresh download. ");
			System.out.println("NOW: Paramaters for 'resume' must be setup manually. On the LIST TODO - automate it.");
			System.out.println("Otherwise it takes 5 arguments: (1) disieaseAbbr (like 'brca'), (2) analysis date (format YYYY_MM_DD), (3) method name (step_<X>)");
			System.out.println("(4) currFileOrDirName (put \"null\" if need all dirs and files,  (5) startFromAlquot - put \"1\" if start from the beginning");
					
			System.exit(0);
		}
		
		
		System.out.println("DONE CN_Level4");
	}
	
	
}
