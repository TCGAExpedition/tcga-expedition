package edu.pitt.tcga.httpclient.module.maf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MafRecordBean {
	private List<String> barcodes = new ArrayList<String>();
	private File tempFile = null;
	private String tumSampleBarcode = null, normSampleBarcode = null;
	
	private String seqSource = null;
	private String refGenome = null;
	private String platform = null;
	
	
	public List<String> getBarcodes() {
		return barcodes;
	}
	public void addBarcode(String barcode) {
		if(!barcodes.contains(barcode))
			barcodes.add(barcode);
	}
	public File getTempFile() {
		return tempFile;
	}
	public void setTempFile(File tempFile) {
		this.tempFile = tempFile;
	}
	public String getTumSampleBarcode() {
		return tumSampleBarcode;
	}
	public void setTumSampleBarcode(String tumSampleBarcode) {
		this.tumSampleBarcode = tumSampleBarcode;
	}
	public String getNormSampleBarcode() {
		return normSampleBarcode;
	}
	public void setNormSampleBarcode(String normSampleBarcode) {
		this.normSampleBarcode = normSampleBarcode;
	}
	public String getSeqSource() {
		return seqSource;
	}
	public void setSeqSource(String seqSource) {
		this.seqSource = seqSource;
	}
	public String getRefGenome() {
		return refGenome;
	}
	public void setRefGenome(String refGenome) {
		this.refGenome = refGenome;
	}

	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	
	
	public boolean match(String tBc, String nBc){
		if(tumSampleBarcode == null || normSampleBarcode == null)
			return false;
		return (tumSampleBarcode.equals(tBc) && normSampleBarcode.equals(nBc));
	}

	public void dispose(){
		platform = null;
		tempFile = null;
		seqSource = null;
		refGenome = null;
		normSampleBarcode = null;
		tumSampleBarcode = null;
		
		barcodes.clear();
		barcodes = null;
	}

}
