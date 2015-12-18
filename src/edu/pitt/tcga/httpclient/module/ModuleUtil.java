package edu.pitt.tcga.httpclient.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.pitt.tcga.httpclient.correction.MetadataHelper;
import edu.pitt.tcga.httpclient.exception.QueryException;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.storage.StorageFactory;
import edu.pitt.tcga.httpclient.storage.VirtuosoStorage;
import edu.pitt.tcga.httpclient.transfer.LocalShell;
import edu.pitt.tcga.httpclient.transfer.Transfer;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.PGRRFileVersionHelper;
import edu.pitt.tcga.httpclient.util.QueryHelper;
import edu.pitt.tcga.httpclient.util.MySettings;

public class ModuleUtil {
	// SAVE_METADATA = true, Metadata.tsv log files will be created in each
	// repository directory
	// and contain the same info as in pgrr_meta table (Postgres) or pgrr-meta
	// graph (Virtuoso)
	public static boolean SAVE_METADATA = true;

	// <FileName, current portion>
	public static Map<String, Integer> existingFiles = new HashMap<String, Integer>();
	// <FileName, current portion> for V1 Protected_Mutations
	public static Map<String, Integer> possibleFiles = new HashMap<String, Integer>();

	private static MessageDigest md = null;
	public static List<String> additionList = new ArrayList<String>();

	private static Pattern portionPattern = Pattern
			.compile("_[a-zA-Z1-9\\-]+_(?!.*_)");
	private static Pattern endDigitsPattern = Pattern.compile("[1-9]+$");
	// check for file existence
	public static final int NOT_EXISTS = 0;
	public static final int SAME_CHEKCSUM = 1;
	public static final int ANOTHER_CHECKSUM = 2;
	public static final int MUST_DELETE = 3; // file has been deleted from the
												// current version of archive

