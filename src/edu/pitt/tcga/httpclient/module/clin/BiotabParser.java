package edu.pitt.tcga.httpclient.module.clin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

/**
 * Parses file and keeps track of the missing barcodes from the previous local
 * version.
 * <p>
 * Prints out missing barcodes for further process. I still have no idea how to
 * interpret missing records.
 * <p>
 * <b>Note:</b> No biospecimen_diagnostic_slides yet, since there is no own
 * slide barcode in TCGA, uuid only. And this uuid is not a part of TCGA
 * metadata browser.
 * 
 * @author opm1
 * 
 */
public class BiotabParser {

	private BiotabSplit bioStlit = new BiotabSplit();

	/**
	 * Parses file based on its name. Reports (prints to log and output) missing
	 * barcodes for a particular file.
	 * 
	 * @param levBeans
	 *            - clinical data file.
	 * @return Nothing.
	 */
	public void parse(List<LineBean> levBeans) {
		HttpClient httpclient = TCGAHelper.getHttpClient();
		InputStream is = null;
		CSVReader reader = null;
		List<String[]> data = null;
		String currName = null;
		for (LineBean lb : levBeans) {
			data = null;
			if (!TCGAModule.CAN_PROCEED
					&& lb.getName().equals(TCGAModule.START_FROM_FILE))
				TCGAModule.CAN_PROCEED = true;
			if (TCGAModule.CAN_PROCEED) {
				is = TCGAHelper.getGetResponseInputStream(httpclient,
						lb.getFullURL());

				reader = new CSVReader(new BufferedReader(
						new InputStreamReader(is)), '\t');
				try {
					data = reader.readAllToList();
					is.close();
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (data != null) {
					currName = lb.getName();
					// System.out.println("BiotabParser currName: "+currName);
					if (currName.indexOf("biospecimen_aliquot") != -1) {
						bioStlit.splitTSV(data, lb.getUrl(), "aliquot",
								"bcr_aliquot_barcode", "bcr_aliquot_uuid",
								"bcr_sample_barcode", true, true);
					}

					else if (currName.indexOf("biospecimen_shipment_portion") != -1) {
						bioStlit.splitTSV(data, lb.getUrl(), "shipped_portion",
								"shipment_portion_bcr_aliquot_barcode",
								"bcr_shipment_portion_uuid",
								"bcr_sample_barcode", true, true);
					}

					else if (currName.indexOf("biospecimen_analyte") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "analyte",
								"bcr_analyte_barcode", "bcr_analyte_uuid",
								"bcr_sample_barcode", true, true);
					else if (currName.indexOf("biospecimen_cqcf") != -1)
						bioStlit.splitTSV(data, lb.getUrl(),
								"biospecimen-cqcf", "bcr_patient_barcode",
								null, "bcr_patient_barcode", false, false);
					else if (currName.indexOf("biospecimen_normal_control") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "normal-control",
								null, "bcr_sample_uuid", "bcr_patient_barcode",
								true, true);
					else if (currName.indexOf("biospecimen_portion") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "portion",
								"bcr_portion_barcode", "bcr_portion_uuid",
								"bcr_sample_barcode", true, true);
					else if (currName.indexOf("biospecimen_sample") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "sample",
								"bcr_sample_barcode", "bcr_sample_uuid",
								"bcr_sample_barcode", true, true);
					else if (currName.indexOf("biospecimen_slide") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "slide",
								"bcr_slide_barcode", "bcr_slide_uuid",
								"bcr_sample_barcode", true, true);
					else if (currName.indexOf("biospecimen_tumor_sample") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "tumor-sample",
								null, "bcr_sample_uuid", "bcr_patient_barcode",
								true, true);
					else if (currName.indexOf("clinical_cqcf") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "clinical-cqcf",
								"bcr_patient_barcode", null,
								"bcr_patient_barcode", false, false);
					else if (currName.indexOf("clinical_drug") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "drug",
								"bcr_drug_barcode", "bcr_drug_uuid",
								"bcr_patient_barcode", false, true);
					else if (currName.indexOf("clinical_nte") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "clinical-nte",
								"bcr_patient_barcode", null,
								"bcr_patient_barcode", false, false);

					else if (currName.indexOf("clinical_omf") != -1) {
						bioStlit.splitTSV(data, lb.getUrl(),
								getNameWithVersion(currName, "clinical_omf_v"),
								"bcr_omf_barcode", "bcr_omf_uuid",
								"bcr_patient_barcode", false, true);
					}

					else if (currName.indexOf("clinical_patient") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "patient",
								"bcr_patient_barcode", "bcr_patient_uuid",
								"bcr_patient_barcode", false, true);
					else if (currName.indexOf("clinical_radiation") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "radiation",
								"bcr_radiation_barcode", "bcr_radiation_uuid",
								"bcr_patient_barcode", false, true);
					else if (currName.indexOf("clinical_follow") != -1
							&& currName.indexOf("_nte_") == -1) {
						bioStlit.splitTSV(
								data,
								lb.getUrl(),
								getNameWithVersion(currName,
										"clinical_follow_up_v"),
								"bcr_followup_barcode", "bcr_followup_uuid",
								"bcr_patient_barcode", false, true);
					} else if (currName.indexOf("clinical_follow_up_v4.0_nte") != -1)
						bioStlit.splitTSV(data, lb.getUrl(),
								"follow-up-v4.0-nte", "bcr_followup_barcode",
								null, "bcr_patient_barcode", false, true);
					else if (currName.indexOf("biospecimen_protocol") != -1)
						bioStlit.splitTSV(data, lb.getUrl(), "protocol",
								"bcr_analyte_barcode", null,
								"bcr_sample_barcode", true, true);

				}
				data.clear();
				data = null;
			}

		}
		ClinicalHelper.reportMissedRecords();
		httpclient.getConnectionManager().shutdown();

	}

	/**
	 * Subtracts clinical file type with version number.
	 * 
	 * @param name
	 *            (example:
	 *            nationwidechildrens.org_clinical_follow_up_v4.0_brca.txt)
	 * @param pattern
	 *            (example: clinical_follow_up_v)
	 * @return (example: follow-up-v4.0)
	 */
	public String getNameWithVersion(String name, String pattern) {
		String toret = pattern;
		int st = name.indexOf(pattern);
		int end = name.indexOf("_", st + pattern.length());
		toret = name.substring(st + 9, end);
		toret = toret.replaceAll("_", "-");

		return toret;
	}

}
