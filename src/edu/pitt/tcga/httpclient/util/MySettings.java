package edu.pitt.tcga.httpclient.util;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

public class MySettings {
	// barcode uuid redaction
	//http://gdac.broadinstitute.org/runs/sampleReports/latest/BLCA_Redactions.html
	//http://gdac.broadinstitute.org/runs/sampleReports/latest/BRCA_Observations.html
	
	//TEMPO:
	public static boolean CAN_PROCESS = false;
	
	
	//public static final String ACCESS = "controlled";
	public static final String ACCESS = "public";
	
	
	public static final boolean IS_SHERLOCK = true;
	//for last modified:
	public static String DATE_FORMAT="T10:00:00.000Z";
	public static String TIMESTAMP_FORMAT = "dd-MM-yyyy HH:mm:ss";
	public static String TIMESTAMP_MILLI_FORMAT= "yyyy-MM-dd HH:mm:ss";
	public static String REPORT_DATE_FORMAT = "yyyy-MM-dd";
	//public static String dateTimeSuff = "^^<http://www.w3.org/2001/XMLSchema#dateTime>";
	
	public static String DAY_FORMAT = "dd-MM-yyyy";
	public static SimpleDateFormat dayFormat = null ;
	
	
	// tab and end symbols
	public static final String TAB = "\t";
	public static final String END = "\n";
	public static final String SPACE = " ";
	
	
	
	// tcga code table
	public static final String CODETABLE_URL = "https://tcga-data.nci.nih.gov/datareports/codeTablesReport.htm?codeTable=Tissue%20Source%20Site";
	
	public static final String CGHUB_PATH = "https://cghub.ucsc.edu/cghub/metadata/analysisObject?analysis_id=";
	
	//Virtuoso
	//see examples: http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VirtSesame2Provider
	/*public static final String VIRT_PROTOCOL = "http";
	public static final String VIRT_HOST = "virtuoso.sam.pitt.edu";
	public static final String VIRT_PORT = "8890";
	public static final String VIRT_SPARQL_ENDPOINT = "sparql-auth/";
	public static final String VIRT_UPDATE_ENDPOINT = "DAV/subscription/yy";
	public static final String VIRT_USER = "subscription";
	public static final String VIRT_PWD = "noitpircsbus";*/
			
			// sesame
	
	//sesame urls
		//public static final String REPO_ID = "test";
		//public static final String RDF_SERVER ="http://localhost:8080/openrdf-sesame";
		//public static final String INSERT_URL = "http://localhost:8080/openrdf-sesame/repositories/"+REPO_ID+"/statements";
		//public static final String SPARQL_URL = "http://localhost:8080/openrdf-sesame/repositories/"+REPO_ID+"?";
	//sherlock urls
		public static final String REPO_ID = "ds";
		public static final String RDF_SERVER ="https://localhost:4430";
		private static String INSERT_URL = null;
		private static String SPARQL_URL = null;
		
		
		//tcga public url
		public static final String PUB_ROOT_URL = "https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/";
		// tcga controlled url
		public static String CON_ROOT_URL = null;
												   
		//public static final String ROOT_URL = PUB_ROOT_URL;
		//public static final String ROOT_URL = CON_ROOT_URL;
		
		
		
		// list of info (not data) files
		public static List<String> infoFilesList = Arrays.asList("README",
				"MANIFEST","CHANGES","ALTERED_FILES","md5Changes","ADDITION",
				"DESCRIPTION","DISCLAIMER","CHANGES_DCC", ".idf.txt", "Parent Directory");
		
		public static List<String> excludedHREFList = Arrays.asList("?C=N;O=D","lost+found","tar.gz",
				"md5ChangesCont.log", "md5ChangesCont.sh", "archive");
		
		public static List<String> excludedHREFListNoTAR = Arrays.asList("?C=N;O=D","lost+found",
				"md5ChangesCont.log", "md5ChangesCont.sh", "archive");
		
		public static String CHANGES = "CHANGES_DCC.txt";
		
		public static List<String>skipOnDiseaseLevel_List = Arrays.asList("cntl","misc", "lost+found/");
		
		// file status:
		public static final String INITIAL = "+";
		public static final String DELETED = "-";
		public static final String REVISED = "R";
		public static final String UNCHANGED = "U";
		
