package edu.pitt.tcga.httpclient.module.cnv;

import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class Wustle_GenWideSnp6  extends CNVModule{

	private String[] acceptableEndingArr = {".cel", "sdrf.txt", ".dat"}; //using lower case for comparison
	

	@Override
	protected String[] getAcceptableEndings() {
		return acceptableEndingArr;
	}
	
	@Override
	protected void processMultiSampleFile(LineBean lb){ }

	@Override
	protected int getFileColNum(DataMatrix dm, String lbName) {	
		if("1".equals(getLevel())){
			
			return dm.getColumnNum("Array Data File");  
		}
		else if("2".equals(getLevel())){
			int occurance = -1;
			if(lbName.endsWith(".intensities.dat"))
				occurance = 1;
			else if(lbName.endsWith(".genotype.dat"))
				occurance = 2;
			else if(lbName.endsWith(".pairedcn.dat"))
				occurance = 3;
			else if(lbName.endsWith(".alleleSpecificCN.dat"))
				occurance = 4;
			
			return dm.getColumnNum("Derived Array Data Matrix File",occurance); 
		}else { // level3
			return dm.getColumnNum("Derived Array Data File",1); 	
		}
		
		
	}

	@Override
	protected String getAlgorithmName(String lbName) {
		String toret = "N/A";
		if("2".equals(getLevel())){
			toret = "DNACopy: circular binary segmentation";
		}
		else if("3".equals(getLevel())){
			toret = "Segmentation";	
		}
		
		return toret;
	}

	@Override
	protected String getFileType(String lbName) {
		String toret = "cel";
		if("2".equals(getLevel())){
			if(lbName.endsWith(".intensities.dat"))
				toret = "intensities";
			else if(lbName.endsWith(".genotype.dat"))
				toret = "genotype";
			else if(lbName.endsWith(".pairedcn.dat"))
				toret = "paired-cn";
			else if(lbName.endsWith(".alleleSpecificCN.dat"))
				toret = "byallele-cn";
			
		}else if ("3".equals(getLevel())){ // level3
			toret = "seg";	
		}
		
		return toret;
		
	}

	@Override
	protected String getPortion(String defPortion, String lbName) {
		String toret = defPortion; // for levels 1
		if("2".equals(getLevel())){
			if(lbName.endsWith(".intensities.dat"))
				toret = "intensities";
			else if(lbName.endsWith(".genotype.dat"))
				toret = "genotype";
			else if(lbName.endsWith(".pairedcn.dat"))
				toret = "paired-cn";
			else if(lbName.endsWith(".alleleSpecificCN.dat"))
				toret = "byallele-cn";
			
		}else if ("3".equals(getLevel())){ // level3
			toret = "seg";	
		}
		
		return toret;
	}

	@Override
	protected String getRefGenome(String lbName) {
		return "unknown";
	}

	@Override
	public String getAnalysisDirName() {
		return "snp";
	}

	@Override
	public String getResourceKey() {
		return "cnv_snp";
	}

	@Override
	public String dataAccessType() {
		return (level.equals("3"))?TCGAModule.PUBLIC:TCGAModule.CONTROLLED;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MySettings.PGRR_META_NQ_FILE = "_BRCA_BROAD_gwSNP6_TEST.nq";
		
		Wustle_GenWideSnp6 pmf = new Wustle_GenWideSnp6();
		String[] urls = {
				
				// below just test
				//MySettings.getControlledRoot()+"laml/cgcc/genome.wustl.edu/genome_wide_snp_6/snp/"
				MySettings.PUB_ROOT_URL+"laml/cgcc/genome.wustl.edu/genome_wide_snp_6/snp/"
				
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			pmf.processArchiveLevel(TCGAHelper.recentOnly(list));
		}

		System.out.println("done Wustle_GenWideSnp6");
	}

}
