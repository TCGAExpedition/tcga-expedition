package edu.pitt.tcga.httpclient.module.bam;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.transfer.LocalShell;
import edu.pitt.tcga.httpclient.transfer.Transfer;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.ReferenceGenomeUtil;
import edu.pitt.tcga.httpclient.util.MySettings;

/**
 * Creates metadata about downloaded BAM files and moves BAM files to
 * repository.
 * 
 * Steps: 1. Scan /supercell/bam_requests/finished_downloads for
 * <one_level>/final_files, readin finished_*.tsv 2. create a orig_finished_* 3.
 * Create Metadata.tsv and RDF triples for finished files 4. save resulting tsv
 * as DONE_bam_status_<disAbbr>_<dataType>.tsv in /supercell/bam_requests/done
 * dir 5. Remove finished_*.tsv 6. move files: ATT: Wait until the disease is
 * Done! SOURCE=/supercell/tcga_downloads/brca DEST=/supercell/tcga/brca cd
 * ${SOURCE}; find . -type d -exec mkdir -p ${DEST}/\{} \; find . -type f -exec
 * mv -i \{} ${DEST}/\{} \; 7. clear empty dirs in the SOURCE dir 8. upload RDF
 * triples to Postgres - ATTN: now it's a manual process!
 * 
 * @author opm1
 * @version 1
 * @since Dec 11, 2015
 * 
 */
public class BAMMetadataManager {
	
	/**
	 * Constructor
	 */
	public BAMMetadataManager() {
	}
	
	public static String addSlashAndCreateDirIfNeed(String propName, String subDirName){
		String dirName = MySettings.getStrProperty(propName);
		if(!dirName.endsWith("/"))
			dirName = dirName+"/";
		if(subDirName != null)
			dirName = dirName+subDirName+"/";
		File f = new File(dirName);
		if(!f.exists())
			f.mkdirs();
		return dirName;
	}

	/**
	 * Finds *.tsv files with list of downloaded BAM files in
	 * 'finished.bam.tsv.dir' and post-process them
	 */
	public void postProcess() {
		List<File> finishedFileList = new ArrayList<File>();
		scanFinishedDir(
				new File(addSlashAndCreateDirIfNeed("top.bam.tsv.dir", "finished_downloads")),
				finishedFileList);

		for (File f : finishedFileList) {
			postProcessFinishedFile(f);
		}

	}

	/**
	 * Post-process selected *.tsv file.
	 * 
	 * @param finishedFile
	 *            - File to be post-processed
	 */
	public void postProcessFinishedFile(File finishedFile) {
		if (!finishedFile.getName().startsWith("finished_")) {
			System.err
					.println("BAM postProcessFinishedFile: file name must start with  'finished_' prefix. Carr name: "
							+ finishedFile.getName());
			return;
		}
		createCopy(finishedFile);
		createMetadata(finishedFile.getAbsolutePath(),
				getSuffix(finishedFile.getName()));
		finishedFile.delete();
	}

	/**
	 * Post-process selected *.tsv file.
	 * 
	 * @param finishedFileNameWithPath
	 *            - String filename to be p-processed
	 */
	public void postProcessFinishedFile(String finishedFileNameWithPath) {
		File f = new File(finishedFileNameWithPath);
		postProcessFinishedFile(f);
	}

	/**
	 * Recursively searches for *.tsv files with a list completely downloaded
	 * BAM files without created metadata.
	 * 
	 * @param currFile
	 *            - filenames which start with 'finished_' are candidates for
	 *            post-process.
	 * @param finishedFileList
	 *            -
	 */
	public void scanFinishedDir(File currFile, List<File> finishedFileList) {
		if (currFile.isDirectory()) {
			for (File f : currFile.listFiles())
				scanFinishedDir(f, finishedFileList);
		} else if (currFile.getName().startsWith("finished_"))
			finishedFileList.add(currFile);

	}

