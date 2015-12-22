package edu.pitt.tcga.httpclient.correction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.MySettings;

/**
 * Used for debugging. Correct Metadata.tsv log files. 
 * 
 * If ModuleUtil.SAVE_METADATA = true, these log files will be created in each repository directory
 * and contain the same info as in pgrr_meta table (Postgres) or pgrr-meta graph (Virtuoso)
 * 
 * @author opm1
 *
 */
public class MetadataHelper {

	/**
	 * Corrects Metadata.tsv file.
	 * All arrays MUST be of the same size
	 * 
	 * @param dir with Metadata.tsv file
	 * @param lookupColName
	 * @param lookupColVals
	 * @param colsToBeCorected
	 * @param nawValues
	 */
	public static void correctMetadata(String dir, String[] lookupColNames,
			String[] lookupColVals, String[] colsToBeCorected,
			String[] newValues) {
		DataMatrix dm = getMetadata(dir);
		if (dm != null) {
			int row = 0, col = 0;
			int sz = lookupColVals.length;
			for (int i = 0; i < sz; i++) {
				row = dm.getDataRow(lookupColVals[i],
						dm.getColumnNum(lookupColNames[i]));
				col = dm.getColumnNum(colsToBeCorected[i]);
				dm.setValue(row, col, newValues[i]);
			}
			List<String[]> dmList = dm.asList();
			System.out.println(" ********************************");
			System.out.println("PATH: " + dir);
			for (String[] sArr : dmList)
				System.out.println(" ** ROW: " + Arrays.asList(sArr));

			overwriteFile(dir + MySettings.METADATA_FILE, dmList);
			dm.clear();
		}

	}

	/**
	 * Returns DataMatrix representation of Metadata.rsv file.
	 * 
	 * @param dir with Metadata.tsv file
	 * @return
	 */
	public static DataMatrix getMetadata(String dir) {
		return DataMatrix.getDataMatrix(dir + MySettings.METADATA_FILE, '\t');
	}

	/**
	 * Completely overwrites Metadata.tsv file.
	 * 
	 * @param filePath
	 * @param list - List of new lines
	 */
	public static void overwriteFile(String filePath, List<String[]> list) {
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(
					filePath), false));
			output.write(ModuleUtil.listArrayToString(list, "\t"));

			list.clear();
			list = null;

			output.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
