package edu.pitt.tcga.httpclient.module.pmnosplit;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class WUSTL_IllumHiAutomatedNoSplit extends WUSTL_IllumHiNoSplit{
	
	/**
	 * If no attached "-<varcaller>", add "unknown
	 * Example: 
	 * TCGA-83-5908-01A-21D-2284-08	TCGA-83-5908-01A-21D-2284-08-[Samtools]	
	 * TCGA-83-5908-10A-01D-2284-08	TCGA-83-5908-10A-01D-2284-08-[Sniper]	
	 * TCGA-83-5908-01A-21D-2284-08-[Sniper]	
	 * TCGA-83-5908-10A-01D-2284-08-[VarscanSomatic]	
	 * TCGA-83-5908-01A-21D-2284-08-[VarscanSomatic]	
	 * TCGA-83-5908-10A-01D-2284-08-[Strelka]	TCGA-83-5908-01A-21D-2284-08-[Strelka]
	 */
	@Override
	public String getVCPipeline(DataMatrix header, String sampleInfo) {
		Map<String, String> sampleIDBarcode = getSampleIdBarcodeMap(header);
		List<String> vars = new ArrayList<String>();
		String val = null,variant = null;
		StringBuilder sb = new StringBuilder();
		
		for (Map.Entry<String, String> entry : sampleIDBarcode.entrySet()) {
			val = entry.getKey();
			if(val.length() == 28 )
				variant = "unknown";
			else 
				variant =  val.substring(val.lastIndexOf("-[")+2, val.length()-1);
			
			if(!vars.contains(variant)){
				vars.add(variant);
				sb.append(variant+";");
			}
		}
		
		sb.setLength(sb.length() - 1);
		sampleIDBarcode.clear();
		sampleIDBarcode = null;
		vars.clear();
		vars = null;
		return sb.toString();
	}

	public static void main(String[] args) {
		
		MySettings.PGRR_META_NQ_FILE = "_THYM_VCF_WUSTL_IllumHIAutomated.nq";
		TCGAExpedition.setUniqueDirs();
		
		TCGAModule.clearTempoDir();
		
		WUSTL_IllumHiAutomatedNoSplit m = new WUSTL_IllumHiAutomatedNoSplit();
		String[] urls = {
				MySettings.getControlledRoot()+
				"esca/gsc/genome.wustl.edu/illuminahiseq_dnaseq_cont_automated/mutations_protected/genome.wustl.edu_ESCA.IlluminaHiSeq_DNASeq_Cont_automated.Level_2.1.1.0/"
		};
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}
		System.out.println("Done WUSTL_IllumHiModule");

	}

}
