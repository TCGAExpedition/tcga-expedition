package edu.pitt.tcga.httpclient.module.msi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;


import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;
/**
 * Only the tumor sample barcode is listed in the mage-tab as a source for both normal and tumor *.fsa files. 
 * For example, for aliquot barcode TCGA-FZ-5919-01A-01D-YYYY-23
 * fsa files:
 *   TCGA-FZ-5919-01A-11D_OPTIPLEX-XE_2015-02-05_040.fsa - tumor
 *   TCGA-FZ-5919-11A-02D_OPTIPLEX-XE_2015-02-05_040.fsa - solid tissue normal
 * and one .txt file:
 *   TCGA-FZ-5919-01A-01D-YYYY-23.txt
 *
 * The normal sample aliquot is unknown - I can derive only the analyte barcode (like TCGA-FZ-5919-11A-02D for the above example).
 * In the TCGA metadata browser (https://tcga-data.nci.nih.gov/uuid/uuidBrowser.htm) only the tumor aliquot (TCGA-FZ-5919-01A-01D-YYYY-23 ) 
 * can be found as a source for MSC analysis and not for normal analyte TCGA-FZ-5919-11A-02D.
 * 
 * Therefore, both *.fsa files are going into the tumour sample directory (they should hhave different portions and reference coresponding TCGa *.fsa files
 * @author opm1
 *
 */

public class MSI_Module extends TCGAModule{

	private String[] endings =  {".sdrf.txt", ".txt", ".fsa"};
	protected DataMatrix dataMatrix = null;

	@Override
	public String[] getResourceEndings() {
		return endings;
	}

	@Override
	public String getDataType() {
		return "Fragment_Analysis_Results";
	}

	@Override
	public String getAnalysisDirName() {
		return "fragment_analysis";
	}

	@Override
	public String getResourceKey() {
		return "msi";
	}

	@Override
	public String dataAccessType() {
		return TCGAModule.CONTROLLED;
	}

	@Override
	public boolean canProcessArchive(LineBean archiveBean) {
		String archiveName = archiveBean.getName();
		return (archiveName.indexOf(".Level_") != -1  || archiveName.indexOf(".mage-tab.") != -1);
	}

	@Override
	public void processData(List<LineBean> levBeans) {
		int sz = levBeans.size();
		int cn = 0;
		for (LineBean lb : levBeans) {
				if (acceptableEnding(lb.getName(), getResourceEndings()) && !TCGAHelper.stringContains(lb.getName(), MySettings.infoFilesList)) {		
			
					cn++;

					clearTempoDir();
					System.gc();
								
		System.out.println(lb.getDiseaseStudy()+" : "+cn+" out of "+sz+"  lb.n = " + lb.getUrl());
		
					Aliquot al = null;
					try{
										
						if(lb.getName().endsWith(".sdrf.txt")){
							if(dataMatrix == null)
								dataMatrix = new DataMatrix("MageTabSDRF");
							else 
								dataMatrix.clear();
							
							readDataToMatrix(dataMatrix, lb.getFullURL(), '\t');
	
						}else {
							al = constructAliquot(lb);	
							File tempoFile = new File(MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".txt");
							TEMP_FILE_NUM++;
							if(al != null){
								al.setTempoFile(tempoFile);
								TCGAHelper.copyURLToFile(new URL(lb.getFullURL()), tempoFile, dataAccessType().equals("controlled"));
							}
						}
						
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if(al != null){
						ModuleUtil.transferNew(new Aliquot[]{al});	
					}
		
				} //fNameEnding != null
	
		} // for(LineBean lb:levBeans)
		
	}
	
	/**
	 * ATT: This column has INCORECT barcodes!!! (see paad:
	 * TCGA-F2-A44G-01A-11D_OPTIPLEX-XE_2015-01-29_033.fsa AND TCGA-F2-A44G-10A-01D_OPTIPLEX-XE_2015-01-29_033.fsa
	 * have the same barcode: TCGA-F2-A44G-01A-01D-YYYY-23
	 *
	 * @return
	 */
	public String getBarcodeColName() {
		return "Comment [TCGA Barcode]";
	}
	
	public String getFileExtension(String lbName) {
		return lbName.substring(lbName.lastIndexOf(".")+1);
	}
	
	
	public  Aliquot constructAliquot(LineBean lb){
		String lbName = lb.getName();
	
		int rowNum = dataMatrix.getDataRow(lbName, getFileColNum(dataMatrix, lbName));
System.out.println("ROW: "+rowNum+" for "+lbName);	
		if (rowNum > -1) {
			Aliquot al = new Aliquot(lb.getUrl(), getDataType(),
					getFileExtension(lb.getName()));
			
			al.setLevel("1");
			getFileExtension(lb.getName());
			al.setBarcode(dataMatrix.getRowColValue(getBarcodeColName(), rowNum));
	System.out.println("BC: "+al.getBarcode());
			
			al.setOrigUUID(ModuleUtil.getUUIDByBarcode(al.getBarcode()));
			al.setFileFractionType("aliquot");

			al.setCenterName(CodesUtil.getCenterNameFromArchive(al.getArchiveName()));
			al.setCenterCode(CodesUtil.getCenterAbbFromArchName(al.getCenterName()));
			al.setPlatform(lb.getPlatform());
			al.setFileType(getFileType(lbName));
			al.setPortion(getPortion(al.getPortion(), lbName));
			return al;
		} else {
			String err = "MSI_Module: can't find in mage-tab a row for "+lb.getFullURL()+"\n name: "+lbName;
			System.err.println(err);
			ErrorLog.logFatal(err);
			return null;
		}
	}
	
	public String getPortion(String defPortion, String lbName){
		if (lbName.endsWith(".fsa"))
			return "fsa";
		else
			return defPortion; 
	}
	
	public String getFileType(String lbName){
		if (lbName.endsWith(".fsa"))
			return "fsa";
		else 
			return "txt";	
	}
	
	
	
	public int getFileColNum(DataMatrix dm, String lbName) {
		if (lbName.endsWith(".fsa"))
			return dm.getColumnNum("Derived Data File",1); 
		else
			return dm.getColumnNum("Derived Data File",2); //for ".txt"		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TCGAExpedition.setUniqueDirs();
		TCGAModule.clearTempoDir();
		
		MSI_Module m = new MSI_Module();
		String[] urls = {
				MySettings.getControlledRoot()+"paad/cgcc/nationwidechildrens.org/microsat_i/fragment_analysis/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		
		System.out.println("Done MSI_Module");

	}

}

