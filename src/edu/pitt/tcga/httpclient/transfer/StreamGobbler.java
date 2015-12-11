package edu.pitt.tcga.httpclient.transfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread {
	StringBuilder outputBuffer = new StringBuilder();
	InputStream is;
	String type;

	public StreamGobbler(InputStream is, String type) {
		this.is = is;
		this.type = type;
	}

	public void run() {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(
					is));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				//System.out.println("SG: "+line);
				outputBuffer.append(line + "\n");
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				bufferedReader.close();
			} catch (IOException e) {
				// ignore this one
			}
		}

	}
	
	public StringBuilder getOutput(){
		return outputBuffer;
	}
}
