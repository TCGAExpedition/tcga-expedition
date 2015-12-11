package edu.pitt.tcga.httpclient.util;

import java.util.regex.Pattern;

import edu.pitt.tcga.httpclient.log.ErrorLog;

public class PGRR_RecordBean {
	
	private String barcode = "";
	private String tcgaArchivePath = "";
	private String pgrrFileName = "";
	private String fileNameUpToPortionNum = "";
	private String fileNameUpToVersion = "";
	private String fileFractionType = "";
	private String fileType = "";
	private String refGenome = "";
	private String pgrrArchivePath = "";
	private String fileExtension = "";
	private String md5Sum = "";
	private String pgrrUUID = "";
	private String level = "";
	
	private String tcgaUpPathUpToCenter = "";
	private String tcgaArchiveWithCenterID = "";
	private int expCenterID = -1;
	private int expNum = -1;
	
	private Integer currPortionNum = 1;
	private Integer currVersion = 1;
	
	
	public PGRR_RecordBean(String tcgaArchivePath, String pgrrArchivePath, String pgrrFileName,String curVerStr, String fileExtension, 
			String md5Sum, String pgrrUUID,  String fileFractionType, String refGenome, String level, String barcode, String fileType){
		this.tcgaArchivePath = tcgaArchivePath;
		int[] res = TCGAHelper.getCenterExperimentFromArchive(tcgaArchivePath);
		if(res != null){
			expCenterID = res[0];
			expNum = res[1];
		}
		this.pgrrArchivePath = pgrrArchivePath;
		this.refGenome = (refGenome == null)?"":refGenome;
		this.fileExtension = fileExtension;
		this.fileFractionType = fileFractionType;
		this.md5Sum = md5Sum;
		this.pgrrUUID = pgrrUUID;
		this.currVersion = Integer.valueOf(curVerStr);
		this.pgrrFileName = pgrrFileName;
		this.level = level;
		this.tcgaUpPathUpToCenter = TCGAHelper.tcgaArchiveUpToCenter(tcgaArchivePath);
		this.tcgaArchiveWithCenterID = TCGAHelper.tcgaArchiveWithCenterID(tcgaArchivePath);
		this.barcode = barcode;
		this.fileType = fileType;
		//setFileNameUpToPortionNum(pgrrFileName);	
		//setFileNameUpToVersion(pgrrFileName);
	}
	
	public int getExpCenterID(){
		return expCenterID;
	}
	public int getExpNum(){
		return 	expNum;
	}
	
	public String getBarcode(){
		return barcode;
	}
	public void setBarcode(String barcode){
		this.barcode = barcode;
	}
	
	public void setFileType(String fileType){
		this.fileType = fileType;
	}
	
	public String getFileType(){
		return fileType;
	}


	public String getTcgaArchivePath() {
		return tcgaArchivePath;
	}

	public void setTcgaArchivePath(String tcgaArchivePath) {
		this.tcgaArchivePath = tcgaArchivePath;
	}
	
	public String getMd5Sum() {
		return md5Sum;
	}

	public void setMd5Sum(String md5Sum) {
		this.md5Sum = md5Sum;
	}
	
	public String getPgrrUUID() {
		return pgrrUUID;
	}

	public void setPgrrUUID(String pgrrUUID) {
		this.pgrrUUID = pgrrUUID;
	}


	public String getFileNameUpToPortionNum() {
		return fileNameUpToPortionNum;
	}
	
	public boolean hasTCGAPathWithCenterID(String otherPathWithCenterID){
		return otherPathWithCenterID.equals(tcgaArchiveWithCenterID);
	}