	/**
	 * Creates *.tsv file copy in the same directory with prefix 'orig_' in the
	 * name. In some environments FileUtils.copyFile don't work, so to be save,
	 * we use UNIX command for copy.
	 * 
	 * @param finishedFile
	 */
	public void createCopy(File finishedFile) {
		String copyFileName = finishedFile.getParent() + File.separator
				+ "orig_" + finishedFile.getName();
		Transfer.execBash("cp -f " + finishedFile.getAbsolutePath() + " "
				+ copyFileName, false);

		/*
		 * File dest = new
		 * File(finishedFile.getParent()+File.separator+"orig_"+finishedFile
		 * .getName()); try {
		 * 
		 * //FileUtils.copyFile(finishedFile, dest); } catch (IOException e) {
		 * // TODO Auto-generated catch block e.printStackTrace(); }
		 */
	}

	/**
	 * Returns DISEASEABBR_DATATYPE used for creating metadata
	 * 
	 * @param fName
	 *            example: finished_bam_status_GBM_miRNA-Seq.tsv
	 * @return example: GBM_miRNA-Seq
	 */
	private String getSuffix(String fName) {
		fName = fName.substring(0, fName.indexOf("."));
		String[] split = fName.split("_");
		int len = split.length;
		String toret = split[len - 2] + "_" + split[len - 1];
		split = null;
		return toret;
	}

