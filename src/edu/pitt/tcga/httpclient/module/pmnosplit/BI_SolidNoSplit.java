package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class BI_SolidNoSplit extends UCSC_IllumGANoSplit{

	@Override
	public String getCenter(DataMatrix header) {
		return (super.getCenter(header)).replaceAll("\"", "");
	}

	@Override
	public String getSampleBarcode(String sInfo) {
		int ind = sInfo.indexOf("SampleName=");
		if( ind == -1) return "none";
		return sInfo.substring(ind + 11, sInfo.indexOf(",", ind + 1));
	}

	@Override
	public String getBamFile(DataMatrix header, String sampleInfo) {
		int ind = sampleInfo.indexOf("File=");
		if (ind == -1) return "none";
		ind = ind+5;
		int endInd = sampleInfo.indexOf(",",ind+1);
		if (endInd == -1)
			endInd = sampleInfo.indexOf(">",ind+1); // might be an end of the string
		return  sampleInfo.substring(ind, endInd);
	}
	
	
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		BI_SolidNoSplit m = new BI_SolidNoSplit();
		String[] urls = {
				MySettings.getControlledRoot()+"read/gsc/broad.mit.edu/solid_dnaseq_cont/mutations_protected/broad.mit.edu_READ.SOLiD_DNASeq_Cont.Level_2.1.2.0/"
		};
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}

		System.out.println("Done BI_SolidModuleNoSplit");

	}

}

