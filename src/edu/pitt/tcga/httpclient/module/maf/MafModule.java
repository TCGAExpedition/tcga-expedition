package edu.pitt.tcga.httpclient.module.maf;

import java.util.ArrayList;
import java.util.List;

import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public abstract class MafModule extends TCGAModule {
	// see https://www.biostars.org/p/69222/

	//ATT: skip tsv line if firts value starts with # 
	// (in prad/gsc/broad.mit.edu/illuminaga_dnaseq_cont_curated/mutations_protected/) - several lines
	
	
	public abstract String getPortion();
	
	public String getFileType() {
		return "maf";
	}
	
	
	
}
