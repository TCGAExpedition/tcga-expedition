package edu.pitt.tcga.httpclient;


import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;



import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.module.cnv.CN_Level4;
import edu.pitt.tcga.httpclient.module.ep.MassSpecModule;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;


public class TCGAExpedition {
	
	private static List notTCGATypes= Arrays.asList("cn_level4",
			"mass_spectrometry");
	
	public static List<String> notFoundRNASeq_Level3 = new ArrayList<String>();
	public static List<String> notFoundRNASeqV2_Level3 = new ArrayList<String>();
	public static List<String> notFoundProtMut_Level2 = new ArrayList<String>();
	
	public static void createDirs(){
		String [] dirs = {MySettings.SUPERCELL_HOME, MySettings.RUNTIME_DATA_DIR,
				MySettings.TEMPO_DIR, MySettings.NQ_UPLOAD_DIR,
				MySettings.NQ_UPLOAD_HISTORY_DIR, MySettings.REPORTS_DIR,
				MySettings.LOG_DIR, MySettings.BAM_SUMMARY_DIR, MySettings.NEW_TSS_DIR, MySettings.METADATA_DIR};
	
		for (String d:dirs) {
			File f = new File(d);
			f.mkdirs();
		}
	}
	
	
	/**
	 * 
	 * @param levBeans
	 * @param prevTarget
	 * @param analysisType
	 * @param extension
	 * @param map <dataType, List of diseases>
	 */
	public static void scrapeForDataType(List<LineBean> levBeans, String prevTarget, 
			String analysisType,String extension, Map<String, List<String>> map){
		boolean isArchiveLevel = TCGAHelper.isArchiveLevel(levBeans);
		if (isArchiveLevel && analysisType.equals(levBeans.get(0).getAnalysisType())){
			List<LineBean> fileBeans = TCGAHelper.getPageBeans(levBeans.get(0).getFullURL());
	
			int readMax = 30; // read max 30 file names
			int pos =0;
			for(LineBean lb:fileBeans){
				if(lb.getName().endsWith(extension) ){
					String p = lb.getDataTypeCenterPlatform();
					String d = lb.getDiseaseStudy();
					if(map.get(p) == null){
						List<String> list = new ArrayList<String>();
						list.add(d);
						map.put(p, list);
					}
					else if(!map.get(p).contains(d)){
						map.get(p).add(d);
					}
					break;
				}
					if(pos == readMax)
						break;
					pos++;
			}
		} else {
			for(LineBean lba:levBeans){
				if(lba.getUrl().endsWith("/"))
					scrapeForDataType(TCGAHelper.getPageBeans(prevTarget+lba.getName()+"/"), prevTarget+lba.getName()+"/",
							analysisType, extension, map);
			}
		}
	}
	
	public static void redirectSystemOut(String filePath){
		try {
		    System.setOut(new PrintStream(new File(filePath)));
		} catch (Exception e) {
		     e.printStackTrace();
		}
	}
	
	public static void scrapeForDataTypeOnly(List<LineBean> levBeans, String prevTarget, 
			String analysisType, Map<String, List<String>> map){

		if (levBeans.size() != 0){
			if( levBeans.get(0).isDataTypeLevel() && analysisType.equals(levBeans.get(0).getAnalysisType())){
		
				String p = levBeans.get(0).getDataTypeCenterPlatform();
				String d = levBeans.get(0).getDiseaseStudy();
				if(map.get(p) == null){
					List<String> list = new ArrayList<String>();
					list.add(d);
					map.put(p, list);
				}
				else if(!map.get(p).contains(d)){
					map.get(p).add(d);
				}
			} else if (levBeans.get(0).getDepth() < 7) {
				for(LineBean lba:levBeans){
					if(lba.getUrl().endsWith("/"))
						scrapeForDataTypeOnly(TCGAHelper.getPageBeans(prevTarget+lba.getName()+"/"), prevTarget+lba.getName()+"/",
								analysisType, map);
				}
			}
		}
	}

	
	public static void scrape(List<LineBean> levBeans, String resourceKey,String analysisDirName ){
		MySettings.PGRR_META_NQ_FILE = "_"+analysisDirName+"_"+resourceKey+".nq";
		
		//create dirs if needed:
		setUniqueDirs();
		
		TCGAModule.scrape(levBeans, TCGAModule.getListProperty(resourceKey, ","), resourceKey, analysisDirName);

	}
	
