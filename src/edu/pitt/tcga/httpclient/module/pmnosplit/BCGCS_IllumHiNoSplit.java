package edu.pitt.tcga.httpclient.module.pmnosplit;

import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class BCGCS_IllumHiNoSplit extends BI_IllumGANoSplit{
	
	/*##fileformat=VCFv4.1
			##fileDate=20140315
			##tcgaversion=1.1
			##vcfProcessLog=<InputVCF=<all.somatic.snvs.vcf>,InputVCFSource=<strelka>,InputVCFVer=<1.0.6>,InputVCFParam=<isSkipDepthFilters=1>,InputVCFgeneAnno=<none>>
			##reference=<ID=hg19,Source=http://www.bcgsc.ca/downloads/genomes/Homo_sapiens/hg19/1000genomes/bwa_ind/genome/GRCh37-lite.fa>
			##center=BCGSC
			##phasing=none
			##source=strelka
			##source_version=2.0.7
			##SAMPLE=<ID=NORMAL,SampleUUID=4af05379-ed6d-488f-932c-43921f43e40b,
			SampleTCGABarcode=TCGA-BC-4073-10A-01D-A12Z-10,Individual=TCGA-BC-4073,
			softwareName=strelka,softwareVer=<1.0.6>,softwareParam=<isSkipDepthFilters=1>,
			File=TCGA-BC-4073-10A-01D-A12Z-10_Illumina.bam,
			Platform=IlluminaHiSeq_DNASeq,Source=CGHUB,Accession=0a7a7807-77c1-41a5-a48b-bcb6e6936f6a>

##SAMPLE=<ID=TUMOR,SampleUUID=fcc3deff-4d29-489d-834f-f2c98fd750d8,SampleTCGABarcode=TCGA-BC-4073-01B-02D-A12Z-10,Individual=TCGA-BC-4073,softwareName=strelka,softwareVer=<1.0.6>,softwareParam=<isSkipDepthFilters=1>,File=TCGA-BC-4073-01B-02D-A12Z-10_Illumina.bam,Platform=IlluminaHiSeq_DNASeq,Source=CGHUB,Accession=65f2da5f-e47f-4038-95ba-da2f403a9635>
*/

	
	@Override
	public String getVCPipeline(DataMatrix header, String sampleInfo) {
		//actually remove ##source= from the header;
		int ind = getLineFromList("##source=", header,0);
		ind = getLineFromList("##vcfProcessLog=", header,0);
		if (ind == -1) return "none";
		String s = header.getRowColValue(0,ind);
		ind = s.indexOf("InputVCFSource=<")+16;
		if (ind == -1) return "none";

		return s.substring(ind, s.indexOf(">",ind+1));
	}
	
	@Override
	public String getPatientBarcode(DataMatrix header,String sInfo) {
		int ind = sInfo.indexOf("Individual=");
		if( ind == -1) return "none";
		String patient_id = sInfo.substring(11, sInfo.indexOf(",",ind+1));
		return patient_id;	
	}
	
	@Override
	public String getReferenceSource(DataMatrix header) {
		int ind = getLineFromList("##reference=", header,0);
		if (header.getRowColValue(0,ind).toLowerCase().indexOf(",source=") == -1)
			return "none";
		String ref = header.getRowColValue(0,ind).toLowerCase();

		ref = ref.substring(ref.indexOf(",source=") + 8,
				ref.length() - 1);
		header.removeRow(ind);
		return ref;
	}
	
	@Override
	public String getBamFile(DataMatrix header, String sampleInfo) {
		int ind = sampleInfo.indexOf("File=");
		if(ind == -1)
			return "none";
		return sampleInfo.substring(ind+5, sampleInfo.indexOf(",", ind+1));
	}

	@Override
	public String getBamFileStorage(DataMatrix header, String sampleInfo) {
		return "none";
	}
	
	public static void main(String[] args) {
		long stT = System.currentTimeMillis();
		
		TCGAModule.clearTempoDir();
		
		BCGCS_IllumHiNoSplit m = new BCGCS_IllumHiNoSplit();
		String[] urls = {
				MySettings.getControlledRoot()+"lihc/gsc/bcgsc.ca/illuminahiseq_dnaseq_cont_automated/mutations_protected/bcgsc.ca_LIHC.IlluminaHiSeq_DNASeq_Cont_automated.Level_2.1.0.0/"

		};
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processData(list);
		}
		
		
		long endT = System.currentTimeMillis();
		System.out.println("done BCGCS_IllumHiNoSplit in "+ (endT-stT)/1000+" sec");


	}

}