	/**
	 * Creates and stores matadata.
	 * 
	 * @param filePath
	 *            - path to *.tsv file
	 * @param nqSuff
	 *            from getSuffix (eg. GBM_miRNA-Seq)
	 */
	public void createMetadata(String filePath, String nqSuff) {
		File logUpdate = new File(MySettings.LOG_DIR + "progressMetaData_"
				+ nqSuff + "_txt");

		String dtSuff = "_(cgHub)";
		String buf_nq_file = MySettings.PGRR_META_NQ_FILE;
		MySettings.PGRR_META_NQ_FILE = "_bamMetadata_" + nqSuff + ".nq";
		String destHome = MySettings.SUPERCELL_HOME + File.separator;
		String sourceHome = addSlashAndCreateDirIfNeed("downloaded.bamfiles.dir", null);
				
		String donePrefix = "DONE";

		DataMatrix dm = DataMatrix.getDataMatrix(filePath, '\t');

		int statusCol = dm.getColumnNum("status");
		int bcCol = dm.getColumnNum("barcode");
		int ownIDCol = dm.getColumnNum("analysis_id");
		int patientIDCol = dm.getColumnNum("participant_id");
		int sampleIDCol = dm.getColumnNum("sample_id");
		int aliquotIDCol = dm.getColumnNum("aliquot_id");
		int centerCodeCol = dm.getColumnNum("center");
		int platformCol = dm.getColumnNum("platform_name");
		int pgrrPathCol = dm.getColumnNum("pgrr_file_path");
		int fileNameCol = dm.getColumnNum("filename");
		int dataTypeCol = dm.getColumnNum("library_type");
		int refGCol = dm.getColumnNum("assembly");
		int fszCol = dm.getColumnNum("files_size");
		int md5Col = dm.getColumnNum("checksum");

		int numR = dm.numRows();
		String[] row = null;
		String genURL = null, fileExt = null, saveToDir = null;
		Aliquot al = null;
		boolean createMetadata = true; // do not create if there is no file in
										// source or dest dir
		String recordError = null;
		for (int i = 0; i < numR; i++) {
			row = dm.getRowData(i);
			createMetadata = true;
			recordError = null;
			if (row[statusCol] != null
					&& row[statusCol].equalsIgnoreCase("FINISHED")) {
				// check if there is a file in source or dest dir
				File sF = new File(sourceHome
						+ row[pgrrPathCol] + File.separator + row[fileNameCol]);
				File dF = new File(destHome + row[pgrrPathCol] + File.separator
						+ row[fileNameCol]);
				if (!sF.exists()) {
					// maybe it's already in dest dir?
					if (!dF.exists()) {
						createMetadata = false;
						if (!donePrefix.startsWith("PROBLEMS")) {
							donePrefix = "PROBLEMS_" + donePrefix;
						}

						recordError = "	!!!!  BAMMetedataMenager: No file in: "
								+ sF.getAbsolutePath() + "  ANd in DEST_DIR: "
								+ dF.getAbsolutePath();

					} else if (dF.exists()) {
						recordError = "		*** Already in DEST dir "
								+ dF.getAbsolutePath();
						ErrorLog.log(recordError, logUpdate);
					}

					if (recordError != null) {

						File errFile = new File(MySettings.LOG_DIR
								+ MySettings.getDayFormat()
								+ "_errorsBAMPostProcess.txt");
						ErrorLog.log(recordError, errFile);
						System.err.println(recordError);
					}

				} else if (row[statusCol] == null || row[statusCol].equals("")
						|| row[statusCol].equalsIgnoreCase("InProcess")) {
					if (!donePrefix.startsWith("PROBLEMS")) {
						donePrefix = "PROBLEMS_" + donePrefix;
					}
					createMetadata = false;
				}

				if (createMetadata) {
					fileExt = row[fileNameCol].substring(row[fileNameCol]
							.lastIndexOf(".") + 1);
					al = new Aliquot(MySettings.CGHUB_PATH + row[ownIDCol],
							row[dataTypeCol] + dtSuff, fileExt);
					al.setHasOwnFileName(true);
					al.setOwnFileName(row[fileNameCol]);
					al.setPittPath(row[pgrrPathCol] + "/");
					al.setTCGAFileName(row[fileNameCol]);

					al.setBarcode(row[bcCol]);
					al.setOrigUUID(row[aliquotIDCol]);
					al.setFileFractionType("aliquot");
					al.setLevel("1");
					al.setDataAccessType(TCGAModule.CONTROLLED);
					al.setFileSize(row[fszCol]);
					al.setChecksum(row[md5Col]);
					al.setTcgaArchivePath(al.getTcgaPath());

					al.setCenterCode(row[centerCodeCol]);

					al.setRefGenome(row[refGCol]);
					genURL = ReferenceGenomeUtil.getGenomeURL(row[refGCol]);
					if (genURL == null)
						ErrorLog.log("BAMMetadataManager: NO refGenome URL for "
								+ row[refGCol]);
					else
						al.setRefGenomeSource(genURL);
					al.setPlatform(row[platformCol]);
					if (row[fileNameCol].toLowerCase().indexOf("fastq.") != -1)
						al.setFileType("fastq");
					else
						al.setFileType("bam");

					// check uuids
					checkUUID("patient", row[patientIDCol], ModuleUtil
							.getUUIDByBarcode(al.getPatientBarcode())
							.toLowerCase());
					checkUUID(
							"sample",
							row[sampleIDCol],
							ModuleUtil.getUUIDByBarcode(
									CodesUtil.getFullSampleBarcode(row[bcCol]))
									.toLowerCase());
					checkUUID(
							"aliquot",
							row[aliquotIDCol],
							ModuleUtil.getUUIDByBarcode(
									CodesUtil.getAliquotBarcode(row[bcCol]))
									.toLowerCase());

					saveToDir = destHome
							+ row[pgrrPathCol].substring(0,
									row[pgrrPathCol].lastIndexOf('/'))
							+ File.separator;
					// save Metadata & triples
					ModuleUtil.saveMetaData(al, saveToDir);
					String logStr = "BAM Meta " + (i + 1) + " out of " + numR
							+ " for analysis:  " + row[ownIDCol]
							+ " saved to : " + saveToDir;
					ErrorLog.log(logStr, logUpdate);
					al.clear();
					System.out.println(logStr);

					// mkdir and move file
					mkDirsAndMove(sourceHome
							+ row[pgrrPathCol] + File.separator, destHome
							+ row[pgrrPathCol] + File.separator);

					al.clear();
					al = null;
				} // end if (createMetadata)
				else {

				}
				// modify status col
				dm.setValue(i, statusCol, (createMetadata) ? "Done"
						: "NotFound");

				// clear empty dirs in sourceDir
				removeEmptyDirs(sourceHome + row[pgrrPathCol],
						sourceHome);

			}

		}
		// set back default (just in case)
		MySettings.PGRR_META_NQ_FILE = buf_nq_file;

		// save file dm
		String doneFileDir = addSlashAndCreateDirIfNeed("top.bam.tsv.dir", "done");

		String newFilePath = doneFileDir + donePrefix + "_bam_status_" + nqSuff
				+ ".tsv";
		// check if such file name already exists
		File checkMe = new File(newFilePath);
		if (checkMe.exists()) {
			newFilePath = doneFileDir + donePrefix + "_bam_status_" + nqSuff
					+ "_" + UUID.randomUUID().toString() + ".tsv";
		}
		dm.saveAs(newFilePath, false);
		dm.destroy();
		dm = null;

	}

