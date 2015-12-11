package edu.pitt.tcga.httpclient.module.rnaseq.level2;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class BCGCC_IllumHi_RNA extends RNASeqLevel2Module{
	
	public BCGCC_IllumHi_RNA(){
		setNumOfSamples(1);
	}
	
	
	/**
	 * returns 
	 * @param origHeader
	 * @return <SAMPLE_ID , List<standard UMPC fields+ other fields>
	 */
	public Map<String, List<String>> toStandard(List<String> h) {
		Map<String, List<String>> toret = new LinkedHashMap<String, List<String>>();
		List<String> samp1 = new LinkedList<String>();
		// read in sample strings
		String[] samplesInfo = getSamplesInfo(h);

		String fileForamt = getFileFormat(h); 	//1
		samp1.add(fileForamt);
		String fileDate = "##file_date="+getFileDate(h);		//2
		samp1.add(fileDate);
		String center = "##center="+getCenter(h);			//3
		samp1.add(center);
		String sCenter = "##sequencing_center="+getSequencingCenter(h);//4
		samp1.add(sCenter);
		String platform = "##platform="+getPlatform(h, samplesInfo[0]);		//5
		samp1.add(platform);
		String platformVer = "##platform_version="+getPlatformVersion(h, samplesInfo[0]);//6
		samp1.add(platformVer);
		String ref = getReference(h);//7
		samp1.add("##reference="+ref);
		String refSrc = getReferenceSource(h);//8
		samp1.add("##reference_source="+refSrc);
		String patintID = "##patient_id="+getPatientBarcode(h, samplesInfo[0]); //9
		samp1.add(patintID);
		
		String sampleBarcode = null;
		String sampleID = null;
		String sInfo = samplesInfo[0];
		als[0].setInfo(sInfo);
		sampleBarcode = getSampleBarcode(sInfo);//10
		sampleID = getSampleID(sInfo);
		als[0].setBarcode(sampleBarcode);
		als[0].setId(sampleID);

		samp1.add("##specimen_id="+sampleBarcode);
		toret.put(sampleID, samp1);

		String alignPipiline = "##alignment_pipeline="+getAlignPipeline(h);//11
		samp1.add(alignPipiline);
		String alignPipilineVer = "##alignment_pipeline_version="+
				getAlignPipelineVer(h); //12
		samp1.add(alignPipilineVer);
		String vcPipeline = getVCPipeline(h, samplesInfo[0]);//13
		samp1.add("##vc_pipeline="+vcPipeline);
		String vcPipelineVer = getVCPipelineVer(h, samplesInfo[0]);//14
		samp1.add("##vc_pipeline_version="+vcPipelineVer);
		// bam files					//15
		samp1.add("##bam_file="+getBamFile(h, als[0].getInfo()));
		// bam file storage				//16
		samp1.add("##bam_file_storage="+getBamFileStorage(h, als[0].getInfo()));
		// add sample string
		samp1.add(als[0].getInfo());
		// set aliquot fields
		for(int k=0; k<als.length; k++){
			als[k].setDataAccessType(dataAccessType());
			als[k].setRefGenome(ref);
			als[k].setRefGenomeSource(refSrc);
			als[k].setCenterName(CodesUtil.getCenterNameFromArchive(als[k]
					.getArchiveName()));
			als[k].setCenterCode(CodesUtil.getCenterAbbFromArchName(als[k].getCenterName()));
			als[k].setPlatform(CodesUtil.getPlatform(als[k].getArchiveName()));

			als[k].setAlgorithmName(getVCPipelineForMetadata(h, samplesInfo[0]));
			//als[k].setAlgorithmVersion(vcPipelineVer);
		}
		
		setAdditionalAliquotInfo(als);
		
		
		// add all  metadata leftovers:
		for(String s:h){
			if(!s.startsWith("##INFO") &&
			   !s.startsWith("##FORMAT") &&
			   !s.startsWith("##FILTER") &&
			   !s.startsWith("##SAMPLE")){
				samp1.add(s);
			}
		}
		
		// add Standard Info 
		String toAdd = null;
		for (String s:ModuleUtil.infoArr){
			toAdd = findOrReplaceDesc(s, h);
			samp1.add(toAdd);
		}
		// add Info leftovers
		for(String s:h){
			if(s.startsWith("##INFO"))
				samp1.add(s);	
		}
		// add Standard Format 
		for (String s:ModuleUtil.formatArr){
			toAdd = findOrReplaceDesc(s, h);
			samp1.add(toAdd);
		}
		// add Format leftovers
		for(String s:h){
			if(s.startsWith("##FORMAT"))
				samp1.add(s);			
		}
		
		// add all Filter
		for(String s:h){
			if(s.startsWith("##FILTER"))
				samp1.add(s);				
		}
		
		samplesInfo = null;
		
		return toret;
		
	}


	@Override
	public String getReference(List<String> header) {
		// TODO implement MAGE-TAB use
		String toret = super.getReference(header).toLowerCase();
		if(toret.equalsIgnoreCase("hg18"))
			toret = "ncbi36.1";
		else if (toret.toLowerCase().startsWith("hg19"))
			toret = "hg19";
		return toret;
	}

	@Override
	public String getReferenceSource(List<String> header) {
		int ind = getLineFromList("##reference=", header);
		if (ind == -1) return "none";
		String ref = header.get(ind);
		ref = ref.substring(ref.toLowerCase().indexOf(",source=") + 8,
				ref.length() - 1);
		header.remove(ind);
		return ref;
	}

	@Override
	public String getPatientBarcode(List<String> header, String sInfo) {
		int ind = sInfo.indexOf("Individual=");
		if( ind == -1) return "none";
		return sInfo.substring(ind + 11, sInfo.indexOf(",", ind + 1));
	}
	
	@Override
	public void setAdditionalAliquotInfo(Aliquot[] als) {
		
		String tcgaFileName = als[0].getTCGAFileName();
		//String suff = (tcgaFileName.indexOf("hg19") != -1)?"hg19-":"hg18-";
		String suff = "";
		if (tcgaFileName.endsWith("indel.vcf"))
			suff = suff+"indel";
		else if (tcgaFileName.endsWith("snv.vcf"))
			suff = suff+"snv";
		else if (tcgaFileName.endsWith("sv.vcf"))
			suff = suff+"sv";
		else 
			ErrorLog.log("Unknown file exension in BCGCC_IllumHi_RNA. File: "+als[0].getTcgaPath());
		
		als[0].setPortion(suff);
	}
	

	@Override
	public String getAnalysisDirName() {
		return "rnaseq";
	}

	@Override
	public String getResourceKey() {
		return "rnaseq.level2";
	}
		
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		BCGCC_IllumHi_RNA m = new BCGCC_IllumHi_RNA();
		String[] urls = {
				MySettings.getControlledRoot()+"ov/cgcc/bcgsc.ca/illuminahiseq_rnaseq/rnaseq/bcgsc.ca_OV.IlluminaHiSeq_RNASeq.Level_2.1.6.0/"
		};
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}
		
		System.out.println("Done BCGCC_IllumHi_RNA");

	}


}
