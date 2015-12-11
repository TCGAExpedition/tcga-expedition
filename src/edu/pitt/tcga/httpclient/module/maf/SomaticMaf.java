package edu.pitt.tcga.httpclient.module.maf;

import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class SomaticMaf extends ProtectedMaf {

	private String[] endings = {".somatic.maf" };
	
	@Override
	public String dataAccessType() {
		return TCGAModule.PUBLIC;
	}
	
	@Override
	public String[] getResourceEndings(){
		return endings;
	}
	
	@Override
	public String getDataType() {
			return "Somatic_Mutations";

	}
	
	@Override
	public String getAnalysisDirName() {
		return "mutations";
	}
	
	
	@Override
	public String getResourceKey(){
		return "maf.somatic";
	}
	
	@Override
	public  String getPortion(){
		return "sm";
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MySettings.PGRR_META_NQ_FILE = "_LGG_ucsc_MAF_somatic.nq";
		
		SomaticMaf pmf = new SomaticMaf();
		String[] urls = {
				MySettings.PUB_ROOT_URL+"lgg/gsc/ucsc.edu/illuminaga_dnaseq_automated/mutations/"
				
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			pmf.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		System.out.println("done SomaticMaf");

	}

}
