package edu.pitt.tcga.httpclient.module.ep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.module.cna.HG_CGH_CNA;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.ReferenceGenomeUtil;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

/**
 * 
 * @author opm1
 * 
 */

public class ExpProtein_Module extends TCGAModule {
	/**
	 * 
	 * Level 0 AND Level 1 go to
	 * <diseaseAbbbr>/<analysistype>/<centerCode>_<platform>/<tcga_archveName>/
	 * 
	 * uses UUID for aliquot ident. Col "Sample Name" - has UPPER CASE = > MUST
	 * do toLowerCase()
	 * 
	 * "N/A" for refGenome
	 * 
	 * Level 0: (txt) mdanderson.org_ACC.MDA_RPPA_Core.array_design.txt
	 * (FileType = MDA_RPPA Slide Design, col "Array Design File") - ATTENTION:
	 * can be multiple designs (see BRCA)), col "Array Design File" (txt)
	 * mdanderson.org_ACC.MDA_RPPA_Core.antibody_annotation.txt (FileType =
	 * Antibody Annotations), col "Annotations File"
	 * 
	 * Level1: (raw) {.tif} - "Image File" (AF) - (FileType = Array Slide Image)
	 * {.txt} - "Array Data File" (AN) (FileType = RPPA Slide Image
	 * Measurements) Take '14-3-3_beta-R-V_GBLxxxxxx.txt' for example,
	 * '14-3-3_beta' is the name of the antibody itself, 'R' means the antibody
	 * was derived from rabbit (and 'M' for mouse, 'G' for goat). The 'V' that
	 * follows means means the antibody is validated ('C' means 'use with
	 * caution', and 'E' means validation information is not available or 'under
	 * evaluation'). The last part, starting with 'GBL', is the bar code of the
	 * slide, unique to each physical slide.
	 * 
	 * Level2: (processed), algorithm: SuperCurve .txt -
	 * "Derived Array Data File" (AW) (FileType = SuperCurve Results)
	 * 
	 * Level3: (segmented), algorithm: normalized .txt -
	 * "Derived Array Data Matrix File" (BF) (FileType = Normalized)
	 * 
	 * 
	 * Sample REF 08E6497A-47F5-4283-A534-7C04EEC936E9 - by UUID, col
	 * "Sample Name"
	 * 
	 */

	protected String level = null;
	protected DataMatrix dataMatrix = null;

	protected Map<String, String> uuidBarcodeMap = new HashMap<String, String>();

	private String[] endings = { ".txt", ".tif", ".tiff", "sdrf.txt",
			".array_design.txt", ".antibody_annotation.txt" };
	private String currTCGAArchivePath = "";

	private String[] metaFileNames = { "Array Design File", "Annotations File" };

	private String diseaseAbbr = "";
	private Map<File, String> metaFiles = new HashMap<File, String>(); // tcgaUrl,
																		// pgrrFile
	private boolean metaJustCreated = false;
	
	private List<String> mageFileCols = Arrays.asList("Array Data File","Image File","Derived Array Data File",
			"Derived Array Data Matrix File","Sample Name");
	
	//FOR BRCA first run ONLY!!!
	private boolean canDo = false;

	@Override
	public String[] getResourceEndings() {
		return endings;
	}

	@Override
	public String getDataType() {
		return "Expression_Protein";
	}

	@Override
	public String getAnalysisDirName() {
		return "protein_exp";
	}

	@Override
	public String getResourceKey() {
		return "exp_protein";
	}

	@Override
	public String dataAccessType() {
		return TCGAModule.PUBLIC;
	}

	@Override
	public boolean canProcessArchive(LineBean archiveBean) {
		String archiveName = archiveBean.getName();
		return (archiveName.toLowerCase().indexOf(".level_") != -1 || archiveName
				.indexOf(".mage-tab.") != -1);
	}

