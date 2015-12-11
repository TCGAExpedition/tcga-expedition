package edu.pitt.tcga.httpclient.module.eg;

import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class Agilent extends ExpGeneModule {


	@Override
	public String getFileExtension(String lbName) {
		return "txt";
	}

	@Override
	/*
	 * for Agilent *_7_3
	 * @see edu.pitt.tcga.httpclient.module.eg.ExpGeneModule#getFileColNum()
	 */
	public int getFileColNum(DataMatrix dm, String lbName) {
		if("1".equals(getLevel())){
			
			return dm.getColumnNum("Array Data File");  
		}
		else if("2".equals(getLevel())){
			return dm.getColumnNum("Derived Array Data Matrix File",1); 
		}
			
		else return dm.getColumnNum("Derived Array Data Matrix File",2); 
	}

	@Override
	public String getBarcodeColName() {
		return "Source Name";
	}


	@Override
	public String getAlgorithmName() {
		if("1".equals(getLevel()))
			return "N/A";
		else if("2".equals(getLevel()))
			return "Lowess";
		else return "Averaging";
		
	}

	@Override
	public String getFileType(String lbName) {
		return "expGene_Level_"+getLevel();
	}
	
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		Agilent m = new Agilent();
		String[] urls = {
				MySettings.PUB_ROOT_URL+"brca/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		
		System.out.println("Done Agilent");

	}

}
