package edu.pitt.tcga.httpclient.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.pitt.tcga.httpclient.util.MySettings;

public class ErrorLog {
	
	static File errFile = new File (MySettings.LOG_DIR + MySettings.getDayFormat()+"_errors.txt");
	
	public static void log(String err){	
System.out.println("ERROR: "+err);
			log(err, errFile);
	}
	
	public static void log (String err, File file){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
			writer.append(err+"\n");
			writer.close();
			writer = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void logFatal(String err){
		log(err);
		// send me email and
		System.exit(0);
	}
}
