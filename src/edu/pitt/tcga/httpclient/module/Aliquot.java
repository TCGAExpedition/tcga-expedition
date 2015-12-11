package edu.pitt.tcga.httpclient.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.storage.StorageFactory;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class Aliquot {
	
	private String projectName = "TCGA";
	private String barcode = "";
	private String diseaseStudyAbb = null;
	private String tssAbb = null;
	private String tssName = null;
	private String sampleTypeAbb = null;
	private String sampleTypeDesc = null;
	private String sampleAnalyteAbb = null;
	private String analyteDesc = null;
	private String level = null;
	private String fileFractionType = ""; // patient, sample, drug, ... ,aliquot
	private String patientBarcode = null;
	private String sampleBarcode = null;
	private String tcgaPath = ""; // full path to the file in TCGA directory
	private String pittPath = "";
	private File tempoFile = null;
	private String dataAccessType = TCGAModule.PUBLIC;
	private String archiveName = ""; // TCGA archive name
	private String archReason = "";
	private String fileExtension = ""; // extension
	private String version = "1"; // pitt version
	private String dataType= "";
	private String[] metadata = null;
	private String refGenome = "";
	private String refGenomeSource = "";
	private String portion = "1";// files could have the same names, this is a way to distinguish them
	private String tcgaFileName = null;
	private URL tcgaFileURL = null;
	private String dateCreated = null;
	
	// used for cgHub
	private boolean hasOwnFileName = false; // true for cgHub
	private String ownFileName = null;
	private String fileSize = null;
	private String tcgaArchivePath = null;
	
	// uuids
	private String origUUID = "";
	private String patientUUID = "";
	private String sampleUUID = "";
	private String aliquotUUID = "";
	
	private String centerCode = "";
	private String centerName = "";
	private String platform = "";
	
	private String id;  // ##SAMPLE starts with ID like "NORMAL" or "PRIMARY"
	private String info = ""; //vcf ##SAMPLE string
	private String checksum = "";
	
	private String subType = null; // used to set part of clinical name since the dataType is "biotab", but there a lot of files
	private String fileType = "";
	private String pgrrUUID = "";
	
	private String algorithmName = "";
	private String algorithmVersion = "";
	
	// for aliases (VCF saved in patient with aliases in samples)
	private boolean saveInPatientDir = false;
	private boolean hasAlias = false;
	private String aliasPath = null;
	private boolean hasTempoFile = true;
	
	// MAF has sequencesource
	private String sequenceSource = null;
	
	private String centerExperiment = null;
	
	
	private boolean hasOrigBarcode = true; // false for clin cqcf, follow-up
	
	private static Pattern endDigitsPattern = Pattern.compile("[1-9]+$");
	
	public static List<String> fractionWithNoSampleBC = Arrays.asList("drug", 
			"radiation", "follow-up-v1.5", "follow-up-v2.0","follow-up-v2.1","follow-up-v4.0","follow-up-v4.0-nte", 
			"clinical-cqcf","biospecimen-cqcf", "patient","clinical-nte");
	

	
	public Aliquot(String tcgaPath, String dataType, String fileExtension, boolean hasOrigBarcode){	
		this(tcgaPath,dataType, fileExtension);
		setHasOrigBarcode(hasOrigBarcode);
	}
	
	public Aliquot(String tcgaPath, String dataType, String fileExtension){
		setTcgaPath(tcgaPath);
		setArchiveName(tcgaPath);
		setDataType(dataType);
		setFileExtension(fileExtension);
	}
	
	public void setDateCreated(String dateCreated){
		this.dateCreated = dateCreated;
	}
	
	public String getDateCreated(){
		return dateCreated;
	}
	
	public void setSequenceSource(String sequenceSource){
		this.sequenceSource = sequenceSource;
	}
	
	public String getSequenceSource(){
		return sequenceSource;
	}
	
	public void setHasOwnFileName(boolean hasOwnFileName){
		this.hasOwnFileName = hasOwnFileName;
	}
	
	public boolean getHasOwnFileName(){
		return hasOwnFileName;
	}
	
	public void setOwnFileName(String ownFileName){
		this.ownFileName = ownFileName;
		setHasOwnFileName(true);
	}
	
	public String getOwnFileName(){
		return ownFileName;
	}

	
	public void setTcgaFileUrl(URL tcgaFileURL){
		this.tcgaFileURL = tcgaFileURL;
	}
	
	public URL getTcgaFileUrl(){
		return tcgaFileURL;
	}
	
	public void setSaveInPatientDir(boolean saveInPatientDir){
		this.saveInPatientDir = saveInPatientDir;
	}
	
	public boolean isSaveInPatientDir(){
		return saveInPatientDir;
	}
	
	public void setHasAlias(boolean hasAlias){
		this.hasAlias = hasAlias;
	}
	
	public boolean isHasAlias(){
		return hasAlias;
	}
	
	public void setHasTempoFile(boolean hasTempoFile){
		this.hasTempoFile = hasTempoFile;
	}
	
	public boolean isHasTempoFile(){
		return hasTempoFile;
	}
	
	
	public void setAliasPath(String  aliasPath){
		this.aliasPath = aliasPath;
	}
	
	public String getAliasPath(){
		return aliasPath;
	}
	
	
	public void setAlgorithmName(String algorithmName){
		this.algorithmName = algorithmName;
	}
	
	public void setAlgorithmVersion(String algorithmVersion){
		this.algorithmVersion = algorithmVersion;
	}
	
	public void setFileType(String fileType){
		this.fileType = fileType;
	}
	
	public String getFileType(){
		return fileType;
	}
	
	public void setPgrrUUID(String pgrrUUID){
		this.pgrrUUID = pgrrUUID;
	}
	
	public String getPgrrUUID(){
		return pgrrUUID;
	}
	
	public void setProjectName(String projectName){
		this.projectName = projectName;
	}
	
	public String getProjectName(){
		return projectName;
	}
	
	public void setHasOrigBarcode(boolean b){
		hasOrigBarcode = b;
	}
	
	public boolean getHasOrigBarcode(){
		return hasOrigBarcode;
	}
	
	public void setOrigUUID(String origUUID){
		this.origUUID = origUUID;
	}
	
	public String getOrigUUID(){
		return origUUID;
	}
	
	public void setPatientUUID(String patientUUID){
		this.patientUUID = patientUUID;
	}
	
	public String getPatientUUID(){
		return patientUUID;
	}
	
	public void setSampleUUID(String sampleUUID){
		this.sampleUUID = sampleUUID;
	}
	
	public String getSampleUUID(){
		return sampleUUID;
	}
	
	public void setAliquotUUID(String aliquotUUID){
		this.aliquotUUID = aliquotUUID;
	}
	
	public String getAliquotUUID(){
		return aliquotUUID;
	}
	
	public void setFileFractionType(String fileFractionType){
		this.fileFractionType = fileFractionType;
	}
	
	public String getFileFractionType(){
		return fileFractionType;
	}

	public void setDiseaseStudyAbb(String diseaseStudyAbb){
		this.diseaseStudyAbb = diseaseStudyAbb;
	}
	
	public String getDiseaseStudyAbb(){
		if(diseaseStudyAbb ==null)
			diseaseStudyAbb = CodesUtil.getDiseaseAbbFromBarcode(barcode);
		return diseaseStudyAbb;
	}
	
	public void setTssAbb(String tssAbb){
		this.tssAbb = tssAbb;
	}
	
	public String getTssAbb(){
		if(tssAbb == null)
			tssAbb = CodesUtil.getTSSAbb(barcode);
		return tssAbb;
	}
	
	public void setTssName(String tssName){
		this.tssName = tssName;
	}
	
	public String getTssName(){
		if(tssName == null)
			tssName = CodesUtil.getTSSName(barcode);	
		return tssName;
	}
	
	public void setSampleTypeAbb(String sampleTypeAbb){
		this.sampleTypeAbb = sampleTypeAbb;
	}
	
	public String getSampleTypeAbb(){
		if(sampleTypeAbb != null ) return sampleTypeAbb;
		
		if(fractionWithNoSampleBC.contains(fileFractionType) || fileFractionType.indexOf("follow") != -1)
			sampleTypeAbb = "";
		if(sampleTypeAbb == null)
			sampleTypeAbb = CodesUtil.getSampleTypeAbb(barcode);	
		return sampleTypeAbb;
	}
	
	public void setSampleTypeDesc(String sampleTypeDesc){
		this.sampleTypeDesc = sampleTypeDesc;
	}
	
	public String getSampleTypeDesc(){
		if(sampleTypeDesc == null){
			sampleTypeDesc = CodesUtil.getSampleTypeDesc(barcode);	
			if(sampleTypeDesc == null)
				sampleTypeDesc = "";
		}
		return sampleTypeDesc;
	}
	
	public void setSampleAnalyteAbb(String sampleAnalyteAbb){
		this.sampleAnalyteAbb = sampleAnalyteAbb;
	}
	
	public String getSampleAnalyteAbb(){
		if(sampleAnalyteAbb == null)
			sampleAnalyteAbb = CodesUtil.getSampleAnalyteAbb(barcode);	
		return sampleAnalyteAbb;
	}
	
	public void setAnalyteDesc(String analyteDesc){
		this.analyteDesc = analyteDesc;
	}
	
	public String getAnalyteDesc(){
		if(analyteDesc == null){
			analyteDesc = CodesUtil.getAnalyteDesc(barcode);
			if(analyteDesc == null)
				analyteDesc = "";
		}
		return analyteDesc;
	}

	public void setLevel(String level){
		this.level = level;
	}
	
	public String getLevel(){
		if(level == null)
			level = CodesUtil.getLevel(archiveName);
		return level;
	}
	
	
	public void setPatientBarcode(String patientBarcode){
		this.patientBarcode = patientBarcode;
	}
	
	public String getPatientBarcode(){
		if(patientBarcode == null)
			patientBarcode = CodesUtil.getPatientBarcode(barcode);
		return patientBarcode;
	}
	
	public void setSampleBarcode(String sampleBarcode){
		this.sampleBarcode = sampleBarcode;
	}
	
	public String getSampleBarcode(){
		if( sampleBarcode == null && fileFractionType.indexOf("follow") ==-1)
			sampleBarcode = CodesUtil.getSampleBarcode(barcode);
		return sampleBarcode;
	}
	
	/** 
	 * used in clin data only
	 * @param pittPath
	 */
	public void setPittPath(String pittPath){
		this.pittPath = pittPath;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		//workaround for TCGA-04-1347--01A
		// /tumor/ov/bcr/intgen.org/tissue_images/slide_images/intgen.org_OV.tissue_images.Level_1.12.4.0/TCGA-04-1347--01A-01-TS1.bce719cf-462c-418c-8646-c52770efe5e0.svs
		barcode = barcode.replaceAll("--", "-");
		this.barcode = barcode;
	}
	
	public String getTcgaPath() {
		return tcgaPath;
	}

	public void setTcgaPath(String tcgaPath) {
		this.tcgaPath = tcgaPath;
	}

	public String getArchiveName() {
		return archiveName;
	}
	
	/**
	 * 
	 * @return center-exp from archive name
	 */
	public String getCenterExperiment(){
		if(centerExperiment != null)
			return centerExperiment;
		if(!archiveName.endsWith(".0")) {
			centerExperiment = "";
			return centerExperiment;
		}
		int ind = archiveName.lastIndexOf(".", archiveName.length()-3);
		
		//String  exp = archiveName.substring(ind+1,archiveName.lastIndexOf("."));
		//String  center = archiveName.substring(archiveName.lastIndexOf(".",ind-1)+1,ind); 
		
		centerExperiment =  archiveName.substring(archiveName.lastIndexOf(".",ind-1)+1,ind)+"."+archiveName.substring(ind+1,archiveName.lastIndexOf("."));
		return centerExperiment;
		
	}
	
	public void setPortion(String portion) {
		this.portion = portion;
	}

	public String getPortion() {
		return portion;
	}
	

	/** extracts archive name from the tcgaPath
	 * TO DO: check for ending "/"
	 * Used for getLevel() only
	 * ATT: some archives (firehose.org) DO NOT have slashes!
	 * @param tcgaPath
	 */
	public void setArchiveName(String tcgaPath) {
		int ind = tcgaPath.lastIndexOf("/");
		if(ind != -1){
			archiveName = tcgaPath.substring(0,ind);
			archiveName = archiveName.substring(archiveName.lastIndexOf("/")+1);
		} 
	}
	
	public void setArchReason(String reason){
		archReason = reason;
	}
	
	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}
	
	public String getDataType(){
		return dataType;
	}
	
	public void setDataType(String dataType){
		this.dataType = dataType;
	}
	
	public String getDataAccessType(){
		return dataAccessType;
	}
	
	public void setDataAccessType(String dataAccessType){
		this.dataAccessType = dataAccessType;
	}
	
	
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public File getTempoFile() {
		return tempoFile;
	}

	public void setTempoFile(File tempoFile) {
		this.tempoFile = tempoFile;
	}
	
	public String getRefGenome() {
		
		if(refGenome.indexOf("37") != -1 && !refGenome.toLowerCase().startsWith("grch37")){
			refGenome =  "grch37";
		}
		else if (refGenome.equals("19"))
			refGenome =  "hg19";
		
		return refGenome.toLowerCase();
	}

	public void setRefGenome(String refGenome) {
		refGenome = refGenome.replace("##reference=", "");
		this.refGenome = refGenome.toLowerCase();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
	
	public String getRefGenomeSource() {
		return refGenomeSource;
	}

	public void setRefGenomeSource(String refGenomeSource) {
		this.refGenomeSource = refGenomeSource;
	}
	
	public void setCenterName(String n){
		centerName = n;
	}
	
	public String getCenterName(){
		if(centerName.equals("")){
			if(!getCenterCode().equals(""))
				centerName = CodesUtil.getCenterName(barcode);
		}
		return centerName;
	}
	
	public String getFileName(){
		if(!hasOwnFileName)
			return fileNameUpToVersion()+"_V"+version+((fileExtension.equals(""))?"":"."+fileExtension);
		else return ownFileName;
	}
	
	public String getSubType(){
		return subType;
	}

	public void setSubType(String subType){
		this.subType = subType;
	}
	
	/**
	 * without the top dir (skip tcga or archive)
	 * <disease study abb>/<TCGA patient barcode>/<TCGA sample barcode>/<data type>/<center abbr>+<platform>/
	 * @return
	 */
	public String constractPittPath(){	
		return constractPittPath(false);
		
	}
	
	/**
	 * 
	 * @param doConstruct - CNV_Level4 files are in its own dir, while Medatdata.tsv should be in the regular place.
	 * @return
	 */
	public String constractPittPath(boolean doConstruct){
		StringBuilder sb = new StringBuilder("/");
		if(saveInPatientDir) { 
			if(!"".equals(pittPath) && !doConstruct)
				return pittPath;		
			
			sb.append(getDiseaseStudyAbb()+"/");
			sb.append(getPatientBarcode()+"/");
			sb.append(dataType+"/");
			sb.append(centerCode+"_"+platform+"/");

		}
		else {
			if(!"".equals(pittPath) && !doConstruct)
				return pittPath;		
			
			sb.append(getDiseaseStudyAbb()+"/");
			sb.append(getPatientBarcode()+"/");
			if(!"".equals(getSampleBarcode()))
					sb.append(getSampleBarcode()+"/");
			sb.append(dataType+"/");
			sb.append(centerCode+"_"+platform+"/");

		}
		
		return sb.toString();
	}
	
	public String constractAliasPittPath(){	
		if(hasAlias) {
			if(aliasPath != null)
				return aliasPath;
			StringBuilder sb = new StringBuilder("/");
			sb.append(getDiseaseStudyAbb()+"/");
			sb.append(getPatientBarcode()+"/");
			if(!"".equals(getSampleBarcode()))
				sb.append(getSampleBarcode()+"/");
			sb.append(dataType+"/");
			sb.append(centerCode+"_"+platform+"/");
			aliasPath = sb.toString();
			return sb.toString();
		}	
		return null;
		
	}
	
	public String getTCGAFileName(){
		if(tcgaFileName == null)
			tcgaFileName = tcgaPath.substring(tcgaPath.lastIndexOf("/")+1);
		return tcgaFileName;
	}
	
	public void setTCGAFileName(String tcgaFileName){
		this.tcgaFileName =tcgaFileName;
	}
	
	/*public String trimmedBarcode(){
		return barcode.length()<20?barcode:barcode.substring(0,20);
	}*/


	/**Constructs the file name without (version + extension)
	 * <TCGA barcode>_<disease study abb>_<analysis type>_<platform>_<level>_<revision>.<extension>
	 * @return
	 */
	public String fileNameUpToVersion(){
		if(!hasOwnFileName)
			return fileNameUpToPortion()+portion;
		else return ownFileName;
	}
	
	
	public String fileNameUpToPortion(){
		if(!hasOwnFileName){
			StringBuilder sb = new StringBuilder();
			String pref = barcode;
			if(saveInPatientDir)
				pref = getPatientBarcode();
			sb.append(pref+"_");
			if(dataAccessType.equalsIgnoreCase(TCGAModule.PUBLIC))
				sb.append("PUBL_");
			else
				sb.append("CTRL_");
			sb.append(getDiseaseStudyAbb()+"_");
			if("clin".equals(getCenterCode()))
				sb.append("clin_");
			else if(!"".equals(getCenterCode())){
				//sb.append(centerCode+"_");
				if(!"".equals(getCenterName()))
					sb.append(getCenterName()+"_");
				else
					sb.append(CodesUtil.getCenterName(barcode)+"_");
			}
			if(subType == null)
				sb.append(platform+"_");
			else
				sb.append(subType+"_");
			sb.append("Level-"+getLevel()+"."+getCenterExperiment()+"_");
			if(getRefGenome().equalsIgnoreCase("UNKNOWN") || getRefGenome().equalsIgnoreCase("N/A"))
				sb.append("_");
			else
				sb.append(getRefGenome()+"_");
			
			return sb.toString();
		} else return ownFileName;
	}
	
	public String fileNameUpToPortionNum(){
			
		return fileNameUpToPortion()+portionNoEndingNum();
	}
	
	public String portionNoEndingNum(){
		String currPortion = getPortion().replaceAll("_", "");
		String endDigits = TCGAHelper.getMatch(currPortion, endDigitsPattern,0);

		return currPortion.substring(0,
				currPortion.length()-endDigits.length());
		
	}
	
	
	/**
	 * what name the file would have if saved in a sample dir
	 * @return
	 */
	public String possibleAliasFileNameUpToVersion(){
		
		return possibleAliasFileNameUpToPortion()+portion;
	}
	
	
	public String possibleAliasFileNameUpToPortion(){
		StringBuilder sb = new StringBuilder();
		sb.append(barcode+"_");
		if(dataAccessType.equalsIgnoreCase(TCGAModule.PUBLIC))
			sb.append("PUBL_");
		else
			sb.append("CTRL_");
		sb.append(getDiseaseStudyAbb()+"_");
		if("clin".equals(getCenterCode()))
			sb.append("clin_");
		else if(!"".equals(getCenterCode())){
			//sb.append(centerCode+"_");
			if(!"".equals(getCenterName()))
				sb.append(getCenterName()+"_");
			else
				sb.append(CodesUtil.getCenterName(barcode)+"_");
		}
		if(subType == null)
			sb.append(platform+"_");
		else
			sb.append(subType+"_");
		sb.append("Level-"+getLevel()+"."+getCenterExperiment()+"_");
		if(getRefGenome().equalsIgnoreCase("UNKNOWN") || getRefGenome().equalsIgnoreCase("N/A"))
			sb.append("_");
		else
			sb.append(getRefGenome()+"_");		
		return sb.toString();
	}
	
	public String possibleAliasFileNameUpToPortionNum(){
		return possibleAliasFileNameUpToPortion()+portionNoEndingNum();
	}
	
	public void setFileSize(String fileSize){
		this.fileSize = fileSize;
	}
	
	public String getFileSize(){
		if(fileSize == null)
			return String.valueOf(tempoFile.length());
		else return fileSize;
	}
	
	public void setCenterCode(String abb){
		centerCode = abb;
	}
	
	public String getCenterCode(){
		if(centerCode.equals(""))
			centerCode = CodesUtil.getCenterAbb(barcode);
		
		return  centerCode;
	}
	
	public void setPlatform(String p){
		platform = p.toLowerCase();
	}
	
	public String getPlatform(){
		return platform;
	}
	
	public String getChecksum(){
		if(checksum.equals(""))
			try {
				checksum = ModuleUtil.calcCheckSum(new FileInputStream(tempoFile));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				ErrorLog.log("Aliquot tcga: "+getTcgaPath()+getTCGAFileName()+" pgrr: "+constractPittPath()+getFileName()+"  ERR: "+e.getMessage());
			}
		return checksum;
	}
	
	public void setChecksum(String s){
		checksum = s;
	}
	
	/**
	 * full metadata with header
	 * TODO data archived not setting
	 * @return
	 */
	public List<String[]> getMetadataList(boolean withHeader){
		List<String[]> toret = new ArrayList<String[]>();
		if(withHeader)
			toret.add(MySettings.moduleHeader);	
		toret.add(getMetadata());
		return toret;
	}
	
	public String[] getMetadata(){
		String[] metadata = getPartialMetadata();
		metadata[16] = getFileName();
		metadata[17] = version; // no version yet
		metadata[18] = constractPittPath(); // no pitt pass yet
		metadata[22] = archReason;
		metadata[38] = pgrrUUID;
		
		return metadata;
	}
	
	public String getArchiveDirTCGA(){
		if(tcgaArchivePath == null)
			return tcgaPath.substring(0, tcgaPath.lastIndexOf("/")+1);
		else 
			return tcgaArchivePath;
	}
	
	public void setTcgaArchivePath(String tcgaArchivePath){
		this.tcgaArchivePath = tcgaArchivePath;
	}
	
	
	public String[] getPartialMetadata(){
		if(metadata != null)
			return metadata;
		metadata = new String[MySettings.moduleHeader.length];
		metadata[0] = projectName;
		metadata[1] = barcode;//(hasOrigBarcode)?barcode:"";
		metadata[2] = getDiseaseStudyAbb();
		metadata[3] = getTssAbb();
		metadata[4] = getTssName();
		metadata[5] = dataType;
		metadata[6] = platform;
		metadata[7] = getSampleTypeAbb();
		metadata[8] = getSampleTypeDesc();
		metadata[9] = getSampleAnalyteAbb();
		metadata[10] = getAnalyteDesc();
		metadata[11] = getCenterCode();
		metadata[12] = getCenterName();
		metadata[13] = getLevel();
		metadata[14] = fileFractionType; // patient, sample, drug, ... , aliquot
		metadata[15] = fileExtension;		
		metadata[16] = fileNameUpToVersion(); // NOT a full filename //pgrrFileName
		metadata[17] = ""; // no version yet
		metadata[18] = ""; // no pgrr path yet //pgrrPath
		metadata[19] =  getTCGAFileName();//  tcgaFileName; // no version yet
		metadata[20] = (dateCreated == null)?StorageFactory.getStorage().formatNowMetaFile():dateCreated;
		metadata[21] = ""; // no date archived yet
		metadata[22] = ""; // no reason for archiving yet
		metadata[23] = getFileSize();
		metadata[24] = (dataAccessType.equals(TCGAModule.PUBLIC))?"yes":"no"; // does it matter in PGRR?
		metadata[25] = getRefGenome(); // TODO add ref_renome
		metadata[26] = refGenomeSource; // TODO add ref_renome_source
		//add more identifiers
		metadata[27] = getArchiveDirTCGA();//tcga-archive-path
		metadata[28] = (hasOrigBarcode)?getOwnUUID(barcode, origUUID).toLowerCase():"";
		//special case add temporal barcode-uuid for *4.0-nte
		if(fileFractionType.equals("follow-up-v4.0"))
			ModuleUtil.origBarcodeUUIDMap.put(barcode, metadata[28].toLowerCase());
		metadata[29] = ModuleUtil.getUUIDByBarcode(getPatientBarcode()).toLowerCase();
		metadata[30] = (!fractionWithNoSampleBC.contains(fileFractionType)  && fileFractionType.indexOf("follow") == -1)? 
				ModuleUtil.getUUIDByBarcode(CodesUtil.getFullSampleBarcode(barcode)).toLowerCase():"";
		if(fileFractionType.equalsIgnoreCase("aliquot") && dataType.equals("Expression_Protein")) // no alnalyte portion in Exp_Protein
			metadata[31] = ModuleUtil.getUUIDByBarcode(barcode).toLowerCase();//aliquotUUID
		else
			metadata[31] = ModuleUtil.getUUIDByBarcode(CodesUtil.getAliquotBarcode(barcode)).toLowerCase();//aliquotUUID
		metadata[32] = getPatientBarcode();
		metadata[33] = (!fractionWithNoSampleBC.contains(fileFractionType) && fileFractionType.indexOf("follow") == -1)?
				CodesUtil.getSampleBarcode(barcode):"";		
		metadata[34] = (!fractionWithNoSampleBC.contains(fileFractionType) && fileFractionType.indexOf("follow") == -1)?
				CodesUtil.getSampleVial(barcode):"";		
		metadata[35] = CodesUtil.getAliquotBarcode(barcode);	
		try {
			if(checksum.equals(""))
				setChecksum(ModuleUtil.calcCheckSum(new FileInputStream(tempoFile)));
			metadata[36] = getChecksum();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		metadata[37] = fileType;
		
		// skip metadata[38] - pgrrUUID
		
		metadata[39] = algorithmName; // algorithmName
		metadata[40] = algorithmVersion; // algorithmVersion
		return metadata;
	}
	
	private String getOwnUUID(String barcode, String uuidHere){
		if(fileFractionType.equals("clinical-cqcf") || 
				fileFractionType.equals("biospecimen-cqcf") ||
				fileFractionType.startsWith("clinical-nte"))


			return "";
		if(!uuidHere.equals(""))
			return uuidHere;
		String origID_LookedUp = ModuleUtil.getUUIDByBarcode(barcode);

		if (origID_LookedUp == null){
			ErrorLog.log("Aliquot: no UUID found for "+barcode+" fileFractionType: "+fileFractionType+" in experiment: "+tcgaPath);
			return "";
		}
		
		else {
			origUUID = origID_LookedUp.toLowerCase();
			return origID_LookedUp;
		}


	}
	
	
	public void clear(){
		metadata = null;
		tempoFile = null;
	}
	
	
	public void print(){
		List<String[]> md = getMetadataList(true);
		
		String[] header = md.get(0);
		String[] vals = md.get(1);
		int sz = header.length;
		for(int i=0; i<sz; i++)
			System.out.println("i: "+i+"  :: "+header[i]+"   VAL: "+vals[i]);
	}

}
