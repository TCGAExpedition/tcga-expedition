package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.util.List;

import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class HGSC_IllumSolidNoSplit extends UCSC_IllumGANoSplit {

	@Override
	public String getCenter(DataMatrix header) {
		return (super.getCenter(header)).replaceAll("\"", "");
	}

	@Override
	public String getPlatform(DataMatrix header, String sampleStr) {
		int ind = sampleStr.indexOf("Platform=");
		if (ind == -1) return "none";
		ind = ind+9;
		String toret = sampleStr.substring(ind, sampleStr.indexOf(",",ind+1));
		toret = toret.replaceAll("\"", "");
		
		return Character.toUpperCase(toret.charAt(0)) + toret.substring(1);
	}

	@Override
	public String getPlatformVersion(DataMatrix header, String sampleStr) {
		return "none";
	}

	@Override
	public String getReferenceSource(DataMatrix header) {
		int ind = getLineFromList("##reference=", header,0);
		if (ind == -1) return "none";
		String ref = header.getRowColValue(0,ind);
		// no quotes around
		ref = ref.substring(ref.indexOf(",source=") + 8,
				ref.length() - 1);
		header.removeRow(ind);
		if(ref.equals("."))
			ref = "none";
		return ref;
	}

	@Override
	public String getPatientBarcode(DataMatrix header, String sInfo) {
		return CodesUtil.getPatientBarcode(getSampleBarcode(sInfo));
	}

	@Override
	public String getVCPipeline(DataMatrix header, String sampleInfo) {
		int ind = sampleInfo.indexOf("softwareName=<");
		if(ind == -1) return "none";
		ind = ind+14;
		return sampleInfo.substring(ind, sampleInfo.indexOf(">",ind+1));
	}

	@Override
	public String getVCPipelineVer(DataMatrix header, String sampleInfo) {
		int ind = sampleInfo.indexOf("softwareVer=<");
		if(ind == -1) return "none";
		ind = ind+13;
		return sampleInfo.substring(ind, sampleInfo.indexOf(">",ind+1));
	}

	@Override
	public String getBamFile(DataMatrix header, String sampleInfo) {
		return (super.getBamFile(header, sampleInfo)).replaceAll("\"", "");
	}
	
	public static void main(String[] args) {
		
		MySettings.PGRR_META_NQ_FILE = "_TGCT_VCF_IlluminaGA_DNASeq_Cont_automated.nq";
		TCGAExpedition.setUniqueDirs();
		
		HGSC_IllumSolidNoSplit m = new HGSC_IllumSolidNoSplit();

		
		String[] urls = {
				MySettings.getControlledRoot()+
				"thym/gsc/hgsc.bcm.edu/illuminaga_dnaseq_cont_automated/mutations_protected/hgsc.bcm.edu_THYM.IlluminaGA_DNASeq_Cont_automated.Level_2.1.0.0/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}
		
		System.out.println("Done HGSC_IllumModuleNoSplit");

	}

	
}


