package edu.pitt.tcga.httpclient.module.eg;

import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class AffymetrixHG_U133_plus_2 extends AffymetrixHT_HG_U133A {


	@Override
	public String getAlgorithmName() {
		if("1".equals(getLevel()))
			return "N/A";
		else if("2".equals(getLevel()))
			return "MAS5";
		else return "Averaging";
	}

	
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		AffymetrixHG_U133_plus_2 m = new AffymetrixHG_U133_plus_2();
		String[] urls = {
				MySettings.PUB_ROOT_URL+"laml/cgcc/genome.wustl.edu/hg-u133_plus_2/transcriptome/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		System.out.println("Done AffymetrixHG_U133_plus_2");
	}

}

