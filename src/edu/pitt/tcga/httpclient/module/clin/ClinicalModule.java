package edu.pitt.tcga.httpclient.module.clin;

import java.util.ArrayList;
import java.util.List;

import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

/**
 * Extracts clinical data from TCGA /bio/clin
 * Only Level_2 archives are considered
 * <p>
 * 
 * @author opm1
 * 
 */

public class ClinicalModule extends TCGAModule {

	public static final String LOCAL_CLIN_DIR = "clin/bio/";
	public static final String LEVEL = "2";
	public static final String DATATYPE = "Clinical";
	public static final String PROJECT = "TCGA";

	public static final String PATIENT_TYPE = "patient";
	public static final String SAMPLE_TYPE = "sample";
	public static final String ALIQUOT_TYPE = "aliquot";

	private List<LineBean> level2URL = new ArrayList<LineBean>();

	private String[] endings = { "txt" };

	private BiotabParser bioParser = new BiotabParser();
	
	@Override
	/**
	 * @return Data access type.
	 */
	public String dataAccessType() {
		return TCGAModule.PUBLIC;
	}

	/**  
	 *
	 * @return resourceKey to look up for correct module in knownPlatforms.conf
	 */
	@Override
	public String getResourceKey() {
		return "clinical";
	}

	/**  
	 *
	 * @return array of possible file endings to scrape for the unknown platforms by TCGAModule.unknownPlatformsByDataTypeFileExt method
	 */
	@Override
	public String[] getResourceEndings() {
		return endings;
	}

	
	/**  
	 *
	 * @return "Clinical" data type
	 */
	@Override
	public String getDataType() {
		return DATATYPE;
	}

	/**  
	 *
	 * @return TCGA analysis directory name to look up for correct module in knownPlatforms.conf
	 */
	@Override
	public String getAnalysisDirName() {
		return "clin";
	}

	/**
	 * Pre-process archives to skip "Level_1"
	 * 
	 * @param LineBean archiveBean
	 * @return true if archive name contains ".Level_2." pattern
	 */
	@Override
	public boolean canProcessArchive(LineBean archiveBean) {
		String level = CodesUtil.getLevel(CodesUtil.getArchiveName(archiveBean
				.getFullURL()));
		return level.equals("2");
	}

	
	/**
	 * Collects data files to be downloaded from the TCGA archive list
	 * 
	 * @param List<LineBean> levBeans - list of TCGA archives 
	 * @return Nothing.
	 */
	@Override
	public void processArchiveLevel(List<LineBean> levBeans) {		
		for (LineBean lb : levBeans) {
			level2URL.clear();
			clearTempoDir();
			System.gc();

			List<LineBean> levBeans1 = TCGAHelper.getPageBeans(lb.getFullURL());
			if (!CAN_PROCEED && START_FROM_ARCHIVE.equals(lb.getFullURL()))
				CAN_PROCEED = true;

			//if(canDownload && canProcess(lb.getUrl()) && canProcessArchive(lb)){
			if(CAN_PROCEED && canProcessArchive(lb)){
			
				System.out.println(" *** Clin dir: " + lb.getFullURL());
				// fill in level2URL list
				for (LineBean lb1 : levBeans1) {
					if (acceptableEnding(lb1.getName(), getResourceEndings()))
						level2URL.add(lb1);
				}
			}
			if (level2URL.size() != 0)
				bioParser.parse(level2URL);
		}

	}

	/**
	 * Downloads data from the TCGA archive list.
	 * 
	 * @param List<LineBean> levBeans - list of TCGA data files in one archive 
	 * @return Nothing.
	 */
	@Override
	public void processData(List<LineBean> levBeans) {
		bioParser.parse(level2URL);
	}

	/**
	 * Resumes download from a particular record
	 * 
	 * @param tcgaUrl full URL of the TCGA archive like 
	 *  <b>
	 *  "https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/
	 *  blca/bcr/nationwidechildrens.org/bio/clin/nationwidechildrens.org_BLCA.bio.Level_2.0.43.0/"
	 *  </b>
	 * @param fileName - name of file to be processed
	 * @param startFromNumStr - record number to start download from.
	 * @return Nothing.
	 */
	public void resume(String tcgaArchiveURL, String fileName, String startFromNumStr) {
		setResumeParams(tcgaArchiveURL, fileName, startFromNumStr);
		LineBean lb = new LineBean(tcgaArchiveURL);
		List<LineBean> archLevelBeans = new ArrayList<LineBean>();
		archLevelBeans.add(lb);
		
		processArchiveLevel(archLevelBeans);

	}

	public static void main(String[] args) {

		TCGAExpedition.setUniqueDirs();

		ClinicalModule m = new ClinicalModule();

		// TCGA-AR-A1AR with 2 follow_up_v2.1), 3 drugs, 1 radiation
		String archive = MySettings.PUB_ROOT_URL
				+ "blca/bcr/nationwidechildrens.org/bio/clin/nationwidechildrens.org_BLCA.bio.Level_2.0.43.0/";
		LineBean lb = new LineBean(archive);

		List<LineBean> list = new ArrayList<LineBean>();
		list.add(lb);
		m.processArchiveLevel(list);
		// m.resume(archive,
		// "nationwidechildrens.org_biospecimen_normal_control_blca.txt", "0");
	

		System.out.println("Done in ClinicalModule");
	}

}
