package edu.pitt.tcga.httpclient.module.methyl;
/**
 * DNA humanMethylation platforms 27 and 450
 */

import java.io.File;
import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class HumanMethyl extends MethylModule{
	
	private String level = null;

	@Override
	public String getFileExtension(String lbName) {
		if("1".equals(getLevel()))
			return "idat";
		else
			return "txt";
	}

	@Override
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
		return "Comment [TCGA Barcode]";
	}


	@Override
	public String getAlgorithmName(String archiveName) {
		return archiveName.toLowerCase().indexOf("methylation27") != -1?"Illumina Infinium HumanMethylation27":"Illumina Infinium HumanMethylation450";
		
	}
	
	
	
	@Override
	public String getFileType(String lbName) {
		if("1".equals(getLevel()))
			return lbName.toLowerCase().endsWith("red.idat")?"methyl_red_Level_1":"methyl_grn_Level_1";
		else
			return "methyl_Level_"+getLevel();
	}
	
	@Override
	public String getRefGenome(int rowNum){
		return dataMatrix.getRowColValue("Comment [Genome reference]", rowNum).toLowerCase();
	}
	
	@Override
	public String getRefGenomeSource(String archiveName){
		if(archiveName.toLowerCase().indexOf("methylation27") != -1)
			return "https://tcga-data.nci.nih.gov/docs/integration/adfs/tcga/jhu-usc.edu_TCGA_HumanMethylation27.v2.adf.txt.zip";
		else
			return "https://tcga-data.nci.nih.gov/docs/integration/adfs/tcga/jhu-usc.edu_TCGA_HumanMethylation450.adf.txt.zip";
	}
	
	
	@Override
	public String getPortion(String defPortion, String lbName){
		if("1".equals(getLevel()))
			return lbName.toLowerCase().endsWith("red.idat")?"red":"grn";
		else
			return defPortion;
	}
	
	@Override
	public boolean needADF(){
		return false;
	}
	
	@Override
	public void setLevel(String l){
		level = l;
	}
	
	@Override
	public String getLevel(){
		return level;
	}
	
	@Override
	public boolean needTempoFile(){
		return false;
	}
	
	@Override
	public void constructFile(String lbFullURL, File saveAs, String archiveName){
	}
	
	
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		HumanMethyl m = new HumanMethyl();
		String[] urls = {
				MySettings.PUB_ROOT_URL+"hnsc/cgcc/jhu-usc.edu/humanmethylation450/methylation/"
				//MySettings.PUB_ROOT_URL+"ov/cgcc/jhu-usc.edu/humanmethylation450/methylation/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		
		System.out.println("Done HumanMethyl");

	}

}