		public static List<String> binaryList = Arrays.asList("cel", "idat", "tif", "fsa","bam", "svs");
		
		
		public static String[] centerCodesHeader = {"code","centerName", "centerType","displayName","shortName"};
		public static String[] diseaseStudyHeader = {"studyAbbreviation", "studyName"};
		public static String[] dataTypeHeader = {"centerType", "displayName","ftpName", "available"};
		public static String[] tssHeader = {"code","sourceSite","studyName", "bcr"};
		public static String[] portionHeader = {"code", "definition"};
		public static String[] sampleTypeHeader = {"code", "definition", "shortCode"};
		
		// insert OK code sesame
		public static final int INSERT_STATUS_OK = 204;
		
		public static final int MAX_PER_ROUTE = 5000;
		
		private static Properties params = null;
		private static final String configName = "tcgaexpedition.conf";
		private static String rdfPrefix = null;
		
		
		//=======================
		// for email
		//=======================
		/*public static final String PGRR_EMAIL_NAME = "P Pgrr-Pitt";
		public static final String PGRR_EMAIL_PWD = "PGRR@#125438";
		public static final String PGRR_EMAIL = "pgrr.pitt@gmail.com";*/
		
		/*public static final String PGRR_EMAIL_NAME = "pgrr";
		public static final String PGRR_EMAIL_PWD = "pgzr491%";
		public static final String PGRR_EMAIL = "pgrr@pitt.edu";
		public static final String PGRR_EMAIL_NAME = "opm1";
		public static final String PGRR_EMAIL_PWD = "Pilgrim@#03";
		public static final String PGRR_EMAIL = "opm1@pitt.edu";*/
		
		//=======================
		// for moduls
		//=======================
		// Dirs: RUNTIME_DATA_DIR 
		////////////repository/tcga
		////////////archive/tcga
		////////////log
		////////////upload
		////////////reports
		////////////tempo
		
		//public static final String RUNTIME_DATA_DIR = "C:/tcgaTEST/";
		//public static final String TOP_ARCH = "C:/tcgaTEST/archive/tcga/";
		//public static final String TEMPO_DIR = "C:/tcgaTEST/tempo/";
		public static final String TEMPO_FILE_NAME = "tempoRNA_Level3a.txt";
		
		
		//public static final String RUNTIME_DATA_DIR = System.getProperty("app.home")+"/testRem/";
		//public static final String RUNTIME_DATA_DIR = System.getProperty("app.home")+"/tcgaFeb12/";
		
		
		public static  String RUNTIME_DATA_DIR = System.getProperty("user.dir")+File.separator+"runtime";
		public static  String TEMPO_DIR = RUNTIME_DATA_DIR +"/tepmo/";
		public static  String LOG_DIR = RUNTIME_DATA_DIR + "/log/";
		
		public static String BAM_SUMMARY_DIR = RUNTIME_DATA_DIR + "/bam_summary/";
		public static String NQ_UPLOAD_DIR = RUNTIME_DATA_DIR +"/upload/";
		public static String NQ_UPLOAD_HISTORY_DIR = RUNTIME_DATA_DIR + "/uploadhistory/";
		public static String NEW_TSS_DIR = RUNTIME_DATA_DIR + "/new_tss/";
		public static String BAM_LOG_DIR = RUNTIME_DATA_DIR + "log_bam/";
		public static String FIREHOSE_GET = RUNTIME_DATA_DIR+"/bin/firehose_get";
		public static String REPORTS_DIR = RUNTIME_DATA_DIR+"/reports/";
		
		
		public static final String METADATA_FILE = "Metadata.tsv";
		
		
		public static String PGRR_META_NQ_FILE = "_start_from_LAML__MAF_protectred.nq";
		public static String TCGA_BC_UUID_NQ_FILE = "_tcga_bc_uuid.nq";
		public static final String CLIN_META_NQ_FILE = "_upload_clin_metaWithURL.nq";
		public static  String LOG_UPDATE_FILE = "_modifyDiseaseDataTypePairs.txt";
		
		
		
		public static String PGRR_PREFIX_URI = "http://purl.org/pgrr/core#";
		public static String PGRR_PREFIX = "pgrr";
		public static final String TCGA_PRE = PGRR_PREFIX_URI;

		public static String SUPERCELL_HOME = getRepositoryHome();
		public static String METADATA_DIR = SUPERCELL_HOME + File.separator + "/metadata.dir/";
		
		
		
