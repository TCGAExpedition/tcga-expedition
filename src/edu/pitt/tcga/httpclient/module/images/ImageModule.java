package edu.pitt.tcga.httpclient.module.images;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

import edu.pitt.tcga.httpclient.module.Aliquot;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.module.TCGAModule;
import edu.pitt.tcga.httpclient.util.CodesUtil;
import edu.pitt.tcga.httpclient.util.LineBean;
import edu.pitt.tcga.httpclient.util.MySettings;
import edu.pitt.tcga.httpclient.util.TCGAHelper;

public class ImageModule extends TCGAModule {

	private String dataType = "";


	private String[] endings = {".svs"};
	

	@Override
	public String[] getResourceEndings() {
		return endings;
	}

	

	@Override
	public String getAnalysisDirName() {
		return "slide_images";
	}

	@Override
	public String getResourceKey() {
		return "images";
	}

	@Override
	public String dataAccessType() {
		return TCGAModule.PUBLIC;
	}
	
	@Override
	public  boolean canProcessArchive(LineBean archiveBean){		
		return true;
	}

	public static boolean doIt = false;

	/**
	 * Any file name: <sampleWithVial>+<whaever>.uuid
	 * 1. Own barcode = <sampleWithVial>+<whaever>
	 * 2. uuid MUST be converted to LOWERCASE
	 * Example:
	 * TCGA-AR-A1AR-01Z-00-DX1.E7B7F6F0-9CC0-4D4F-8C9F-443A74D2BE40.svs 
	 * FileUtils.copyURLToFile(URL, File)
	 * 
	 */
	@Override
	public void processData(List<LineBean> levBeans) {	
		int sz = levBeans.size();
		int cn = 0;
		for (LineBean lb : levBeans) {
			if (lb.getName().endsWith(".svs")) {
//if (lb.getName().endsWith(".svs") && lb.getName().indexOf("TCGA-A6-2674") != -1){
//if(!doIt && lb.getUrl().equals("/tumor/ucec/bcr/nationwidechildrens.org/tissue_images/slide_images/nationwidechildrens.org_UCEC.tissue_images.Level_1.228.1.0/TCGA-D1-A3JQ-01A-01-TSA.F375AA8A-10E9-47D6-B5A7-1FC0D0F567C3.svs")){
	
//			doIt = true;
//} 

//if(doIt && lb.getName().endsWith(".svs")){
	
			setDataType(lb.getUrl());
						cn++;
		// clear tempo dir
				clearTempoDir();
				System.gc();
							
	System.out.println(lb.getDiseaseStudy()+" : "+cn+" out of "+sz+"  lb.n = " + lb.getUrl());
								
				Aliquot al = null;
				try{

					al = constructAliquot(lb);	
					if(al != null){
						File tempoFile = new File(MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".txt");
						if(al != null){
							al.setTempoFile(tempoFile);
	System.out.println("Creating tempoFile: "+MySettings.TEMPO_DIR + "f"+String.valueOf(TEMP_FILE_NUM)+".txt");
							FileUtils.copyURLToFile(new URL(lb.getFullURL()), tempoFile);
						}
						TEMP_FILE_NUM++;
					}
				} catch (IOException e) {
				  // TODO Auto-generated catch block
					e.printStackTrace();
				}

				if(al != null){
					ModuleUtil.transferNew(new Aliquot[]{al});	
				}
			} //lb.getName().endsWith(".svs")
	
		} // for(LineBean lb:levBeans)
	}
	
	public Aliquot constructAliquot(LineBean lb){
		Aliquot al = new Aliquot(lb.getUrl(), getDataType(),
				"svs");
		String lbName = lb.getName();
		int pos = lbName.indexOf(".");
		String barcode = lbName.substring(0,pos);
		// work around TCGA error:
		// /tumor/lusc/bcr/intgen.org/tissue_images/slide_images/intgen.org_LUSC.tissue_images.Level_1.77.1.0/d061c1d9-d0da-4faf-b631-43a0b50e43b0.svs
		if(!barcode.startsWith("TCGA"))
			return null;
		al.setBarcode(barcode);
		if(CodesUtil.getDiseaseAbbrFromURL(lb.getUrl()).equalsIgnoreCase("FPPP"))
			al.setDiseaseStudyAbb(CodesUtil.getDiseaseAbbFromBarcode(al.getBarcode()));
		// well, it looks like sometimes a wrong uuid is being attached to the file name
		/*try{
			al.setOrigUUID((lbName.substring(pos+1, lbName.indexOf(".svs"))).toLowerCase());
		} catch (StringIndexOutOfBoundsException e){}*/
		al.setFileFractionType("slide_image");
		al.setCenterName(CodesUtil.getCenterNameFromArchive(al.getArchiveName()));
		al.setCenterCode(CodesUtil.getCenterAbbFromArchName(al.getCenterName()));
		al.setPlatform(lb.getPlatform());
		al.setFileType("svs");
		return al;
	}
	
	public String getPlatformIdent(){
		return "";
	}
		

	public void setDataType(String lineBeanURL){
		if(lineBeanURL.indexOf("/diagnostic_images/") != -1 && !dataType.equals("Diagnostic_Images") )
			dataType = "Diagnostic_Images";
		else if(lineBeanURL.indexOf("/tissue_images/") != -1 && !dataType.equals("Tissue_Images") )
			dataType = "Tissue_Images";
	}
	


	@Override
	public String getDataType() {
		return dataType;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ImageModule im = new ImageModule();
		String url = MySettings.PUB_ROOT_URL+"kirp/bcr/nationwidechildrens.org/diagnostic_images/slide_images/";
		List<LineBean> list = TCGAHelper.getPageBeans(url);
		im.processArchiveLevel(TCGAHelper.recentOnly(list));
	
	
		System.out.println("done ImageModule");

	}

}
