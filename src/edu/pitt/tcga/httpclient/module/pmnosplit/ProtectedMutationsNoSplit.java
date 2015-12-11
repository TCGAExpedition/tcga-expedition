package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

/**
 * No split files by samples in PGRR hard copy is stored under patient + soft
 * copies under samples Each soft copy has it's own Aliquot + one Aliquot for
 * each sample in hard copy location
 * 
 * @author opm1
 * 
 */

public abstract class ProtectedMutationsNoSplit extends TCGAModule {

	// {"kirc","kirp","lgg","luad"}
	List<String> DNASeq_Cont_aut = Arrays.asList("TCGA-B3-3925",
			"TCGA-B3-4103", "TCGA-B3-4104");

	List<String> currList = DNASeq_Cont_aut;

	protected Aliquot[] als = null;

	protected String PLATFORM_SUFF = null;

	protected int stNum = 1, endNum = 100000000;

	private String[] endings = { "vcf", ".vcf.gz" };
	private String authenticationStr = MySettings.getStrProperty("tcga.user")
			+ ":" + MySettings.getStrProperty("tcga.pwd") + "@";

	/*
	 * public void setNumOfSamples(int i){ this.numOfSamples = i; }
	 */

	public String dataAccessType() {
		return TCGAModule.CONTROLLED;
	}

	public String getDataType() {
		return "Protected_Mutations";
	}

	public String getFileType() {
		return "vcf";
	}

	@Override
	public boolean canProcessArchive(LineBean archiveBean) {
		return true;
	}

	/**
	 * 
	 * @param stNum
	 *            - inclusive
	 * @param endNum
	 *            - exclusive
	 */
	public void setStartEndRecordNumbers(int stNum, int endNum) {
		this.stNum = stNum;
		this.endNum = endNum;
	}

	/**
	 * 
	 * @param fileName
	 * @param list
	 *            <patient barcodes>
	 * @return
	 */
	private static boolean needCorection(String fileName, List<String> list) {
		for (String s : list) {
			if (fileName.indexOf(s) != -1)
				return true;
		}
		return false;
	}

	public void processData(List<LineBean> levBeans) {
		int testNum = 0;
		int sz = levBeans.size();
		int cn = 0;

		String diseaseStudyAbb = CodesUtil.getDiseaseAbbrFromURL(levBeans
				.get(0).getFullURL());

		PLATFORM_SUFF = null;

		for (LineBean lb : levBeans) {

			if (lb.getName().endsWith(".vcf")
					|| lb.getName().endsWith(".vcf.gz")) {

				if (PLATFORM_SUFF == null) {
					String platform = CodesUtil.getPlatform(CodesUtil
							.getArchiveName(lb.getFullURL()));
					int ind = platform.lastIndexOf("_");
					if (ind != -1)
						PLATFORM_SUFF = platform.substring(ind + 1);
					else
						PLATFORM_SUFF = "";
				}
				testNum++;
				cn++;

				// if(testNum < 3) {
				// clear tempo dir
				if (canDeleteTempoFiles()) {
					clearTempoDir();
					System.gc();
				}

				System.out.println("lb.n = " + lb.getName() + " is " + cn
						+ " out of " + sz);

				// remove authentication
				String urlStr = lb.getFullURL().replace(authenticationStr, "");
				;
				File downloadedF = TCGAHelper.urlToFile(urlStr,
						MySettings.TEMPO_DIR, true);
				CSVReader reader = null;
				try {
					reader = new CSVReader(new BufferedReader(new FileReader(
							downloadedF)), '\t');
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					ErrorLog.logFatal("ProtectedMutationsNoSplit can't read file : "
							+ downloadedF.getAbsolutePath());
				}
				BufferedWriter writer = null;

				List<String[]> origHeader = new LinkedList<String[]>();
				boolean isHeader = true;

				String[] readLine = null;
				int[] indices = null;
				int numSamples = 1;
				try {
					while ((readLine = reader.readNext()) != null) {
						if (readLine[0].startsWith("#CHROM")) {
							isHeader = false;
							DataMatrix header = new DataMatrix("", origHeader,
									false);
							Map<String, String> sampleIDBarcode = getSampleIdBarcodeMap(header);

							numSamples = sampleIDBarcode.size();

							// init tempo file, writer
							File file = new File(MySettings.TEMPO_DIR + "f"
									+ String.valueOf(TEMP_FILE_NUM) + ".vcf");
							writer = new BufferedWriter(new FileWriter(file,
									true));
							TEMP_FILE_NUM++;
							// init Aliquots for hard copies
							als = new Aliquot[numSamples];
							for (int k = 0; k < numSamples; k++) {
								Aliquot al = new Aliquot(lb.getUrl(),
										getDataType(), getFileType());
								al.setDiseaseStudyAbb(diseaseStudyAbb);
								al.setFileFractionType("aliquot");
								al.setFileType("vcf");
								als[k] = al;
								als[k].setTempoFile(file);
								if (numSamples > 1) {
									als[k].setSaveInPatientDir(true);
									als[k].setHasAlias(true);
								}
								// attach tempo data file only for one aliquot
								if (k > 0)
									als[k].setHasTempoFile(false);

							}

							List<String> standardHeader = toStandard(header);
							indices = renameAndReorderCols(readLine,
									sampleIDBarcode);
							// write header
							writer.write(ModuleUtil.listOfStrToString(
									standardHeader, MySettings.END));
							// clean up
							header.destroy();
							sampleIDBarcode.clear();
							sampleIDBarcode = null;

						}
						if (!isHeader) {
							writer.write(ModuleUtil.copyPartArrayToStr(
									readLine, indices, MySettings.TAB));

						} else
							// if isHeader = true
							origHeader.add(readLine);

					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						// is.close();
						if (reader != null)
							reader.close();
						if (writer != null)
							writer.close();

					} catch (IOException e) {
						e.printStackTrace();
					}
					// httpclient.getConnectionManager().shutdown();
				}

				als = prepareForTransfer(als);

				if (als != null) {
					ModuleUtil.transferNew(als);
					clearTempoDir();
					als = null;
				}

			}// if (lb.getName().endsWith(".vcf")

		} // for(LineBean lb:levBeans)
	}

