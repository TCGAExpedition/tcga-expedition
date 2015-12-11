package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.util.List;

import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class UCSC_SolidNoSplit extends UCSC_IllumGANoSplit{

	@Override
	public String getReference(DataMatrix header) {
		int ind = getLineFromList("##reference=", header,0);
		if (ind == -1) return "none";
		String ref = header.getRowColValue(0,ind);

		return ref.substring(17, ref.indexOf(" "));
	}
	
	
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		UCSC_SolidNoSplit m = new UCSC_SolidNoSplit();
		String[] urls = {
				//MySettings.getControlledRoot()+"coad/gsc/ucsc.edu/solid_dnaseq_cont/mutations_protected/ucsc.edu_COAD.SOLiD_DNASeq_Cont.Level_2.1.1.0/"
				//,
				MySettings.getControlledRoot()+"read/gsc/ucsc.edu/solid_dnaseq_cont/mutations_protected/ucsc.edu_READ.SOLiD_DNASeq_Cont.Level_2.1.1.0/"
		};
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}
		
		//FrankShell.desrtoy();
		System.out.println("Done UCSC_SolidModule");

	}

}
