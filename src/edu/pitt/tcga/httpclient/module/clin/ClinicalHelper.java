package edu.pitt.tcga.httpclient.module.clin;

import java.util.ArrayList;
import java.util.List;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.storage.StorageFactory;

/**
 * Compares set of records in storage with the latest one in TCGA repository for
 * each clinical file for reporting the missing records in the current archive.
 * 
 * @author opm1
 * 
 */
public class ClinicalHelper {

	private static String currDiseaseAbbr = "";
	private static String curTCGAFileName = "";
	private static String curTCGAPath = "";
	private static String[] recordFields = { "barcode", "tcgaarchivepath",
			"pgrruuid" };

	private static List<ClinicalBean> currClinBeans = new ArrayList<ClinicalBean>();

	/**
	 * Orders to recreate a list of ClinicalBeans if there is another disease or
	 * file to downloaded
	 * 
	 * @param disAbbr
	 *            - compares to currDiseaseAbbr
	 * @param tcgaPath
	 *            - sets it as current if needs to recreate list. Used for
	 *            missing records log message.
	 * @param tcgaFileName
	 *            - compares to curTCGAFileName
	 * @return Nothing.
	 */
	public static void setList(String disAbbr, String tcgaPath,
			String tcgaFileName) {
		if (!disAbbr.equals(currDiseaseAbbr)
				|| !tcgaFileName.equals(curTCGAFileName)) {
			reportMissedRecords();
			curTCGAPath = tcgaPath;
			currDiseaseAbbr = disAbbr;
			curTCGAFileName = tcgaFileName;
			createClinList();

		}
	}

	/**
	 * Quering storage to get list of ClinicalBeans for current disease and
	 * clinical file name.
	 * 
	 * @return Nothing.
	 */
	public static void createClinList() {
		currClinBeans.clear();

		Storage storage = StorageFactory.getStorage();
		String q = storage.getStrProperty("CURRENT_CLIN_DATA_BY_DIS_FILE");
		q = q.replaceAll("<diseaseabbr>", currDiseaseAbbr);
		q = q.replaceAll("<tcgafilename>", curTCGAFileName);

		List<String[]> res = storage.resultAsList(q, recordFields);
		// query returns:
		// barcode, tcgaarchivepath, uuid

		for (String[] s : res) {

			ClinicalBean clb = new ClinicalBean(s[0], s[1], s[2]);
			currClinBeans.add(clb);

		}

	}

	/**
	 * Removes ClinicalBean from list by barcode. Non-empty list after file
	 * processing means that some records are missing.
	 * 
	 * @param bc
	 *            - barcode
	 * @return Nothing.
	 */
	public static void checkBarcode(String bc) {
		for (ClinicalBean clb : currClinBeans) {
			if (clb.exists(bc)) {
				currClinBeans.remove(clb);
				break;
			}
		}
	}

	/**
	 * Reports missing records from the current TCGA archive. <b>How to process
	 * these missing records?</b>
	 * 
	 * @return Nothing.
	 */
	public static void reportMissedRecords() {
		String repTempl = "Mising CLINICAL data for disease: "
				+ currDiseaseAbbr + " from curr archive: " + curTCGAPath
				+ " in curr file " + curTCGAFileName;
		for (ClinicalBean clb : currClinBeans) {
			// TODO:
			// archive those records?
			System.err.println(repTempl + clb.toString());
			ErrorLog.log(repTempl + clb.toString());
		}
	}

}
