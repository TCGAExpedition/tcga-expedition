package edu.pitt.tcga.httpclient.util;

import java.util.List;

public class LineBeanHelper {
	
	public static LineBean getBeanByName(String name, List<LineBean> beans){
		for(LineBean lb:beans)
			if(lb.getName().equals(name))
				return lb;
		return null;
	}
	
	/**
	 * Returns the last folder OR the file name from URL
	 * @param url - path to folder/file
	 * @return
	 */
	public static String getNameFromURL(String url){
		String toret = (url.endsWith("/"))?url.substring(0,url.length()-1):url;
		return toret.substring(toret.lastIndexOf("/")+1);
	}

}