	/**
	 * Makes new directory in repository (if needed) and moves BAM file to it.
	 * 
	 * @param sourceDir
	 *            - move from directory
	 * @param destDir
	 *            - move to directory
	 */
	private void mkDirsAndMove(String sourceDir, String destDir) {
		// System.out.println("mkDirsAndMove src: "+sourceDir+" dest: "+destDir);
		File dDir = new File(destDir);
		dDir.mkdirs();

		File sDir = new File(sourceDir);
		if (sDir.exists() && sDir.list().length > 0) {
			for (File f : sDir.listFiles()) {
				try {
					FileUtils.moveFile(f, new File(destDir + f.getName()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
		} else if (!dDir.exists() || dDir.list().length == 0) {
			File errFile = new File(MySettings.LOG_DIR
					+ MySettings.getDayFormat() + "_errorsBAMPostProcess.txt");
			ErrorLog.log(
					"	!!!!  BAMMetedataMenager.mkDirsAndMove: No file in: "
							+ sourceDir + "  ANd in DEST_DIR:  " + destDir,
					errFile);
			System.err
					.println("	!!!!  BAMMetedataMenager.mkDirsAndMove: No file in SRC_DIR: "
							+ sourceDir + " ANd in DEST_DIR: " + destDir);
		}

	}

	/**
	 * Removes empty directories up to ".downloaded.bamfiles.dir" after BAM
	 * files move.
	 * 
	 * @param dirName
	 */
	private void removeEmptyDirs(String dirName, String stopAtDirName) {
		if (dirName.equals(stopAtDirName))
			return;

		File dFile = new File(dirName);

		if (dFile.isDirectory() && dFile.list().length == 0) {
			String parentName = dFile.getParent();
			dFile.delete();
			removeEmptyDirs(parentName, stopAtDirName);

		}
	}

	/**
	 * Sometimes *.tsv file has uuids in upper case. Logs error if lowcase cgHub
	 * uuid doesn't match TCGA uuid.
	 * 
	 * @param subj
	 * @param cgHubUUID
	 * @param tcgaUUID
	 */
	private void checkUUID(String subj, String cgHubUUID, String tcgaUUID) {
		if (!cgHubUUID.toLowerCase().equals(tcgaUUID))
			ErrorLog.log("BAMMetadataManager: NOT matching UUIDS: for " + subj
					+ " cgHubUUID: " + cgHubUUID + "  tcgaUUID: " + tcgaUUID);

	}

	/**
	 * 
	 * @param args
	 *            : 1. absolute path to the finished download *.tsv file 2.
	 *            nqSuff - suffix for a generated *.nq file HOW TO move:
	 *            SOURCE=/supercell/tcga_downloads/brca
	 *            DEST=/supercell/tcga/brca cd ${SOURCE}; find . -type d -exec
	 *            mkdir -p ${DEST}/\{} \; find . -type f -exec mv -i \{}
	 *            ${DEST}/\{} \;
	 * 
	 *            find . -type d -exec mkdir -p /supercell/tcga/gbm/\{} \; find
	 *            . -type f -exec mv -i \{} /supercell/tcga/gbm/\{} \;
	 * 
	 * 
	 *            Make sure to set the SOURCE and DEST variables. The first
	 *            "find" command makes any required directories under
	 *            /supercell/tcga/brca. The second find command moves all the
	 *            files to the appropriate directory. If the mv would overwrite
	 *            any existing file, it will prompt you whether you want to
	 *            proceed, but none of these files should already be in
	 *            /supercell/tcga/<dis>.
	 */
	public static void main(String[] args) {
		int sz = args.length;

		if (sz == 0) {

			BAMMetadataManager bm = new BAMMetadataManager();
			bm.postProcess();

			System.out.println("Done BAMMetadataManager ");
		} else if (sz == 1) {
			BAMMetadataManager bm = new BAMMetadataManager();
			bm.postProcessFinishedFile(args[0]);

			System.out.println("Done BAMMetadataManager ");
		} else
			System.out
					.println("No arguments - postProcess all finished files in '.finished.bam.tsv.dir' directory.\n"
							+ "1 argument - absolute path to finished_*.tsv file; post-process only one file.");

	}

}