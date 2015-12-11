package edu.pitt.tcga.httpclient.module.rnaseq.level3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.HttpClient;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.module.rnaseq.level2.BCGCC_IllumHi_RNA;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class RNASeqLevel3 extends RNASeqLevel3Module{

	
	private String[] endings =  {".sdrf.txt", "exon.quantification.txt",
			"gene.quantification.txt","spljxn.quantification.txt"};
	


	@Override
	public String getDataType() {
		return "RNASeq";
	}
	
	@Override
	public String getPortion(String lbName) {
		String toret = acceptedEnding(lbName);
		return toret.substring(0, toret.indexOf("."));
		
	}
	
	@Override
	public String getFileColName(){
		return "Derived Data File";

	}

	@Override
	public String getBarcodeColName() {
		return "Comment [TCGA Barcode]";
	}

	@Override
	public String getOrigUUIDColName() {
		return "Extract Name";
	}

	/**
	 * example value: unc.edu:reverse_transcription:IlluminaHiSeq_RNASeq:01
	 */
	@Override
	public String getCenterName(int rowNum) {
		String toret = dataMatrix.getRowColValue("Protocol REF", rowNum);
		return toret.substring(0, toret.indexOf(":"));
	}

	@Override
	public String getReferenceSourceColName() {
		return "Annotation REF";
	}

	/**
	 * example value: hg19 (GRCh37)
	 */
	@Override
	public String getReference(int rowNum) {
		String toret = dataMatrix.getRowColValue("Comment [Genome reference]", rowNum);
		toret = toret.replaceAll("_", "-").toLowerCase();
		if(toret.indexOf(" ") != -1)
			return toret.substring(0, toret.indexOf(" "));
		else
			return toret;
	}

	/**
	 * example value: unc.edu:reverse_transcription:IlluminaHiSeq_RNASeq:01
	 */
	@Override
	public String getPlatform(int rowNum) {
		String toret = dataMatrix.getRowColValue("Protocol REF", rowNum);
		toret = new StringBuilder(toret).reverse().toString();
		int st = toret.indexOf(":");
		toret = toret.substring(st+1, toret.indexOf(":", st+1));
		toret = toret.replaceAll("_","-");
		return new StringBuilder(toret).reverse().toString();
	}

	@Override
	public String getFileType(String lbName) {
		return getPortion(lbName);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TCGAModule.clearTempoDir();
		
		RNASeqLevel3 m = new RNASeqLevel3();
		String[] urls = {

				MySettings.PUB_ROOT_URL+"stad/cgcc/bcgsc.ca/illuminaga_rnaseq/rnaseq/"
		};
		
		for(String dir:urls){
			List<LineBean> list = TCGAHelper.getPageBeans(dir);
			m.processArchiveLevel(TCGAHelper.recentOnly(list));
		}
		
		
		System.out.println("Done RNASeqLevel3");

	}

	@Override
	public String[] getResourceEndings() {
		return endings;
	}

	@Override
	public String getAnalysisDirName() {
		return "rnaseq";
	}

	@Override
	public String getResourceKey() {
		return "rnaseq.level3";
	}

}