	/**
	 * Example 1: "TCGA-A6-2670-01A-01-BS1_PUBL_coad_clin_slide_Level-2.0.1__1_V1.txt"
	 * in old Ver ".0.1" <expID.expNum> is mised
	 * Example 2: "TCGA-A6-2670-01A-01-BS1_PUBL_coad_clin_slide_Level-2__1_V1.txt"
	 * @param 
	 */
	public void setFileNameUpToPortionNum(String pgrrFileName) {

		if(pgrrFileName.indexOf("_Level-") != -1){
			fileNameUpToPortionNum = pgrrFileName.substring(0, pgrrFileName.lastIndexOf("_"));
			String currPortionStr = fileNameUpToPortionNum.substring(fileNameUpToPortionNum.lastIndexOf("_"), fileNameUpToPortionNum.length());
			//String currPortionStr = TCGAHelper.getMatch(pgrrFileName, PGRRFileVersionHelper.portionPattern, 0);
			//currPortionStr = currPortionStr.replaceAll("_", "");
			
			setCurrPortionNum(currPortionStr);

			if(fileNameUpToPortionNum.endsWith(String.valueOf(currPortionNum)))
				fileNameUpToPortionNum = fileNameUpToPortionNum.substring(0, fileNameUpToPortionNum.lastIndexOf(String.valueOf(currPortionNum)));
			
			
		} else if(pgrrFileName.startsWith(MySettings.CGHUB_PATH)){
			fileNameUpToPortionNum = pgrrFileName.substring(0, pgrrFileName.indexOf("."));
		}
		else {
			ErrorLog.log("PGRR_RecordBean.setFileNameUpToPortionNum: Can't get portion for pgrrArchivePath: "+pgrrArchivePath+" pgrrFileName: "+pgrrFileName);
			fileNameUpToPortionNum = pgrrFileName.substring(0, pgrrFileName.indexOf("."));
		}
	}
	
	public void setFileNameUpToVersion(String fName){
		fileNameUpToVersion = fName.substring(0, fName.lastIndexOf("_"));
	}
	
	public boolean hasSame(String pgrrPath, String fFractionType, String fExtension, String lev, String bc, String refG, String fType){
		return(barcode.equals(bc) && pgrrArchivePath.equals(pgrrPath) && 
				fileFractionType.equals(fFractionType) && fileExtension.equals(fExtension) && level.equals(lev) && refGenome.equals(refG) && fileType.equals(fType));
	}
	
	
	public boolean identifiedUpToPortion(String tcgaPath, String pgrrPath, String pgrrfnUpToPortion, String fExtension, String lev, String bc, String refG){
		refG =  (refG == null)?"":refG;
		return(barcode.equals(bc) && tcgaArchivePath.equals(tcgaPath) && pgrrArchivePath.equals(pgrrPath) && 
				fileNameUpToPortionNum.equals(pgrrfnUpToPortion) && fileExtension.equals(fExtension) && level.equals(lev) && refGenome.equals(refG));
	}
	
	public boolean identifiedUpToVersion(String tcgaPath, String pgrrPath, String fractionType, String fExtension, String lev, 
			String bc, String refG){
		if(!pgrrArchivePath.equals(pgrrPath)) return false;
		refG =  (refG == null)?"":refG;
		return(barcode.equals(bc) && fileFractionType.equals(fractionType) && fileExtension.equals(fExtension) && level.equals(lev) && refGenome.equals(refG));
	}
	
	public String getPgrrArchivePath() {
		return pgrrArchivePath;
	}

	public void setPgrrArchivePath(String pgrrArchivePath) {
		this.pgrrArchivePath = pgrrArchivePath;
	}

	public String getPgrrFileName() {
		return pgrrFileName;
	}

	public void setPgrrFileName(String pgrrFileName) {
		this.pgrrFileName = pgrrFileName;
	}

	public Integer getCurrPortionNum() {
		return currPortionNum;
	}

	public void setCurrPortionNum(String  fileNameWithPortion) {
		String endDigits = TCGAHelper.getMatch(fileNameWithPortion, PGRRFileVersionHelper.endDigitsPattern,0);
		if(!endDigits.equals("")) {// number at the end
			try{
				this.currPortionNum = Integer.valueOf(endDigits);
			} catch (NumberFormatException e){
				System.err.println("ModuleUtil endDigits: "+endDigits);
				e.printStackTrace();
			}
		}
	}

	public Integer getCurrVersion() {
		return currVersion;
	}

	public void setCurrVersion(Integer currVersion) {
		this.currVersion = currVersion;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//PGRR_RecordBean rb = new PGRR_RecordBean("", "","TCGA-A6-2670-01A-01-BS1_PUBL_coad_clin_slide_Level-2__abcd33_V1.txt", "2", "txt", "","");
		//PGRR_RecordBean rb = new PGRR_RecordBean("", "", "TCGA-AY-4071-10A-01W-1073-09_IlluminaGA-DNASeq_exome.bam_HOLD_QC_PENDING", "1", "txt", "","");
		//System.out.println("getFileNameUpToPortionNum: "+rb.getFileNameUpToPortionNum()+"  currPortion: *"+rb.getCurrPortionNum()+"*");

	}

}
