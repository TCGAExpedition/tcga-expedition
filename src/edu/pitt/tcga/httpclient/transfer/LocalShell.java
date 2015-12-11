package edu.pitt.tcga.httpclient.transfer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.transfer.exec.SystemCommandExecutor;
import edu.pitt.tcga.httpclient.util.CSVReader;
import edu.pitt.tcga.httpclient.util.DataMatrix;
import edu.pitt.tcga.httpclient.util.MySettings;

public class LocalShell extends Transfer {

	/*@Override
	public boolean checkFileExists(String dir, String fName) {
		File f = new File(dir+fName);
		return (f.exists())?true:false;
	}*/
	
	
	public String toUnderScores(String s){
		s = s.replaceAll(" ", "_");
		return s.replaceAll("-", "_");
	}
	
	public String toDefis(String s){
		s = s.replaceAll(" ", "_");
		return s.replaceAll("-", "_");
	}
	
	
	
	@Override
	public DataMatrix getMetadata(String dir){
		DataMatrix metadata = null;
		try {
			CSVReader reader = new CSVReader(new BufferedReader(
					new FileReader(new File(dir+MySettings.METADATA_FILE))), '\t');
			metadata = new DataMatrix("Metadata", reader.readAllToList());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return metadata;
	}

	@Override
	public void writeToTSVFile(List<String[]> list, String serverDir,
			String fileSaveAs) {
		File f = new File(serverDir);
		f.mkdirs();
		// if file exists, don't write header
		File nf = new File(serverDir + fileSaveAs);
		boolean fileExists = nf.exists();
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(nf, true));
			if (!fileExists) 
				output.write(ModuleUtil.copyArrayToStr(list.get(0), "\t"));
			
			list.remove(0);
		
	
			output.write(ModuleUtil.listArrayToString(list, "\t"));
			
			list.clear();
			list = null;

			output.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void moveToServer(String sftServerDir, File f, String fileSaveAs,
			int mode) {
		File nf = new File(sftServerDir);
		nf.mkdirs();
		// fileSaveAs exists, delete it;
		File saveTo = new File(sftServerDir + fileSaveAs);
		if(saveTo.exists())
			saveTo.delete();
		// do NOT move: could have alias
		//execBash("mv "+f.getAbsolutePath()+" \""+sftServerDir + fileSaveAs+"\"", false);
		/*try {
			FileUtils.copyFile(f, saveTo);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		execBash("cp '" + f.getAbsolutePath() + "' '" + sftServerDir + fileSaveAs+"'", false);
	}
	
	@Override
	public void copyURLToServer(String sftServerDir, URL sourceURL,
			String fileSaveAs, int mode) {
		File nf = new File(sftServerDir);
		nf.mkdirs();
		// fileSaveAs exists, delete it;
		File saveTo = new File(sftServerDir + fileSaveAs);
		if(saveTo.exists())
			saveTo.delete();
		
		try {
			FileUtils.copyURLToFile(sourceURL, saveTo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public void moveToServer(String sftServerDir, String data,
			String fileSaveAs, int mode) {
		File nf = new File(sftServerDir);
		nf.mkdirs();
		// fileSaveAs exists, delete it;
		File saveTo = new File(sftServerDir + fileSaveAs);
		if(saveTo.exists())
			saveTo.delete();
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(saveTo, false));
			writer.write(data);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	public String execNotWorkinkWithBash(List<String> comm) {
		StringBuilder sb = null;
		//System.out.println(comm);
		try {
			
			SystemCommandExecutor commandExecutor = new SystemCommandExecutor(comm);
		    int result = commandExecutor.executeCommand();

		    // get the stdout and stderr from the command that was run
		    sb = commandExecutor.getStandardOutputFromCommand();		   

		} catch (Exception e) {
			 e.printStackTrace();
		}
		comm.clear();
		comm = null;
System.out.println("exec out: "+sb.toString());
		return sb.toString();
	}

	public static void main(String[] args) {
		LocalShell fl = new LocalShell();
		
		String  dir = "/home/rcrowley/opm1/tcgaFeb12/repository/tcga/hnsc/TCGA-CV-5430/TCGA-CV-5430-01/Protected_Mutations/UCSC_IlluminaGA-DNASeq-Cont/";
		String softF = "TCGA-CV-5430_CTRL_hnsc_ucsc.edu_IlluminaGA-DNASeq-Cont_Level-2_grch37_primary-blood_V2.vcf";  
		
		/*List<String> cm = new ArrayList<String>();
			cm.add("/bin/bash");
			cm.add("-c");
			cm.add("if [ -f /home/rcrowley/opm1/howTo/How_TO_Torrent.txt ] ;then echo yes; else echo no;fi");
			*///fl.exec(cm.toArray(new String[0]));	
		 
		
		/*List<String> ls = new ArrayList<String>();
		ls.add("ls");
		ls.add("-a");
		fl.execNotWorkinkWithBash(ls);*/
		
		//fl.exec(new String[]{"/bin/bash", "-c", "cd /home/rcrowley/opm1; ls -a"});
	}
	
}
