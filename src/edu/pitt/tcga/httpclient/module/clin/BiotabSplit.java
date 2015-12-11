package edu.pitt.tcga.httpclient.module.clin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.MySettings;

/**
 * Writes clinical data to appropriate patient/sample locations in local
 * repository and creates metadata.
 * 
 * @author opm1
 * @version 1
 * @since Dec 5, 2015
 * 
 */

public class BiotabSplit {

	public static int index = 0;

	private int currRowNum = 0;

	/**
	 * 
	 * @param table
	 *            - Data as a list of arrays with header
	 * @param tcgaUrl
	 *            - data file URL
	 * @param fractionType
	 *            - (example: patient, sample, portion, etc.)
	 * @param ownBarcodeField
	 *            - own barcode field name
	 * @param ownUUIDField
	 *            - own uuid field name
	 * @param parentBacodeField
	 *            - parent barcode field name if any.
	 * @param saveToSampleDir
	 *            - false if needs to save in the patient directory
	 * @param hasOrigBarcode
	 *            - some data types, like clinical-cqcf don't have it.
	 * @return Nothing.
	 */
	public void splitTSV(List<String[]> table, String tcgaUrl,
			String fractionType, String ownBarcodeField, String ownUUIDField,
			String parentBacodeField, boolean saveToSampleDir,
			boolean hasOrigBarcode) {

		ClinicalHelper.setList(CodesUtil.getDiseaseAbbrFromURL(tcgaUrl),
				CodesUtil.getArchivePathFromFile(tcgaUrl),
				CodesUtil.getFileNameFromPath(tcgaUrl));
		boolean isHeader = true;
		currRowNum = 0;
		String origOwnBC = null, origOwnUUID = null, origParentBC = null;

		// get barcode/uuid columns first
		String[] topH = table.get(0);
		int col_parentBC = -1;
		if (parentBacodeField != null)
			col_parentBC = getColumnNum(topH, parentBacodeField, tcgaUrl);
		int col_ownBC = -1;
		if (ownBarcodeField != null)
			col_ownBC = getColumnNum(topH, ownBarcodeField, tcgaUrl);
		int col_ownUUID = -1;
		if (ownUUIDField != null)
			col_ownUUID = getColumnNum(topH, ownUUIDField, tcgaUrl);

		List<String[]> head = new LinkedList<String[]>();
		String parentBC = null;
		String ownBC = null, ownUUID = null;
		int sz = table.size();
		for (String[] sArr : table) {

			if (isHeader)
				head.add(sArr);

			if (!isHeader) {

				if (!TCGAModule.CAN_PROCEED
						&& TCGAModule.START_FROM_RECORD == currRowNum)
					TCGAModule.CAN_PROCEED = true;
				if (TCGAModule.CAN_PROCEED) {
					

					// get sample barcode without vial for local repository path

					parentBC = (col_parentBC == -1) ? "" : sArr[col_parentBC];
					ownBC = (col_ownBC == -1) ? "" : sArr[col_ownBC];
					ownUUID = (col_ownUUID == -1) ? "" : sArr[col_ownUUID];

					origOwnBC = ownBC;
					origOwnUUID = ownUUID;
					origParentBC = parentBC;

					// for special cases of clinical biospecimen-normal-control
					// and tumor-sample
					if (ownBC.equals("") && !ownUUID.equals("")
							&& !parentBC.equals("")) {
						if ("normal-control".equals(fractionType)
								|| "tumor-sample".equals(fractionType)) {

							parentBC = ModuleUtil.getBarcodeByUUIDParentBC(
									ownUUID, parentBC);
							ownUUID = "";
							ownBC = parentBC;
						}
					}
					
					/*System.out.println("BiotabSplit about to process curNum: "
							+ currRowNum + " out of "+sz+" BC: "+ownBC+"  tcgaUrl: " + tcgaUrl);*/		

					// save data file in tempo
					String tempoFileName = MySettings.TEMPO_DIR
							+ String.valueOf(index) + "_" + ownBC + ".txt";
					List<String[]> dList = new LinkedList<String[]>(head);
					dList.add(sArr);

					String saveTo = MySettings.TEMPO_DIR
							+ String.valueOf(index) + "_" + ownBC + ".txt";
					saveTempoDataFile(saveTo, dList);

					Aliquot aliquot = new Aliquot(tcgaUrl,
							ClinicalModule.DATATYPE, "txt", hasOrigBarcode);

					aliquot.setBarcode(ownBC);
					aliquot.setLevel(ClinicalModule.LEVEL);
					aliquot.setTempoFile(new File(saveTo));
					aliquot.setCenterName("clin");
					aliquot.setCenterCode("clin");
					aliquot.setSubType(fractionType);
					aliquot.setFileFractionType(fractionType);
					aliquot.setPlatform("biotab");
					aliquot.setFileType(fractionType);
					// there might be no such uuid OR sample - skip this record
					try {
						if (saveToSampleDir)
							aliquot.setPittPath(getPGRRSamplePath(CodesUtil
									.getSampleBarcode(parentBC)));
						else
							aliquot.setPittPath(getPGRRPatientPath(CodesUtil
									.getPatientBarcode(parentBC)));

						aliquot.setOrigUUID(ownUUID);

						ClinicalHelper.checkBarcode(ownBC);
						ModuleUtil.transferNew(new Aliquot[] { aliquot });
						//ModuleUtil.transfer(new Aliquot[] { aliquot });

					} catch (StringIndexOutOfBoundsException e) {
						// e.printStackTrace();
						ErrorLog.log("BiotabSplit NO such  ownBC: " + origOwnBC
								+ "  uuid: " + origOwnUUID + "  parentBC: "
								+ origParentBC + "  .Error in " + tcgaUrl);
					}

					index++;
					// }// end test
				}
				currRowNum++;
			}

			if (isHeader
					&& (sArr[0].startsWith("CDE_ID") || sArr[0].equals("")))
				isHeader = false;

		}
	}

