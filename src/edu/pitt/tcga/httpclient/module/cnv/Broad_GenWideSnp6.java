package edu.pitt.tcga.httpclient.module.cnv;


import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class Broad_GenWideSnp6 extends CNVModule{
	
	private String[] endings = {".cel", ".idat"};
	//!!! USE one-by-one when search for the new centers
	//private String[] endings = {".cel"};
	
	private String[] acceptableEndingArr = {".cel", "sdrf.txt", ".txt"}; //using lower case for comparison
	
	@Override
	public String dataAccessType() {
		 return (level.equals("3"))?TCGAModule.PUBLIC:TCGAModule.CONTROLLED;
	}
	
	@Override
	protected void processMultiSampleFile(LineBean lb){ }

	
	@Override
	public String[] getResourceEndings() {
		return endings;
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
	protected String[] getAcceptableEndings(){
		return acceptableEndingArr;
	}	
	
	
	@Override
	protected int getFileColNum(DataMatrix dm, String lbName) {
		int occurance = 1;
		if("1".equals(getLevel())){
			
			return dm.getColumnNum("Array Data File");  
		}
		else if("2".equals(getLevel())){
			if(lbName.endsWith(".ismpolish.data.txt"))
				occurance = 1;
			else if(lbName.endsWith(".birdseed.data.txt"))
				occurance = 2;
			else if(lbName.endsWith(".raw.copynumber.data.txt"))
				occurance = 3;
			else if(lbName.endsWith(".byallele.copynumber.data.txt"))
				occurance = 4;
			else if(lbName.endsWith(".tangent.copynumber.data.txt"))
				occurance = 5;
			
			return dm.getColumnNum("Derived Array Data Matrix File",occurance); 
		}else {
			if(lbName.endsWith(".hg18.seg.txt"))
				occurance = 1;
			else if(lbName.endsWith(".hg19.seg.txt"))
				occurance = 2;
			else if(lbName.endsWith(".nocnv_hg18.seg.txt"))
				occurance = 3;
			else if(lbName.endsWith(".nocnv_hg19.seg.txt"))
				occurance = 4;
		}
		
		return dm.getColumnNum("Derived Array Data File",occurance); 	
	}
	
	@Override
	protected String getPortion(String defPortion, String lbName){
		String toret = defPortion; // for levels 1
		if("2".equals(getLevel())){
			if(lbName.endsWith(".ismpolish.data.txt"))
				toret = "ismpolish";
			else if(lbName.endsWith(".birdseed.data.txt"))
				toret = "birdseed";
			else if(lbName.endsWith(".raw.copynumber.data.txt"))
				toret = "raw-cn";
			else if(lbName.endsWith(".byallele.copynumber.data.txt"))
				toret = "byallele-cn";
			else if(lbName.endsWith(".tangent.copynumber.data.txt"))
				toret = "tangent-cn";
		}else if("3".equals(getLevel())){
			if(lbName.endsWith(".hg18.seg.txt") || lbName.endsWith(".hg19.seg.txt"))
				toret = "seg";
			
			else if(lbName.endsWith(".nocnv_hg18.seg.txt") || lbName.endsWith(".nocnv_hg19.seg.txt"))
				toret = "nocnv-seg";

		}
			
		return toret; 
	}
	
	@Override
	protected String getRefGenome(String lbName){
		if(lbName.indexOf("hg18.") != -1)
			return "hg18";
		else if (lbName.indexOf("hg19.") != -1)
			return "hg19";
		else return "unknown";
	}
	
	
	
	@Override
	protected String getAlgorithmName(String lbName) {
		String toret = "N/A";
		if("2".equals(getLevel())){
			if(lbName.endsWith(".ismpolish.data.txt"))
				toret = "Invariant Set Median-Polish Values";
			else if(lbName.endsWith(".birdseed.data.txt"))
				toret = "Birdseed Genotypes";
			else if(lbName.endsWith(".raw.copynumber.data.txt"))
				toret = "Copy-Numbers";
			else if(lbName.endsWith(".byallele.copynumber.data.txt"))
				toret = "Allele-Specific Copy-Numbers";
			else if(lbName.endsWith(".tangent.copynumber.data.txt"))
				toret = "Tangent Copy-Numbers";
		}
		else if("3".equals(getLevel())){
			toret = "Segmentation";	
		}
		
		return toret;
	}
	
	@Override
	protected String getFileType(String lbName) {
		//return "snp_Level_"+getLevel();
		String toret = "cel";
		if("2".equals(getLevel())){
			if(lbName.endsWith(".ismpolish.data.txt"))
				toret = "ismpolish";
			else if(lbName.endsWith(".birdseed.data.txt"))
				toret = "birdseed";
			else if(lbName.endsWith(".raw.copynumber.data.txt"))
				toret = "raw-cn";
			else if(lbName.endsWith(".byallele.copynumber.data.txt"))
				toret = "byallele-cn";
			else if(lbName.endsWith(".tangent.copynumber.data.txt"))
				toret = "tangent-cn";
		}
		else if("3".equals(getLevel())){
			if(lbName.endsWith(".hg18.seg.txt") || lbName.endsWith(".hg19.seg.txt"))
				toret = "seg";
			
			else if(lbName.endsWith(".nocnv_hg18.seg.txt") || lbName.endsWith(".nocnv_hg19.seg.txt"))
				toret = "nocnv-seg";

		}
		
		return toret;
	}
	
	
public static void main(String[] args){
	// below just test
		MySettings.PGRR_META_NQ_FILE = "_BRCA_BROAD_gwSNP6_TEST.nq";
		
		Broad_GenWideSnp6 pmf = new Broad_GenWideSnp6();
		String[] urls = {
				
				MySettings.getControlledRoot()+"brca/cgcc/broad.mit.edu/genome_wide_snp_6/snp/"
				
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			pmf.processArchiveLevel(TCGAHelper.recentOnly(list));
		}

		System.out.println("done Broad_GenWideSnp6");
	}
	

}
