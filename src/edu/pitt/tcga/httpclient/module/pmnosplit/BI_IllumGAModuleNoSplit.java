package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class BI_IllumGAModuleNoSplit extends UCSC_IllumGAModuleNoSplit{
	// <barcode, tempoFile>
	private Map<String, Aliquot>tempoAliquotMap = new HashMap<String, Aliquot>();
	private List<Aliquot> mergedAliquots = new ArrayList<Aliquot>();

	@Override
	public String getSampleBarcode(String sInfo) {
		String bc = super.getSampleBarcode(sInfo);
		if(!"none".equals(bc))
			return bc;
		int ind = sInfo.indexOf("SampleName=");
		if( ind == -1) return "none";
		return sInfo.substring(ind + 11, sInfo.indexOf(",", ind + 1));
	}
	
	
	@Override
	public String getVCPipeline(DataMatrix header, String sampleInfo) {
		int ind = sampleInfo.indexOf("softwareName=<");
		if(ind == -1)
			return "none";
		ind = ind+14;
		return sampleInfo.substring(ind, sampleInfo.indexOf(">",ind+1));
		
	}

	@Override
	public String getVCPipelineVer(DataMatrix header, String sampleInfo) {
		int ind = sampleInfo.indexOf("softwareVer=<");
		if(ind == -1)
			return "none";
		ind = ind+13;
		return sampleInfo.substring(ind, sampleInfo.indexOf(">",ind+1));
	}

	@Override
	public String getBamFile(DataMatrix header, String sampleInfo) {
		return "none";
	}

	@Override
	public String getBamFileStorage(DataMatrix header, String sampleInfo) {
		return "none";
	}
	
	@Override
	public Aliquot[] prepareForTransfer(Aliquot[] als){
// check if this is read ...
	
		if(als[0].getTCGAFileName().endsWith(".vcf.fix.vcf"))
			return super.prepareForTransfer(als);
		
		for(Aliquot al:als){
			if (tempoAliquotMap.get(al.getBarcode()) == null)
				tempoAliquotMap.put(al.getBarcode(), al);
			else {
				mergeVCFs(tempoAliquotMap.get(al.getBarcode()), al);
				tempoAliquotMap.remove(al.getBarcode());	
			}
		}
			
		if(tempoAliquotMap.size() == 0){
			Aliquot[] toret = mergedAliquots.toArray(new Aliquot[0]);  
			mergedAliquots.clear();
			return toret;
		}
		return null;
	}
	
	public void mergeVCFs(Aliquot al1, Aliquot al2){
		String saveTo = MySettings.TEMPO_DIR + "merge"+String.valueOf(TEMP_FILE_NUM)+".vcf";
		TEMP_FILE_NUM++;
		File out = MergeVCF.merge(al1.getTempoFile().getPath(), 
				al2.getTempoFile().getPath(), saveTo);
		if(out == null){
			ErrorLog.logFatal("Can't mergeVCFs: for "+al1.getBarcode()+
					"  path_1: "+al1.getTcgaPath()+"    path_2: "+al2.getTcgaPath());
		}
		
		al1.setTempoFile(out);
		al1.setSubType(null);
		al1.setTCGAFileName(al1.getTCGAFileName()+"; "+al2.getTCGAFileName());
		mergedAliquots.add(al1);
	}
	
	@Override
	public boolean canDeleteTempoFiles(){
		return tempoAliquotMap.size() == 0?true:false;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		BI_IllumGAModuleNoSplit m = new BI_IllumGAModuleNoSplit();
		String[] urls = {
		MySettings.getControlledRoot()+"blca/gsc/broad.mit.edu/illuminaga_dnaseq_cont/mutations_protected/broad.mit.edu_BLCA.IlluminaGA_DNASeq_Cont.Level_2.0.3.0/"
		};

		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
	
		}
		
		//FrankShell.desrtoy();
		System.out.println("Done BI_IllumHiModule");

	}

	
	
}