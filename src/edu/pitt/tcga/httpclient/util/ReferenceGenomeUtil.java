package edu.pitt.tcga.httpclient.util;

import java.util.HashMap;

import java.util.Map;

/**
 * see  https://wiki.nci.nih.gov/pages/viewpage.action?pageId=71434962&navigatingVersions=true
 * also: https://tcga-data.nci.nih.gov/tcga/tcgaPlatformDesign.jsp
 * @author opm1
 *
 */

public class ReferenceGenomeUtil {
	
	private static Map<String,String> rgMap = null; // <name, url>
	
	public static String getGenomeURL(String name){
		if(rgMap == null)
			initMap();
		return rgMap.get(name.toLowerCase());
	}
	
	private static void initMap(){
		rgMap = new HashMap<String,String>();
		rgMap.put("ncbi-human-build36", "ftp://ftp.ncbi.nih.gov/genomes/H_sapiens/ARCHIVE/BUILD.36.3/");
		rgMap.put("hg18","ftp://hgdownload.cse.ucsc.edu/goldenPath/hg18/chromosomes/");
		rgMap.put("ncbi36_bccagsc_variant","ftp://ftp.ncbi.nih.gov/genomes/H_sapiens/ARCHIVE/BUILD.36.3/special_requests/assembly_variants/");
		rgMap.put("ncbi36_bcm_variant","ftp://ftp.ncbi.nih.gov/genomes/H_sapiens/ARCHIVE/BUILD.36.3/special_requests/assembly_variants/");
		rgMap.put("ncbi36_wugsc_variant","ftp://ftp.ncbi.nih.gov/genomes/H_sapiens/ARCHIVE/BUILD.36.3/special_requests/assembly_variants/");
		rgMap.put("hg18_broad_variant","ftp://ftp.ncbi.nih.gov/genomes/H_sapiens/ARCHIVE/BUILD.36.3/special_requests/assembly_variants/");
		rgMap.put("grch37","ftp://ftp.ncbi.nih.gov/genbank/genomes/Eukaryotes/vertebrates_mammals/Homo_sapiens/GRCh37/");
		rgMap.put("grch37-lite","ftp://ftp.ncbi.nih.gov/genbank/genomes/Eukaryotes/vertebrates_mammals/Homo_sapiens/GRCh37/special_requests/");
		rgMap.put("hg19","ftp://hgdownload.cse.ucsc.edu/goldenPath/hg19/chromosomes/");
		rgMap.put("hg19 (grch37)", "ftp://hgdownload.cse.ucsc.edu/goldenPath/hg19/chromosomes/");
		rgMap.put("19","ftp://hgdownload.cse.ucsc.edu/goldenPath/hg19/chromosomes/");
		rgMap.put("grch37-lite-+-hpv_redux-build","https://browser.cghub.ucsc.edu/help/assemblies/");
		rgMap.put("grch37-lite_wugsc_variant_1","ftp://genome.wustl.edu/pub/reference/GRCh37-lite_WUGSC_variant_1/");
		rgMap.put("grch37-lite_wugsc_variant_2","ftp://genome.wustl.edu/pub/reference/GRCh37-lite_WUGSC_variant_2/");
		rgMap.put("grch37_bi_variant","https://browser.cghub.ucsc.edu/help/assemblies/");
		rgMap.put("hg19_broad_variant","https://browser.cghub.ucsc.edu/help/assemblies/");
		rgMap.put("hs37d5","https://browser.cghub.ucsc.edu/help/assemblies/");
		rgMap.put("unaligned","N/A");
		rgMap.put("N/A","N/A");
		rgMap.put("grch37-lite-+hpv_redux-build","ftp://genome.wustl.edu/pub/reference/GRCh37-lite-+-HPV_Redux-build/");

		rgMap.put("ncbi36_harvard_cgh-415k-g4124a","tcga-data.nci.nih.gov/docs/integration/fasta/hms.harvard.edu_HG-CGH-415K_G4124A.fa.txt.zip");
		rgMap.put("ncbi36_mskcc_cgh-244a","tcga-data.nci.nih.gov/docs/integration/fasta/mskcc.org_TCGA_HG-CGH-244A_v080227.fas.zip");
		rgMap.put("ncbi36_harvard_cgh-244a","tcga-data.nci.nih.gov/docs/integration/fasta/harvard.Agilent244k.fa.zip");
		rgMap.put("ncbi36.1_mskcc_cgh-1x1m-g4447a","https://tcga-data.nci.nih.gov/docs/integration/fasta/mskcc.org_CGH-1x1M_G4447A.fa.zip");	
		
	}

}
