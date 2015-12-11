package edu.pitt.tcga.httpclient.module.eg;

import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class HMS_Illum_MRNA_DGE extends ExpGeneModule{

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
		else if("3".equals(getLevel())){
			if(lbName.endsWith("_genes.txt"))
				return dm.getColumnNum("Derived Array Data File",3); 
			else
				return dm.getColumnNum("Derived Array Data File",2); // for "_tags.txt"
		}
			
		else return dm.getColumnNum("Derived Array Data File",1); // for level 2
	}
	
	public String getPortion(String defPortion, String lbName){
		if("3".equals(getLevel())){
			if(lbName.endsWith("_genes.txt"))
				return "genes"; 
			else
				return "tags"; // for "_tags.txt"
		}
			
		else return defPortion; // for levels 1, 2
	}

	@Override
	public String getBarcodeColName() {
		return "Comment [TCGA Barcode]";
	}

	@Override
	public String getAlgorithmName() {
		if("1".equals(getLevel()))
			return "qcFilteredRawReads";
		else if("2".equals(getLevel()))
			return "frequencyDistribution";
		else return "tagMapping";
	}

	@Override
	public String getFileType(String lbName) {
		return "expGene_Level_"+getLevel();
	}

	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
				
		HMS_Illum_MRNA_DGE m = new HMS_Illum_MRNA_DGE();
		String[] urls = {
				MySettings.PUB_ROOT_URL+"ov/cgcc/hms.harvard.edu/illuminaga_mrna_dge/transcriptome/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		System.out.println("Done HMS_Illum_MRNA_DGE");
	}

}