	/**
	 * Writes extracted data to temporal file
	 * 
	 * @param saveTo
	 *            - file name with path
	 * @param data
	 *            - what to save
	 * @return Nothing.
	 */
	public void saveTempoDataFile(String saveTo, List<String[]> data) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(saveTo, true));
			writer.write(ModuleUtil.listArrayToString(data, "\t"));
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Constructs path to sample directory in a local repository <b>Note:</b>
	 * Sample directory name does not contains vial.
	 * 
	 * @param sample_barcodeNoVial
	 * @return path to sample directory in format
	 *         <disease_abbr>/<patient_barcode>/TCGA-XX-XXXX-XX
	 */
	public String getPGRRSamplePath(String sample_barcodeNoVial) {
		String saveTo = "/"
				+ CodesUtil.getDiseaseAbbFromBarcode(sample_barcodeNoVial)
				+ "/" + CodesUtil.getPatientBarcode(sample_barcodeNoVial) + "/"
				+ sample_barcodeNoVial + "/" + ClinicalModule.LOCAL_CLIN_DIR;
		return saveTo;
	}

	/**
	 * Constructs path to patient directory in a local repository.
	 * 
	 * @param patientBC
	 *            - patient barcode
	 * @return path to patient directory
	 */
	public String getPGRRPatientPath(String patientBC) {
		String saveTo = "/" + CodesUtil.getDiseaseAbbFromBarcode(patientBC)
				+ "/" + CodesUtil.getPatientBarcode(patientBC) + "/"
				+ ClinicalModule.LOCAL_CLIN_DIR;
		return saveTo;
	}

	/**
	 * 
	 * @param header
	 *            - array of Strings, tsv file header
	 * @param colName
	 *            - lookup for this column name
	 * @param tcgaUrl
	 *            - used to log error
	 * @return column position in the header
	 */
	public int getColumnNum(String[] header, String colName, String tcgaUrl) {
		for (int p = 0; p < header.length; p++)
			if (header[p].equalsIgnoreCase(colName))
				return p;
		ErrorLog.log("CLIN BIOTAB: NO such column :" + colName + " in "
				+ tcgaUrl);
		return -1;
	}

}
