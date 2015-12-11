package edu.pitt.tcga.httpclient.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.storage.StorageFactory;

public class PGRRFileVersionHelper {
	
	
	protected static Pattern portionPattern = Pattern.compile("_[a-zA-Z1-9\\-]+_(?!.*_)");
	protected static Pattern endDigitsPattern = Pattern.compile("[1-9]+$");
	
	
	private String diseaseAbbr = "";
	private String analysisType = "";
	private Map<String, PGRR_RecordBean> currRecordsMapBymd5 = new HashMap<String, PGRR_RecordBean> (); //<md5, PGRR_ReocordBean>
	private String[] recordFields = {"tcgaarchivepath","pgrrpath","pgrrfilename","version","fileextension","md5checksum","pgrruuid",
			"fractiontype","refgenomename","level","barcode","filetype"};

	
	private String DATE_FORMAT = "yyyy-MM-dd";
	public SimpleDateFormat dayFormat = new SimpleDateFormat(DATE_FORMAT);
	
			
	
	public void setDiseaseAnalysisType(String dAbb, String aType){
		if(!diseaseAbbr.equals(dAbb) || !analysisType.equals(aType)){
			setDiseaseAbbr(dAbb);
			setAnalysisType(aType);
			uploadNewMap();
			
		}
	}
	
	public List<PGRR_RecordBean> listTCGAArchiveWithCenterID(String tcgaArchiveWithCenterID){

		List<PGRR_RecordBean> list = new ArrayList<PGRR_RecordBean>();
		PGRR_RecordBean currRB = null;
		for (Map.Entry<String, PGRR_RecordBean> entry : currRecordsMapBymd5.entrySet()) {
			currRB = entry.getValue();
			if(currRB.hasTCGAPathWithCenterID(tcgaArchiveWithCenterID))
				list.add(currRB);
		}
		return list;
	}
	
	
	public void setDiseaseAbbr(String diseaseAbbr){
		this.diseaseAbbr =diseaseAbbr;
	}
	
	public String getDiseaseAbbr(){
		return diseaseAbbr;
	}
	
	public void setAnalysisType(String analysisType){
		this.analysisType =analysisType;
	}
	
	public String getAnalysisType(){
		return analysisType;
	}
	
	public void clear(){
		currRecordsMapBymd5.clear();
	}
	
	/**
	 * If there is the file with the same input params, but another md5 return increased portion number
	 * @param tcgaArchivePath
	 * @param pgrrArchivePath
	 * @param pgrrFileNameUpToPortion
	 * @param defaultPortionNum
	 * @param fileExtension
	 * @return
	 */
/*	public String getNextPortionNum(String tcgaArchivePath, String pgrrArchivePath, String pgrrFileNameUpToPortion, 
			String defaultPortionNum, String fileExtension, String md5Sum, String lev, String bc, String refG){
		
		PGRR_RecordBean currRB = null;
		int currPortionNum = 0;
		
		
		for (Map.Entry<String, PGRR_RecordBean> entry : currRecordsMapBymd5.entrySet()) {
			currRB = entry.getValue();
			if(currRB.identifiedUpToPortion(tcgaArchivePath, pgrrArchivePath, pgrrFileNameUpToPortion, fileExtension, lev, bc, refG) && currRB.getCurrPortionNum() > currPortionNum &&
					!currRB.getMd5Sum().equals(md5Sum)){
				currPortionNum = currRB.getCurrPortionNum();
			}
		}

		if(currPortionNum == 0)
			return defaultPortionNum;
		else
			return String.valueOf(currPortionNum+1);
	}*/
	
	/**
	 * 
	 * @param al
	 * @return false - if no need to update
	 */
	public boolean setNextVersionPortion(Aliquot alq){
		
		String nextVersion = "1", nextPortion = "1";	
		String pgrrPath = alq.constractPittPath();

		String md5Sum = alq.getChecksum();
		
		String dir = MySettings.SUPERCELL_HOME + pgrrPath;
		
		// if such md5sum exists:
		// (1) if tcgaPath is different - change the tcgaArchive only
		// (2) if tcgaPath is the same - do nothing
		PGRR_RecordBean rbWithSameMD5 = fileWithSameMD5(md5Sum);
		
		if(rbWithSameMD5 != null)
			return false;		
		List<PGRR_RecordBean> sameCenterRecords = listTCGAArchiveWithCenterID(TCGAHelper.tcgaArchiveWithCenterID(alq.getTcgaPath()));
		// if no such records == no such experiment => create brand new record
		if(sameCenterRecords.size() > 0 ){
			//get all with the same parameters
			List<PGRR_RecordBean> marchedRecords = getMatchedSubset(sameCenterRecords, pgrrPath, alq.getFileFractionType(), 
					alq.getFileExtension(), alq.getLevel(), alq.getBarcode(), alq.getRefGenome(), alq.getFileType());
			
			// if no record in this ==> add brand new record
			if(marchedRecords.size() > 0 ){
				// compare curent experiment num with the one from subset
				if(marchedRecords.get(0).getExpNum() < TCGAHelper.getExperimentNumFromArchive(alq.getTcgaPath())){
					nextVersion = String.valueOf(Integer.valueOf(marchedRecords.get(0).getCurrVersion())+1);
					// archive existing and increase version
					for(PGRR_RecordBean rb:marchedRecords){
						String timeStampStr = StorageFactory.getStorage().formatNowMetaFile();
						ModuleUtil.archiveRecord(dir, "pgrrUUID", rb.getPgrrUUID(), "modified", timeStampStr);
						if(alq.isHasAlias())
							ModuleUtil.archiveMetadata(alq.constractAliasPittPath(), "pgrrUUID", rb.getPgrrUUID(), "modified", timeStampStr);

						currRecordsMapBymd5.remove(rb.getMd5Sum());									
					}
					
				}
				else{ // same current archive, change portion
					nextVersion = String.valueOf(Integer.valueOf(marchedRecords.get(0).getCurrVersion()));
					nextPortion = alq.portionNoEndingNum()+String.valueOf(marchedRecords.size()+1);		
					alq.setPortion(nextPortion);
				}
				
			}
			marchedRecords.clear();
			marchedRecords = null;
			
		}
		sameCenterRecords.clear();
		sameCenterRecords = null;
		
		alq.setVersion(nextVersion);
		
		return true;
			
	}
	
