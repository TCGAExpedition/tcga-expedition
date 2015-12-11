package edu.pitt.tcga.httpclient.module.cna;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.transfer.Transfer;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

/**
 *
 * 07/15/2015: Has only level 3 for BLCA: 2 samples in each file
 * Header:
 * Chromosome	Start	End	Tumor_Count	Normal_Count	Segment_Mean
 * RENAMED to:
 * Chromosome	Start	End	<tumor_Barcode>	<normal_Barcode>	Segment_Mean
 * 
 * @author opm1
 * @version 1
 * @since Dec 11, 2015
 *
 */
public class IllumHiSeq_CNA extends CNAModule{
	
	private String[] endings = {"sdrf.txt", "tsv"};
	
	@Override
	public String getDataType() {
		return "CNV_(Low_Pass_DNASeq)";
	}

	public  Aliquot constructAliquot(LineBean lb){
		return null;
	}

	@Override
	protected String[] getAcceptableEndings() {
		return endings;
	}

	@Override
	protected int getFileColNum(DataMatrix dm, String lbName) {
		//level 3
		return dm.getColumnNum("Derived Data File",1);
	}

	@Override
	protected String getAlgorithmName(String lbName) {
		return "Segmented";
	}

	@Override
	protected String getFileType(String lbName) {
		return "segmented";
	}


	/**
	 * get value from dataMatrix using "Comment [Genome reference]" column
	 */
	@Override
	protected String getRefGenome(LineBean lb) {
		String lbName = lb.getName();
		int rowNum = -1;
		
		try{
			rowNum =  dataMatrix.getDataRow(lbName, getFileColNum(dataMatrix, lbName));
		} catch (NullPointerException e){ }
		
		return dataMatrix.getRowColValue("Comment [Genome reference]", rowNum);
	}

	/**
	 * normal and tumor samples
	 * 1. get barcodes
	 * 2. create 2 aliquots (one with 
	 */
	@Override
	protected void processMultiSampleFile(LineBean lb) {

		String lbName = lb.getName();
		List<Integer> occurRowsList = dataMatrix.getStartsWithIgnoreCaseDataRows(lbName, getFileColNum(dataMatrix, lbName));
		int sz = occurRowsList.size();
		if(sz != 2)
			ErrorLog.logFatal("IllumHiSeq_CNA.processMultiSampleFile: MUST be 2 rows (got "+sz+") for file "+lb.getUrl());
		Aliquot[] als = new Aliquot[2];

		//get barcodes
		String barcode = null;
		String [] barcodes = new String[2];
		barcodes[0] = dataMatrix.getRowColValue(getBarcodeColName(), occurRowsList.get(0));
		barcodes[1] = dataMatrix.getRowColValue(getBarcodeColName(), occurRowsList.get(1));
		File tempoFile = new File(MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".txt");
		TEMP_FILE_NUM++;
		try {
			TCGAHelper.copyURLToFile(new URL(lb.getFullURL()), tempoFile, dataAccessType().equals("controlled"));
			Transfer.execBash("sed -i '1 s/Tumor_Count/"+getBarcodeByType(barcodes, false)+"/' '"+tempoFile.getAbsolutePath()+"'", false);
			Transfer.execBash("sed -i '1 s/Normal_Count/"+getBarcodeByType(barcodes, true)+"/' '"+tempoFile.getAbsolutePath()+"'", false);
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
			
		for(int k=0; k<sz; k++){
			barcode = barcodes[k];	
			als[k] = constructAliquot(lb, barcode);
			als[k].setSaveInPatientDir(true);
			als[k].setHasAlias(true);
			
			als[k].setTempoFile(tempoFile);
			
			if (k > 0)
			als[k].setHasTempoFile(false);
			
		}
		
		if(als != null){
			ModuleUtil.transferNew(als);
		}
		barcodes = null;
		
	}
	
	private String getBarcodeByType(String[] barcodes, boolean isNormal){
		Integer sampleTypeCode = -1;
		for(String b:barcodes){
			sampleTypeCode = Integer.valueOf(CodesUtil.getSampleTypeAbb(b));
			
			if(isNormal && (sampleTypeCode >= 10 && sampleTypeCode <= 14 ))
					return b;
			else if(!isNormal &&(sampleTypeCode < 10 || sampleTypeCode > 14))
				return b;
		}
		String err = "IllumHiSeq_CNA.getBarcodeByType Can't find match: isNormal = "+isNormal+" barcodes: "+Arrays.asList(barcodes);
		System.err.println(err);
		ErrorLog.logFatal(err);
		return null;
	}

	@Override
	public String getResourceKey() {
		return "cnv_low_pass_Level3";
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MySettings.PGRR_META_NQ_FILE = "_BLCA_IllumHiSeq_CNA_cna_TEST.nq";
		
		IllumHiSeq_CNA pmf = new IllumHiSeq_CNA();
		
		// below just test
		String[] urls = {

				MySettings.PUB_ROOT_URL+"blca/cgcc/hms.harvard.edu/illuminahiseq_dnaseqc/cna/" 
				
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			pmf.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		System.out.println("done IllumHiSeq_CNA");

	}

}