	/**
	 * For restart need to set DieasesAbbr first
	 */
	@Override
	public void processData(List<LineBean> levBeans) {
		int sz = levBeans.size();
		int cn = 0;
		String pgrrPath = "";
		for (LineBean lb : levBeans) {
			if (acceptableEnding(lb.getName(), getResourceEndings())
					&& !TCGAHelper.stringContains(lb.getName(),
							MySettings.infoFilesList)) {
			System.out.println("ExpProtein.processData working with: "+lb.getUrl());
				// clear meta for a new archive
				if (lb.getName().endsWith(".sdrf.txt")
						|| lb.getName().indexOf(".array_design.") != -1
						|| lb.getName().indexOf(".antibody_annotation.") != -1) {
					if (!diseaseAbbr.equals(lb.getDiseaseStudy())) {
						diseaseAbbr = lb.getDiseaseStudy();
						metaJustCreated = true;
						if (dataMatrix == null) {
							dataMatrix = new DataMatrix("MageTabSDRF");

						} else {
							dataMatrix.clear();
							metaFiles.clear();
						}

					}
					if (lb.getName().endsWith(".sdrf.txt")) {
System.out.println("Start Read Metadata file");
						HttpClient httpclient = TCGAHelper.getHttpClient();
						InputStream is = TCGAHelper.getGetResponseInputStream(
								httpclient, lb.getFullURL());

						CSVReader reader = new CSVReader(new BufferedReader(
								new InputStreamReader(is)), '\t');
						String[] readLine = null;
						boolean isHeader = true;
						List<String[]> allData = new LinkedList<String[]>();
						int newSz = mageFileCols.size();
						int[] colNums = new int[newSz];
						try {
							while ((readLine = reader.readNext()) != null) {
								if (isHeader) {
									//match columns to be copied
									int k = 0;
									int headK = 0;
									for(String sHead: readLine){
										if(mageFileCols.contains(sHead)){
											colNums[k] = headK;
											k++;
										}
										headK++;	
									}
									allData.add(subsetFromRow(colNums, readLine));
									isHeader = false;
								} else{
									allData.add(subsetFromRow(colNums, readLine));
								}
							
									
							}		
									
							dataMatrix.setData(allData);
				System.out.println("Done Read Metadata file");
							reader.close();
							is.close();
							httpclient.getConnectionManager().shutdown();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
		System.out
				.println("		*** ERROR: Can't create dataMatrix for "
						+ lb.getFullURL());
						}

					} else if (lb.getName().indexOf(".array_design.") != -1
							|| lb.getName().indexOf("antibody_annotation.") != -1) {
						// save metadata in
						pgrrPath = MySettings.SUPERCELL_HOME
								+ File.separator
								+ lb.getDiseaseStudy()
								+ File.separator
								+ getDataType()
								+ File.separator
								+ CodesUtil.getCenterCodeFromCenterName(lb
										.getCenterName()) + "_"
								+ lb.getPlatform().replaceAll("_", "-")
								+ File.separator
								+ CodesUtil.getArchiveName(lb.getUrl());

						// make dir if not exist
						File metadataDir = new File(pgrrPath);
						metadataDir.mkdirs();

						File metadataFile = new File(
								metadataDir.getAbsolutePath() + File.separator
										+ lb.getName());

						try {
							TCGAHelper.copyURLToFile(new URL(lb.getFullURL()),
									metadataFile, false);

						} catch (MalformedURLException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						metaFiles.put(metadataFile, lb.getUrl());

					}

				}

				else if (!lb.getName().endsWith("idf.txt")) {
					// System.out.println("		WWWWW metaJustCreated:  "+metaJustCreated);
					// at this point the metadata files should be created, so
					// create common metafiles
					if (metaJustCreated) {/*
						metaJustCreated = false;
						File metaF = null;
						List<Integer> selectedRows = null;
						String colName = "";
			System.out.println("metaFiles.sz: " + metaFiles.size());
						for (Map.Entry<File, String> entry : metaFiles
								.entrySet()) {
							metaF = entry.getKey();

							if (metaF.getName().endsWith(
									"antibody_annotation.txt"))
								colName = "Annotations File";
							else
								colName = "Array Design File";

							selectedRows = dataMatrix.getStartsWithDataRows(
									metaF.getName(),
									dataMatrix.getColumnNum(colName, 1));
			System.out.println("selectedRows.sz: "
					+ selectedRows.size());

							List<String> uniqueUUIDs = getUniqueUUIDS(
									dataMatrix, selectedRows, getUUIDColName());
							int szz = uniqueUUIDs.size();
							int cc = 1;
							for (String uuid : uniqueUUIDs) {
								Aliquot al = constructSharedAliquot(uuid,
										metaF, entry.getValue(), "0");
								String metadataPath = MySettings.SUPERCELL_HOME
										+ al.constractPittPath(true);
								// make dir if not exist
								File metadataFile = new File(metadataPath);
								metadataFile.mkdirs();
			System.out.println("   *** SHARED file: "+ metaF.getName() + "   num " + cc + "  of " + szz);
								// al.print();
								ModuleUtil.saveMetaData(al, metadataPath);
								cc++;
							}
							uniqueUUIDs.clear();
							uniqueUUIDs = null;
						}

					*/}
					cn++;

					// clear tempo dir
					clearTempoDir();
					System.gc();

					//System.out.println(lb.getDiseaseStudy() + " : " + cn+ " out of " + sz + "  lb.url = " + lb.getUrl()+ " level: " + lb.getLevel());		
if(!canDo && lb.getUrl().equals("/tumor/brca/cgcc/mdanderson.org/mda_rppa_core/protein_exp/mdanderson.org_BRCA.MDA_RPPA_Core.Level_2.1.2.0/mdanderson.org_BRCA.MDA_RPPA_Core.SuperCurve.Level_2.010B8D0C-9E26-464A-9820-87356E82DEBC.txt"))
	canDo = true;
if(canDo){
	System.out.println(lb.getDiseaseStudy() + " : " + cn+ " out of " + sz + "  lb.url = " + lb.getUrl()+ " level: " + lb.getLevel());		
					// level 1 has mulitple "samples" in one file.
					// (1) save it in
					// <diseaseAbbbr>/<analysistype>/<centerCode>_<platform>/<tcga_archveName>/
					// dir
					// (2) Get all the uuids for this files from mage-tab and
					// create sharable files
					if (lb.getLevel().equals("1")) {
						setLevel("1");
						// (1)
						pgrrPath = MySettings.SUPERCELL_HOME
								+ File.separator
								+ lb.getDiseaseStudy()
								+ File.separator
								+ getDataType()
								+ File.separator
								+ CodesUtil.getCenterCodeFromCenterName(lb
										.getCenterName()) + "_"
								+ lb.getPlatform().replaceAll("_", "-")
								+ File.separator
								+ CodesUtil.getArchiveName(lb.getUrl());

						// make dir if not exist
						File metadataDir = new File(pgrrPath);
						metadataDir.mkdirs();

						File level1File = new File(
								metadataDir.getAbsolutePath() + File.separator
										+ lb.getName());

						try {
							TCGAHelper.copyURLToFile(new URL(lb.getFullURL()),
									level1File, false);

						} catch (MalformedURLException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						// (2)
						List<Integer> selectedRows = dataMatrix
								.getStartsWithDataRows(
										level1File.getName(),
										getFileColNum(dataMatrix,
												level1File.getName()));
	System.out.println("selectedRows.sz: "+ selectedRows.size());

						List<String> uniqueUUIDsLev1 = getUniqueUUIDS(
								dataMatrix, selectedRows, getUUIDColName());
						int szzz = uniqueUUIDsLev1.size();
						int ccc = 1;
						for (String uuid : uniqueUUIDsLev1) {
							Aliquot al = constructSharedAliquot(uuid,
									level1File, lb.getUrl(), "1");
							String metadataPath = MySettings.SUPERCELL_HOME
									+ al.constractPittPath(true);
							// make dir if not exist
							File metadataFile = new File(metadataPath);
							metadataFile.mkdirs();
System.out.println("   *** Level 1 file: "+ level1File.getName() + "   num " + ccc+ "  of " + szzz);
							// al.print();
							ModuleUtil.saveMetaData(al, metadataPath);
							ccc++;
						}
						uniqueUUIDsLev1.clear();
						uniqueUUIDsLev1 = null;
					} else {

						Aliquot al = null;
						try {
							setLevel(lb.getLevel());
							al = constructAliquot(lb);
							File tempoFile = new File(MySettings.TEMPO_DIR
									+ "f" + String.valueOf(TEMP_FILE_NUM) + ".txt");
							TEMP_FILE_NUM++;
							if (al != null) {
								al.setTempoFile(tempoFile);
								FileUtils.copyURLToFile(
										new URL(lb.getFullURL()), tempoFile);
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}


						if (al != null) {
							 al.print();				
							ModuleUtil.transferNew(new Aliquot[] { al });

						}
					}
}// if canDo
				} // fNameEnding != null
			}

		} // for(LineBean lb:levBeans)

	}
		
		private String[] subsetFromRow(int[] colNums, String[] row){
			String [] dat = new String[colNums.length];
			int i = 0;
			for(int k:colNums){
				dat[i] = row[k].toLowerCase();
				i++;
			}
			return dat;
		}

	public List<String> getUniqueUUIDS(DataMatrix dm,
			List<Integer> selectedRows, String uuidColName) {
		List<String> list = new ArrayList<String>();
		int uuidCol = dm.getColumnNum(getUUIDColName());
		String val = null;
		for (Integer rowNum : selectedRows) {
			val = dm.getRowColValue(uuidCol, rowNum);
			if (!list.contains(val))
				list.add(val);
		}
		Collections.sort(list);
		return list;
	}

	public String getTCGAFileName(String uuid, String metaColName) {
		int rowNum = dataMatrix.getRowNumStartsWithIgnoreCaseData(uuid,
				dataMatrix.getColumnNum(getUUIDColName(), 1));
		return dataMatrix.getRowColValue(metaColName, rowNum);
	}

	public void setLevel(String l) {
		level = l;
	}

	public String getLevel() {
		return level;
	}

	public int getFileColNum(DataMatrix dm, String lbName) {
		int occurance = 1;
		if ("1".equals(getLevel())) {
			if (lbName.endsWith(".txt"))
				return dm.getColumnNum("Array Data File");
			else
				return dm.getColumnNum("Image File");
		} else if ("2".equals(getLevel())) {
			return dm.getColumnNum("Derived Array Data File", occurance);
		} else
			// level 3

			return dm.getColumnNum("Derived Array Data Matrix File", occurance);
	}

	public int getDataRowNum(String lbName, int colNum) {
		try {
			return dataMatrix.getDataRow(lbName, colNum);
		} catch (NullPointerException e) {
			return -1;
		}
	}

	public String getFileExtension(String lbName) {
		return lbName.substring(lbName.lastIndexOf(".") + 1, lbName.length());
	}

	public String getUUIDColName() {
		return "Sample Name";
	}

	public Aliquot constructSharedAliquot(String uuid, File pgrrFile,
			String tcgaUrl, String level) {
		System.out
				.println("ExpProtein_Module constructSharedAliquot for uuid: "
						+ uuid + " pgrrFile: " + pgrrFile);

		Aliquot al = new Aliquot(tcgaUrl, getDataType(),
				getFileExtension(pgrrFile.getName()));
		al.setOrigUUID(uuid);
		al.setBarcode(getBarcodeByUUID(uuid));

		al.setDataAccessType(dataAccessType());

		String centerName = CodesUtil.getCenterNameFromArchive(pgrrFile
				.getParentFile().getName());
		al.setCenterName(centerName);
		al.setCenterCode(CodesUtil.getCenterCodeFromCenterName(centerName));

		al.setPlatform(CodesUtil
				.getPlatform(pgrrFile.getParentFile().getName()));
		al.setFileType(getFileType(pgrrFile.getName()));

		al.setPortion("");
		al.setTcgaArchivePath(tcgaUrl);
		al.setTCGAFileName(pgrrFile.getName());
		al.setLevel(level);

		al.setOwnFileName(pgrrFile.getName());
		al.setFileFractionType("shipped_portion");

		al.setFileSize(String.valueOf(pgrrFile.length()));

		try {
			String checksum = ModuleUtil.calcCheckSum(new FileInputStream(
					pgrrFile));
			al.setChecksum(checksum);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String pittPath = pgrrFile.getAbsolutePath().substring(
				MySettings.SUPERCELL_HOME.length(),
				pgrrFile.getAbsolutePath().length());
		pittPath = pittPath.substring(0, pittPath.lastIndexOf("/") + 1);
		al.setPittPath(pittPath);
		return al;
	}

	public Aliquot constructAliquot(LineBean lb) {

		String lbName = lb.getName().toLowerCase();
		String uuid = "";
		int rowNum = getDataRowNum(lbName, getFileColNum(dataMatrix, lbName));
		if (rowNum > -1) {
			Aliquot al = new Aliquot(lb.getUrl(), getDataType(),
					getFileExtension(lbName));

			uuid = dataMatrix.getRowColValue(getUUIDColName(), rowNum)
					.toLowerCase();
			System.out.println("GETTING UUID: " + uuid + "  rowNM: " + rowNum
					+ "  getFileColNum(dataMatrix, lbName): "
					+ getFileColNum(dataMatrix, lbName));
			al.setOrigUUID(uuid);
			al.setBarcode(getBarcodeByUUID(uuid));

			al.setFileFractionType("shipped_portion");

			al.setCenterName(CodesUtil.getCenterNameFromArchive(al
					.getArchiveName()));
			al.setCenterCode(CodesUtil.getCenterAbbFromArchName(al
					.getCenterName()));
			al.setRefGenome("N/A");
			al.setRefGenomeSource("N/A");
			al.setPlatform(lb.getPlatform());

			al.setFileType(getFileType(lbName));
			al.setAlgorithmName(getAlgorithmName(lbName));
			al.setPortion(getPortion(al.getPortion(), lbName));

			return al;
		} else {
			String err = "   ***** ExpProteinModule: can't find in mage-tab a row for "
					+ lb.getFullURL() + "\n name: " + lbName;
			System.err.println(err);
			ErrorLog.log(err);
			return null;
		}
	}

	protected String getFileType(String lbName) {
		if (lbName.endsWith(".array_design.txt"))
			return "array_design";
		else if (lbName.endsWith(".antibody_annotation.txt"))
			return "antibody_annotation";
		else {
			String toret = "raw";
			if ("2".equals(getLevel()))
				toret = "processed";
			else if ("3".equals(getLevel())) {
				toret = "segmented";
			}
			return toret;
		}
	}

	protected String getAlgorithmName(String lbName) {
		if (lbName.endsWith(".array_design.txt")
				|| lbName.endsWith(".antibody_annotation.txt"))
			return "N/A";
		String toret = "";
		if ("2".equals(getLevel()))
			toret = "SuperCurve";
		else if ("3".equals(getLevel())) {
			toret = "Normalized";
		}
		return toret;
	}

	protected String getPortion(String defaultPortion, String lbName) {
		String toret = defaultPortion;
		if ("1".equals(getLevel())) {
			toret = lbName.substring(0, lbName.indexOf("_GBL"));
		}

		return toret;
	}

	protected String getBarcodeByUUID(String uuid) {
		String toret = uuidBarcodeMap.get(uuid);
		if (toret == null) {
			/*System.out
					.println("  @@@ ExpProtein.getBarcodeByUUID GOING to mapping");*/
			toret = CodesUtil.mapping(CodesUtil.UUID_STR, uuid);
			uuidBarcodeMap.put(uuid, toret);
		}
		return toret;
	}

	public static void main(String[] args) {
		TCGAExpedition.setUniqueDirs();
												// anything
		MySettings.PGRR_META_NQ_FILE = "_ep_TEST.nq";

		ExpProtein_Module pmf = new ExpProtein_Module();

		// below just test
		String[] urls = { MySettings.PUB_ROOT_URL
				+ "brca/cgcc/mdanderson.org/mda_rppa_core/protein_exp/"

		};

		for (String dir : urls) {
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			pmf.processArchiveLevel(TCGAHelper.recentOnly(list));
		}

		System.out.println("done ExpProtein_Module");
	}

}
