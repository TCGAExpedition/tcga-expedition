package edu.pitt.tcga.httpclient.module.rnaseq.level3;


import edu.pitt.tcga.httpclient.log.ErrorLog;

public class RNASeqV2Level3 extends RNASeqLevel3Module{
	
	private String[] ending =  {"sdrf.txt", 
			"exon_quantification.txt", "junction_quantification.txt",
			"rsem.genes.results","rsem.genes.normalized_results",
			"rsem.isoforms.results","rsem.isoforms.normalized_results"};
	
	@Override
	public String getDataType() {
		return "RNASeqV2";
	}
	
	@Override
	public String getPortion(String lbName) {
		String strEnd = acceptedEnding(lbName);
		if(strEnd.indexOf("exon_quantification") != -1)
			return "exon";
		else if(strEnd.indexOf("junction_quantification") != -1)
			return "junction";
		else if(strEnd.indexOf("rsem.genes.results") != -1)
			return "genes";
		else if(strEnd.indexOf("rsem.genes.normalized_results") != -1)
			return "genes-normalized";
		else if(strEnd.indexOf("rsem.isoforms.results") != -1)
			return "isoform";
		else if(strEnd.indexOf("rsem.isoforms.normalized_results") != -1)
			return "isoform-normalized";
		else{
			ErrorLog.log("RNASeqV2Level3: can't map file ending for :"+lbName);
			return "";
		}
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
		toret = toret.replaceAll("_",  "-");
		return new StringBuilder(toret).reverse().toString();
	}
	
	
	@Override
	public String getFileType(String lbName) {
		return getPortion(lbName);
	}

	@Override
	public String getAnalysisDirName() {
		return "rnaseqv2";
	}

	@Override
	public String getResourceKey() {
		return "rnaseqv2.level3";
	}

	@Override
	public String[] getResourceEndings() {
		return ending;
	}

}
