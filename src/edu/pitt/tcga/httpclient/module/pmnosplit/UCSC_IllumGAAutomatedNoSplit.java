package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.util.List;

import edu.pitt.tcga.httpclient.TCGAExpedition;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class UCSC_IllumGAAutomatedNoSplit extends UCSC_IllumGANoSplit{

	@Override
	public String getReferenceSource(DataMatrix header) {
		int ind = getLineFromList("##reference=", header,0);
		if (ind == -1 || header.getRowColValue(0,ind).toLowerCase().indexOf(",source=") == -1) {
			ind =  getLineFromList("##vcfGenerator", header,0);
			if(ind == -1)
				return "none";
			else { //special cases for {kich, thca}
				//dnaTumorFastaFilename=</inside/depot/fa/GRCh37-lite.fa>,
				//dnaTumorFastaFilename=</inside/depot/fa/Homo_sapiens_assembly19.fasta>,
				//--
				//dnaNormalFastaFilename=</inside/depot/fa/GRCh37-lite.fa>,
				//dnaNormalFastaFilename=</inside/depot/fa/Homo_sapiens_assembly19.fasta>,
				String refStr = header.getRowColValue(0,ind);
				int gInd = refStr.indexOf("dnaNormalFastaFilename");
				if(gInd == -1)
					return "none";
				else {
					refStr = refStr.substring(refStr.indexOf("<", gInd)+1,refStr.indexOf(">",gInd));
					return refStr;
				}
			}
		} else {
			String ref = header.getRowColValue(0,ind).toLowerCase();

			ref = ref.substring(ref.indexOf(",source=") + 8,
					ref.length() - 1);
			header.removeRow(ind);
			return ref;
		}
	}
	
	@Override
	public String getPlatform(DataMatrix header, String sampleStr) {	
		return super.getPlatform(header,  sampleStr).replaceAll("\"","");
	}
	
	@Override
	public String getBamFile(DataMatrix header, String sampleInfo) {
		return super.getBamFile(header,  sampleInfo).replaceAll("\"","");
	}
	
	public static void main(String[] args) {
		long stT = System.currentTimeMillis();
		
		MySettings.PGRR_META_NQ_FILE = "_TGCT_VCF_UCSC_IllumGAAutomated.nq";
		TCGAExpedition.setUniqueDirs();
		
		UCSC_IllumGAAutomatedNoSplit m = new UCSC_IllumGAAutomatedNoSplit();
		
		//automated
		/*String[] urls = {
				MySettings.getControlledRoot()+"kirp/gsc/ucsc.edu/illuminaga_dnaseq_cont_automated/mutations_protected/ucsc.edu_KIRP.IlluminaGA_DNASeq_Cont_automated.Level_2.1.1.0/"

		};*/
		
		// gzip
		String[] urls = {
				MySettings.getControlledRoot()+
				"thym/gsc/ucsc.edu/illuminaga_dnaseq_cont_automated/mutations_protected/ucsc.edu_THYM.IlluminaGA_DNASeq_Cont_automated.Level_2.1.0.0/"

		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}
		
		
		long endT = System.currentTimeMillis();
		System.out.println("done UCSC_IllumGAModuleNoSplit in "+ (endT-stT)/1000+" sec");

	}
}
