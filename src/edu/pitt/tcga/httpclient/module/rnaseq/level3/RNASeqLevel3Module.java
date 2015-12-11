package edu.pitt.tcga.httpclient.module.rnaseq.level3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public abstract class RNASeqLevel3Module extends TCGAModule{
	
	protected DataMatrix dataMatrix = null;
	// search as contains string
	protected static List<String> skipRNASeq = Arrays.asList(
		"UNCID_286961.TCGA-AA-3984-01A-02R-1022-07.100723_UNC2-RDR300275_0016_62330AAXX.8.trimmed.annotated.translated_to_genomic.spljxn.quantification.txt",
		"UNCID_286989.TCGA-AA-3984-01A-02R-1022-07.100723_UNC2-RDR300275_0016_62330AAXX.8.trimmed.annotated.gene.quantification.txt",
		"UNCID_287028.TCGA-AA-3984-01A-02R-1022-07.100723_UNC2-RDR300275_0016_62330AAXX.8.trimmed.annotated.translated_to_genomic.exon.quantification.txt",
		"UNCID_432132.TCGA-AL-3471-01A-02R-1351-07.110816_UNC9-SN296_0229_BB09YNABXX.8.trimmed.annotated.gene.quantification.txt",
		"UNCID_432163.TCGA-AL-3471-01A-02R-1351-07.110816_UNC9-SN296_0229_BB09YNABXX.8.trimmed.annotated.translated_to_genomic.spljxn.quantification.txt",  
       	"UNCID_432288.TCGA-AL-3471-01A-02R-1351-07.110816_UNC9-SN296_0229_BB09YNABXX.8.trimmed.annotated.translated_to_genomic.exon.quantification.txt",
       	"UNCID_374601.TCGA-AK-3426-01A-02R-1325-07.110429_UNC14-SN744_0106_AB066TABXX.7.trimmed.annotated.gene.quantification.txt",
       	"UNCID_374614.TCGA-AK-3426-01A-02R-1325-07.110429_UNC14-SN744_0106_AB066TABXX.7.trimmed.annotated.translated_to_genomic.exon.quantification.txt",
       	"UNCID_374642.TCGA-AK-3426-01A-02R-1325-07.110429_UNC14-SN744_0106_AB066TABXX.7.trimmed.annotated.translated_to_genomic.spljxn.quantification.txt",
       	"UNCID_377773.TCGA-B0-4707-01A-01R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.5.trimmed.annotated.translated_to_genomic.spljxn.quantification.txt",
       	"UNCID_377778.TCGA-B0-4707-01A-01R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.5.trimmed.annotated.gene.quantification.txt",
       	"UNCID_377818.TCGA-B0-4707-01A-01R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.5.trimmed.annotated.translated_to_genomic.exon.quantification.txt",
       	"UNCID_377698.TCGA-AK-3453-01A-02R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.3.trimmed.annotated.translated_to_genomic.spljxn.quantification.txt",
       	"UNCID_377716.TCGA-AK-3453-01A-02R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.3.trimmed.annotated.gene.quantification.txt",
       	"UNCID_377815.TCGA-AK-3453-01A-02R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.3.trimmed.annotated.translated_to_genomic.exon.quantification.txt",
       	"UNCID_377685.TCGA-AK-3454-01A-02R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.4.trimmed.annotated.translated_to_genomic.spljxn.quantification.txt",
       	"UNCID_377688.TCGA-AK-3454-01A-02R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.4.trimmed.annotated.gene.quantification.txt",
       	"UNCID_377697.TCGA-AK-3454-01A-02R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.4.trimmed.annotated.translated_to_genomic.exon.quantification.txt",
       	"UNCID_377773.TCGA-B0-4691-01A-01R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.5.trimmed.annotated.translated_to_genomic.spljxn.quantification.txt",
       	"UNCID_377778.TCGA-B0-4691-01A-01R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.5.trimmed.annotated.gene.quantification.txt",
       	"UNCID_377818.TCGA-B0-4691-01A-01R-1277-07.110324_UNC9-SN296_0160_AB038BABXX.5.trimmed.annotated.translated_to_genomic.exon.quantification.txt"
		);


	@Override
	public String dataAccessType() {
		return TCGAModule.PUBLIC;
	}
	
	
	public String getFileExtension(){
		return "txt";
	}
	
	@Override
	public  boolean canProcessArchive(LineBean archiveBean){
		return CodesUtil.getLevel(CodesUtil.getArchiveName(archiveBean.getFullURL())).equalsIgnoreCase("3");
	}

	public void processData(List<LineBean> levBeans) {
		int sz = levBeans.size();
		int cn = 0;
		boolean foundFirts = false;
		String fNameEnding = null;
		for (LineBean lb : levBeans) {
				fNameEnding = acceptedEnding(lb.getName());
				if (acceptableEnding(lb.getName(),getResourceEndings()) && !skipRNASeq.contains(lb.getName())) {
					cn++;
		
			// clear tempo dir
					clearTempoDir();
					System.gc();
								
		System.out.println(lb.getDiseaseStudy()+" : "+cn+" out of "+sz+"  lb.n = " + lb.getName());
					HttpClient httpclient = TCGAHelper.getHttpClient();
					InputStream is = TCGAHelper.getGetResponseInputStream(
							httpclient, lb.getFullURL());
		
					CSVReader reader = new CSVReader(new BufferedReader(
							new InputStreamReader(is)), '\t');
					BufferedWriter writer = null;
					Aliquot al = null;
					try{
						List<String[]> data = reader.readAllToList();				
						if(lb.getName().endsWith(".sdrf.txt")){
							if(dataMatrix == null)
								dataMatrix = new DataMatrix("MageTabSDRF");
							else 
								dataMatrix.clear();
							
							dataMatrix.setData(data);
						}else {
							al = constructAliquot(lb);	
							File tempoFile = new File(MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".txt");
							TEMP_FILE_NUM++;
							if(al != null){
								al.setTempoFile(tempoFile);
								writer = new BufferedWriter(new FileWriter(tempoFile, true));
								writer.write(ModuleUtil.listArrayToString(data, MySettings.TAB));
							}
						}
						reader.close();
						if(writer != null)
							writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							is.close();					
						} catch (IOException e) {
							e.printStackTrace();
						}
						httpclient.getConnectionManager().shutdown();
					}
	
					if(al != null){
						ModuleUtil.transferNew(new Aliquot[]{al});	
					}
				} //fNameEnding != null
	
		} // for(LineBean lb:levBeans)
	}
	
	public  Aliquot constructAliquot(LineBean lb){
		String lbName = lb.getName();
		int rowNum = getDataRowNum(lbName, getFileColName());
		if (rowNum > -1) {
			Aliquot al = new Aliquot(lb.getUrl(), getDataType(),
					getFileExtension());
			al.setBarcode(dataMatrix.getRowColValue(getBarcodeColName(), rowNum));
			al.setOrigUUID(dataMatrix.getRowColValue(getOrigUUIDColName(), rowNum));
			al.setFileFractionType("aliquot");
			al.setPortion(getPortion(lbName));
			String centerName = getCenterName(rowNum);
			al.setCenterName(centerName);
			al.setCenterCode(CodesUtil.getCenterAbbFromArchName(centerName));
			al.setRefGenome(getReference(rowNum));
			al.setRefGenomeSource(dataMatrix.getRowColValue(getReferenceSourceColName(), rowNum));
			al.setPlatform(getPlatform(rowNum));
			al.setFileType(getFileType(lbName));
			al.setAlgorithmName("rnaseq");
			return al;
		} else {
			String err = "RNASeqLevel3Module: can't find in mage-tab a row for "+lb.getFullURL();
			ErrorLog.log(err);
			return null;
		}
	}
	
	public abstract String getDataType();
	public abstract String getFileColName();
	public abstract String getBarcodeColName();
	public abstract String getOrigUUIDColName();
	public abstract String getCenterName(int rowNum);
	public abstract String getReferenceSourceColName(); //name
	public abstract String getReference(int rowNum); //url
	public abstract String getPlatform(int rowNum);
	public abstract String getPortion(String lbName);
	public abstract String getFileType(String lbName);
	
	
	public String acceptedEnding(String lbName){
		String[] endings = getResourceEndings();
		for(String s:endings)
			if(lbName.endsWith(s))
				return s;
		return null;
	}
	
	
	public int getDataRowNum(String lbName, String colName){
		/*System.out.println("lbName = "+lbName+"     colName: "+colName);
		System.out.println("sz = "+dataMatrix.getData().size());*/
		return dataMatrix.getDataRow(lbName, dataMatrix.getColumnNum(colName));
	}
}