	public static void setUniqueDirs(){
		String uuid = UUID.randomUUID().toString();
		MySettings.TEMPO_DIR = MySettings.TEMPO_DIR + uuid + File.separator;
		MySettings.LOG_DIR = MySettings.LOG_DIR + uuid + File.separator;

		MySettings.PGRR_META_NQ_FILE = "_metadata_"+uuid+".nq";
		createDirs();
		TCGAModule.clearTempoDir();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//org.apache.log4j.BasicConfigurator.configure(new NullAppender());

		
		// create unique directories for this particular instance;
		String[] diseaseList = null;
		String analysisType = "";
		String accessType = "";
		
		/*//test
		args = new String[3];
		args[0] = "esca";
		args[1] = "protected_mutations";
		args[2] = "controlled";*/
		
		//test 2
		/*args = new String[3];
		args[0] = "acc";
		args[1] = "clinical";
		args[2] = "public";*/
		
		
		// 0 -diseaseList (comma separated) , use "ALL" for all
		// 1 - analysisType
		// 2 - accessType (controlled or public
		if(args !=null && args.length==3){
			if(!args[0].equalsIgnoreCase("ALL")){
				diseaseList = args[0].split(",");
			} 
			analysisType = args[1].toLowerCase();
			analysisType = analysisType.replaceAll("\\(", "");
			analysisType = analysisType.replaceAll("\\)", "");
			accessType = args[2].toLowerCase();
		} else{
			System.err.println("**********\n" +
					"* USAGE: * \n" +
					"**********\n"+
					"TCGAScraper takes 3 arguments: \n" +
					"(1) - comma separated list of disease abbreviations (eg. 'acc,brca'), or use 'ALL' for the whole set.\n" +
					"(2) - ONE Analysis Type. Available analysis types description is below.\n"+
					"(3) - ONE Access Type (controlled or public).\n" +
					"      NOTE: For TCGA data check TCGA access type.\n" +
					"******************************\n"+
					" Available analysis type and access type sets:\n" +
					"*** TCGA data ***\n" +
					" * Analysis Types *:  \n" +
					"   clinical, cnv_(low_pass_dnaseq), cnv_(cn_array), cnv_(snp_array), images, dna_methylation, expression_gene,\n" +
					"   expression_protein, fragment_analysis, mirnaseq, protected_mutations, protected_mutations_maf\n" +
					"   rnaseq, rnaseqv2, somatic_mutations\n" +
					" * Access Types *: controlled or public - same as at TCGA \n\n"+
					"*** Firehose ***\n" +
					" * Analysis Type *: cn_level4 \n * Access type *: controlled \n\n"+
					"*** Georgetown ***\n" +
					" * Analysis Type *: mass_spectrometry \n * Access type *: public \n\n"+
					"*** cgHub BAM data ***\n" +
					" Coming soon\n\n"+
					"************\n" +
					"* Example: *\n" +
					"************\n"+
					"java -jar tcgaExpedition.jar acc clinical public");
			System.exit(0);
		}
		
		long stTime = System.currentTimeMillis();
		
		if(notTCGATypes.contains(analysisType)){
			if(analysisType.equals("mass_spectrometry")){
				MassSpecModule msm = new MassSpecModule();
				msm.doDownload();
			}
			else if(analysisType.equals("cn_level4")){
				CN_Level4 lev4 = new CN_Level4();
				lev4.doDownload();
			}
				
		}
			
		else{
			String resourceKey = TCGAModule.getProperty(analysisType+"_"+accessType+".resourcekey");
			String analysisDirName = TCGAModule.getProperty(analysisType+"_"+accessType+".analysisdir");
			String root = MySettings.getRoot(accessType);
			
			
		
			
			if(diseaseList != null) {
				
				for(String disAbbr:diseaseList){
		//System.out.println("dir: "+ root+disAbbr+"/");
					scrape(TCGAHelper.getPageBeans(root+disAbbr+"/"), resourceKey, analysisDirName);
				}
			} else // scan all
				scrape(TCGAHelper.getPageBeans(root), resourceKey, analysisDirName);
		}
		
	
	long endTime = System.currentTimeMillis();
	
		System.out.println("Done justScrape in "+(endTime-stTime)/1000+" sec");
		
	}


}
