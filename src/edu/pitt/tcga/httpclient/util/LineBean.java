package edu.pitt.tcga.httpclient.util;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.StringUtils;

public class LineBean implements Comparable<LineBean> {
	private String url;
	private String fullURL;
	private String name = null;
	private String size = "0";
	private String modifiedDate="";
	
	
	public LineBean(String url, String t, String size){
		setFullURL(url);
		setUrl(url);
		setSize(size);
		setModifiedDate(t);
	}
	
	public LineBean(String url){
		setFullURL(url);
		setUrl(url);
	}
	
	/**
	 * from /tumor/<diseaseStudy>/.../ ,
	 * so start index = 7; 
	 * @return
	 */
	public String getDiseaseStudy(){
		int indEnd = getUrl().indexOf("/",8);
		return getUrl().substring(7, indEnd);
	}
	
	public void setFullURL(String url){
		this.fullURL = url;
	}
	 public String getFullURL(){
		 return fullURL;
	 }
	
	public String getUrl() {
		return url;
	}
	
	public String getLevel(){
		int stInd = fullURL.toLowerCase().indexOf("level_");
		if(stInd == -1) return null;
		return fullURL.substring(stInd+6, fullURL.indexOf(".", stInd+1));
	}
	
	public boolean isArchiveLevel(){
		int count = url.length() - url.replace("/", "").length();		
		return (count < 8)?false:true;
	}
	
	/**
	 * /tumor/acc/bcr/
	 * @return
	 */
	public boolean isCenterTypeLevel(){
		int count = url.length() - url.replace("/", "").length();		
		return (count == 4)?true:false;
	}
	
	/**
	 * /tumor/acc/bcr/
	 * @return
	 */
	public String getCenterTypeName(){
		String[] split = url.split("/");
		//would be 7-th
		if(split.length < 4)
			return "";
		return split[3];
	}
	
	/**
	 * /tumor/acc/bcr/nationwidechildrens.org/tissue_images/slide_images/
	 * @return
	 */
	public boolean isDataTypeLevel(){
		int count = url.length() - url.replace("/", "").length();		
		return (count == 7)?true:false;
	}
	
	public boolean isDirectory(){
		if(getName().endsWith("txt") || getName().endsWith("html"))
			return false;
		List<LineBean> lbs = TCGAHelper.getPageBeans(fullURL);
		if(lbs !=null && lbs.size() > 0){
			lbs.clear();
			lbs = null;
			return true;
		}
		return false;
	}
	
	/**
	 * Since public and controlled data have different ulrs up to the "/tumor/" point
	 * @param url
	 */
	public void setUrl(String url) {
		int ind = url.indexOf("/tumor/");
		try{
			this.url = url.substring(url.indexOf("/tumor/"));
		} catch (StringIndexOutOfBoundsException ex){
			this.url = url;
		}
	}
	
	public String getName(){
		if (name != null) return name;
		String toret = (url.endsWith("/"))?url.substring(0,url.length()-1):url;
		name = toret.substring(toret.lastIndexOf("/")+1);
		return name;
	}
	
	/**
	 * For hms.harvard.edu_BLCA.IlluminaHiSeq_DNASeqC.mage-tab.1.5.0
	 * @return hms.harvard.edu_BLCA.IlluminaHiSeq_DNASeqC.mage-tab
	 */
	public String archiveNameUpToRevision(){
		String endStr = "";
		if(getName().endsWith(".0"))
			endStr = ".0";
		else if(name.indexOf(".0.tar") != -1)
			endStr=".0.tar";
		String toret = name.substring(0, name.lastIndexOf(endStr));
		toret = toret.substring(0, toret.lastIndexOf("."));	
		return toret+".";
	}
	
	/**
	 * 
	 * @return [center, experiment]
	 */
	public int[] getCenterExperiment(){
		String name = getName();
		if(!name.endsWith(".0")) return null;
		int ind = name.lastIndexOf(".", name.length()-3);
		try{
			int exp = Integer.valueOf(name.substring(ind+1,name.lastIndexOf(".")));
			int  center = Integer.valueOf(name.substring(name.lastIndexOf(".",ind-1)+1,ind)); 
			int[] res = new int[2];
			res[0] = center;
			res[1] = exp;
			return res;
		} catch (NumberFormatException e) {return null;}	
	}
	
	
	/**
	 * /tumor/disease/dataType/center/platform/analysisType/archive/file
	 * @return
	 */
	public String getAnalysisType(){
		String[] split = url.split("/");
		//would be 7-th
		if(split.length < 7)
			return null;
		return split[6];
		
	}
	
	/**
	 * /tumor/disease/dataType/center/
	 * @return
	 */
	public String getCenterName(){
		String[] split = url.split("/");
		//would be 7-th
		if(split.length < 5)
			return null;
		return split[4];
	}
	
	/**
	 * 
	 * @return dataType/center/platform/analysisType/
	 */
	public String getModuleIdent(){
		String[] split = url.split("/");
		//would be 7-th
		if(split.length < 7)
			return null;
		String toret =  split[3]+"/"+split[4]+"/"+split[5]+"/"+split[6]+"/";
		split = null;
		return toret;
		
	}
	
	public int getDepth(){
		String[] split = url.split("/");
		int d = split.length;
		split = null;
		return d;
	}
	
	public String getPlatform(){
		String[] split = url.split("/");
		String toret = split[5];
		
		split = null;
		return toret.replaceAll("_", "-");
	}
	
	public String getDataTypeCenterPlatform(){
		String[] split = url.split("/");
		if(split.length < 6)
			return null;
		return split[3]+"/"+split[4]+"/"+split[5];
	}
	
	public String getDataTypeToAnalysisType(){
		String[] split = url.split("/");
		if(split.length < 7)
			return null;
		return split[3]+"/"+split[4]+"/"+split[5]+"/"+split[6]+"/";
	}
	
	public String getCenterTypetoDataType(){
		String[] split = url.split("/");
		if(split.length < 7)
			return null;
		return split[3]+"/"+split[4]+"/"+split[5]+"/"+split[6]+"/";
	}
	
	public String getSize() {
		return size;
	}
	
	/**
	 * Sets size in KB
	 * @param size
	 */
	public void setSize(String size) {
		this.size = size;
	}
	
	public String getModifiedDate() {
		return modifiedDate;
	}
	
	public void setModifiedDate(String modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	@Override
	/**
	 * compare by name
	 */
	public int compareTo(LineBean otherBean) {
		// TODO Auto-generated method stub
		return  getName().compareTo(otherBean.getName());
	}
	

}
