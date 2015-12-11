package edu.pitt.tcga.httpclient.module.mirnaseq;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.module.eg.Agilent;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.ReferenceGenomeUtil;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

/**
 * Has only Level 3 data in TCGA
 * @author opm1
 *
 */

public class miRNASeqModule extends TCGAModule{

	private String[] endings =  {".sdrf.txt", ".txt"};
	protected DataMatrix dataMatrix = null;

	@Override
	public String[] getResourceEndings() {
		return endings;
	}

	@Override
	public String getDataType() {
		return "miRNASeq";
	}

	@Override
	public String getAnalysisDirName() {
		return "mirnaseq";
	}

	@Override
	public String getResourceKey() {
		return "mirnaseq";
	}

	@Override
	public String dataAccessType() {
		return TCGAModule.PUBLIC;
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
								FileUtils.copyURLToFile(new URL(lb.getFullURL()), tempoFile);
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
	
	
	public String getBarcodeColName() {
		return "Comment [TCGA Barcode]";
	}
	
	public String getFileExtension(String lbName) {
		return "txt";
	}
	
	public String getRefGenColName(){
	return "Comment [Genome reference]";
	}
	
	public  Aliquot constructAliquot(LineBean lb){
		String lbName = lb.getName();
	
		int rowNum = dataMatrix.getDataRow(lbName, getFileColNum(dataMatrix, lbName));

		
		if (rowNum > -1) {
			Aliquot al = new Aliquot(lb.getUrl(), getDataType(),
					getFileExtension(lb.getName()));
			
			al.setLevel("3");
			getFileExtension(lb.getName());
			al.setBarcode(dataMatrix.getRowColValue(getBarcodeColName(), rowNum));
			
			al.setOrigUUID(ModuleUtil.getUUIDByBarcode(al.getBarcode()));
			al.setFileFractionType("aliquot");

			al.setCenterName(CodesUtil.getCenterNameFromArchive(al.getArchiveName()));
			al.setCenterCode(CodesUtil.getCenterAbbFromArchName(al.getCenterName()));
			al.setRefGenome(dataMatrix.getRowColValue(getRefGenColName(),rowNum).toLowerCase());
			al.setRefGenomeSource(ReferenceGenomeUtil.getGenomeURL(al.getRefGenome()));
			al.setPlatform(lb.getPlatform());
			al.setFileType(getFileType(lbName));
			al.setPortion(getPortion(al.getPortion(), lbName));
			return al;
		} else {
			String err = "miRNASeqModule: can't find in mage-tab a row for "+lb.getFullURL()+"\n name: "+lbName;
			System.err.println(err);
			ErrorLog.logFatal(err);
			return null;
		}
	}
	
	public String getPortion(String defPortion, String lbName){
		if(lbName.indexOf(".isoform.") != -1)
			return "isoform";
		else if(lbName.indexOf(".mirna.") != -1)
			return "mirna";	
			
		else return defPortion; 
	}
	
	public String getFileType(String lbName){
		if(lbName.indexOf(".isoform.") != -1)
			return "isoform";
		else 
			return "mirna";	
	}
	
	
	
	public int getFileColNum(DataMatrix dm, String lbName) {
			return dm.getColumnNum("Derived Data File",1); 
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TCGAExpedition.setUniqueDirs();
		TCGAModule.clearTempoDir();
		
		miRNASeqModule m = new miRNASeqModule();
		String[] urls = {
				MySettings.PUB_ROOT_URL+"stad/cgcc/bcgsc.ca/illuminahiseq_mirnaseq/mirnaseq/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		
		System.out.println("Done miRNASeqModule");

	}

}