	/**
	 * return indices of location should be in a new list
	 * 
	 * @param list
	 * @return
	 */
	public int[] getNewIndices(List<String> origList, List<String> newList) {

		int len = newList.size();
		int[] indices = new int[len];
		for (int index = 0; index < len; index++) {
			indices[index] = origList.indexOf(newList.get(index));
		}

		return indices;

	}

	/**
	 * returns
	 * 
	 * @param origHeader
	 * @return <SAMPLE_ID , List<standard UMPC fields+ other fields>
	 */
	public List<String> toStandard(DataMatrix header) {
		List<String> toret = new LinkedList<String>();

		String[] sampleRows = getSamplesInfo(header);
		int numOfSamples = sampleRows.length;

		StringBuilder samplesBarcodes = new StringBuilder();
		List<String> existing = new ArrayList<String>();
		String currBC = null;
		for (int i = 0; i < sampleRows.length; i++) {
			currBC = getSampleBarcode(sampleRows[i]);
			if (!existing.contains(currBC)) {
				samplesBarcodes.append(currBC + "; ");
				existing.add(currBC);
			}
		}
		existing.clear();
		samplesBarcodes.setLength(samplesBarcodes.length() - 2);

		toret.add(getFileFormat(header)); // 1
		toret.add("##file_date=" + getFileDate(header)); // 2
		toret.add("##center=" + getCenter(header)); // 3
		toret.add("##sequencing_center=" + getSequencingCenter(header));// 4
		toret.add("##platform=" + getPlatform(header, sampleRows[0])); // 5
		toret.add("##platform_version="
				+ getPlatformVersion(header, sampleRows[0]));// 6
		String ref = getReference(header);
		String refSrc = getReferenceSource(header);

		toret.add("##reference=" + ref);// 7
		toret.add("##reference_source=" + refSrc);// 8
		toret.add("##patient_id=" + getPatientBarcode(header, sampleRows[0])); // 9

		String sampleBarcode = null;
		String sampleID = null;
		List<String> curSam = null;
		for (int i = 0; i < numOfSamples; i++) {
			als[i].setInfo(sampleRows[i]);
			sampleBarcode = getSampleBarcode(sampleRows[i]);
			sampleID = getSampleID(sampleRows[i]);
			als[i].setBarcode(sampleBarcode);
			als[i].setId(sampleID);
		}

		toret.add("##specimen_id=" + samplesBarcodes.toString()); // 10

		toret.add("##alignment_pipeline=" + getAlignPipeline(header));// 11
		toret.add("##alignment_pipeline_version=" + getAlignPipelineVer(header)); // 12

		String pipeline = getVCPipeline(header, sampleRows[0]);
		String pipelineVer = getVCPipelineVer(header, sampleRows[0]);
		toret.add("##vc_pipeline=" + pipeline);// 13
		toret.add("##vc_pipeline_version=" + pipelineVer);// 14

		StringBuilder surrBamSB = new StringBuilder();
		StringBuilder surrBamStorSB = new StringBuilder();
		for (int i = 0; i < numOfSamples; i++) {
			currBC = getBamFile(header, als[i].getInfo());
			if ("none".equals(currBC) || !existing.contains(currBC)) {
				surrBamSB.append(currBC + "; ");
				surrBamStorSB
						.append(getBamFileStorage(header, als[i].getInfo())
								+ "; ");
				existing.add(currBC);
			}
		}
		if (surrBamSB.toString().endsWith("; ")) {
			surrBamSB.setLength(surrBamSB.length() - 2);
			surrBamStorSB.setLength(surrBamStorSB.length() - 2);
		}
		existing.clear();
		existing = null;

		toret.add("##bam_file=" + surrBamSB.toString()); // 15
		toret.add("##bam_file_storage=" + surrBamStorSB.toString());// 16

		// set aliquot fields
		for (int k = 0; k < als.length; k++) {
			als[k].setDataAccessType(dataAccessType());
			als[k].setRefGenome(ref);
			als[k].setRefGenomeSource(refSrc);
			als[k].setCenterName(CodesUtil.getCenterNameFromArchive(als[k]
					.getArchiveName()));
			als[k].setCenterCode(CodesUtil.getCenterAbbFromArchName(als[k]
					.getCenterName()));
			als[k].setPlatform(CodesUtil.getPlatform(als[k].getArchiveName()));
			als[k].setAlgorithmName(pipeline);
			als[k].setAlgorithmVersion(pipelineVer);
		}

		setAdditionalAliquotInfo(als);

		List<String[]> headData = header.getData();

		// add all metadata leftovers:
		for (String[] sArr : headData) {
			for (String s : sArr) {
				if (!s.startsWith("##INFO") && !s.startsWith("##FORMAT")
						&& !s.startsWith("##FILTER")) {
					// !s.startsWith("##SAMPLE")){
					toret.add(s);
				}
			}
		}

		// add Standard Info
		String toAdd = null;
		for (String s : ModuleUtil.infoArr) {
			toAdd = findOrReplaceDesc(s, header);
			toret.add(toAdd);
		}

		// add Info leftovers
		for (String[] sArr : headData) {
			for (String s : sArr) {
				if (s.startsWith("##INFO")) {
					toret.add(s);
				}
			}
		}
		// add Standard Format
		for (String s : ModuleUtil.formatArr) {
			toAdd = findOrReplaceDesc(s, header);
			toret.add(toAdd);
		}
		// add Format leftovers
		for (String[] sArr : headData) {
			for (String s : sArr) {
				if (s.startsWith("##FORMAT")) {
					toret.add(s);
				}
			}
		}

		// add all Filter
		for (String[] sArr : headData) {
			for (String s : sArr) {
				if (s.startsWith("##FILTER")) {
					toret.add(s);
				}
			}
		}

		sampleRows = null;

		return toret;

	}