		// RDF store Graph names 
		//public static String CLIN_META_GRAPH = "clin-meta";
		public static String PGRR_META_GRAPH = "pgrr-meta";
		public static String TCGA_BC_UUID_GRAPH = "tcga-bc-uuid";
		
		//--- use serialized lists/maps for frank job
		//public static boolean RUN_AS_JOB = true;
		//--- from just rank:
		public static boolean RUN_AS_JOB = false;
		
		public static String[] moduleHeader = {"project","barcode","diseaseAbbr",
				"tssAbbr","tssName","analysisType","analysisPlatform","sampleTypeCode",
				"sampleTypeDesc", 
				"analyteCode","analyteDesc","centerCode","centerName","level",
				"fractionType", "fileExtension","pgrrFileName",
				"version","pgrrPath","tcgaFileName","dateCreated","dateArchived",
				"reasonArchived","fileSizeInBytes","isPublic","refGenomeName",
				"refGenomeURL","tcgaArchivePath",
				"origUUID","patientUUID","sampleUUID", "aliquotUUID",
				"patientBarcode","sampleBarcode", "sampleVial","aliquotBarcode","md5Checksum",
				"fileType","pgrrUUID", "algorithmName", "algorithmVersion"};
		
		// <tcga_path, module_name>
		public static Map<String, String> moduleMap = null;
		
		// too many vcf runs for luad/gsc/genome.wustl.edu/illuminahiseq_dnaseq_cont/mutations_protected/
		public static List<String> excludeSampleSuff =  Arrays.asList("VarscanSomatic", 
				"Strelka","Pindel","GatkSomaticIndel","Samtools","Sniper");
		public static final String PUBLIC_START = "https://tcga-data.nci.nih.gov/tcgafiles/";
		
		public static String getRoot(String accessType){
			return accessType.equalsIgnoreCase("CONTROLLED")?getControlledRoot():PUB_ROOT_URL;
		}
		
		public static String getControlledRoot(){
			if(CON_ROOT_URL == null)
				CON_ROOT_URL = "https://"+getStrProperty("tcga.user")+":"+getStrProperty("tcga.pwd")+"@tcga-data-secure.nci.nih.gov/tcgafiles/tcga4yeo/tumor/";
			
			return CON_ROOT_URL;
		}
		
		
		public static String getRepositoryHome(){
			if(SUPERCELL_HOME == null){
				String h = getStrProperty("repository.home");
				if(h.endsWith("/"))
					h = h.substring(0,h.lastIndexOf("/"));
				return h;
			}
			return SUPERCELL_HOME;
		}
		
