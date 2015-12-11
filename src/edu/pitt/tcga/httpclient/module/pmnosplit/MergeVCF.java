package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.MySettings;

/**
 * merges snp and indel files
 * 
 * @author opm1
 * 
 */

public class MergeVCF {
	
	private static Double longSz = 1000000.0;

	public static File merge(String inFile1, String inFile2, String outFile) {
		File out = new File(outFile);
		// find the biggest file
		long f1sz = (new File(inFile1)).length();
		long f2sz = (new File(inFile2)).length();
		String longFile = null, shortFile = null;
		if (f1sz >= f2sz) {
			longFile = inFile1;
			shortFile = inFile2;
		} else {
			longFile = inFile2;
			shortFile = inFile1;
		}

		CSVReader rShort = null, rLong = null;
		BufferedWriter writer = null;
		List<String[]> sList = null, longList = null;

		// small is going to CVSReader, bigger - line by line for header only
		try {
			rShort = new CSVReader(new FileReader(shortFile), '\t');
			sList = rShort.readAllToList();
			longList =null;
			// remove first two lines
			sList.remove(0);
			sList.remove(0);
		
			rLong = new CSVReader(new BufferedReader(new FileReader(longFile)),
					'\t');

			writer = new BufferedWriter(new FileWriter(out));

			boolean isHeader = true;
			String[] readLine = null;
			int sameRecordInd = -1;
			int numRec = 0;
			while ((readLine = rLong.readNext()) != null && isHeader) {
				// remove the same line in short
				if (!readLine[0].startsWith("#CHROM")) {
					sameRecordInd = -1;
					for(int k = 0; k < sList.size(); k++){
						if(sList.get(k)[0].equals(readLine[0])){
							sameRecordInd = k;
							break;
						}	
					}
					if(sameRecordInd > -1)
						sList.remove(sameRecordInd);
				}
				else  { //if (readLine[0].startsWith("#CHROM"))
					isHeader = false;
						int  removeNumRows = 0;
						for (int k = 0; k < sList.size(); k++) {
							removeNumRows++;
							if (sList.get(k)[0].startsWith("#CHROM")) {
								break;
							}
							// write short header leftovers
							writer.write(ModuleUtil.copyArrayToStr(
									sList.get(k), MySettings.TAB));
						}
						// remove all header lines in sList
						if(removeNumRows > 0 ){
							for(int n=0; n < removeNumRows; n++){
								sList.remove(0);
							}
						}
					}
	
					writer.write(ModuleUtil.copyArrayToStr(readLine,
							MySettings.TAB));
					numRec++;
			} // end isHeader
			
			rLong.close();
			rLong = new CSVReader(new FileReader(longFile), '\t');
			longList = rLong.readAllToList(numRec);
			longSz = Double.valueOf(longList.size());
			longList.addAll(sList);
			Collections.sort(longList, new Comparator<String[]> () {
			    public int compare(String[] a, String[] b) {
			    	int c = getChromAsNumber(a[0]).compareTo(getChromAsNumber(b[0]));
			    	return c == 0?Integer.valueOf(a[1]).compareTo(Integer.valueOf(b[1])):c;
			    }
			});
			for(String[]s:longList)
				writer.write(ModuleUtil.copyArrayToStr(s,
						MySettings.TAB));
				

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
			try {
				sList.clear();
				sList = null;
				longList.clear();
				longList = null;
				if (rShort != null)
					rShort.close();
				if (rLong != null)
					rLong.close();
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return out;
	}

	public static Double getChromAsNumber(String chStr) {
		try {
			return Double.valueOf(chStr);
		} catch (NumberFormatException e) {
			if (chStr.equalsIgnoreCase("X"))
				return 23.0;
			else if (chStr.equalsIgnoreCase("Y"))
				return 24.0;
			else if (chStr.equalsIgnoreCase("<M>"))
				return 25.0;
			else if (chStr.toLowerCase().startsWith("<gl")) {
				try {
					Double toret = Double.parseDouble(chStr.substring(3,
							chStr.indexOf(">")));
					return toret+longSz;
				} catch (NumberFormatException ex) {
					return longSz;
				}
			} else
				return longSz;
		}
	}

	public static void main(String[] args) {
		long stT = System.currentTimeMillis();
		// TODO Auto-generated method stub
		/*
		 * String f1 = "/home/rcrowley/opm1/tcgaFeb12/tempo/f9.vcf"; String f2 =
		 * "/home/rcrowley/opm1/tcgaFeb12/tempo/f11.vcf"; String out =
		 * "/home/rcrowley/opm1/tcgaFeb12/tempo/StAloneMerge.vcf";
		 */
		String f1 = "C:/DevSrc/cvs/tcgaMonitor/tests/indel.vcf";
		String f2 = "C:/DevSrc/cvs/tcgaMonitor/tests/snp.vcf";
		String out = "C:/DevSrc/cvs/tcgaMonitor/tests/cur.vcf";
		merge(f1, f2, out);
		
		long endT = System.currentTimeMillis();

		System.out.println("done with merge in "+(endT-stT)/1000 +" sec");
	}

}
