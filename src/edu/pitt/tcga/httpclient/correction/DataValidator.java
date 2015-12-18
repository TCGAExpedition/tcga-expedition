package edu.pitt.tcga.httpclient.correction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.exception.QueryException;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.bam.PreprocessRequest;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.storage.StorageFactory;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class DataValidator {

	// check multiple uuids for one barcode
	/**
	 * Check multiple pgrr_meta.uuids for a single barcode Look for correct uuid
	 * for a barcode using TCGA REST service If found => correct pgrr_meta.uuid
	 * if found If not => set pgrr_meta.uuid="";
	 * 
	 */
	public static void checkMultipleOrigUUIDS() {
		Storage storage = StorageFactory.getStorage();
		// topKey =pgrruuid, innerKey = pgrr.origuuid, innerValue = barcode
		Map<String, Map<String, String>> resMap = storage.getMapOfMap(storage.getStrProperty("GET_BC_MULTI_ORIGUUID"),
				"pgrruuid", "origuuid", "barcode");
		String graph = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI, MySettings.PGRR_META_GRAPH);
		String p = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI, "origUUID");
		String subj = null, oldO = null, newO = null;
		String pgrrUUID = null, pgrrOrigUID = null;
		int count = 0;
		int totalSz = resMap.size();
		// check id in TCGA REST Service
		for (Map.Entry<String, Map<String, String>> entry : resMap.entrySet()) {
			pgrrUUID = entry.getKey();
			count++;
			for (Map.Entry<String, String> innerEntry : entry.getValue().entrySet()) {

				try {
					String uuid = CodesUtil.doMapIt(CodesUtil.BARCODE_STR, innerEntry.getValue());
					pgrrOrigUID = innerEntry.getKey();
					if (uuid != null && !uuid.equals("") && pgrrOrigUID != null && !pgrrOrigUID.equals("")
							&& !pgrrOrigUID.equals(uuid)) {
						// correct pgrr_meta.uuid

						// get origuuid here
						subj = storage.nameWithPrefixUUID(MySettings.PGRR_PREFIX_URI, pgrrUUID);
						oldO = storage.literal(pgrrOrigUID);
						newO = storage.literal(uuid);
						System.out.println("*** count = " + count + " out of " + totalSz
								+ "  NEED to Modify prigUUID for BC:  " + innerEntry.getValue() + " pgrrUUID: "
								+ pgrrUUID + "   pgrrOrigUID: " + pgrrOrigUID + " new pgrrUUID: " + uuid);

						try {
							StorageFactory.getStorage().modify(subj, p, oldO, subj, p, newO, graph);

						} catch (QueryException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					Thread.sleep(CodesUtil.REST_TCGA_SLEEP);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Goes through the latest cgHub manifest file and archives data if status
	 * is not 'live' anymore for the downloaded files. Looks up for files by
	 * 'checksum' column.
	 */
	public static void checkBAMDataAlive() {
		List<String> existingAnalysIDList = StorageFactory.getStorage()
				.resultAsStrList(StorageFactory.getStorage().getStrProperty("EXISTING_BAM_ANALYSI_IDS"), "origuuid");
		HttpClient httpclient = TCGAHelper.getHttpClient();
		InputStream is = TCGAHelper.getGetResponseInputStream(httpclient, PreprocessRequest.latestManifestURL);

		CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(is)), '\t');
		int analysisID_col = -1, state_col = -1, reason_col = -1;
		boolean isHeader = true;
		Map<String, String> deleteID_ReasonMap = new HashMap<String, String>();
		int count = 0;
		String[] readLine = null;
		try {
			while ((readLine = reader.readNext()) != null) {
				if (String.valueOf(count).endsWith("0000"))
					System.out.println("Processed " + count + " lines");
				count++;
				if (isHeader) {
					isHeader = false;
					// set col numbers used for check
					reason_col = ArrayUtils.indexOf(readLine, "reason");
					analysisID_col = ArrayUtils.indexOf(readLine, "analysis_id");

					state_col = ArrayUtils.indexOf(readLine, "state"); // look
																		// for
																		// "Suppressed"

				} else if (existingAnalysIDList.contains(readLine[analysisID_col].toLowerCase())
						&& !readLine[state_col].equalsIgnoreCase("LIVE")) {
					deleteID_ReasonMap.put(readLine[analysisID_col].toLowerCase(),
							readLine[state_col] + ": " + readLine[reason_col]);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				reader.close();
				if (is != null)
					is.close();
				if (httpclient != null)
					httpclient.getConnectionManager().shutdown();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		
		archiveBAMFiles(deleteID_ReasonMap);

	}
	
	/**
	 * 
	 * @param toDeleteMap <String: oridUUID (which is analysis_id in cgHub),String: reason to archive> 
	 */
	public static void archiveBAMFiles(Map<String, String> toDeleteMap){
		
		for (Map.Entry<String, String> entryToDel : toDeleteMap.entrySet()) {
		 System.out.println("*** NEED to archive BAM metadata recodr with origuuid: "+entryToDel.getKey()+"    reason: "+entryToDel.getValue());
		}
		
		Storage storage = StorageFactory.getStorage();
		String qTempl = storage.getStrProperty("UUID_BY_ORIGUUID");
		String q = null;
		List<String> res = null;
		for(Map.Entry<String, String> entry:toDeleteMap.entrySet()){
			q = qTempl.replace("<origuuid>", entry.getKey());
			res = storage.resultAsStrList(q,"pgrruuid");
			ModuleUtil.writeNQArchive(res.get(0), entry.getValue(), storage.formatNowMetaFile());
			res.clear();
		}
		
		res = null;
		toDeleteMap.clear();
		toDeleteMap = null;
	}

	public static void main(String[] args) {
		TCGAExpedition.createDirs();
		//checkMultipleOrigUUIDS();
		checkBAMDataAlive();
		System.out.println("Done DataValidator");
	}

}
