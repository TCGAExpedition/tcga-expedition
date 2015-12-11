package edu.pitt.tcga.httpclient.module.rnaseq.level2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public abstract class RNASeqLevel2Module extends TCGAModule {
	protected Aliquot[] als;
	protected int[] cols1 = ModuleUtil.cols1;
	protected int[] cols2 = ModuleUtil.cols2;
	protected int numOfSamples = 2; //  number of samples in the vcf file
	
	private String[] endings = {"vcf"};
	
	public void setNumOfSamples(int i){
		this.numOfSamples = i;
	}
	
	@Override
	public String dataAccessType() {
		return TCGAModule.CONTROLLED;
	}

	@Override
	public String getDataType() {
		return "RNASeq";
	}

	
	public String getFileType() {
		return "vcf";
	}
	
	@Override
	public  boolean canProcessArchive(LineBean archiveBean){
		return CodesUtil.getLevel(CodesUtil.getArchiveName(archiveBean.getFullURL())).equalsIgnoreCase("2");
	}

	public void processData(List<LineBean> levBeans) {
		int testNum = 0;
		int sz = levBeans.size();
		int cn = 0;
		
		String diseaseStudyAbb = CodesUtil.getDiseaseAbbrFromURL(levBeans.get(0).getFullURL());

		for (LineBean lb : levBeans) {
		
			if (lb.getName().endsWith(".vcf")) {
				testNum++;
				cn++;

		// clear tempo dir
				if(canDeleteTempoFiles()){
					clearTempoDir();
					System.gc();
				}
				
	System.out.println("lb.n = " + lb.getName()+" is "+cn+" out of "+sz);
				HttpClient httpclient = TCGAHelper.getHttpClient();
				InputStream is = TCGAHelper.getGetResponseInputStream(
						httpclient, lb.getFullURL());

				CSVReader reader = new CSVReader(new BufferedReader(
						new InputStreamReader(is)), '\t');

				als = null;
				File[] tempoFiles = null;
				BufferedWriter[] writers = null;
				List<int[]> cols = new LinkedList<int[]>();

				//
				List<String> origHeader = new LinkedList<String>();
				boolean isHeader = true;				
				
				String[] readLine = null;
				try {
					while ((readLine = reader.readNext()) != null) {
						
						if (readLine[0].startsWith("#CHROM")) {
							isHeader = false;
							//now start all aliquots and writers based on the number of samples
							//
							String[] samplesInfo = getSamplesInfo(origHeader);
							setNumOfSamples(samplesInfo.length);
							
							als = new Aliquot[numOfSamples];
							tempoFiles = new File[numOfSamples];
							writers = new BufferedWriter[numOfSamples];
							
							Aliquot al = null;
							File file = null;
							BufferedWriter wr = null;
						
							for(int k = 0; k < numOfSamples; k++){
								//aliquots
								al = new Aliquot(lb.getUrl(), getDataType(),
										getFileType());
								al.setDiseaseStudyAbb(diseaseStudyAbb);
								al.setFileFractionType("aliquot");
								al.setFileType("vcf");
								als[k] = al;
								//tempofiles
								file = new File(MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".vcf");
								TEMP_FILE_NUM++;
								tempoFiles[k] = file;
								als[k].setTempoFile(tempoFiles[k]);
								//writers
								try {
									wr = new BufferedWriter(new FileWriter(tempoFiles[k], true));
									writers[k] = wr;
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								
							}
											
			
							Map<String, List<String>> mappedHeaders = toStandard(origHeader);
							Set<String> keys = mappedHeaders.keySet();
							int m = 0;
							for(String s:keys ){
								writers[m].write(ModuleUtil.listOfStrToString(mappedHeaders.get(s), MySettings.END));
								m++;
							}
							mappedHeaders.clear();
							
							
								for (int k = 0; k < als.length; k++){
									if(readLine.length > 9){
										int col = getColumnNumber(readLine, als[k].getId());
										// replace with the barcode
										readLine[col] = als[k].getBarcode();
										cols.add(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, col});
									}
									else if(readLine.length == 8)
										cols.add(new int[] { 0, 1, 2, 3, 4, 5, 6, 7});
									else 
										ErrorLog.logFatal("ProtectedMutations: Can't handle VCF header length is = "+ readLine.length+" for: "+lb.getFullURL());
								}
						}
													
						if (!isHeader) {
							for(int i = 0; i < numOfSamples; i++){
								writers[i].write(ModuleUtil.copyPartArrayToStr(
										readLine, cols.get(i), MySettings.TAB));
							}
						

						} else  // if isHeader = true
							origHeader.add(readLine[0]);

					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						is.close();
						reader.close();
						for(BufferedWriter writer:writers)
							writer.close();
						
					} catch (IOException e) {
						e.printStackTrace();
					}
					httpclient.getConnectionManager().shutdown();
				}
				als = prepareForTransfer(als);
				 
				if(als != null){
					ModuleUtil.transferNew(als);
					tempoFiles = null;
					als = null;
				}

				cols.clear();
				cols = null;
				writers = null;
				

			}// if (lb.getName().endsWith(".vcf")
	

		} // for(LineBean lb:levBeans)
	}
	
	public int getColumnNumber(String[] sArr, String findS){
		int pos = 0;
		for(String s: sArr){
			if(s.equals(findS))
				return pos;
			pos++;
		}
		return -1;
	}
	
	
	/**
	 * returns 
	 * @param origHeader
	 * @return <SAMPLE_ID , List<standard UMPC fields+ other fields>
	 */
	public Map<String, List<String>> toStandard(List<String> h) {
		Map<String, List<String>> toret = new LinkedHashMap<String, List<String>>();
		List<List<String>> samples = new ArrayList<List<String>>();
		for(int i = 0; i< numOfSamples; i++)
			samples.add(new LinkedList<String>());
		
		// read in sample strings
		String[] samplesInfo = getSamplesInfo(h);

		String fileForamt = getFileFormat(h); 	//1
		String fileDate = "##file_date="+getFileDate(h);		//2
		String center = "##center="+getCenter(h);			//3
		String sCenter = "##sequencing_center="+getSequencingCenter(h);//4
		String platform = "##platform="+getPlatform(h, samplesInfo[0]);		//5
		String platformVer = "##platform_version="+getPlatformVersion(h, samplesInfo[0]);//6
		String ref = getReference(h);//7
		String refSrc = getReferenceSource(h);//8
		String patintID = "##patient_id="+getPatientBarcode(h, samplesInfo[0]); //9

		for(List<String> sam:samples){
			sam.add(fileForamt);
			sam.add(fileDate);
			sam.add(center);
			sam.add(sCenter);
			sam.add(platform);
			sam.add(platformVer);
			sam.add("##reference="+ref);
			sam.add("##reference_source="+refSrc);
			sam.add(patintID);
			
		}
		
		String sampleBarcode = null;
		String sampleID = null;
		List<String> curSam = null;
		for(int i=0; i< numOfSamples; i++){
			als[i].setInfo(samplesInfo[i]);
			sampleBarcode = getSampleBarcode(samplesInfo[i]);//10
			sampleID = getSampleID(samplesInfo[i]);
			als[i].setBarcode(sampleBarcode);
			als[i].setId(sampleID);
			
			curSam = samples.get(i);
			curSam.add("##specimen_id="+sampleBarcode);
			toret.put(sampleID, curSam);
		}
		
		String alignPipiline = "##alignment_pipeline="+getAlignPipeline(h);//11
		String alignPipilineVer = "##alignment_pipeline_version="+
				getAlignPipelineVer(h); //12
		String vcPipeline = "##vc_pipeline="+getVCPipeline(h, samplesInfo[0]);//13
		String vcPipelineVer = "##vc_pipeline_version="+getVCPipelineVer(h, samplesInfo[0]);//14
		// bam files					//15
		// bam file storage				//16
		// add sample string

		for(int j=0; j< numOfSamples; j++){
			samples.get(j).add(alignPipiline);
			samples.get(j).add(alignPipilineVer);
			samples.get(j).add(vcPipeline);
			samples.get(j).add(vcPipelineVer);
			samples.get(j).add("##bam_file="+getBamFile(h, als[j].getInfo()));
			samples.get(j).add("##bam_file_storage="+getBamFileStorage(h, als[j].getInfo()));
			samples.get(j).add(als[j].getInfo());
		}
		
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
		}
		
		setAdditionalAliquotInfo(als);
		
		
		// add all  metadata leftovers:
		for(String s:h){
			if(!s.startsWith("##INFO") &&
			   !s.startsWith("##FORMAT") &&
			   !s.startsWith("##FILTER") &&
			   !s.startsWith("##SAMPLE")){
				for(List<String>sam:samples)
					sam.add(s);
			}
		}
		
		// add Standard Info 
		String toAdd = null;
		for (String s:ModuleUtil.infoArr){
			toAdd = findOrReplaceDesc(s, h);
			for(List<String>sam:samples)
				sam.add(toAdd);
		}
		// add Info leftovers
		for(String s:h){
			if(s.startsWith("##INFO")){
				for(List<String>sam:samples)
					sam.add(s);	
			}
		}
		// add Standard Format 
		for (String s:ModuleUtil.formatArr){
			toAdd = findOrReplaceDesc(s, h);
			for(List<String>sam:samples)
				sam.add(toAdd);
		}
		// add Format leftovers
		for(String s:h){
			if(s.startsWith("##FORMAT")){
				for(List<String>sam:samples)
					sam.add(s);	
			}
		}
		
		// add all Filter
		for(String s:h){
			if(s.startsWith("##FILTER")){
				for(List<String>sam:samples)
					sam.add(s);	
			}
		}
		
		samplesInfo = null;
		
		return toret;
		
	}
	

	public  String getVCPipelineForMetadata(List<String> header, String sampleInfo){
		String[] pl = getVCPipeline(header, sampleInfo).split(" ");
		String[] vers = getVCPipelineVer(header, sampleInfo).split(" ");
		int len = pl.length;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i< len; i++){
			sb.append(pl[i]);
			if(len == vers.length && ! vers[i].equals("none"))
				sb.append("_"+vers[i]);
			sb.append(";");
		}
		sb.setLength(sb.length()-1);
		pl = null;
		vers = null;
		return sb.toString();
	}
	
	public  String[] getSamplesInfo(List<String> header){
		List<String>list = new ArrayList<String>();
		String sampleID = null;
		List<String> toremove = new ArrayList<String>();
 		for(String s:header)
			if(s.startsWith("##SAMPLE=")){
				sampleID = getSampleID(s);
				if(!excludeSampleLine(sampleID))
					list.add(s);
				else
					toremove.add(s);
			}
 		// remove unused SAMPLEs:
 		int ind = -1;
 		for(String s:toremove){
 			ind = getLineFromList(s,header);
 			if(ind != -1)
 				header.remove(ind);
 		}
 		toremove.clear();
 		toremove = null;
 		
		String[] toret = list.toArray(new String[list.size()]);
		list.clear();
		list = null;
		return toret;
	}
	
	private boolean excludeSampleLine(String sampleID){
		for(String suffix:MySettings.excludeSampleSuff)
			if(sampleID.endsWith(suffix))
				return true;
		return false;
	}
	

	/**
	 * It also removed the record from list if found
	 * @param find
	 * @param header
	 * @return
	 */
	
	public String findOrReplaceDesc(String find, List<String> header){
		int dbInd = getLineFromList(find, header);
		String toret = null;
		if(dbInd != -1){
			toret = header.get(dbInd);
			header.remove(dbInd);
		} else
			toret = find+"Number=0,Type=Integer,Description=\"NotAvailable\">";

		return toret;
	}
	
	
	public int getLineFromList(String prefix, List<String> list){
		int ind = 0;
		for(String s:list){	
			if(s.startsWith(prefix))
				return ind;
			ind++;
		}
		return -1;
	}
	

	public String getFileFormat(List<String> header) {
		int ind = getLineFromList("##fileformat=", header);
		if (ind == -1) return "none";
		String toret = header.get(ind);
		header.remove(ind);
		return toret;
	}
	

	public String getFileDate(List<String> header) {
		int ind = getLineFromList("##fileDate=", header);
		if (ind == -1) return "none";
		String toret = header.get(ind).substring(header.get(ind).indexOf("=")+1);
		header.remove(ind);
		return toret;
	}

	public String getCenter(List<String> header) {
		int ind = getLineFromList("##center=", header);
		if (ind == -1) return "none";
		String toret = header.get(ind).substring(header.get(ind).indexOf("=")+1);
		toret = toret.replaceAll("\"","");
		header.remove(ind);
		return toret;
	}

	public String getSequencingCenter(List<String> header) {
		return "none";
	}


	public String getPlatform(List<String> header, String sampleStr) {	
		int ind = sampleStr.indexOf("Platform=");
		if (ind == -1) return "none";
		ind = ind+9;
		return sampleStr.substring(ind, sampleStr.indexOf(",",ind+1));
	}
	

	public String getSampleUUID (String sampleStr){
		int ind = sampleStr.indexOf("SampleUUID=");
		if (ind == -1) return "";
		ind = ind+11;
		return sampleStr.substring(ind, sampleStr.indexOf(",",ind+1));	
	}

	
	public String getPlatformVersion(List<String> header, String sampleStr) {
		int ind = sampleStr.indexOf("Platform=");
		if (ind == -1) return "none";
		ind = ind+9;
		String platform = sampleStr.substring(ind, sampleStr.indexOf(",",ind+1));
		String platform_version = "none";
		if(platform.indexOf(".") !=-1)
			platform_version = platform.substring(platform.indexOf(".")+1);
		return platform_version;
	}


	public String getReference(List<String> header) {
		int ind = getLineFromList("##reference=", header);
		if (ind == -1 || header.get(ind).indexOf(",") == -1) {
			ind =  getLineFromList("##vcfGenerator", header);
			if(ind == -1)
				return "none";
			else { //special cases for {kich, thca}
				//dnaTumorFastaFilename=</inside/depot/fa/GRCh37-lite.fa>,
				//dnaTumorFastaFilename=</inside/depot/fa/Homo_sapiens_assembly19.fasta>,
				//--
				//dnaNormalFastaFilename=</inside/depot/fa/GRCh37-lite.fa>,
				//dnaNormalFastaFilename=</inside/depot/fa/Homo_sapiens_assembly19.fasta>,
				String refStr = header.get(ind);
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
			String ref = header.get(ind).replaceAll("\"","");
			//if (ref.indexOf(",") == -1)
			//	return "none";

		return ref.substring(16, ref.indexOf(",")).toLowerCase();
		}
	}

	
	public String getReferenceSource(List<String> header) {
		int ind = getLineFromList("##reference=", header);
		if (ind == -1 || header.get(ind).toLowerCase().indexOf(",source=") == -1) {
			ind =  getLineFromList("##vcfGenerator", header);
			if(ind == -1)
				return "none";
			else { //special cases for {kich, thca}
				//dnaTumorFastaFilename=</inside/depot/fa/GRCh37-lite.fa>,
				//dnaTumorFastaFilename=</inside/depot/fa/Homo_sapiens_assembly19.fasta>,
				//--
				//dnaNormalFastaFilename=</inside/depot/fa/GRCh37-lite.fa>,
				//dnaNormalFastaFilename=</inside/depot/fa/Homo_sapiens_assembly19.fasta>,
				String refStr = header.get(ind);
				int gInd = refStr.indexOf("dnaNormalFastaFilename");
				if(gInd == -1)
					return "none";
				else {
					refStr = refStr.substring(refStr.indexOf("<", gInd)+1,refStr.indexOf(">",gInd));
					return refStr;
				}
			}
		} else {
			String ref = header.get(ind).toLowerCase();
			//if (ref.indexOf(",source=") == -1)
			//	return "none";
			ref = ref.substring(ref.indexOf(",source=") + 9,
					ref.length() - 2);
			header.remove(ind);
			return ref;
		}
	}

	
	public String getPatientBarcode(List<String> header,String sInfo) {
		int ind = getLineFromList("##INDIVIDUAL=",header);
		if( ind == -1) return "none";
		String patient_id = header.get(ind).substring(13);
		header.remove(ind);
		return patient_id;	
	}

	
	public String getSampleBarcode(String sInfo) {
		int ind = sInfo.indexOf("SampleTCGABarcode=");
		if( ind == -1) return "none";
		return sInfo.substring(ind + 18, sInfo.indexOf(",", ind + 1));
	}

	
	public String getSampleID(String sInfo) {
		int ind = sInfo.indexOf("##SAMPLE=<ID=");
		if( ind == -1) return "none";
		return sInfo.substring(ind + 13,sInfo.indexOf(","));
	}

	
	public String getAlignPipeline(List<String> header) {
		return "none";
	}

	
	public String getAlignPipelineVer(List<String> header) {
		return "none";
	}

	
	public String getVCPipeline(List<String> header, String sampleInfo) {
		//actually remove ##source= from the header;
		int ind = getLineFromList("##source=", header);
		if (ind != -1) 
			header.remove(ind);
		ind = getLineFromList("##vcfProcessLog=", header);
		if (ind == -1) return "none";
		String s = header.get(ind);
		ind = s.indexOf("InputVCFSource=<")+16;
		if (ind == -1) return "none";

		return s.substring(ind, s.indexOf(">",ind+1));
	}

	
	public String getVCPipelineVer(List<String> header, String sampleInfo) {
		int ind = getLineFromList("##vcfProcessLog=", header);
		if (ind == -1) return "none";
		String s = header.get(ind);
		ind = s.indexOf("InputVCFVer=<")+13;
		if (ind == -1) return "none";

		return s.substring(ind, s.indexOf(">",ind+1));
	}

	
	public String getBamFile(List<String> header, String sampleInfo) {
		int ind = sampleInfo.indexOf("File=");
		if (ind == -1) return "none";
		ind = ind+5;
		return  sampleInfo.substring(ind, sampleInfo.indexOf(",",ind+1));
	}

	
	public String getBamFileStorage(List<String> header, String sampleInfo) {
		return "none";
	}
	
	
	public void setAdditionalAliquotInfo(Aliquot[] als) {
		String tcgaFileName = null;
		for (Aliquot a:als){
			tcgaFileName = a.getTCGAFileName();
			if (tcgaFileName.indexOf("_blood") != -1)
				a.setPortion(getFullPortion(tcgaFileName,"_blood"));
			else if (tcgaFileName.indexOf("_adjacent") != -1)
				a.setPortion(getFullPortion(tcgaFileName,"_adjacent"));
		}
		
	}
	
	
	public Aliquot[] prepareForTransfer(Aliquot[] als){
		return als;
	}
	

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
	


	@Override
	public String[] getResourceEndings() {
		return endings;
	}
}
