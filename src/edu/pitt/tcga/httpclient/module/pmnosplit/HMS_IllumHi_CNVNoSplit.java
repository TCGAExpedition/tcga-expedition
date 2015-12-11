package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.util.List;

import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class HMS_IllumHi_CNVNoSplit extends UCSC_IllumGANoSplit{
	
	public String getDataType() {
		return "CNV_(Low_Pass_DNASeq)";
	}

	@Override
	public String getReference(DataMatrix header) {
		int ind = getLineFromList("##reference=", header,0);
		if (ind == -1) return "none";
		String ref = header.getRowColValue(0,ind);
		header.removeRow(ind);
		return ref.substring(12).toLowerCase();
	}

	@Override
	public String getReferenceSource(DataMatrix header) {
		int ind = getLineFromList("##assembly=", header,0);
		if (ind == -1) return "none";
		String ref = header.getRowColValue(0,ind);
		header.removeRow(ind);
		return ref.substring(11);
	}
	
	@Override
	public String getAnalysisDirName() {
		return "cna";
	}

	@Override
	public String getResourceKey() {
		return "cnv_low_pass_Level2";
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TCGAExpedition.createDirs();
		TCGAModule.clearTempoDir();
		
		HMS_IllumHi_CNVNoSplit m = new HMS_IllumHi_CNVNoSplit();
		String[] urls = {
				MySettings.getControlledRoot()+"blca/cgcc/hms.harvard.edu/illuminahiseq_dnaseqc/cna/hms.harvard.edu_BLCA.IlluminaHiSeq_DNASeqC.Level_2.1.0.0/"
		};
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}
		
		//FrankShell.desrtoy();
		System.out.println("Done HMS_IllumHi_CNVModule");
	}

}