	// UPMS submission fields
	public static String[] infoArr = { "##INFO=<ID=DB,", "##INFO=<ID=DP,",
			"##INFO=<ID=MQ," };
	public static String[] formatArr = { "##FORMAT=<ID=AD,",
			"##FORMAT=<ID=MQ,", "##FORMAT=<ID=DP," };
	// coluns to extract
	public static int[] cols1 = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
	public static int[] cols2 = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 10 };

	public static List<String> skipIfEmpty = Arrays.asList("dateArchived",
			"reasonArchived");

	public static List<String> tempoV2Dirs = new ArrayList<String>();

	private static PGRRFileVersionHelper versionHelper = null;
	public static Transfer transfer = new LocalShell();

	public static Map<String, String> origBarcodeUUIDMap = StorageFactory
			.getStorage().getMap(StorageFactory
					.getStorage().getStrProperty("TCGA_BARCODE_UUID_Q"), "tcgabarcode", "tcgauuid");
	
	public static List<String> currentArchives = StorageFactory.getStorage()
			.resultAsStrList(
					StorageFactory.getStorage().getStrProperty(
							"CURRENT_ARCHIVIES"), "tcgaarchivepath");
	public static Map<String, String> tssNamePrimary = StorageFactory
			.getStorage().getMap(
					StorageFactory.getStorage().getStrProperty(
							"TSS_NAME_PRIMARY"), "tssName", "tssNamePrimary");
	public static Map<String, String> tssAbbrPrimary = StorageFactory
			.getStorage().getMap(
					StorageFactory.getStorage().getStrProperty(
							"TSS_ABBR_PRIMARY"), "tssAbbr", "tssNamePrimary");
	private static List<String> diseaseAbbrList = StorageFactory.getStorage()
			.resultAsStrList(
					StorageFactory.getStorage().getStrProperty(
							"ALL_DISEASES_ABBR_Q"), "studyabbreviation");

	public static String getBarcodeByUUIDParentBC(String uuid,
			String parentBarcode) {
		String toret = "";
		uuid = uuid.toLowerCase();
		// System.out.println("ModuleUtil.getBarcodeByUUIDParentBC uuid: "+uuid+"  parentBarcode: "+parentBarcode);
		for (Map.Entry<String, String> e : origBarcodeUUIDMap.entrySet()) {
			toret = e.getKey();
			if ((e.getValue()).equals(uuid) && toret.startsWith(parentBarcode))
				return toret;
		}
		return "";
	}

	public static String getBarcodeByUUID(String uuid) {
		String toret = null;
		for (Map.Entry<String, String> e : origBarcodeUUIDMap.entrySet()) {
			if ((e.getValue()).equals(uuid))
				return e.getKey();
		}
		return toret;
	}

	public static List<String> getTCGADiseaseAbbrList() {
		if (!diseaseAbbrList.contains("fppp"))
			diseaseAbbrList.add("fppp");
		return diseaseAbbrList;
	}

	// list of shared metadata predicates
	public static List<String> sharedPredicates = new LinkedList<String>(
			Arrays.asList("patient", "sample", "portion", "analyte", "aliquot"));

	/**
	 * Lookup uuid in the map, if not there - look in TCGA REST If found in
	 * TCGA-REST, add to a in-memory map since there is no clinical data exists.
	 * (example: LUAD)
	 * 
	 * @param barcode
	 * @return
	 */
	public static String getUUIDByBarcode(String barcode) {
		if (barcode == null || barcode.equals(""))
			return "";
		String uuid = origBarcodeUUIDMap.get(barcode);
		if (uuid == null) {
			// System.out.println("  @@@ ModuleUtil.getUUIDByBarcode GOING to mapping barcode: "+barcode);
			uuid = CodesUtil.mapping(CodesUtil.BARCODE_STR, barcode);
			if (uuid != null) {
				addToOrigBarcodeUUIDMap(barcode, uuid.toLowerCase());
			} else
				ErrorLog.log("NO UUID for barcode=" + barcode);
		}
		return (uuid == null) ? "" : uuid.toLowerCase();
	}

	public static void addToOrigBarcodeUUIDMap(String barcode, String uuid) {
		if(barcode != null && !barcode.equals("") && uuid != null 
				&& !uuid.equals("") && !origBarcodeUUIDMap.containsKey(barcode) && !origBarcodeUUIDMap.containsValue(uuid)){
			origBarcodeUUIDMap.put(barcode, uuid.toLowerCase());
	
			// write to storage
			//Storage storage = StorageFactory.getStorage();
			Storage storage = VirtuosoStorage.getInstace();
			String g = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,
					MySettings.TCGA_BC_UUID_GRAPH);
			String subj = storage.nameWithPrefixUUID(
					MySettings.PGRR_PREFIX_URI, uuid);
			String p = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,
					"tcgaBarcode");
			String obj = storage.literal(barcode);
			
			String toWrite = subj + " " + p + " " + obj + " " + g + " .";
			if(Storage.UPDATE_IN_REAL_TIME) {
				writeToStorage(toWrite);
				/*try {
					storage.insert(subj, p, obj, g);
				} catch (QueryException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			} else{
				String fileName = MySettings.NQ_UPLOAD_DIR + MySettings.getDayFormat()
				+ MySettings.TCGA_BC_UUID_NQ_FILE;
				PrintWriter writer = null;
				try {
					writer = new PrintWriter(new FileWriter(fileName,
							true));
					writer.println(toWrite);
					writer.close();
					writer = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}

	/*
	 * public static void changeMetadataField(String dir, String
	 * lookUpFiledName, String lookupValue, String fieldNameToChange, String
	 * changeToValue){ MetadataHelper.correctMetadataOneColumnWithLookup(dir,
	 * lookUpFiledName, lookupValue, fieldNameToChange, changeToValue); }
	 */

	public static PGRRFileVersionHelper updateVersionHelper(String diseaseAbbr,
			String analysisType) {
		if (versionHelper == null)
			versionHelper = new PGRRFileVersionHelper();
		if (!versionHelper.getDiseaseAbbr().equals(diseaseAbbr)
				|| !versionHelper.getAnalysisType().equals(analysisType)) {
			versionHelper.clear();
			versionHelper.setDiseaseAnalysisType(diseaseAbbr, analysisType);
		}
		return versionHelper;
	}

	public static void transferNew(Aliquot[] alqs) {
		updateVersionHelper(alqs[0].getDiseaseStudyAbb(), alqs[0].getDataType());
		int len = alqs.length;
		boolean createNewRecord = false;
		for (int i = 0; i < len; i++) {
			if (alqs[i].getTempoFile() == null
					&& alqs[i].getTcgaFileUrl() != null) {
				File tempoFile = new File(MySettings.TEMPO_DIR + "f"
						+ String.valueOf(TCGAModule.TEMP_FILE_NUM)
						+ alqs[i].getFileExtension());
				try {
					FileUtils
							.copyURLToFile(alqs[i].getTcgaFileUrl(), tempoFile);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				TCGAModule.TEMP_FILE_NUM++;
				alqs[i].setTempoFile(tempoFile);
				alqs[i].setTcgaFileUrl(null);
			}
			createNewRecord = versionHelper.setNextVersionPortion(alqs[i]);

			if (createNewRecord)
				saveAll(alqs[i],
						MySettings.SUPERCELL_HOME + alqs[i].constractPittPath(),
						0, versionHelper);

			alqs[i].clear();

		} // for(int i=0; i<len; i++)
		alqs = null;
		System.gc();

	}

	public static void archiveMetadata(String metadataDir,
			String pgrrUUIDColName, String pgrrUUIDVal, String reasonArchived,
			String timeStampStr) {
		if(!SAVE_METADATA) return;
		
		// pgrrUUIDVal might be like
		// http://purl.org/pgrr/core#0787fbc2-76f8-417c-bb6a-089395c5d956
		if (pgrrUUIDVal.startsWith(MySettings.PGRR_PREFIX_URI))
			pgrrUUIDVal = pgrrUUIDVal.substring(MySettings.PGRR_PREFIX_URI
					.length());
			MetadataHelper.correctMetadata(metadataDir, new String[] {
					pgrrUUIDColName, pgrrUUIDColName }, new String[] {
					pgrrUUIDVal, pgrrUUIDVal }, new String[] { "dateArchived",
					"reasonArchived" }, new String[] { timeStampStr,
					reasonArchived });
	}

	public static void archiveRecord(String metadataDir,
			String pgrrUUIDColName, String pgrrUUIDVal, String reasonArchived,
			String timeStampStr) {
		// write to Metadata
		try {

			archiveMetadata(metadataDir, pgrrUUIDColName, pgrrUUIDVal,
					reasonArchived, timeStampStr);
			writeNQArchive(pgrrUUIDVal, reasonArchived, timeStampStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeNQArchive(String pgrrUUID, String reasonArchived,
			String timeStampStr) {

		String fileName = MySettings.NQ_UPLOAD_DIR + MySettings.getDayFormat()
				+ MySettings.PGRR_META_NQ_FILE;

		// use here the default storage, since upload to DB is done through the
		// tempo table which handles NQ RDFrepresentation
		
		Storage storage = VirtuosoStorage.getInstace();

		String g = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,
				MySettings.PGRR_META_GRAPH);
		String subj = storage.nameWithPrefixUUID(MySettings.PGRR_PREFIX_URI,
				pgrrUUID);
		String p = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,
				"dateArchived");

		String archDate = formatIfTime(timeStampStr, storage);

		String pR = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,
				"reasonArchived");
		String toWriteDate = subj + " " + p + " " + archDate + " " + g + " .";
		String toWriteReason =  subj + " " + pR +  " " +storage.literal(reasonArchived) + " " + g
				+ " .";

		try {
			if (!Storage.UPDATE_IN_REAL_TIME) {
				PrintWriter writer = new PrintWriter(new FileWriter(fileName,
						true));
				writer.println(toWriteDate);
				writer.println(toWriteReason);

				writer.close();
				writer = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeToStorage(toWriteDate);
		writeToStorage(toWriteReason);

	}

	private static void saveAll(Aliquot alq, String dir, int mode,
			PGRRFileVersionHelper versionHelper) {
		System.out.println("SAVING new: " + alq.getBarcode() + " disease: "
				+ alq.getDiseaseStudyAbb() + " tcgaFileName: "
				+ alq.getTCGAFileName());
		if (alq.isHasTempoFile()) {
			if (alq.getTcgaFileUrl() != null)
				transfer.copyURLToServer(dir, alq.getTcgaFileUrl(),
						alq.getFileName(), 0);

			else
				transfer.moveToServer(dir, alq.getTempoFile(),
						alq.getFileName(), mode);
			alq.setTempoFile(new File(dir + alq.getFileName()));
		}
		saveMetaData(alq, dir, versionHelper);
	}

	private static void saveAll(Aliquot alq, String dir, int mode) {
		// write data ChannelSftp.OVERWRITE == 0 - for FrankShell
		if (alq.isHasTempoFile()) {
			if (alq.getTcgaFileUrl() != null)
				transfer.copyURLToServer(dir, alq.getTcgaFileUrl(),
						alq.getFileName(), 0);

			else
				transfer.moveToServer(dir, alq.getTempoFile(),
						alq.getFileName(), mode);
			alq.setTempoFile(new File(dir + alq.getFileName()));
		}
		saveMetaData(alq, dir);
	}

	public static void saveMetaData(Aliquot alq, String dir,
			PGRRFileVersionHelper versionHelper) {
		updateVersionHelper(alq.getDiseaseStudyAbb(), alq.getDataType());
		// generate uuid
		String pgrrUUID = UUID.randomUUID().toString();
		versionHelper.addToMap(alq.getTcgaPath(), alq.constractPittPath(),
				alq.getFileName(), alq.getVersion(), alq.getFileExtension(),
				alq.getChecksum(), pgrrUUID, alq.getFileFractionType(),
				alq.getRefGenome(), alq.getLevel(), alq.getBarcode(),
				alq.getFileType());

		saveCurrentMetadata(alq, dir, pgrrUUID.toString());
	}

	public static void saveMetaData(Aliquot alq, String dir) {
		saveCurrentMetadata(alq, dir, UUID.randomUUID().toString());
	}

	public static void saveCurrentMetadata(Aliquot alq, String dir,
			String pgrrUUID) {
		alq.setPgrrUUID(pgrrUUID);
		List<String[]> metadata = alq.getMetadataList(true);

		List<String[]> aliasMetadata = new LinkedList<String[]>();
		if (alq.isHasAlias())
			aliasMetadata.addAll(metadata);
		// write metadata nq

		writePGRR_Nq(metadata, pgrrUUID, alq);
		// write metadata.tsv file
		if (SAVE_METADATA) {

			transfer.writeToTSVFile(metadata, dir, MySettings.METADATA_FILE);
			// if has alias save metadata in sample dir too and create soft link
			if (alq.isHasAlias()) {
				String aliasFilePath = alq.constractAliasPittPath();
				transfer.writeToTSVFile(aliasMetadata,
						MySettings.SUPERCELL_HOME + aliasFilePath,
						MySettings.METADATA_FILE);
				// save soft link if not exists
				/*
				 * String softDir = MySettings.SUPERCELL_HOME + aliasFilePath;
				 * String softLink = softDir + alq.getFileName();
				 * if(transfer.checkFileExists(softDir, alq.getFileName())){
				 * transfer.execBash("rm -rf \""+ softLink+"\"",false); }
				 * if(!transfer.checkFileExists(softDir, alq.getFileName())){
				 * 
				 * String hardCopy = MySettings.SUPERCELL_HOME +
				 * alq.constractPittPath()+alq.getFileName();
				 * transfer.execBash("ln -s \""
				 * +hardCopy+"\" \""+softLink+"\"",false); }
				 */
			}
		}
		aliasMetadata.clear();
		aliasMetadata = null;
	}

	/**
	 * 
	 * @param metadata
	 *            <header[], values[]>
	 */
	public static void writePGRR_Nq(List<String[]> metadata, String pgrrUUID,
			Aliquot alq) {

		try {
			// use here the default storage
			Storage storage = VirtuosoStorage.getInstace();

			String fileName = MySettings.NQ_UPLOAD_DIR
					+ MySettings.getDayFormat() + MySettings.PGRR_META_NQ_FILE;
			PrintWriter writer = null;
			if (!Storage.UPDATE_IN_REAL_TIME)
				writer = new PrintWriter(new FileWriter(fileName, true));
			String g = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,
					MySettings.PGRR_META_GRAPH);
			String subj = storage.nameWithPrefixUUID(
					MySettings.PGRR_PREFIX_URI, pgrrUUID);

			String[] predicates = metadata.get(0);
			String[] values = metadata.get(1);
			int rowLen = metadata.get(0).length;
			String p = null, o = null;
			String toWrite = "";
			for (int i = 0; i < rowLen; i++) {
				p = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,
						predicates[i]);
				o = null;
				if ((values[i] != null && !values[i].equals(""))) {
					o = formatIfTime(values[i], storage);
					if (o != null) {
						toWrite = subj + " " + p + " " + o + " " + g + " .";
						if (!Storage.UPDATE_IN_REAL_TIME)
							writer.println(toWrite);

						writeToStorage(toWrite);
					}
				}

			}

			if (alq.getSequenceSource() != null) {
				toWrite = subj
						+ " "
						+ storage.nameWithPrefixPorG(
								MySettings.PGRR_PREFIX_URI, "sequencesource")+ " "
						+ storage.literal(alq.getSequenceSource()) + " " + g + " .";
				if (!Storage.UPDATE_IN_REAL_TIME)
					writer.println(toWrite);
				writeToStorage(toWrite);
			}

			if (writer != null) {
				writer.close();
				writer = null;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void writeToStorage(String nqStr) {
		if (Storage.UPDATE_IN_REAL_TIME) {
			Storage storage = StorageFactory.getStorage();
			String[] uplArr = storage.splitVQString(nqStr);
			try {
				storage.insert(uplArr[0], uplArr[1], uplArr[2], uplArr[3]);
			} catch (QueryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * For compatibility with rdf_db lib
	 * 
	 * @param object
	 * @return object = "\""+object+"\"" if not Time or
	 *         "2015-05-11 16:59:53"^^<http
	 *         ://www.w3.org/2001/XMLSchema#dateTime>";
	 */
	private static String formatIfTime(String object, Storage storage) {
		if (object == null)
			return null;

		SimpleDateFormat sdf = Storage.getDateTimeFormat();
		sdf.setLenient(false);
		String localObj = object.replace("T", " ");
		localObj = localObj.replace("Z", "");
		try {

			// if not valid, it will throw ParseException
			Date date = sdf.parse(localObj);

		} catch (ParseException e) {
			return storage.literal(object);
		}

		object = "\"" + object
				+ "\"^^<http://www.w3.org/2001/XMLSchema#dateTime>";
		return object;
	}

	/*
	 * copy specific columns of one-dimensional array to a delimited string
	 */
	public static String copyPartArrayToStr(String[] src, int[] cols, String del) {
		StringBuilder sb = new StringBuilder();
		// System.out.println("src sz: "+src.length+"  src: "+Arrays.asList(src));
		for (int c : cols) {
			sb.append(src[c] + del);
		}
		String toret = sb.toString();
		toret = toret.substring(0, toret.length() - del.length()) + "\n";

		src = null;
		return toret;
	}

	public static String copyArrayToStr(String[] src, String del) {
		StringBuilder sb = new StringBuilder();
		for (String s : src)
			sb.append(s + del);
		String toret = sb.toString();
		toret = toret.substring(0, toret.length() - del.length()) + "\n";
		src = null;
		return toret;
	}

	public static String listArrayToString(List<String[]> src, String del) {
		StringBuilder sb = new StringBuilder();
		for (String[] sArr : src)
			sb.append(copyArrayToStr(sArr, del));
		src.clear();
		src = null;
		return sb.toString();
	}

	/**
	 * do not delete the source file!
	 * 
	 */
	public static String listArrayToStringNoSrcDel(List<String[]> src,
			String del) {
		StringBuilder sb = new StringBuilder();
		for (String[] sArr : src)
			sb.append(copyArrayToStr(sArr, del));
		return sb.toString();
	}

	public static String listOfStrToString(List<String> src, String del) {
		StringBuilder sb = new StringBuilder();
		for (String s : src)
			sb.append(s + del);
		src.clear();
		src = null;
		return sb.toString();
	}

	public static String listOfStrToString(List<String> src, String del,
			boolean hasDelAtEdn) {

		StringBuilder sb = new StringBuilder();
		for (String s : src)
			sb.append(s + del);
		if (!hasDelAtEdn)
			sb.setLength(sb.length() - del.length());
		src.clear();
		src = null;
		return sb.toString();
	}

	public static boolean compareFiles(String path1, String path2) {
		boolean toret = true;

		try {
			BufferedReader br1 = new BufferedReader(new FileReader(path2));
			BufferedReader br2 = new BufferedReader(new FileReader(path1));

			String strLine1 = br1.readLine(), strLine2 = br2.readLine();

			while (strLine1 != null && strLine2 != null) {
				if (!strLine1.equals(strLine2)) {
					toret = false;
					break;
				}
				strLine1 = br1.readLine();
				strLine2 = br2.readLine();
			}

			if (toret
					&& ((strLine1 != null && strLine2 == null) || (strLine1 == null && strLine2 != null)))
				toret = false;

			br1.close();
			br2.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return toret;
	}

	/**
	 * File name is not taken into account
	 * 
	 * @param fName
	 * @return
	 */
	public static String calcCheckSum(String fName) {
		FileInputStream in = null;

		try {
			in = new FileInputStream(fName);
			return calcCheckSum(in);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Calculates checksum using MD5 algorithm
	 * 
	 * @param in
	 * @return
	 */
	public static String calcCheckSum(InputStream in) {
		String toret = null;

		try {
			MessageDigest mdd = getMessageDigest();
			// byte[] dataBytes = new byte[1024];
			byte[] dataBytes = new byte[8192];

			int nread = 0;
			while ((nread = in.read(dataBytes)) != -1) {
				mdd.update(dataBytes, 0, nread);
			}
			;
			byte[] mdbytes = mdd.digest();
			in.close();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++)
				hexString.append(Integer.toHexString(0xFF & mdbytes[i]));

			toret = hexString.toString();

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// apache - slow
		/*
		 * try{ toret = DigestUtils.md5Hex(in); }catch (IOException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 */

		return toret;
	}

	public static MessageDigest getMessageDigest() {
		if (md == null)
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return md;
	}

	public static TCGAModule getModule(String url) {
		Map<String, String> map = MySettings.getModuleMap();
		for (String key : map.keySet()) {
			if (url.contains(key)) {
				try {
					Class cl = Class.forName(map.get(key));
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

			}

		}
		return null;
	}

	public static void main(String[] args) {

	}

}
