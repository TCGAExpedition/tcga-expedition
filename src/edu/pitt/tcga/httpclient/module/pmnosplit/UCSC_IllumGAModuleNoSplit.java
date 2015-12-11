package edu.pitt.tcga.httpclient.module.pmnosplit;



	import java.util.Collections;
	import java.util.HashMap;
	import java.util.LinkedList;
	import java.util.List;
	import java.util.Map;
	import java.util.Set;

	import edu.pitt.tcga.httpclient.module.Aliquot;
	import edu.pitt.tcga.httpclient.module.TCGAModule;
	import edu.pitt.tcga.httpclient.util.DataMatrix;
	import edu.pitt.tcga.httpclient.util.LineBean;
	import edu.pitt.tcga.httpclient.util.MySettings;
	import edu.pitt.tcga.httpclient.util.TCGAHelper;

	public class UCSC_IllumGAModuleNoSplit extends ProtectedMutationsNoSplit{
		
		@Override
		public int[] renameAndReorderCols(String[] chromRow, Map<String, String> idBarcodeMap){
			List<String> origList = new LinkedList<String>();
			int sstSamplesPos = -1; // actually an offset 
			Set<String> keys = idBarcodeMap.keySet();
			for(int i = 0; i < chromRow.length; i++){
				if(keys.contains(chromRow[i])){
					if(sstSamplesPos == -1)
						sstSamplesPos = i;
					chromRow[i] = idBarcodeMap.get(chromRow[i]);
					origList.add(chromRow[i]);
				}
			}
			
			// create copy and sort it
			List<String> sortedList = new LinkedList<String>();
			sortedList.addAll(origList);
			Collections.sort(sortedList);
			int[] indices = getNewIndices(origList, sortedList);
			
			int[] toret = new int[chromRow.length];
			
			for(int k = 0; k < sstSamplesPos; k++){
				toret[k] = k;
			}		
			//add new indices
			for(int kk = 0; kk < indices.length; kk++){
				toret[kk+sstSamplesPos] = indices[kk]+sstSamplesPos;
			}
			
			origList.clear();
			origList = null;
			sortedList.clear();
			sortedList = null;
			indices = null;
			
			return toret;
		}
		
		
		@Override
		/**
		 * return <sampleID {actually, name of column in data}, samleBarcode>
		 */
		public Map<String, String> getSampleIdBarcodeMap(DataMatrix header){
			Map<String, String> toret = new HashMap<String, String>();
			List<Integer> rows = header.getStartsWithDataRows("##SAMPLE=", 0);
			String sInfo = null;
			for(Integer r:rows){
				sInfo = header.getRowColValue(0, r);
				toret.put(getSampleID(sInfo), getSampleBarcode(sInfo));
			}
			return toret;
		}

		@Override
		public String getFileFormat(DataMatrix header) {
			int ind = getLineFromList("##fileformat=", header,0);
			if (ind == -1) return "none";
			String toret = header.getRowColValue(0,ind);
			header.removeRow(ind);
			return toret;
		}
		
		
		

		@Override
		public String getFileDate(DataMatrix header) {
			int ind = getLineFromList("##fileDate=", header,0);
			if (ind == -1) return "none";
			String toret = header.getRowColValue(0,ind).substring(header.getRowColValue(0,ind).indexOf("=")+1);
			header.removeRow(ind);
			return toret;
		}

		@Override
		public String getCenter(DataMatrix header) {
			int ind = getLineFromList("##center=", header,0);
			if (ind == -1) return "none";
			String toret = header.getRowColValue(0,ind).substring(header.getRowColValue(0,ind).indexOf("=")+1);
			toret = toret.replaceAll("\"","");
			header.removeRow(ind);
			return toret;
		}

		@Override
		public String getSequencingCenter(DataMatrix header) {
			return "none";
		}

		@Override
		public String getPlatform(DataMatrix header, String sampleStr) {	
			int ind = sampleStr.indexOf("Platform=");
			if (ind == -1) return "none";
			ind = ind+9;
			return sampleStr.substring(ind, sampleStr.indexOf(",",ind+1));
		}
		
		@Override
		public String getSampleUUID (String sampleStr){
			int ind = sampleStr.indexOf("SampleUUID=");
			if (ind == -1) return "";
			ind = ind+11;
			return sampleStr.substring(ind, sampleStr.indexOf(",",ind+1));	
		}

		@Override
		public String getPlatformVersion(DataMatrix header, String sampleStr) {
			int ind = sampleStr.indexOf("Platform=");
			if (ind == -1) return "none";
			ind = ind+9;
			String platform = sampleStr.substring(ind, sampleStr.indexOf(",",ind+1));
			String platform_version = "none";
			if(platform.indexOf(".") !=-1)
				platform_version = platform.substring(platform.indexOf(".")+1);
			return platform_version;
		}

		@Override
		public String getReference(DataMatrix header) {
			int ind = getLineFromList("##reference=", header,0);
			if (ind == -1 || header.getRowColValue(0,ind).indexOf(",") == -1) {
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
						refStr = refStr.substring(gInd, refStr.indexOf(">", gInd));
						String revStr = new StringBuilder(refStr).reverse().toString();
						// str starts with atsaf. or af.
						revStr = revStr.substring(revStr.indexOf(".")+1, revStr.indexOf("/"));
						return new StringBuilder(revStr).reverse().toString().toLowerCase();
					}
				}
			}
			else {
				String ref = header.getRowColValue(0,ind).replaceAll("\"","");
				//if (ref.indexOf(",") == -1)
				//	return "none";

			return ref.substring(16, ref.indexOf(",")).toLowerCase();
			}
		}

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
				//if (ref.indexOf(",source=") == -1)
				//	return "none";
				ref = ref.substring(ref.indexOf(",source=") + 9,
						ref.length() - 2);
				header.removeRow(ind);
				return ref;
			}
		}

		@Override
		public String getPatientBarcode(DataMatrix header,String sInfo) {
			int ind = getLineFromList("##INDIVIDUAL=",header,0);
			if( ind == -1) return "none";
			String patient_id = header.getRowColValue(0,ind).substring(13);
			header.removeRow(ind);
			return patient_id;	
		}

		@Override
		public String getSampleBarcode(String sInfo) {
			int ind = sInfo.indexOf("SampleTCGABarcode=");
			if( ind == -1) return "none";
			return sInfo.substring(ind + 18, sInfo.indexOf(",", ind + 1));
		}

		@Override
		public String getSampleID(String sInfo) {
			int ind = sInfo.indexOf("##SAMPLE=<ID=");
			if( ind == -1) return "none";
			return sInfo.substring(ind + 13,sInfo.indexOf(","));
		}
		
		@Override
		public String[] getSamplesInfo(DataMatrix header){
			List<Integer> poss = header.getStartsWithDataRows("##SAMPLE",0);
			String[] toret = new String[poss.size()];
			int i = 0;
			for(Integer p:poss){
				toret[i] = header.getRowColValue(0, p);
				i++;		
			}
			return toret;
		}

		@Override
		public String getAlignPipeline(DataMatrix header) {
			return "none";
		}

		@Override
		public String getAlignPipelineVer(DataMatrix header) {
			return "none";
		}

		@Override
		public String getVCPipeline(DataMatrix header, String sampleInfo) {
			//actually remove ##source= from the header;
			int ind = getLineFromList("##source=", header,0);
			if (ind != -1) 
				header.removeRow(ind);
			ind = getLineFromList("##vcfProcessLog=", header,0);
			if (ind == -1) return "none";
			String s = header.getRowColValue(0,ind);
			ind = s.indexOf("InputVCFSource=<")+16;
			if (ind == -1) return "none";

			return s.substring(ind, s.indexOf(">",ind+1));
		}

		@Override
		public String getVCPipelineVer(DataMatrix header, String sampleInfo) {
			int ind = getLineFromList("##vcfProcessLog=", header,0);
			if (ind == -1) return "none";
			String s = header.getRowColValue(0,ind);
			ind = s.indexOf("InputVCFVer=<")+13;
			if (ind == -1) return "none";

			return s.substring(ind, s.indexOf(">",ind+1));
		}
		
		@Override
		public String getBamFile(DataMatrix header, String sampleInfo) {
			int ind = sampleInfo.indexOf("File=");
			if (ind == -1) return "none";
			ind = ind+5;
			return  sampleInfo.substring(ind, sampleInfo.indexOf(",",ind+1));
		}

		@Override
		public String getBamFileStorage(DataMatrix header, String sampleInfo) {
			return "none";
		}
		
		@Override
		public void setAdditionalAliquotInfo(Aliquot[] als) {
			String tcgaFileName = null;
			String portion = null;
			for (Aliquot a:als){
				portion = a.getPortion();
				tcgaFileName = a.getTCGAFileName();
				if (tcgaFileName.indexOf("_blood") != -1)
					portion = portion+"-"+getFullPortion(tcgaFileName,"_blood");
				else if (tcgaFileName.indexOf("_adjacent") != -1)
					portion = portion+"-"+getFullPortion(tcgaFileName,"_adjacent");
				if(PLATFORM_SUFF != null && !PLATFORM_SUFF.equals(""))
					portion = portion+"-"+PLATFORM_SUFF;
				a.setPortion(portion);		
			}
			
			
		}
		
		@Override
		public Aliquot[] prepareForTransfer(Aliquot[] als){
			return als;
		}
		
		@Override
		public boolean canDeleteTempoFiles(){
			return true;
		}
		
		/**
		 * Name examples: TCGA-EM-A3FQ_D_metastatic_blood.vcf OR TCGA-EM-A3FQ_D_primary_blood.vcf 
		 * @param tcgaFileName
		 * @param regex ("_blood" or "_adjacent")
		 * @return full name like "metastatic_blood"
		 */
		private String getFullPortion(String tcgaFileName, String  part){
			String revStr = new StringBuilder(tcgaFileName).reverse().toString();
			String revReg = new StringBuilder(part).reverse().toString();
			int stInd = revStr.indexOf(revReg);
			revStr = revStr.substring(stInd, revStr.indexOf("_",stInd+part.length()+1));
			String toret = new StringBuilder(revStr).reverse().toString();
			return toret.replaceAll("_", "-");
		}
			
		
		/**
		 * @param args
		 */
		public static void main(String[] args) {
			long stT = System.currentTimeMillis();
			
			TCGAModule.clearTempoDir();
			
			UCSC_IllumGAModuleNoSplit m = new UCSC_IllumGAModuleNoSplit();
			// m.setSampleInfo("##SAMPLE=<ID=NORMAL,SampleUUID=44ee7757-ca5e-4616-a3fc-5e30c8b6088c,SampleTCGABarcode=TCGA-A1-A0SD-10A-01D-A110-09,Individual=TCGA-A1-A0SD,Description=\"Normal Sample\",File=/inside/home/cwilks/bb_pipeline/runs/brca_freeze/bams/TCGA-A1-A0SD-10A-01D-A110-09_IlluminaGA-DNASeq_exome.bam,Platform=Illumina,Source=CGHub,Accession=05abbd4f-ae15-4b4e-9e87-2bba930a5f05,SequenceSource=WGS,softwareName=<bambam>,sotwareVer=<1.4>,softwareParam=<minSuppSNP=1,minSuppIndel=1,minSuppSV=2,minQ=20,minNQS=10,minMapQ=20,minMapQIndel=1,avgMapQ=10,inProb=0.97,lProb=0.999,tProb=0.001,fracGerm=0.1>>",0);
			//String dir = MySettings.getControlledRoot()+"brca/gsc/ucsc.edu/illuminaga_dnaseq_cont/mutations_protected/ucsc.edu_BRCA.IlluminaGA_DNASeq_Cont.Level_2.1.1.0/";
			String[] urls = {
					MySettings.getControlledRoot()+"thca/gsc/ucsc.edu/illuminaga_dnaseq_cont/mutations_protected/ucsc.edu_THCA.IlluminaGA_DNASeq_Cont.Level_2.1.1.0/"

			};
			for(String dir:urls){
				List<LineBean> list = TCGAHelper.getPageBeans(dir);
				m.processData(list);
			}
			
			/*System.out.println("full: *"+m.getFullPortion("TCGA-EM-A3FQ_D_metastatic_blood.vcf", "_blood")+"*");
			System.out.println("full: *"+m.getFullPortion("TCGA-ET-A25J_D_primary_adjacent.vcf ", "_adjacent")+"*");
			*/
			//FrankShell.desrtoy();
			
			long endT = System.currentTimeMillis();
			System.out.println("done UCSC_IllumGAModuleNoSplit in "+ (endT-stT)/1000+" sec");

		}	
	}

