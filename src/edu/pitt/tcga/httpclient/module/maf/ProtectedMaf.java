package edu.pitt.tcga.httpclient.module.maf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.ReferenceGenomeUtil;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class ProtectedMaf extends MafModule{
	// Naming convention
	//<domain>_<disease_abbrev>.<platform>.Level_2.<serial_index>[.<optional_tag>].protected.maf
	
	private String[] endings = {".protected.maf"};
	private DataMatrix dataMatrix = null;
	

	@Override
	public String dataAccessType() {
			return TCGAModule.CONTROLLED;

	}
	
	@Override
	public String[] getResourceEndings(){
		return endings;
	}
	
	@Override
	public String getDataType() {
			return "Protected_Mutations_MAF";

	}
	
	@Override
	public String getAnalysisDirName() {
		return "mutations_protected";
	}
	
	
	@Override
	public String getResourceKey(){
		return "maf.protected";
	}
	
	@Override
	public  boolean canProcessArchive(LineBean archiveBean){
		return true;
	}
	
	/**
	 * 
	 * @param levBeans - most recent archives
	 */
	public void processData(List<LineBean> levBeans){
		List<String[]> sharedHeader = new LinkedList<String[]>();
		List<MafRecordBean> oneTumNormList = new ArrayList<MafRecordBean>();
		
		BufferedWriter writer = null;
		int num = 1;
		for (LineBean lb : levBeans) {
			if (acceptableEnding(lb.getName(), getResourceEndings()) && !TCGAHelper.stringContains(lb.getName(), MySettings.infoFilesList)) {


				String diseaseStudyAbb = CodesUtil.getDiseaseAbbrFromURL(levBeans.get(0).getFullURL());
System.out.println("DIS: "+diseaseStudyAbb+" in ProtectedMaf.processData # "+num+" (out of "+levBeans.size()+")  lb getName "+lb.getName());
				num++;
				String centerName = lb.getCenterName();
				String centerCode = CodesUtil.getCenterCodeFromCenterName(centerName);
				// use platform from lb name although "Sequencer" column has its own name
				String platform = lb.getPlatform();
				sharedHeader.clear();
				
					
					boolean foundHeader = false;
					boolean isNewFile = false;
					HttpClient httpclient = TCGAHelper.getHttpClient();
					InputStream is = TCGAHelper.getGetResponseInputStream(
							httpclient, lb.getFullURL());
					CSVReader reader = new CSVReader(new BufferedReader(
							new InputStreamReader(is)), '\t');
					
					String[] readLine = null;
					int tumBarcodeColNum = -1;
					int normBarcodeColNum = -1;
					int tumRNABarcodeColNum = -1;
					int seqSourceColNum = -1;
					int refGenomeColNum = -1;
					//int platformColNum = -1;
					
					MafRecordBean currMRBean = null;
					//patientData.clear();
					//uniqueAliquotList.clear();
					String currTumorSampleBc = "";
					String currNormSampleBc = "";
					try {
						while ((readLine = reader.readNext()) != null) {
		
							if(readLine[0].startsWith("#"))
								sharedHeader.add(readLine);
							else if(!readLine[0].startsWith("#") && !foundHeader){ // headed
								foundHeader = true;
								sharedHeader.add(readLine);
								tumBarcodeColNum = CSVReader.getColumnNumber(readLine, "Tumor_Sample_Barcode");
								normBarcodeColNum = CSVReader.getColumnNumber(readLine, "Matched_Norm_Sample_Barcode");
								tumRNABarcodeColNum = CSVReader.getColumnNumber(readLine, "Tumor_Sample_Barcode_RNA");
								seqSourceColNum = CSVReader.getColumnNumber(readLine, "Sequence_Source");
								refGenomeColNum = CSVReader.getColumnNumber(readLine, "NCBI_Build");
								//platformColNum = CSVReader.getColumnNumber(readLine, "Sequencer");
								
							} 
							else if(foundHeader){ //actual data	
		
								// new subset
								if(!currTumorSampleBc.equals(CodesUtil.justSampleBarcode(readLine[tumBarcodeColNum])) ||
										!currNormSampleBc.equals(CodesUtil.justSampleBarcode(readLine[normBarcodeColNum]))){
									//save previous tempoFile
									if(writer != null) {
										writer.close();
										writer = null;
										
										
									}

									currTumorSampleBc = CodesUtil.justSampleBarcode(readLine[tumBarcodeColNum]);
									currNormSampleBc = CodesUtil.justSampleBarcode(readLine[normBarcodeColNum]);
	
	
									currMRBean = getMatched(currTumorSampleBc, currNormSampleBc, oneTumNormList);
									if(currMRBean == null){

										currMRBean = new MafRecordBean();
										currMRBean.setRefGenome(readLine[refGenomeColNum]);
										currMRBean.setSeqSource(readLine[seqSourceColNum]);
										currMRBean.addBarcode(readLine[tumBarcodeColNum]);
										currMRBean.addBarcode(readLine[normBarcodeColNum]);
										currMRBean.setTumSampleBarcode(currTumorSampleBc);
										currMRBean.setNormSampleBarcode(currNormSampleBc);
	
										oneTumNormList.add(currMRBean);
										
									}
									
									isNewFile = false;
									File tempoFile = currMRBean.getTempFile();
									if(tempoFile == null){
										TEMP_FILE_NUM++;

										tempoFile = new File(MySettings.TEMPO_DIR + "f"
												+ String.valueOf(TEMP_FILE_NUM) + ".maf");
										currMRBean.setTempFile(tempoFile);
										isNewFile = true;
									}
									writer = new BufferedWriter(new FileWriter(tempoFile,
											true));
									if(isNewFile){
										writer.write(ModuleUtil.listArrayToStringNoSrcDel(
												sharedHeader, MySettings.TAB));
										isNewFile = false;
										
									}									
									
								} //if(!currTumorAliquotBc.equals(readLine[tumBarcodeColNum]))
								
								if(tumRNABarcodeColNum != -1)
									currMRBean.addBarcode(readLine[tumRNABarcodeColNum]);
								
								writer.write(ModuleUtil.copyArrayToStr(
										readLine, MySettings.TAB));
								
							}
						
						} //while ((readLine = reader.readNext()) != null)
						
						//finally close writer
						if(writer != null) {
							writer.close();
							writer = null;
							
						}
						
						// transfer and create Metadata
						//for each temp file
						List<String> aliqList = null;
						Aliquot[] als = null;
						int sz = oneTumNormList.size();
						int cou = 1;
						for(MafRecordBean mrb : oneTumNormList){
			System.out.println("ProtMAF:TUM: "+mrb.getTumSampleBarcode()+" NORM: "+mrb.getNormSampleBarcode()+ "  #"+cou+" (out of "+sz+")  LB: "+lb.getUrl()+"  TempFile: "+mrb.getTempFile());
							cou++;
							aliqList = mrb.getBarcodes();
							int numAliquots = aliqList.size();
							als = new Aliquot[numAliquots];
							int k = 0;
							for(String alBarcode:aliqList){
								Aliquot al = new Aliquot(lb.getUrl(),
									getDataType(), getFileType());
								if(diseaseStudyAbb.equalsIgnoreCase("FPPP"))
									al.setDiseaseStudyAbb(CodesUtil.getDiseaseAbbFromBarcode(alBarcode));
								else
									al.setDiseaseStudyAbb(diseaseStudyAbb);
								al.setBarcode(alBarcode);
								al.setDataAccessType(dataAccessType());
								al.setFileFractionType("aliquot");
								al.setFileType(getFileType());
								al.setCenterName(centerName);
								al.setCenterCode(centerCode);
								al.setSequenceSource(mrb.getSeqSource());
								al.setRefGenome(mrb.getRefGenome());
								al.setPlatform(platform);
								al.setPortion(getPortion());
								String genURL = ReferenceGenomeUtil.getGenomeURL(al.getRefGenome());
								if(genURL == null)
									ErrorLog.log("ProtectedMaf: NO refGenome URL for "+al.getRefGenome());
								else
									al.setRefGenomeSource(genURL);
								als[k] = al;
								als[k].setTempoFile(mrb.getTempFile());
								if (numAliquots > 1) {
									als[k].setSaveInPatientDir(true);
									als[k].setHasAlias(true);
								}
							// attach tempo data file only for one aliquot
								if (k > 0)
									als[k].setHasTempoFile(false);
								
								k++;
							}
							if (als != null) {
								ModuleUtil.transferNew(als);
								als = null;
							}
							mrb.dispose();

						}
						oneTumNormList.clear();
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							is.close();
							reader.close();
							if(writer != null)
								writer.close();

						} catch (IOException e) {
							e.printStackTrace();
						}
						httpclient.getConnectionManager().shutdown();
					} 
			} //if
			clearTempoDir();
			sharedHeader.clear();

		} //for
	}
	
	@Override
	public  String getPortion(){
		return "";
	}
	
	protected MafRecordBean getMatched(String tBc, String nBc, List<MafRecordBean> oneTumNormList){
		for(MafRecordBean mb:oneTumNormList){
			if(mb.match(tBc, nBc))
				return mb;
		}
		return null;
	}
	
	
	
	protected void saveCurrentSubset(LineBean lb, List<String> uniqueAliquotBcList, File tmpFile){
		
	}
	
	public static void main(String[] args){
		
		MySettings.PGRR_META_NQ_FILE = "_KIRP_ucsc_MAF_protectred.nq";
		
		ProtectedMaf pmf = new ProtectedMaf();
		String[] urls = {
				//MySettings.getControlledRoot()+"brca/gsc/unc.edu/mixed_dnaseq_cont_automated/mutations_protected/"
				//MySettings.getControlledRoot()+"acc/gsc/ucsc.edu/illuminaga_dnaseq_cont_automated/mutations_protected/"
				//MySettings.getControlledRoot()+"read/gsc/hgsc.bcm.edu/solid_dnaseq_cont/mutations_protected/"
				//MySettings.getControlledRoot()+"acc/gsc/hgsc.bcm.edu/mixed_dnaseq_cont_curated/mutations_protected/"
				//MySettings.getControlledRoot()+"fppp/gsc/hgsc.bcm.edu/mixed_dnaseq_cont_automated/mutations_protected/"
				//MySettings.getControlledRoot()+"kirc/gsc/hgsc.bcm.edu/mixed_dnaseq_cont/mutations_protected/"
				//MySettings.getControlledRoot()+"acc/gsc/hgsc.bcm.edu/illuminaga_dnaseq_cont_curated/mutations_protected/"
				//MySettings.getControlledRoot()+"acc/gsc/hgsc.bcm.edu/illuminaga_dnaseq_cont_automated/mutations_protected/"
				//MySettings.getControlledRoot()+"coad/gsc/hgsc.bcm.edu/illuminaga_dnaseq_cont/mutations_protected/"
				
				// below - not there anymore
				//MySettings.getControlledRoot()+"prad/gsc/broad.mit.edu/illuminaga_dnaseq_cont_curated/mutations_protected/"
				
				// below just test
				MySettings.getControlledRoot()+"kirp/gsc/ucsc.edu/illuminaga_dnaseq_cont_automated/mutations_protected/"
				
				
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			pmf.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		System.out.println("done ProtectedMaf");
	}



}
