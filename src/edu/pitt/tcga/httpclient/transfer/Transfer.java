package edu.pitt.tcga.httpclient.transfer;

import java.io.File;
import java.net.URL;
import java.util.List;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.util.DataMatrix;

public abstract class Transfer {
	
	//public abstract boolean checkFileExists(String dir, String fName);
	public abstract void writeToTSVFile(List<String[]> list, String sftServerDir, String fileSaveAs);
	public abstract void moveToServer(String sftServerDir, File f, String fileSaveAs, int mode);
	public abstract void copyURLToServer(String sftServerDir, URL sourceURL, String fileSaveAs, int mode);
	public abstract void moveToServer(String sftServerDir, String data, String fileSaveAs, int mode);
	public abstract DataMatrix getMetadata(String dir);
	public abstract void destroy();
	
	
	public static String execBash(String comm, boolean doOutput){
		String[] cmd = new String[3];
		cmd[0] = "/bin/bash";
		cmd[1] = "-c";
		cmd[2] = comm;
//System.out.println("EXEC: "+comm);
		return exec(cmd, doOutput);
	}
	
	/*public void execBash(String comm){
		String[] cmd = new String[3];
		cmd[0] = "/bin/bash";
		cmd[1] = "-c";
		cmd[2] = comm;
//System.out.println("EXEC: "+comm);
		exec(cmd);
	}*/
	
	public static String exec(String[] cmd, boolean doOutput){
		String cmdStr = null;
		if(cmd.length == 3 )
			cmdStr = cmd[2];
		else
			cmdStr = cmd[0];
		try{
			Runtime rt = Runtime.getRuntime();
		StringBuilder sb = new StringBuilder();
		for(String s:cmd)
			sb.append(s+" ");
 //System.out.println("Executing " + sb.toString());
        Process proc = rt.exec(cmd);
        // any error message?
        StreamGobbler errorGobbler = new 
            StreamGobbler(proc.getErrorStream(), "ERROR");            
        
        // any output?
        StreamGobbler outputGobbler = new 
            StreamGobbler(proc.getInputStream(), "OUTPUT");
            
        // kick them off
        errorGobbler.start();
        outputGobbler.start();
                                
        // any error???
        int exitVal = proc.waitFor();
     //System.out.println("ExitValue: " + exitVal);
     //System.out.println("out: "+outputGobbler.getOutput().toString());
     //System.out.println("out err: "+errorGobbler.getOutput().toString());
  
        
        if(exitVal != 0)
        	ErrorLog.logFatal("Transfer exec (exitVal != 0) exitVal="+exitVal+"  for commd: "+cmdStr);
        cmd = null;
        
        if(doOutput)
        	return outputGobbler.getOutput().toString();
        else
        	return null;
		
    } catch (Throwable t)
      {
        t.printStackTrace();
        ErrorLog.logFatal("Transfer exec (Throwable): "+cmdStr);
        return null;
      }
	
	}

}
