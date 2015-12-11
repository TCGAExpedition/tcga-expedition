package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class WUSTL_IllumHiNoSplit extends UCSC_IllumGANoSplit{
	
	private Pattern digitsOnlyPattern = Pattern.compile("^[1-9]+$");


	@Override
	public String getReference(DataMatrix header) {
		int ind = getLineFromList("##reference=", header,0);
		if (ind == -1) return "none";
		String ref = header.getRowColValue(0,ind).toLowerCase();
		if(ref.indexOf("grch37-lite") != -1)
			return "grch37-lite";
		
		// handle error:
		ErrorLog.log("WUSTL_IllumHiModule: no genome reference for: "+als[0].getTcgaPath());
		return "none";
	}

	@Override
	public String getReferenceSource(DataMatrix header) {
		int ind = getLineFromList("##reference=", header,0);
		if (ind == -1) return "none";
		String toret = header.getRowColValue(0,ind).substring(12);
		header.removeRow(ind);
		return toret;
	}

	@Override
	public String getPatientBarcode(DataMatrix header, String sInfo) {
		return CodesUtil.getPatientBarcode(getSampleBarcode(sInfo));
	}

	/**
	 * If no attached "-<varcaller>", add "unknown
	 * Example: 
	 * TCGA-83-5908-01A-21D-2284-08	TCGA-83-5908-01A-21D-2284-08-Samtools	
	 * TCGA-83-5908-10A-01D-2284-08	TCGA-83-5908-10A-01D-2284-08-Sniper	
	 * TCGA-83-5908-01A-21D-2284-08-Sniper	
	 * TCGA-83-5908-10A-01D-2284-08-VarscanSomatic	
	 * TCGA-83-5908-01A-21D-2284-08-VarscanSomatic	
	 * TCGA-83-5908-10A-01D-2284-08-Strelka	TCGA-83-5908-01A-21D-2284-08-Strelka
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
				variant =  val.substring(val.lastIndexOf("-")+1);
			
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

	@Override
	public String getVCPipelineVer(DataMatrix header, String sampleInfo) {
		return "none";
	}
	
	/**
	 * remove those with "-Variant" attached
	 */
	@Override
	public Aliquot[] prepareForTransfer(Aliquot[] als){
		List<Aliquot> list = new ArrayList<Aliquot>();
		for (Aliquot al:als){
			if(al.getId().length() == 28)
				list.add(al);
			else
				al.clear();
		}
		als = null;
		return list.toArray(new Aliquot[list.size()]);
	}
	
	/** 
	 * Do nothing, since this type of analysis might be removed in future
	 */
	@Override
	public int[] renameAndReorderCols(String[] chromRow, 
			Map<String, String> idBarcodeMap){
		int[] indices = new int[chromRow.length];
		for(int i= 0; i < chromRow.length; i++)
			indices[i] = i;
		return indices;
	}
	
	
	@Override
	public void setAdditionalAliquotInfo(Aliquot[] als) {
		String suffPart = null, tcgaFileName = null;
		int numPart = -1;
		for(Aliquot a:als){
			tcgaFileName = a.getTCGAFileName();
			if(tcgaFileName.indexOf(".indel.") != -1){
				a.setPortion("indel-"+getSuffix(tcgaFileName, ".indel."));
			}
			else if (tcgaFileName.indexOf(".snv.") != -1){
				a.setPortion("snv-"+getSuffix(tcgaFileName, ".snv."));
			}else
				ErrorLog.log("WUSTL_IllumHiModule: No .indel. or .snv. in finame. Can't set Portion: "+a.getTcgaPath());;
		}
	}
	
	/**
	 * if suffixPart just a number - use it, otherwise, it should be ""
	 */
	private String getSuffix(String tcgaFileName, String preSuff){
		int numPart = tcgaFileName.indexOf(preSuff) + preSuff.length();
		String suffPart = tcgaFileName.substring(numPart,tcgaFileName.indexOf(".",numPart+1));
		return (TCGAHelper.getMatch(suffPart, digitsOnlyPattern, 0)).equals("")?"":suffPart;
		
	}

	
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		WUSTL_IllumHiNoSplit m = new WUSTL_IllumHiNoSplit();
		String[] urls = {
				MySettings.getControlledRoot()+"luad/gsc/genome.wustl.edu/illuminahiseq_dnaseq_cont/mutations_protected/genome.wustl.edu_LUAD.IlluminaHiSeq_DNASeq_Cont.Level_2.1.0.0/"
		};
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}

		System.out.println("Done WUSTL_IllumHiModule");

	}

}