	public void addToMap(String tcgaArchivePath, String pgrrArchivePath, String pgrrFileName, String curVerStr, String fileExtension, 
			String md5Sum, String pgrrUUID, String fileFractionType, String refGenome, String level, String barcode, String fileType){
		PGRR_RecordBean rb = new PGRR_RecordBean(tcgaArchivePath, pgrrArchivePath, pgrrFileName, curVerStr, fileExtension, md5Sum, pgrrUUID, 
				fileFractionType, refGenome, level, barcode, fileType);
		currRecordsMapBymd5.put(md5Sum, rb);
	}
	
	/**
	 * 
	 * @return
	 */
	/*public String getNextVersion(String tcgaArchivePath, String pgrrArchivePath,String fileNameUpToVersion, 
			String fileExtension, String md5Sum, String defaultVersion, String level, String barcode, String refGenome){

		PGRR_RecordBean rb = getRecordWithMaxVersion(tcgaArchivePath, pgrrArchivePath, fileNameUpToVersion, 
		 fileExtension, md5Sum, defaultVersion, level, barcode, refGenome);
		if(rb == null)
			return defaultVersion;
		
		else if(tcgaArchivePath.equals(rb.getTcgaArchivePath())) /// if the same archive is still in process
			return String.valueOf(rb.getCurrVersion());
		else
			return String.valueOf(rb.getCurrVersion() +1);
	}*/
	
	
	// use pgrrFilePath = {barcode, centerName, platform}, refGenome, fractiontype, fileextension, level
	/*public PGRR_RecordBean getRecordWithMaxVersion(String tcgaArchivePath, String pgrrArchivePath, String fileFractionType, 
			String fileExtension, String md5Sum, String defaultVersion, String level, String bc, String refGenome){

		PGRR_RecordBean currRB = null;
		PGRR_RecordBean toret = null;
		int currVersion = 0;
		for (Map.Entry<String, PGRR_RecordBean> entry : currRecordsMapBymd5.entrySet()) {
			currRB = entry.getValue();
			if(currRB.identifiedUpToVersion(tcgaArchivePath,pgrrArchivePath, fileFractionType, fileExtension, level, bc, refGenome) && 
					currRB.getCurrVersion() > currVersion &&
					!currRB.getMd5Sum().equals(md5Sum)){
				currVersion = currRB.getCurrVersion();
				toret = currRB;

			}
		}

		return currRB;
	}*/
	
	/**
	 * for preselected records with the same tcga archive path with identical center ID
	 * @param pgrrArchivePath
	 * @param fileFractionType
	 * @param fileExtension
	 * @param md5Sum
	 * @param defaultVersion
	 * @param level
	 * @param bc
	 * @param refGenome
	 * @return
	 */
	public List<PGRR_RecordBean> getMatchedSubset(List<PGRR_RecordBean> sameCenterIDList, String pgrrArchivePath, String fileFractionType, 
			String fileExtension, String level, String bc, String refGenome, String fileType){
		List<PGRR_RecordBean> toret = new ArrayList<PGRR_RecordBean>();
		for(PGRR_RecordBean rb:sameCenterIDList){
			if(rb.hasSame(pgrrArchivePath, fileFractionType,fileExtension, level, bc, refGenome, fileType))
				toret.add(rb);
		}
		sameCenterIDList.clear();
		sameCenterIDList = null;
		
		return toret;
	}
	
	
	public PGRR_RecordBean fileWithSameMD5(String md5Sum){
		return currRecordsMapBymd5.get(md5Sum);
	}
	
	
	
	private void uploadNewMap(){
		clear();
		
		Storage storage = StorageFactory.getStorage();
		String q = storage.getStrProperty("PGRR_FILE_PORTION");
		q = q.replaceAll("<diseaseAbbr>", diseaseAbbr);
		q = q.replaceAll("<analysisType>", analysisType);
		q = q.replaceAll("<dateTime>", dayFormat.format(new Date()));		
		List<String[]> res = storage.resultAsList(q,recordFields);
		// query returns:
		//tcgaarchivepath, pgrrpath, pgrrfilename, version, fileextension, md5checksum, pgrrUUID, fractiontype, refgenome, level, barcode, filetype
		for(String[] s:res) {
			PGRR_RecordBean pb = new PGRR_RecordBean(s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7], s[8], s[9], s[10], s[11]);
			//by md5sum
			currRecordsMapBymd5.put(s[5], pb);
		}
	}
	
	
	
	
	public static void main(String[] args){
		
		PGRRFileVersionHelper p = new PGRRFileVersionHelper();
		p.setDiseaseAnalysisType("coad", "WXS_(cgHub)");
		
	}

}
