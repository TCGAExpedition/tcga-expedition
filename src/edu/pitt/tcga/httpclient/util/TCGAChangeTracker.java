package edu.pitt.tcga.httpclient.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;

public class TCGAChangeTracker {
	private Map<String, String> fileStatus = new HashMap<String, String>();

	/**
	 * 
	 * @param curBean
	 *            - archive folder to be examined
	 * @param parentList
	 *            - list of all archives
	 * @return map of fileName, fileStatus (
	 */
	public static Map<String, String> getExperimentRevisionsChanges(
			LineBean curBean, List<LineBean> parentList) {
		List<String> newerFolders = inBetweenExperimentFolders(curBean,
				parentList);
System.out.println(" FOR "+curBean.getName()+"  archiveNameUpToRevision: "+curBean.archiveNameUpToRevision());
		// get list of filenames in the current folder:
		Map<String, String> curFiles = dataFileNamesWithStatus(curBean
				.getFullURL());
		// integrate changes up to current version
		for (String n : newerFolders) {
			LineBean lb = LineBeanHelper.getBeanByName(n, parentList);
System.out.println(" *************************** inBetweenExperimentFolders "+lb.getName());
			List<String[]> changes = revisionOfDataFilesOnly(TCGAHelper.changeDCC(lb.getFullURL()
					+ MySettings.CHANGES));

			// update status
			for (String[] sArr : changes) {
				if(!(curFiles.get(sArr[0]) != null && curFiles.get(sArr[0]).equals("+")))
					curFiles.put(sArr[0], sArr[1]);
			}
			
		}
		return curFiles;
	}

	/**
	 * filter out all the info files
	 * 
	 * @param list
	 * @return
	 */
	public static List<String[]> revisionOfDataFilesOnly(List<String[]> list){
		List<String[]> toret = new ArrayList<String[]>();
		String n= null;
		for (String[] sArr:list){
			n = sArr[0];
System.out.println("BEFOR ADD: n: "+n+"   ARR"+Arrays.asList(sArr));
			if (n != null && !n.equals("") && !TCGAHelper.stringContains(n,
					MySettings.excludedHREFList) && !TCGAHelper.stringContains(n, MySettings.infoFilesList)
			&& !n.contains(MySettings.CHANGES) && !n.startsWith("#")){
	System.out.println("ADDING: n: "+n+"   ARR"+Arrays.asList(sArr));
				toret.add(sArr);
			}
		}
		list.clear();
		list = null;
		return toret;
		
	}

	/**
	 * 
	 * @param curBean
	 * @param parentList
	 * @return all folders up to the most recent one in incremental order
	 */
	public static List<String> inBetweenExperimentFolders(LineBean curBean,
			List<LineBean> parentList) {
		int[] curExRev = curBean.getCenterExperiment();
		List<String> toret = new ArrayList<String>();
		String archNameUpToRevision = curBean.archiveNameUpToRevision();
		int[] nums = null;
		for (LineBean lb : parentList) {
			if(lb.getName().startsWith(archNameUpToRevision)){
				nums = lb.getCenterExperiment();
				if (nums[0] == curExRev[0] && nums[1] > curExRev[1])
					toret.add(lb.getName());
			}
		}
		return sortAccending(toret);
	}

	/**
	 * filters out all the info, tar.gz and other file names
	 * 
	 * @param folderName
	 * @return <fileName, fileStatus{"U", "R", "+", "-"}>
	 */
	public static Map<String, String> dataFileNamesWithStatus(String url) {
		HttpClient httpclient = TCGAHelper.getHttpClient();
		Map<String, String> names = new HashMap<String, String>();
		try {
			InputStream in = TCGAHelper.getGetResponseInputStream(httpclient,
					url);
			if (in == null)
				return names;

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));

			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					if (line.contains("a href")
							&& !TCGAHelper.stringContains(line,
									MySettings.excludedHREFList)
							&& !TCGAHelper.stringContains(line,
									MySettings.infoFilesList)
							&& !line.contains(MySettings.CHANGES))
						names.put(TCGAHelper.getMatch(line, TCGAHelper.hrefP),
								"U"); //
				}
				return names;

			} catch (IOException e) {
				e.printStackTrace();
				return names;
			} finally {
				try {
					in.close();
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	/**
	 * Sort experiment in "human" revision order. So that "xx.2.0" would be in
	 * front of "xx.12.0"
	 */
	public static List<String> sortAccending(List<String> list) {
		String[] strArr = new String[list.size()];
		int pos = 0, ind = 0;
		String interm = null;
		for (String s : list) {
			interm = s.substring(0, s.lastIndexOf("."));
			pos = interm.lastIndexOf("."); // experiment revision
			interm = interm.substring(pos + 1);
			// add "0" in front of one digit number
			if (interm.length() == 1) {
				interm = new StringBuilder(s).insert(pos + 1, "0").toString();
				s = interm;
			}
			strArr[ind] = s;
			ind++;
		}

		Arrays.sort(strArr);
		// now remove "0" in front of revision
		ind = 0;
		for (String s : strArr) {
			interm = s.substring(0, s.lastIndexOf("."));
			pos = interm.lastIndexOf("."); // experiment revision
			interm = interm.substring(pos + 1);
			if (interm.startsWith("0"))
				strArr[ind] = new StringBuilder(s)
						.replace(pos + 1, pos + 2, "").toString();
			ind++;
		}

		return Arrays.asList(strArr);
	}

	public static void track(LineBean currTCGAFolder, LineBean latestTCGAFolder) {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.setProperty("https.proxyHost", "tcga-data.nci.nih.gov");
		
		/*
		 * Map<String,String> m = dataFileNamesWithStatus(
		 * "https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/brca/bcr/nationwidechildrens.org/bio/clin/nationwidechildrens.org_BRCA.bio.Level_1.216.4.0/"
		 * ); for (Map.Entry<String, String> entry : m.entrySet())
		 * System.out.println("k: "+entry.getKey()+"   v: "+entry.getValue());
		 */

		List<LineBean> parentList = TCGAHelper
				.getPageBeans("https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/brca/cgcc/unc.edu/illuminahiseq_rnaseqv2/rnaseqv2/");
		LineBean curBean = new LineBean(
				"https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/brca/cgcc/unc.edu/illuminahiseq_rnaseqv2/rnaseqv2/unc.edu_BRCA.IlluminaHiSeq_RNASeqV2.Level_3.1.7.0/");

		/*
		 * List<String> r = inBetweenExperimentFolders(curBean, parentList);
		 * for(String s:r) System.out.println("s: "+s);
		 */

		Map<String, String> m = getExperimentRevisionsChanges(curBean,
				parentList);

		// Map<String, String> m =
		// dataFileNamesWithStatus(curBean.getFullURL());
		/*for (Map.Entry<String, String> entry : m.entrySet())
			System.out.println("k: " + entry.getKey() + "   v: "
					+ entry.getValue());*/
	}

}
