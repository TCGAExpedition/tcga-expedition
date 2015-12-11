package edu.pitt.tcga.httpclient.module.rnaseq.level2;

import java.util.List;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class BCGCC_IllumGA_RNA extends BCGCC_IllumHi_RNA{

	@Override
	public String getReference(List<String> header) {
		int ind = getLineFromList("##reference=", header);
		if (ind == -1) return "none";
		String ref = header.get(ind).toLowerCase();

		return ref.substring(16, ref.indexOf(",")).toLowerCase();
	}
	
	@Override
	public String getPatientBarcode(List<String> header,String sInfo) {
		int ind = getLineFromList("##INDIVIDUAL=",header);
		if (ind != -1)
			header.remove(ind);
		
		String patient_id = CodesUtil.getPatientBarcode(getSampleBarcode(sInfo));
		return patient_id;	
	}
	
	@Override
	public void setAdditionalAliquotInfo(Aliquot[] als) {
		String tcgaFileName = als[0].getTCGAFileName();
		String suff = "";
		if (tcgaFileName.endsWith("sv.vcf"))
			suff = "sv";
		else if (tcgaFileName.endsWith("snv.vcf"))
			suff = "snv";
		else if (tcgaFileName.endsWith(".indel."))
			suff = "indel";
		else 
			ErrorLog.log("Unknown file exension in BCGCC_IllumGA_RNA. File: "+als[0].getTcgaPath());
		
		als[0].setPortion(suff);
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
TCGAModule.clearTempoDir();
		
BCGCC_IllumGA_RNA m = new BCGCC_IllumGA_RNA();
		String[] urls = {
				MySettings.getControlledRoot()+"laml/cgcc/bcgsc.ca/illuminaga_rnaseq/rnaseq/bcgsc.ca_LAML.IlluminaGA_RNASeq.Level_2.1.2.0/"
		};
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}
		
		System.out.println("Done BCGCC_IllumGA_RNA");
	}

}
