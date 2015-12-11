package edu.pitt.tcga.httpclient.module.ep;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;


import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.storage.StorageFactory;

import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.ExcelReader;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.ReferenceGenomeUtil;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class MassSpecModule extends TCGAModule{

	Pattern initP = null;
	
	private static final String MASS_SPEC_PATH = "https://cptc-xfer.uis.georgetown.edu/publicData/Phase_II_Data/";
	private static final String FOLDER_DATE_FORMAT = "dd-MMM-yyyy HH:mm";
	private DateFormat folderDF = new SimpleDateFormat(FOLDER_DATE_FORMAT,Locale.ENGLISH);
	private static final String FOLDERNAME_DATE_FORMAT = "yyyyMMdd";
	private DateFormat folderNameDF = new SimpleDateFormat(FOLDERNAME_DATE_FORMAT,Locale.ENGLISH);
	private SimpleDateFormat df = null;
	
	private Properties params = null;
	

	private Map<String, String> newClinFileNamesFullDate = new HashMap<String, String>();
	private Map<String, String> origClinNewFileNames = new HashMap<String, String>();
	private String currClinVersion="1";
	
	private Map<String, String> newOrigReportFileNames = new HashMap<String, String>();
	private Map<String, String> origReportFileAndPath = new HashMap<String, String>();
	private String currReportVersion="1";
	
	private String currDataVersion = "1";
	
	
	private Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
	
	private String REPORT_DIR_TEMPL = "";
	private int prefixSz = MySettings.SUPERCELL_HOME.length();
	private String topDestDir = "/other/mass_spectrometry/";
	


	
	public MassSpecModule(){
		initP = TCGAHelper.dateP;
		TCGAHelper.dateP = Pattern.compile("(\\d{2}-[A-Z,a-z]{3}-\\d{4}\\s\\d{2}\\:\\d{2})");
		loadParams();
		REPORT_DIR_TEMPL =  MySettings.SUPERCELL_HOME+topDestDir+"<disAbbr>"+
				File.separator+"data"+File.separator+"reports"+File.separator;
	}
	
	public void doDownload(){
		TCGAExpedition.setUniqueDirs();
		
		upload(listProperty("disease.list",","), "data");
	
		TCGAHelper.dateP = initP;
		System.out.println("DONE MassSpecModule");
	}
	
	public void upload(List<String> disList, String subtype){
		String currClinPath = null;
		for(String disAbbr:disList){
			newClinFileNamesFullDate.clear();
			origClinNewFileNames.clear();
			newOrigReportFileNames.clear();
			origReportFileAndPath.clear();
			
System.out.println("MassSpecModule goiging to archiveClinData for disAbbr: "+disAbbr);
			//TODO: correct archiveClinData before use. 
			//boolean sameAsInPGRR = archiveClinData(disAbbr, subtype);
			boolean sameAsInPGRR = false;
System.out.println("MassSpecModule DONE archiveClinData for disAbbr: "+disAbbr+" sameAsInPGRR: "+sameAsInPGRR);
			if(!sameAsInPGRR){		
				currClinPath = loadClinFiles(disAbbr,subtype);
				DataMatrix clinDataMatrix = getClinDataMatrix(disAbbr, currClinPath);
				
				loadReports(disAbbr);
				createClinMetadata(disAbbr, currClinPath, clinDataMatrix);	
				//upload data
				List<String> datatypes = listProperty(disAbbr+".datatypes",",");
				for(String dt:datatypes)
					loadData(disAbbr, subtype, dt, clinDataMatrix, 1, null);
					
			}
		}		
	}
	

	
	public void loadReports(String disAbbr){
		List<String> reportDirList = listProperty(disAbbr+".reports",",");
		
		String saveTo = REPORT_DIR_TEMPL.replace("<disAbbr>", disAbbr);
		String destDirName = "", lbName = "";
		String newName = "", fullDate = "", partialDate = "";
		File pgrrRepDir = null;
		String archiveDir = "";
		for(String s:reportDirList){

			destDirName = strProperty(disAbbr+"."+s+".dir");
			
			pgrrRepDir = new File(saveTo+destDirName);
			pgrrRepDir.mkdirs();
			
			archiveDir = strProperty(disAbbr+"."+s);
			
			List<LineBean> lbList = TCGAHelper.getPageBeans(archiveDir);
			for(LineBean lb:lbList){
				lbName = lb.getName();
				if(!lbName.equalsIgnoreCase("Parent Directory") && !lbName.endsWith(".cksum")){
System.out.println("Uploading report : "+saveTo+destDirName+File.separator+lbName);
					try{
						fullDate = reformatDate(lb.getModifiedDate());
						partialDate = partialReformattedDate(lb.getModifiedDate());
						newName = partialDate+"_"+lb.getName();
						File destFile = new File(saveTo+destDirName+File.separator+newName);
						if(!destFile.exists())
							FileUtils.copyURLToFile(new  URL(lb.getFullURL()), destFile);
						
						newOrigReportFileNames.put(newName,lb.getName());
						origReportFileAndPath.put(lb.getName(), archiveDir);
					
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	public void createClinMetadata(String disAbbr, String clinDataDir, DataMatrix clinDataMatrix){
		//get list of barcodes:
		List<String> colNames = listProperty(disAbbr+".clin-col-names",","); //<patient_barcode, shipped_portion_barcode_column_name, uuid_column_name>
System.out.println("colNames.get(1): "+colNames.get(1));
System.out.println("clin.header: "+Arrays.asList(clinDataMatrix.getHeader()));
System.out.println("clinDataMatrix.getColumnNum(colNames.get(1)): "+clinDataMatrix.getColumnNum(colNames.get(1)));
		List<String> aliquotList = clinDataMatrix.getUniqueValuesInCol(clinDataMatrix.getColumnNum(colNames.get(1)));
System.out.println("aliquotList = "+aliquotList);
		
		String fSize = null;
		String checksum = null;
		Aliquot al = null;
		String metadataPath = null;
		File metadataFile = null;
		String creationDate = null;
		String pittPath = null;
		String fullDate = null;

		for(Map.Entry<String, String> entry : newClinFileNamesFullDate.entrySet()){	
			fullDate = entry.getValue();
			creationDate = fullDate.substring(0,fullDate.indexOf(" "));
			
			File dataF = new File(clinDataDir+entry.getKey());
			fSize = String.valueOf(dataF.length());
			try {
				checksum = ModuleUtil.calcCheckSum(new FileInputStream(dataF));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				ErrorLog.log("MassSpecModule.createClinMetadata : "+dataF.getAbsolutePath()+"  ERR: "+e.getMessage());
			}
			pittPath = dataF.getAbsolutePath().substring(prefixSz, dataF.getAbsolutePath().length());
			pittPath = pittPath.substring(0,pittPath.lastIndexOf("/")+1);
			int sz = aliquotList.size();
			int num = 1;
			for(String bc:aliquotList){
//testing 
//if(bc.indexOf("A2-A0D0-01A") != -1){
				System.out.println("num "+num+" out of " +sz+"  bc: "+bc);
				
				al = constructAliquot(disAbbr, bc, dataF.getName(), dataF.getParentFile().getName(), currClinVersion, 
						true, creationDate, "mass-spec_clin", strProperty(disAbbr+".clin.dir"));
				al.setFileSize(fSize);
				al.setChecksum(checksum);
				al.setPittPath(pittPath);
				metadataPath = MySettings.SUPERCELL_HOME + al.constractPittPath(true);
				// make dir if not exist
				metadataFile = new File(metadataPath);
				metadataFile.mkdirs();						
				ModuleUtil.saveMetaData(al, metadataPath);						
				num++;
//} // end testing
			}
		}
		
	}
	
	//TODO
	public String getFileType(){
		//"clin", "report", "glycoproteome,phosphoproteome,proteome"
		return null;
	}
	
	//TODO
	public String getPlatform(){
		return "psm";
	}
	
	public  Aliquot constructAliquot(String disAbbr, String aliquotBarcode, String analysisFileName, String fileDir,
									String newVersion, boolean hasOwnName, String creationDate, String fileType, String tcgaPath){
		
System.out.println("about to constr: aliquotBarcode: "+aliquotBarcode+"  analysisFileName: "+analysisFileName+" fileDir: "+
									fileDir+ " creationDate: "+creationDate+" fileType: "+fileType);
		

		String exten = "";
		int lastInd = analysisFileName.lastIndexOf(".");
		if(lastInd != -1)
			exten = analysisFileName.substring(lastInd+1, analysisFileName.length());
		Aliquot al = new Aliquot(MASS_SPEC_PATH+fileDir, getDataType(),exten);
		al.setLevel("4");
		al.setBarcode(aliquotBarcode);

		al.setDiseaseStudyAbb(CodesUtil.getDiseaseAbbFromBarcode(aliquotBarcode));

		al.setOrigUUID(ModuleUtil.getUUIDByBarcode(al.getBarcode()));
		if(hasOwnName)
			al.setOwnFileName(analysisFileName);

		al.setFileFractionType("shipped_portion");
		//al.setDateCreated(creationDate+"T00:00:01Z");
		al.setDataAccessType(TCGAModule.PUBLIC);

		al.setCenterName("georgetown.edu");
		al.setCenterCode("georgetown.edu");

		al.setPlatform(getPlatform());
		al.setFileType(fileType);

		al.setTcgaArchivePath(tcgaPath);
		al.setTCGAFileName(analysisFileName.substring(analysisFileName.indexOf("_")+1)); // no date
		
		al.setVersion(newVersion);
//System.out.println("DIS:"+disAbbr+"  aliquotBarcode:"+aliquotBarcode+"  analysisFileName:"+analysisFileName+"  OwnFileName:"+al.getFileName()+" analysisDate:"+creationDate);

		return al;
		
	}
	
	/**
	 * used tp match with report name pattern (like "_BI_Proteome_")
	 * in brca file name has it inversed: "_Proteome_BI_"
	 * @param fName
	 * @param datatype
	 */
	public String getCenterTypePattern(String dAbbr, String fName, String datatype){
		List<String> dataNamePattern = listProperty(dAbbr+".data.pattern."+datatype, ",");
		for(String patt:dataNamePattern){
			
			if(fName.indexOf("_"+patt+"_") != -1){
				return strProperty(dAbbr+".reports.pattern"+patt);
			}
		}
		String mess = "MassSpecModule.getCenterTypePattern NO data.pattern OR .reports.pattern for "+dAbbr;
		System.err.println(mess);
		ErrorLog.logFatal(mess);
		return null;
	}
	
	public void resume(String methodName, String disAbbr, String subtype, String datatype, String stFromStr, String stFromFileWithPath, String clinFileName){
		if(methodName.equals("loadData")){
			String pgrrClinDir = MySettings.SUPERCELL_HOME+topDestDir+disAbbr+
					File.separator+subtype+File.separator+"clin"+File.separator;

			String shName = strProperty(disAbbr+".clin-sheet");
			int skipNum = Integer.valueOf(strProperty(disAbbr+".clin-sheet.skip.header.lines"));

			DataMatrix clinDM = ExcelReader.excelSheetToDataMatrix(pgrrClinDir+clinFileName, shName, skipNum, true);
			
			if(stFromFileWithPath.equalsIgnoreCase("NULL"))
				stFromFileWithPath = null;

			loadData(disAbbr, subtype, datatype, clinDM, Integer.parseInt(stFromStr), stFromFileWithPath);
		}
		TCGAHelper.dateP = initP;
	}
	
	public void loadData(String disAbbr, String subtype, String datatype, DataMatrix clinDataMatrix, int stFrom, String stFromFileWithPath){
		
		String origDataDir = strProperty(disAbbr+"."+datatype+".dir");
		List<String> bcReportsMetaLoaded = new ArrayList<String>();
		
		List<String> clinColNames = listProperty(disAbbr+".clin-col-names", ","); //List<patient_barcode_col_name, shipped_portion_barcode_col_name, uuid_col_name>
		
		String pgrrDataTypeDirTempl = MySettings.SUPERCELL_HOME+topDestDir+"<disAbbr>"+
				File.separator+subtype+File.separator+datatype.toLowerCase()+File.separator;
		
		String dataTypeReportDir = REPORT_DIR_TEMPL.replace("<disAbbr>", disAbbr)+File.separator+datatype;
		File[] reportFiles = (new File(dataTypeReportDir)).listFiles();
		String centerTypeReportPattern = "";
		
		String pgrrDataTypeDir = null;				
		
		// would be the same for both orig and pgrr
		StringBuilder curPathSB = new StringBuilder("");
System.out.println("MassSpecModule.loadData start for disAbbr: "+disAbbr+" subtype: "+subtype+" datatype: "+datatype+" datadir: "+origDataDir);
		List<LineBean> beans = TCGAHelper.getAllPageBeans(origDataDir);
		String lbNameUpcase = "";
		// could be barcode of patient, or sample, or shipped portion
		String bcStr = "";
		String[] bcArr = null;
		String creationDate = ""; // use first  dir after datatype dir
		
		

		for(LineBean lb:beans){
			lbNameUpcase = lb.getName().toUpperCase();
			if(!lb.getName().equalsIgnoreCase("Parent Directory") &&
					lbNameUpcase.indexOf("_"+datatype.toUpperCase()+"_") != -1 &&
					lbNameUpcase.indexOf("CANCER_METADATA") == -1 &&
					lbNameUpcase.indexOf("_REPORT.") == -1 &&
					lbNameUpcase.indexOf("CPTAC_TCGA_COLORECTAL") == -1 &&
					lbNameUpcase.indexOf("TCGA_COLON_VU_PROTEOME_CDAP_") == -1){
System.out.println("LB: "+lb.getName());
				
				// find list of samples
				bcStr =  lbNameUpcase.substring(5); // substring 'TCGA_' or "TCGA-'
System.out.println("lbNameUpcase: "+lbNameUpcase);
System.out.println("  bcStr: "+bcStr);
System.out.println("  Dtype: "+"_"+datatype.toUpperCase());
				bcStr = bcStr.substring(0, bcStr.indexOf("_"+datatype.toUpperCase()));
System.out.println("bcStr = "+bcStr);
				String[] bcArrLoc = bcStr.split("_");
				bcArr = new String[bcArrLoc.length];
				int c = 0;
				for(String s:bcArrLoc){
					bcArr[c]= "TCGA-"+bcArrLoc[c];
					c++;
				}
				bcArrLoc = null;
					
	System.out.println("NAME: "+lb.getName()+"  VALS: "+Arrays.asList(bcArr));
			
				List<Integer> patientRows =clinDataMatrix.getStartsWithDataRows(bcArr[0], clinDataMatrix.getColumnNum(clinColNames.get(1), 1));
				
				String currDisAbbr = "";
				if(clinDataMatrix.getColumnNum("Disease Code") == -1){
					String lookup = bcArr[0];
					if(lookup.toUpperCase().indexOf("CONTROL") != -1)
						lookup = bcArr[1];
					currDisAbbr = CodesUtil.getDiseaseAbbFromBarcode(lookup);	
				}
				else{
					try{
						currDisAbbr = clinDataMatrix.getRowColValue(clinDataMatrix.getColumnNum("Disease Code"), patientRows.get(0)).toLowerCase();
					} catch (IndexOutOfBoundsException e) {
						// do nothing for multi disease data directories like colorectal
					}
				} 
				
				patientRows.clear(); patientRows = null;
				
System.out.println("currDisAbbr: "+currDisAbbr);
// split data for coad and read in colorectal
				if(currDisAbbr.equalsIgnoreCase(disAbbr)){
				List<LineBean> PSMLevelBeans = TCGAHelper.getAllPageBeans(lb.getFullURL());
				for(LineBean lbPSM:PSMLevelBeans){
					String lbPSMUpCase = lbPSM.getName().toUpperCase();
					if(lbPSMUpCase.endsWith("_PSM") || lbPSMUpCase.endsWith("_PSM.TAR.GZ")){
						try {
							creationDate = reformatDateFromFileName(lb.getName().substring(lb.getName().lastIndexOf("_")+1));
						} catch (ParseException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
	
						curPathSB.setLength(0);
						curPathSB.append(lb.getName());
						curPathSB.append(File.separator);
System.out.println("### curPathSB  1: "+curPathSB);						
						File tsvDir = null;
//testing
/*if(lbPSMUpCase.equals("TCGA_A2-A0D0-01A_BH-A0HK-01A_C8-A12T-01A_PHOSPHOPROTEOME_BI_20130329_PSM.TAR.GZ") ||
		lbPSMUpCase.equals("TCGA_A2-A0D0-01A_BH-A0HK-01A_C8-A12T-01A_PROTEOME_BI_20130326_PSM.TAR.GZ")){*/
						pgrrDataTypeDir = pgrrDataTypeDirTempl.replace("<disAbbr>", currDisAbbr);
						File f = new File(pgrrDataTypeDir+lb.getName());
						f.mkdirs();
						//make dir if gzipped - must download all the data first
						if(lbPSMUpCase.endsWith("_PSM.TAR.GZ")){
							centerTypeReportPattern = getCenterTypePattern(disAbbr,lb.getName(), datatype);
						

	System.out.println("about to create Path: "+pgrrDataTypeDir+lb.getName());
							
							File archive = new File(pgrrDataTypeDir+lb.getName()+File.separator+lbPSM.getName());
							try {
								FileUtils.copyURLToFile(new URL(lbPSM.getFullURL()), archive);
								archiver.extract(archive, f);
								FileUtils.deleteQuietly(archive);	
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}							

							//delete all dirs which do not ends with _tsv
							File fDir = new File(pgrrDataTypeDir+lb.getName()+File.separator+lbPSM.getName().replace(".tar.gz",""));
							
							curPathSB.append(lbPSM.getName().replace(".tar.gz",""));
							curPathSB.append(File.separator);
							
							File[] listOfFiles = fDir.listFiles();
							
							for(File ff:listOfFiles){
								if(!ff.getName().endsWith("_tsv"))
									FileUtils.deleteQuietly(ff);
								else
									tsvDir = ff;
							}
							
						} // deal with LineBean if not gzipped
						else{
							List<LineBean> lbTSVLevel = TCGAHelper.getAllPageBeans(lbPSM.getFullURL());
							for(LineBean tsvB:lbTSVLevel){
								if(tsvB.getName().endsWith("_tsv")){
									tsvDir = new File(pgrrDataTypeDir+lb.getName()+File.separator+lbPSM.getName()+File.separator+tsvB.getName());
									tsvDir.mkdirs();
									List<LineBean> psmFileLevel = TCGAHelper.getAllPageBeans(tsvB.getFullURL());
									for(LineBean psmBean:psmFileLevel){
										if(!psmBean.getName().equalsIgnoreCase("Parent Directory")){
											File psmDir = new File(pgrrDataTypeDir+lb.getName()+File.separator+lbPSM.getName()+
												File.separator+tsvB.getName()+File.separator+psmBean.getName());
										
		System.out.println("*** psmDir: " +psmDir.getAbsolutePath());
		System.out.println("*** psmDir.name: " +psmDir.getName());
										
											try {
												FileUtils.copyURLToFile(new URL(tsvB.getFullURL()), psmDir);
												if(curPathSB.indexOf(File.separator+lbPSM.getName()+File.separator) == -1){
													curPathSB.append(lbPSM.getName());
													curPathSB.append(File.separator);
												}
												
												
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
										}
										
									}
								}
							}
							lbTSVLevel.clear();
							lbTSVLevel = null;
						}
						
						if(tsvDir != null){
							// get center abbreviation from *.tsv dir:
							String dirN = tsvDir.getName().toLowerCase();
							String dtStr = "_"+datatype+"_"; 
							int stInd = dirN.indexOf(dtStr)+dtStr.length();
							
							int endInd = dirN.indexOf("_",stInd);
							String centerAbbr = dirN.substring(stInd, endInd);
							String centerName = strProperty(centerAbbr);

		System.out.println(" **** CENTER ABBR: "+centerAbbr.toUpperCase()+" centerName: "+centerName);
							
							
							curPathSB.append(tsvDir.getName());
							curPathSB.append(File.separator);
							String currPath = curPathSB.toString();
							File[] listOfPSMFiles = tsvDir.listFiles();
							String fSize = "", checksum = "";
							int sz = listOfPSMFiles.length;
							int num = 1;
							for(File psmFile:listOfPSMFiles){
			if(!CAN_PROCEED){
				if(stFromFileWithPath == null)
					CAN_PROCEED = true;
			
				else if(psmFile.getAbsolutePath().equals(stFromFileWithPath) && stFrom == num)
					CAN_PROCEED = true;
			}
			
			System.out.println("*** num "+num+" of "+sz+"  data File "+psmFile.getAbsolutePath());
								num++;
								if(CAN_PROCEED) {
								fSize = String.valueOf(psmFile.length());
								try {
									checksum = ModuleUtil.calcCheckSum(new FileInputStream(psmFile));
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									ErrorLog.log("MassSpecModule.loadData : "+psmFile.getAbsolutePath()+"  ERR: "+e.getMessage());
								}
								
								List<Integer> recordRows = null;
								
								for(String partialBC:bcArr){
									if(partialBC.toUpperCase().indexOf("CONTROL") == -1){
			
									String patientBC = partialBC.substring(0,12);
System.out.println("about to CREATE METADATA meta for partialBC: "+partialBC+"    patientBC: " +patientBC+"   clinColNames.get(0): "+clinColNames.get(0));							
									recordRows =clinDataMatrix.getStartsWithDataRows(patientBC, clinDataMatrix.getColumnNum(clinColNames.get(0), 1));
									
					System.out.println("recordRows: "+Arrays.asList(recordRows)+"  for patientBC: "+patientBC+"  in "+clinColNames.get(0)+" num: "+clinDataMatrix.getColumnNum(clinColNames.get(0), 1));
					System.out.println(	"clinColNames.get(2): "+clinColNames.get(2));
									// check with center analyzed the portion
									if(recordRows.size() > 1){
										int rowNum = 0;
											if(disAbbr.equalsIgnoreCase("OV")){
												String lookupPcc = "PNNL";
												if(psmFile.getAbsolutePath().indexOf("_JHUZ_") != -1)
													lookupPcc = "JHU";
												rowNum = clinDataMatrix.getRowFromSubset(recordRows, lookupPcc,clinDataMatrix.getColumnNum("PCC"));
											}
											else{ // look for full partial barcode (column 'TCGA barcode (shipped portion)')
												recordRows =clinDataMatrix.getStartsWithDataRows(partialBC, clinDataMatrix.getColumnNum(clinColNames.get(1), 1));
												rowNum = recordRows.get(0);
											}
										recordRows.clear();
										recordRows.add(rowNum);
									}
					
									String ownID =clinDataMatrix.getRowColValue(clinDataMatrix.getColumnNum(clinColNames.get(2)), recordRows.get(0)).toLowerCase();
									
									String bc = "";
									try{
										bc = clinDataMatrix.getRowColValue(clinDataMatrix.getColumnNum(clinColNames.get(1)), recordRows.get(0)).toUpperCase();
									} catch (IndexOutOfBoundsException e){
										// this means that there is a bug in data: no such shipped portion barcode. (see TCGA-BH-A0E9-01A as an example)  find it by uuid
										bc = CodesUtil.mapping(CodesUtil.UUID_STR,ownID);
									}
									
									Aliquot al = constructAliquot(disAbbr,bc, psmFile.getName(), currPath,
											currDataVersion, true, creationDate, getFileType(), origDataDir+currPath);
									al.setOrigUUID(ownID);
									al.setFileSize(fSize);
									al.setChecksum(checksum);
									String pittPath = tsvDir.getAbsolutePath();
									pittPath = pittPath.substring(MySettings.SUPERCELL_HOME.length());
									al.setPittPath(pittPath);
									al.setFileType(datatype+"_psm");
									al.setRefGenome("grch37"); // currently this is true for all data
									al.setRefGenomeSource(ReferenceGenomeUtil.getGenomeURL("grch37"));
									
									String metadataPath = MySettings.SUPERCELL_HOME + al.constractPittPath(true);
									// make dir if not exist
									File metadataFile = new File(metadataPath);
									metadataFile.mkdirs();						
									ModuleUtil.saveMetaData(al, metadataPath);	
						if(!bcReportsMetaLoaded.contains(bc)){
							bcReportsMetaLoaded.add(bc);
							for(File repFile:reportFiles) {
			System.out.println("repFile.getName(): "+repFile.getName()+"  centerTypeReportPattern: "+centerTypeReportPattern);					
			
								if(repFile.getName().indexOf(centerTypeReportPattern) != -1){
							
									fSize = String.valueOf(repFile.length());
									try {
										checksum = ModuleUtil.calcCheckSum(new FileInputStream(repFile));
									} catch (FileNotFoundException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
										ErrorLog.log("MassSpecModule adding report metedata : "+repFile.getAbsolutePath()+"  ERR: "+e.getMessage());
									}
									pittPath = repFile.getAbsolutePath().substring(prefixSz, repFile.getAbsolutePath().length());
									pittPath = pittPath.substring(0,pittPath.lastIndexOf("/")+1);
									
									String tcgaFileName = newOrigReportFileNames.get(repFile.getName());
										
									al = constructAliquot(disAbbr, bc, repFile.getName(), repFile.getParentFile().getName(), currReportVersion, 
											true, creationDate, "mass-spec_report", origReportFileAndPath.get(tcgaFileName));
									al.setFileSize(fSize);
									al.setChecksum(checksum);
									al.setPittPath(pittPath);
									metadataPath = MySettings.SUPERCELL_HOME + al.constractPittPath(true);
									// make dir if not exist
									metadataFile = new File(metadataPath);
									metadataFile.mkdirs();						
									ModuleUtil.saveMetaData(al, metadataPath);						
		
									
								}
							}
									
								}
								} // if not "CONTROL"
							
								
							}
							} // if CAN_PROCEED
						}
						}

						
//	} //end testing
						
					}
				}
				PSMLevelBeans.clear();
				PSMLevelBeans = null;
			} // for correct diseaseAbbr
				
			}
		}
		beans.clear();
		beans = null;
		
	}
	
	/**
	 * Recursively loads data with "_PSM" suffixes up to ".psm" files.
	 * The path is the SAME  for orig and PGRR files
	 * @param disAbbr
	 * @param datatype
	 * @return
	 */
	public List<LineBean> loadUpToFiles (LineBean lb){
		String lbNameUpcase = lb.getName().toUpperCase();
		//	if(lb.getName())
		return null;
	}
	
	//This needs to be corrected: Check the dateCreated field, since ame in PGRR starts with prefix "YYYY-MM-DD_"
	/*public boolean archiveClinData(String disAbbr, String subtype){
		List<LineBean> topBeans = TCGAHelper.getPageBeans(MASS_SPEC_PATH+strProperty(disAbbr+".top"));
		String clinDir = strProperty(disAbbr+".clin.dir");
		
		String q = StorageFactory.getStorage().getStrProperty("LATEST_DATE_CREATED_VERSION");
		q = q.replace("<diseaseabbr>", disAbbr);
		q = q.replace("<analysistype>",getDataType());
	

		Map<String, String> resMap = StorageFactory.getStorage().getMap(q, "datecreated", "version");

		String latestDateInPGRR = null;
		if(resMap.size() != 0){
			latestDateInPGRR = (String)resMap.keySet().toArray()[0];
			latestDateInPGRR = latestDateInPGRR.replace("T", " ");
			latestDateInPGRR = latestDateInPGRR.substring(0, latestDateInPGRR.indexOf(" "));
		}
		String clinPath = MySettings.SUPERCELL_HOME+File.separator+"other"+File.separator+"mass_spectrometry"+File.separator+disAbbr+
				File.separator+subtype+File.separator+"clin"+File.separator;
		if(latestDateInPGRR != null){
			for(LineBean topB:topBeans ){
				if(topB.getName().equals(clinDir)){
					String currDate = partialReformattedDate(topB.getModifiedDate());
					// archive if there is a current dir with earlier date - archive all the data in it
					if(!latestDateInPGRR.equals(currDate)){
System.out.println("MassSpecModule.archiveClinData about to archhive latestDateInPGRR: "+latestDateInPGRR+"  currDate: "+currDate);	
						TCGAModule.archiveFilesInPGRRPath(disAbbr,getDataType(), clinPath, latestDateInPGRR, currDate, "modified");
						int pgrrVer = Integer.valueOf((String)resMap.entrySet().toArray()[0]);
						currClinVersion = String.valueOf(pgrrVer+1);
	System.out.println("MassSpecModule.archiveClinData DONE");
					}
					else return true;
				}
			}
		}
		return false;
	}*/
	
	private String partialReformattedDate(String origDate){
		try {
			String fullDate = reformatDate(origDate);
			return fullDate.substring(0,fullDate.indexOf(" "));
		} catch (ParseException e) {
			// TODO Auto-generated catch block	
			e.printStackTrace();
			ErrorLog.logFatal("MassSpecModule.partialReformattedDate can't reformat origDate: "+origDate);
			return null;		
		}
		
	}
	
	/**
	 * 
	 * @param disAbbr
	 * @param subtype: 'data' or 'compref'
	 * @return
	 */
	public  String loadClinFiles(String disAbbr, String subtype){
		
		List<LineBean> beans = TCGAHelper.getPageBeans(strProperty(disAbbr+".clin.dir"));
		//String fName = "";
		String fullDate = "";
		String partialDate = "";
		String pgrrClinDir = null;
		String newName = "";

	
		for(LineBean lb:beans){
			if(lb.getName().indexOf(".") != -1){
				try {
					fullDate = reformatDate(lb.getModifiedDate());
					partialDate = partialReformattedDate(lb.getModifiedDate());
					
					//fName =lb.getName();
					newName = partialDate+"_"+lb.getName();
					if(pgrrClinDir == null){
						pgrrClinDir = MySettings.SUPERCELL_HOME+topDestDir+disAbbr+
								File.separator+subtype+File.separator+"clin"+File.separator;
						File f = new File(pgrrClinDir);
						f.mkdirs();
					}
					
					File destF = new File(pgrrClinDir+newName);
					if(!destF.exists())
						FileUtils.copyURLToFile(new  URL(lb.getFullURL()), destF);
					newClinFileNamesFullDate.put(newName,fullDate);
					origClinNewFileNames.put(lb.getName(), newName);
System.out.println("About to copy file: "+lb.getName()+" to "+pgrrClinDir);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		System.out.println("loadClinFiles for "+disAbbr+" newFileNamesFullDate: "+newClinFileNamesFullDate);
		System.out.println("loadClinFiles for "+disAbbr+" origNewFileNames: "+origClinNewFileNames);
		return pgrrClinDir;
		
		
	}
	
	public DataMatrix getClinDataMatrix(String disAbbr, String pgrrClinDir){
		String origClinFile = strProperty(disAbbr+".clin.file");
		
		String pgrrClinFile = origClinNewFileNames.get(origClinFile);
		String shName = strProperty(disAbbr+".clin-sheet");
		int skipNum = Integer.valueOf(strProperty(disAbbr+".clin-sheet.skip.header.lines"));

		DataMatrix dm = ExcelReader.excelSheetToDataMatrix(pgrrClinDir+pgrrClinFile, shName, skipNum, true);
		
		return dm;
	}
	
	private  void loadParams(){
		try {
			params = new Properties();
			params.load(new FileInputStream(System.getProperty("user.dir")+
					File.separator+"resources"+File.separator+"curr_mass_spec_data.conf"));
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	public List<String> listProperty(String key, String separator){
		try{
			String arrStr = params.getProperty(key);
					//.replaceAll(" ", "");
			//arrStr.replaceAll("&", " ");
			return Arrays.asList(arrStr.split(separator));
		} catch (NullPointerException e){
			return new ArrayList<String>();
		}
	}
	
	public  String strProperty(String key){
		return params.getProperty(key);
	}
	

	@Override
	public String[] getResourceEndings() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getDataType() {
		return "Mass_Spectrometry";
	}

	@Override
	public String getAnalysisDirName() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String getResourceKey() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public String dataAccessType() {
		return TCGAModule.PUBLIC;
	}

	@Override
	public boolean canProcessArchive(LineBean archiveBean) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void processData(List<LineBean> levBeans) {
		// TODO Auto-generated method stub
		
	}
	
	
	public void processArchiveLevel(List<LineBean> levBeans) {
		for (LineBean lb : levBeans) {
			if(!ModuleUtil.currentArchives.contains(lb.getUrl()) && canProcessArchive(lb))
				processData(TCGAHelper.getPageBeans(lb.getFullURL()));	
		}

	}
	
	public  SimpleDateFormat getDateTimeFormat(){
		if (df == null ){
			df = new SimpleDateFormat(MySettings.TIMESTAMP_MILLI_FORMAT) ;
		}
		return df;
	}
	
	public void scrape(){
		List<LineBean> beans = TCGAHelper.getPageBeans(MASS_SPEC_PATH);
		String lbName = "";
		for(LineBean lb:beans){
			lbName = lb.getName().toLowerCase();
			if(lbName.startsWith("tcga_") &&
				(lbName.endsWith("_cancer") || lbName.endsWith("_compref"))){
				String lbDate = lb.getModifiedDate();
				try {
					lb.setModifiedDate(reformatDate(lbDate));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					ErrorLog.logFatal("MassSpecModule.scrape Can't format date: "+lbDate+" for dir "+lb.getFullURL());
				}

System.out.println("*** lb.name = "+lb.getName()+" date: "+lb.getModifiedDate());
				List<LineBean> disBeans = TCGAHelper.getPageBeans(lb.getFullURL());
				/*for(LineBean d_lb:disBeans){
					System.out.println("				*** lb.name = "+d_lb.getName()+" date: "+d_lb.getModifiedDate());
				}*/
			}
		}
	}
	
	public  String reformatDate(String dt) throws ParseException{
		try {
			return getDateTimeFormat().format(folderDF.parse(dt));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public  String reformatDateFromFileName(String dt) throws ParseException{
		try {
			return getDateTimeFormat().format(folderNameDF.parse(dt));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public static void main(String[] args){
		
		MassSpecModule msm = new MassSpecModule();
		msm.doDownload();
		
		//test
	//ATT: this is not a good resume  - used for debug now
		/*msm.resume("loadData", "coad","data","proteome", "1", 
				"NULL",	"2013-09-02_COAD_All_clinical_features_TCGAbiotab_release1_090413.xlsx");*/

		System.out.println("DONE MassSpecModule");
		
	}


}
