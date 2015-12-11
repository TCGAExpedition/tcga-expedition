package edu.pitt.tcga.httpclient.timeline;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DateRecord {
	private String dateStr;
	private String intiDateFormat = "yyyy-MM-dd";
	private int numFiles = 0; // total number of files in this archive
	private long days;
	private Map<String, List<String>> fileStatus; // keys: "ADDED", "MODIFIED", "DELETED" "TOTAL_NUM_FILES"; val: file names
	
	
	private static String[] status = {"+","R","_"};
	
	private long milliToDay = 1000*60*60*24; //milliseconds to 24 hours
	
	
	public DateRecord(String dateStr){
		this.dateStr = dateStr;
		try {
			Date date = new SimpleDateFormat(intiDateFormat).parse(dateStr);
			days = date.getTime()/milliToDay;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fileStatus = new HashMap<String, List<String>>();
	}
	
	
	
	
	/**
	 * "+" - added
	 * "-" - removed
	 * "R" - revised
	 * @param status
	 * @param file
	 */
	public void setFileStatus(String status, String file){
		List<String>vList = fileStatus.get(status);
		if(vList == null){
			vList = new ArrayList<String>();
			fileStatus.put(status,  vList);
		}
		vList.add(file);
	}
	
	public int getNumFiles() {
		return numFiles;
	}

	public void setNumFiles(int n) {
		numFiles = numFiles+n;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		String TAB = "\t";
		int j = 0; // used to add tabs;
		for(String s:status){
			for(String fn:fileStatus.get(s)){
				sb.append(dateStr+TAB);
				sb.append(days+TAB);
				// tabs before
				for(int i=0; i<j; i++)
					sb.append(TAB);
				sb.append(fn+TAB);
				//tabs after
				for(int k=j-1; k>1; k--)
					sb.append(TAB);
				sb.append("/n");
			}
			j++;
		}
		return sb.toString();
	}
	
	/**
	 * returns date as yyyy-mm-dd, date as long, number of added, modified and removed files
	 * @return
	 */
	public String integratedString(){
		StringBuilder sb = new StringBuilder();
		String TAB = "\t";
		sb.append(dateStr+TAB);
		sb.append(days+TAB);
		int j = 0;
		for(String s:status){
			if(fileStatus.get(s) != null)
				sb.append(fileStatus.get(s).size());
			else
				sb.append(0);
			if(j<status.length-1)
				sb.append(TAB);
			j++;
		}
		// append total number of files
		sb.append(TAB);
		sb.append(numFiles);
		sb.append("\n");
		
			
		return sb.toString();
	}
	
	public static void main(String[] args){
		
		try {
			
			long millToDay = 1000*60*60*24;
			String dat = "2013-10-11";
			
			Date date = new SimpleDateFormat("yyyy-MM-dd").parse(dat);
			System.out.println("dat: "+dat+"  days: "+date.getTime()/millToDay+" y: "+
			date.getYear()+" m: "+date.getMonth()+" d: "+date.getDay()+" getDate: "+date.getDate());
			
			String dat2 = "2013-09-30";
			Date date2 = new SimpleDateFormat("yyyy-MM-dd").parse(dat2);
			System.out.println("dat: "+dat2+"  days: "+date2.getTime()/millToDay+" y: "+
					date2.getYear()+" m: "+date2.getMonth()+" d: "+date2.getDay()+" getDate: "+date2.getDate());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}




	

}