		public static Map<String, String> getModuleMap(){
			if(moduleMap == null){
				moduleMap = new HashMap<String, String> ();
				/*//clin 
				moduleMap.put("bcr/nationwidechildrens.org/bio/clin/", "edu.pitt.tcga.httpclient.module.clin.ClinicalModule");
				moduleMap.put("bcr/genome.wustl.edu/bio/clin/", "edu.pitt.tcga.httpclient.module.ClinicalModule"); // for laml
				*/
				//**********************
				// PROTECTED MUTATIONS
				//**********************
				//ucsc IllumGA 
				//moduleMap.put("gsc/ucsc.edu/illuminaga_dnaseq_cont/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.UCSC_IllumGANoSplit");
				// UCSC Solid
				//moduleMap.put("gsc/ucsc.edu/solid_dnaseq_cont/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.UCSC_SolidNoSplit");
				//UCSC IllumGA automated vcf and vcf.gz
				//moduleMap.put("gsc/ucsc.edu/illuminaga_dnaseq_cont_automated/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.UCSC_IllumGAAutomatedNoSplit");
				// WUSTL _IllumHi (luad)
				//Map.put("gsc/genome.wustl.edu/illuminahiseq_dnaseq_cont/mutations_protected/","edu.pitt.tcga.httpclient.module.pm.WUSTL_IllumHiModule");
				//hgsc IllumGA 
				//moduleMap.put("gsc/hgsc.bcm.edu/illuminaga_dnaseq_cont/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.HGSC_IllumSolidNoSplit");
				//hgsc Solid
				//moduleMap.put("gsc/hgsc.bcm.edu/solid_dnaseq_cont/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.HGSC_IllumSolidNoSplit");
				//BI Solid
				//moduleMap.put("gsc/broad.mit.edu/solid_dnaseq_cont/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.BI_SolidNoSplit");
				//BI IllumGA
				//moduleMap.put("gsc/broad.mit.edu/illuminaga_dnaseq_cont/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.BI_IllumGANoSplit");
				//BI IllumGA_automated
				//moduleMap.put("gsc/broad.mit.edu/illuminaga_dnaseq_cont_automated/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.BI_IllumGANoSplit");
				//BI IllumGA_curated
				//moduleMap.put("gsc/broad.mit.edu/illuminaga_dnaseq_cont_curated/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.BI_IllumGANoSplit");
				//BCGS_IllumHi_automated
				//moduleMap.put("gsc/bcgsc.ca/illuminahiseq_dnaseq_cont_automated/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.BCGCS_IllumHiNoSplit");
				//HGSC_IlummHi_automated
				//moduleMap.put("gsc/hgsc.bcm.edu/illuminaga_dnaseq_cont_automated/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.HGSC_IllumSolidNoSplit");
				//HGSC_IlummHi_curated
				//moduleMap.put("gsc/hgsc.bcm.edu/illuminaga_dnaseq_cont_curated/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.HGSC_IllumSolidNoSplit");
				//WUSTL _IllumHi (luad)
				//moduleMap.put("gsc/genome.wustl.edu/illuminahiseq_dnaseq_cont/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.WUSTL_IllumHiNoSplit");
				//WUSTL _IllumHi automated (ucs)
				//moduleMap.put("gsc/genome.wustl.edu/illuminahiseq_dnaseq_cont_automated/mutations_protected/","edu.pitt.tcga.httpclient.module.pmnosplit.WUSTL_IllumHiAutomatedNoSplit");
				
				
				
				//harvard IllumGA cna low pass
				//moduleMap.put("cgcc/hms.harvard.edu/illuminahiseq_dnaseqc/cna/", "edu.pitt.tcga.httpclient.module.pmnosplit.HMS_IllumHi_CNVNoSplit");
				
				
				//**********************
				// RNASeq Level 2
				//**********************
				// RNA_BCGCC_IllumHi for "ov", "stad" - takes A LOT OF space
				//moduleMap.put("cgcc/bcgsc.ca/illuminahiseq_rnaseq/rnaseq/","edu.pitt.tcga.httpclient.module.rnaseq.level2.BCGCC_IllumHi_RNA");
				//BCGCC_IllumGA_RNA
				//moduleMap.put("cgcc/bcgsc.ca/illuminaga_rnaseq/rnaseq/","edu.pitt.tcga.httpclient.module.rnaseq.level2.BCGCC_IllumGA_RNA");
				
				//**********************
				// RNASeq Level 3
				//**********************
				// UNC_IllumHiSeq (brca), UNC_IllumGA (coad), BCGSC_IllumGA (stad), BCGSC_IllumHiSeq (stad)
				//moduleMap.put("cgcc/unc.edu/illuminahiseq_rnaseq/rnaseq/","edu.pitt.tcga.httpclient.module.rnaseq.level3.RNASeqLevel3");
				//moduleMap.put("cgcc/unc.edu/illuminaga_rnaseq/rnaseq/","edu.pitt.tcga.httpclient.module.rnaseq.level3.RNASeqLevel3");
				//moduleMap.put("cgcc/bcgsc.ca/illuminaga_rnaseq/rnaseq/","edu.pitt.tcga.httpclient.module.rnaseq.level3.RNASeqLevel3");
				//moduleMap.put("cgcc/bcgsc.ca/illuminahiseq_rnaseq/rnaseq/","edu.pitt.tcga.httpclient.module.rnaseq.level3.RNASeqLevel3");
				
				//**********************
				// RNASeqV2 Level 3
				//**********************
				//moduleMap.put("cgcc/unc.edu/illuminahiseq_rnaseqv2/rnaseqv2/","edu.pitt.tcga.httpclient.module.rnaseq.level3.RNASeqV2Level3");
				//moduleMap.put("cgcc/unc.edu/illuminaga_rnaseqv2/rnaseqv2/","edu.pitt.tcga.httpclient.module.rnaseq.level3.RNASeqV2Level3");
				
				
				//**********************
				//Exp_Gene:
				//**********************
				//  Agilent {07_1, 07_2, 07_3} Lev {1, 2, 3}
				//moduleMap.put("cgcc/unc.edu/agilentg4502a_07_1/transcriptome/","edu.pitt.tcga.httpclient.module.eg.Agilent");
				//moduleMap.put("cgcc/unc.edu/agilentg4502a_07_2/transcriptome/","edu.pitt.tcga.httpclient.module.eg.Agilent");
				//moduleMap.put("cgcc/unc.edu/agilentg4502a_07_3/transcriptome/","edu.pitt.tcga.httpclient.module.eg.Agilent");
				
				// Affymetrix BI HT_HG_U133A
				//moduleMap.put("cgcc/broad.mit.edu/ht_hg-u133a/transcriptome/","edu.pitt.tcga.httpclient.module.eg.AffymetrixHT_HG_U133A");
				//Affymetrix_WUSTL
				//moduleMap.put("cgcc/genome.wustl.edu/hg-u133_plus_2/transcriptome/","edu.pitt.tcga.httpclient.module.eg.AffymetrixHG_U133_plus_2");
				
				//Illum_mrna_DGE
				//moduleMap.put("cgcc/hms.harvard.edu/illuminaga_mrna_dge/transcriptome/","edu.pitt.tcga.httpclient.module.eg.HMS_Illum_MRNA_DGE");
				
				//**********************
				//Methylation:
				//**********************
				//moduleMap.put("cgcc/jhu-usc.edu/humanmethylation27/methylation/","edu.pitt.tcga.httpclient.module.methyl.HumanMethyl");
				//moduleMap.put("cgcc/jhu-usc.edu/humanmethylation450/methylation/","edu.pitt.tcga.httpclient.module.methyl.HumanMethyl");
				
				//moduleMap.put("cgcc/jhu-usc.edu/illuminadnamethylation_oma002_cpi/methylation/","edu.pitt.tcga.httpclient.module.methyl.IllumMethylOMA");
				moduleMap.put("cgcc/jhu-usc.edu/illuminadnamethylation_oma003_cpi/methylation/","edu.pitt.tcga.httpclient.module.methyl.IllumMethylOMA");
			}
			
			return moduleMap;
		}
		
		
		private static void load(){
			try {
				params = new Properties();
				params.load(new FileInputStream(System.getProperty("user.dir")+
						File.separator+"resources"+File.separator+configName));
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		}
		
		public static Properties getProperties(){
			if(params == null )
				load();
			return params;
		}
		
		
		
		public static String getStrProperty(String pName){
			String toret = null;
			try{
				toret= getProperties().getProperty(pName).trim();
			} catch (NullPointerException e){
				
			}
			
			if(toret == null){
				System.err.println("************************");
				System.err.println("*  Please check your *"+ pName+"* PAREMETER  setting in "+System.getProperty("user.dir")+
						File.separator+"resources"+File.separator+configName+"  file.");
				System.err.println("************************");
				System.exit(0);
			}
			return toret;
		}
		
		public static boolean getBooleanProperty(String pName){
			try{
			boolean toret = Boolean.valueOf(getProperties().getProperty(pName).trim()).booleanValue();
			} catch (NullPointerException e) {
				System.err.println("************************");
				System.err.println("*  Please check your *"+ pName+"* PAREMETER  setting in "+System.getProperty("user.dir")+
						File.separator+"resources"+File.separator+configName+"  file.");
				System.err.println("************************");
				System.exit(0);
			}
			return Boolean.valueOf(getProperties().getProperty(pName).trim()).booleanValue();
		}
		
		public static int getIntegerProperty(String pName){
			return Integer.parseInt(getStrProperty(pName));
		}
		
		/*public static String getPGRR_Prefix(){
			return "pgrr";
		}
		
		public static String getPGRR_PrefixURI(){
			return "http://purl.org/pgrr/core#";
		}*/

		/*public static String getRDFPrefix(){
			if(rdfPrefix ==null)
				rdfPrefix = getProperties().getProperty("rdf.prefix");
			return rdfPrefix;
		}*/
		
		/*public static String getInsertURL(){
			if(INSERT_URL == null)
				INSERT_URL =  getProperties().getProperty(getRDFPrefix()+".insert.url");
			return INSERT_URL;
		}
		
		public static String getSparqlURL(){
			if(SPARQL_URL == null)
				SPARQL_URL =  getProperties().getProperty(getRDFPrefix()+".sparql.url");
			return SPARQL_URL;
		}*/
		
		public static String getDayFormat(){
			if(dayFormat == null)
				dayFormat = new SimpleDateFormat(DAY_FORMAT);
			return dayFormat.format(new Date());
		}
		
		
		public static void main(String[] args) {
		
		}

}
