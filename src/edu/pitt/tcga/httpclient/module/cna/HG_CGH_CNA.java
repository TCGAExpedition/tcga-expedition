package edu.pitt.tcga.httpclient.module.cna;

import java.util.List;

import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;
/**
 * 
 * GBM only. No normal samples
 * Level3: per sample, ERROR: need to replace "." with "_" in 'sample' column to match the mage-tab names
 * Genome Ref:  UCSC hg18 (NCBI Build 36), March 2006 (from www.genomics.agilent.com/article.jsp?pageId=1464) - my "hg18"
 * see: https://wiki.nci.nih.gov/pages/viewpage.action?pageId=71434962&navigatingVersions=true
 * also: https://tcga-data.nci.nih.gov/tcga/tcgaPlatformDesign.jsp
 * @author opm1
 *
 * @author opm1
 * @version 1
 * @since Dec 11, 2015
 *
 */

public class HG_CGH_CNA extends CNAModule{
	private String[] endings = {".txt",".mat", "sdrf.txt", "tsv"};

	@Override
	protected String[] getAcceptableEndings() {
		return endings;
	}

	@Override
	protected int getFileColNum(DataMatrix dm, String lbName) {
		int occurance = 1;
		if("1".equals(getLevel())){
			
			return dm.getColumnNum("Array Data File");  
		}
		else if("2".equals(getLevel())){
			return dm.getColumnNum("Derived Array Data Matrix File",occurance); 
		}else  //level 3
		
			return dm.getColumnNum("Derived Array Data File",occurance); 	
	}

	

	@Override
	protected String getFileType(String lbName) {
		String toret = "raw";
		if("2".equals(getLevel()))
			toret = "normalized";
		else if("3".equals(getLevel())){
			toret = "segmented";	
		}
		return toret;
	}
	
	@Override
	protected String getAlgorithmName(String lbName) {
		String toret = "N/A";
		if("2".equals(getLevel()))
			toret = "Normalized";
		else if("3".equals(getLevel())){
			toret = "Segmented";	
		}
		return toret;
	}

	@Override
	protected String getRefGenome(LineBean lb) {
		String lbUrl = lb.getUrl();
		String refName = "";
		if(lbUrl.indexOf("cgcc/mskcc.org/hg-cgh-244a/") != -1){
			refName = "ncbi36_mskcc_cgh-244a";
		}
		else if(lbUrl.indexOf("cgcc/hms.harvard.edu/hg-cgh-244a/") != -1){
			refName = "ncbi36_harvard_cgh-244a";
		}
		else if(lbUrl.indexOf("cgcc/hms.harvard.edu/hg-cgh-415k_g4124a/") != -1){
			refName = "ncbi36_harvard_cgh-415k-g4124a";
		}
		else if(lbUrl.indexOf("cgcc/mskcc.org/cgh-1x1m_g4447a/") != -1){
			refName = "ncbi36.1_mskcc_cgh-1x1m-g4447a";
		}
		return refName;
		
	}

	@Override
	protected void processMultiSampleFile(LineBean lb) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String getResourceKey() {
		return "cnv_cna";
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// below just test
		MySettings.PGRR_META_NQ_FILE = "_GBM_MSKCC_cna_TEST.nq";
		
		HG_CGH_CNA pmf = new HG_CGH_CNA();
		String[] urls = {
				//MySettings.PUB_ROOT_URL+"lusc/cgcc/hms.harvard.edu/hg-cgh-415k_g4124a/cna/"	// 36
				MySettings.PUB_ROOT_URL+"lusc/cgcc/mskcc.org/cgh-1x1m_g4447a/cna/" 				// 36.1
				
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			pmf.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		System.out.println("done HG_CGH_CNA");

	}

}
