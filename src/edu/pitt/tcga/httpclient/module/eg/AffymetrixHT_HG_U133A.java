package edu.pitt.tcga.httpclient.module.eg;

import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class AffymetrixHT_HG_U133A extends ExpGeneModule {

	

	@Override
	public String getFileExtension(String lbName) {
		int ind = lbName.lastIndexOf(".");
		// TODO Auto-generated method stub
		return lbName.substring(ind+1);
	}

	@Override
	public int getFileColNum(DataMatrix dm, String lbName) {
		if("1".equals(getLevel())){
			
			return dm.getColumnNum("Array Data File");  
		}
		else if("2".equals(getLevel())){
			return dm.getColumnNum("Derived Array Data File",1); 
		}
			
		else return dm.getColumnNum("Derived Array Data File",2); 
	}

	@Override
	public String getBarcodeColName() {
		return "Comment [TCGA Barcode]";
	}

	@Override
	public String getAlgorithmName() {
		if("1".equals(getLevel()))
			return "N/A";
		else if("2".equals(getLevel()))
			return "RMA";
		else return "Averaging";
	}

	@Override
	public String getFileType(String lbName) {
		return "expGene_Level_"+getLevel();
	}
	
	
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		AffymetrixHT_HG_U133A m = new AffymetrixHT_HG_U133A();
		String[] urls = {
				//MySettings.PUB_ROOT_URL+"ov/cgcc/bcgsc.ca/illuminahiseq_rnaseq/rnaseq/"  
				//MySettings.PUB_ROOT_URL+"laml/cgcc/bcgsc.ca/illuminaga_rnaseq/rnaseq/"
				//MySettings.PUB_ROOT_URL+"read/cgcc/unc.edu/illuminaga_rnaseq/rnaseq/"
				//MySettings.PUB_ROOT_URL+"brca/cgcc/unc.edu/illuminahiseq_rnaseq/rnaseq/"
				MySettings.PUB_ROOT_URL+"ov/cgcc/broad.mit.edu/ht_hg-u133a/transcriptome/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		System.out.println("Done AffymetrixHT_HG_U133A");
	}

}