	// should remove the field after use
	// meta

	public abstract void setAdditionalAliquotInfo(Aliquot[] als);

	public abstract Aliquot[] prepareForTransfer(Aliquot[] als);

	public abstract boolean canDeleteTempoFiles();

	// / new abstract

	public abstract Map<String, String> getSampleIdBarcodeMap(DataMatrix dm);

	public abstract int[] renameAndReorderCols(String[] chromRow,
			Map<String, String> idBarcodeMap);

	public abstract String getSampleID(String sInfo);

	public abstract String getSampleBarcode(String sInfo);

	public abstract String getFileFormat(DataMatrix dm);

	public abstract String getFileDate(DataMatrix dm);

	public abstract String getCenter(DataMatrix dm);

	public abstract String getSequencingCenter(DataMatrix dm);

	public abstract String getPlatform(DataMatrix header, String sampleStr);

	public abstract String getPlatformVersion(DataMatrix header,
			String sampleStr);

	public abstract String getReference(DataMatrix dm);

	public abstract String getReferenceSource(DataMatrix dm);

	public abstract String getPatientBarcode(DataMatrix header,
			String sampleInfo);

	public abstract String getAlignPipeline(DataMatrix header);

	public abstract String getAlignPipelineVer(DataMatrix header);

	public abstract String getVCPipeline(DataMatrix header, String sampleInfo);

	public abstract String getVCPipelineVer(DataMatrix header, String sampleInfo);

	public abstract String getSampleUUID(String sampleInfo);

	public abstract String[] getSamplesInfo(DataMatrix dm);

	public abstract String getBamFile(DataMatrix dm, String samleInfo);

	public abstract String getBamFileStorage(DataMatrix dm, String samleInfo);

	private boolean excludeSampleLine(String sampleID) {
		for (String suffix : MySettings.excludeSampleSuff)
			if (sampleID.endsWith(suffix))
				return true;
		return false;
	}

	/**
	 * It also removed the record from list if found
	 * 
	 * @param find
	 * @param header
	 * @return
	 */

	public String findOrReplaceDesc(String find, DataMatrix header) {
		int dbInd = getLineFromList(find, header, 0);
		String toret = null;
		if (dbInd != -1) {
			toret = header.getRowColValue(0, dbInd);
			header.removeRow(dbInd);
		} else
			toret = find
					+ "Number=0,Type=Integer,Description=\"NotAvailable\">";

		return toret;
	}

	public int getLineFromList(String prefix, DataMatrix dm, int colNum) {
		return dm.getRowNumStartsWithData(prefix, colNum);
	}

	@Override
	public String[] getResourceEndings() {
		return endings;
	}

	@Override
	public String getAnalysisDirName() {
		return "mutations_protected";
	}

	@Override
	public String getResourceKey() {
		return "vcf.protected";
	}

}
