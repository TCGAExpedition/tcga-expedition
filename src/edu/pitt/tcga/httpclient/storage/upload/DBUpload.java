package edu.pitt.tcga.httpclient.storage.upload;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.util.MySettings;


public class DBUpload {
	private static final int IS_SUBJECT = 0;
	private static final int IS_PREDICATE_OR_GRAPH = 1;
	private static final int IS_OBJECT = 2;
	
	private Storage storage = null;
	
	
	private static String PREFIX = "tmp_"; // use for tempo files/tables;
	private static String DELIM = "\t";

	private PostgreStatements pgSt = null;
	private long stTime = 0;
	private SimpleDateFormat tsf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
	private double maxNumRecords = 0.0;
	
	public DBUpload(){
		initStatements();	
	}
	
	public DBUpload(Storage storage){
		this.storage = storage;
		initStatements();
		
	}
	
	private void initStatements(){
		File f = new File(MySettings.TEMPO_DIR);
		f.mkdir();
		pgSt = new PostgreStatements();	
		maxNumRecords = 300000.0;
	}
	
	public PostgreStatements getPostgreStatements(){
		return pgSt;
	}
	
	public void uploadNQFromDir(String dir){
		File folder = new File(dir);
		  File[] listOfFiles = folder.listFiles(); 
		  int sz = listOfFiles.length;
		 
		  for (int i = 0; i< sz; i++) 
		  {
			   if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".nq")) {
				   uploadNQFromFile(listOfFiles[i].getPath(), false);
				   if(storage != null)
					   storage.moveFileToHistory(listOfFiles[i].getName());
			   }
		  }
		  
		  pgSt.disconnect();
	}
	
	public void uploadNQFromFile(String fPath, boolean onlyOneFile){
		File nqFile = new File(fPath);
		String nqFileName = nqFile.getName();
		String nqFileNameNoExt = nqFileName.substring(0,nqFileName.lastIndexOf("."));

		stTime = System.currentTimeMillis();
		long intermTime = System.currentTimeMillis();
		ErrorLog.log("Start downloading  from file "+fPath+" at  "+ tsf.format(new Date()));
		int lineCounter = 0;
		int partCounter = 0;
		BufferedReader br = null;
		PrintWriter printWriter = null;
		try {
			// upload stripped lines to tempoFile
			String sCurrentLine;
			br = new BufferedReader(new FileReader(fPath));
			String graphName = null, tmpFileName = null, tmpTableName = null;
			boolean isNewFile = true;
			while ((sCurrentLine = br.readLine()) != null) {
				sCurrentLine = replaceSpaceByTab(sCurrentLine, "\t");
				if(isNewFile){
					partCounter++;
					isNewFile = false;
					tmpFileName = PREFIX+nqFileNameNoExt+"_"+String.valueOf(partCounter)+".tsv";
					graphName = graphNameFromNQ(sCurrentLine, "\t");
					tmpTableName = PREFIX+graphName+"_"+String.valueOf(partCounter);
					printWriter = new PrintWriter(new BufferedWriter(new FileWriter(tmpFileName, true)));
					
				}
				
				printWriter.write(stripNQRecord(sCurrentLine, "\t"));
				lineCounter++;
				if(String.valueOf(lineCounter).endsWith("00000")){
					ErrorLog.log("Loaded to tempo file: "+lineCounter+" at  "+ tsf.format(new Date())+" for"+fPath);
	System.out.println("Loaded to tempo file: "+lineCounter+" at  "+ tsf.format(new Date())+" for"+fPath);
				}
				// max num records? - upload to tempoDB -> dest table
				if(String.valueOf(lineCounter/maxNumRecords).endsWith(".0")){
					if (printWriter != null) {
						printWriter.close();
						printWriter.flush();
						printWriter = null;
					}
					isNewFile = true;
		System.out.println("in 1 finished load to tmp FILE "+tmpFileName+" in "+(System.currentTimeMillis()-intermTime)/1000 +"sec.");
		intermTime = System.currentTimeMillis();
					toTempAndDestTables(new File(tmpFileName), tmpTableName, graphName);
					intermTime = System.currentTimeMillis();
				}
			}
			if(!isNewFile){
	System.out.println("in 2 finished load TEMPO_DIR: "+MySettings.TEMPO_DIR+" to tmp FILE "+tmpFileName+" in "+(System.currentTimeMillis()-intermTime)/1000 +"sec.");
	intermTime = System.currentTimeMillis();
				if (printWriter != null){
					printWriter.close();
					printWriter.flush();
					printWriter = null;
				}
				toTempAndDestTables(new File(tmpFileName), tmpTableName, graphName);
			}
			
			if (br != null) br.close();
			if (printWriter != null) printWriter.close();
						
 
		} catch (IOException e) {
			e.printStackTrace();
			ErrorLog.log("UploadThroughTempo.uploadNQFromFile: "+ e.getMessage());
		} finally {
			if(onlyOneFile)
				pgSt.disconnect();
			String logStr = "Finished downloading  from file "+fPath+" at  "+ tsf.format(new Date())+" overall time: "+
				(System.currentTimeMillis()-stTime)/1000+" sec for "+lineCounter+" lines.";
System.out.println(logStr);
			ErrorLog.log(logStr);
		}
		
		
	}
	
	public void toTempAndDestTables(File tmpFile, String tmpTableName, String destTableName){
long intermTime = System.currentTimeMillis();

		pgSt.createNQTable(tmpTableName);
		pgSt.copyIn(tmpFile, tmpTableName, DELIM);
					//tmpFile.delete();
					
System.out.println("finished load to tmp TABLE "+tmpFile.getName()+" in "+(System.currentTimeMillis()-intermTime)/1000 +"sec.");
intermTime = System.currentTimeMillis();
					
		// dump to the destination table
		pgSt.tempToDestTable(tmpTableName, destTableName);
					
System.out.println("finished load to DEST TABLE "+destTableName+" in "+(System.currentTimeMillis()-intermTime)/1000 +"sec.");
intermTime = System.currentTimeMillis();
		
		//delete tmpTable
		pgSt.dropTable(tmpTableName);

		// delete tmpFile
		tmpFile.delete();
	}
		
	public static String  stripNQRecord(String nQuadStr, String delim){	
		String[] strArr = nQuadStr.split(delim);
		// subject
		StringBuilder bs = new StringBuilder(stripRDFValue(strArr[0], IS_SUBJECT) + DELIM);
		//predicate just all to lowcase AND replace 
		bs.append(stripRDFValue(strArr[1].toLowerCase(), IS_PREDICATE_OR_GRAPH) + DELIM);
		//object
		bs.append(stripRDFValue(strArr[2], IS_OBJECT) + DELIM);
		bs.append(stripRDFValue(strArr[3].toLowerCase(), IS_PREDICATE_OR_GRAPH)+"\n");
		strArr = null;
		return bs.toString();
		
	}
	
	public static String graphNameFromNQ(String nQuadStr, String delim){
		String[] strArr = nQuadStr.split(delim);
			
		String toret = stripRDFValue(strArr[3].toLowerCase(), IS_PREDICATE_OR_GRAPH);
		strArr = null;
		return toret;
	}
	
	private static String replaceSpaceByTab(String nQuadStr, String delim){
		String[] strArr = nQuadStr.split(delim);
		if(strArr.length == 5){
			strArr = null;
			return nQuadStr;
		}

		if(strArr.length == 1 && nQuadStr.indexOf("> <") != -1 &&
				nQuadStr.indexOf("> \"") != -1 && 
				//nQuadStr.indexOf("\" <") != -1 && // not true for '"2015-05-12T19:19:32Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> <'
				nQuadStr.indexOf("> .") != -1){ // check if it's a space?
			nQuadStr = nQuadStr.replaceAll("> <", ">"+DELIM+"<");
			nQuadStr = nQuadStr.replaceFirst("> \"", ">"+DELIM+"\"");
			if(nQuadStr.indexOf("\" <") != -1)
				nQuadStr = nQuadStr.replaceFirst("\" <", "\""+DELIM+"<");
			nQuadStr = nQuadStr.replaceFirst("> .", ">"+DELIM+".");
		}

		return nQuadStr;
	}
		
	private static String stripRDFValue(String rdfStr, int ind)	{
		if(ind == IS_OBJECT) {
			//only #dateTime is a special case now
			int dateTimeInd = rdfStr.indexOf("\"^^<http://www.w3.org/2001/XMLSchema#dateTime>");
			if(dateTimeInd > -1){
				rdfStr = rdfStr.substring(0, dateTimeInd);
				// convert #dateTime from 2014-02-19T23:29:09Z  to 2014-02-19 23:29:09
				rdfStr = rdfStr.replace("T"," ");
				rdfStr = rdfStr.replace("Z","");
				if(rdfStr.startsWith("\"")) 
					rdfStr = rdfStr.substring(1);	
			}
			// remove surrounding double quotes
			if(rdfStr.startsWith("\"") && rdfStr.endsWith("\""))
				rdfStr = rdfStr.substring(1,rdfStr.lastIndexOf("\""));
		}
		
		if(rdfStr.indexOf("#") !=-1 && 
				(rdfStr.endsWith(">") || rdfStr.endsWith("> .") || rdfStr.endsWith(">\t.")))
			rdfStr = rdfStr.substring(rdfStr.indexOf("#")+1, rdfStr.lastIndexOf(">"));
		 
		
		if(ind == IS_PREDICATE_OR_GRAPH)
			rdfStr = rdfStr.replaceAll("-", "_");	
		return 	rdfStr;
	}
	
	/**
	 * 
	 * @return tab delimitered s p o g string
	 */
	public static String nqRecordToString(String nqStr){
		String toret= replaceSpaceByTab(nqStr, "\t");
		return stripNQRecord(toret, "\t");
		
	}
	
	
	public static void main(String[] args) {
		if(args.length == 0) {
			//test case 
			DBUpload conv = new DBUpload();
			
			//conv.uploadNQFromFile("C:/DevSrc/cvs/rdf_to_db/upload/corrected_test.nq", true);
			
			conv.uploadNQFromDir("/home/opm1/devSrc/rdf_to_db/upload/");
			//conv.uploadNQFromFile("/home/opm1/devSrc/rdf_to_db/upload/29-05-2015_bamMetadata_HNSC_WXS.nq", true);

			System.out.println("done UploadThroughTempo");
		} else {
		
			if(args.length != 2){
				System.err.println("Application requires two arguments: args[0] - directory or file AND args[1] - isDirectory or isFile.");
				System.exit(0);
			}
					
			DBUpload conv = new DBUpload();
			if(args[1].equalsIgnoreCase("isFile"))
				conv.uploadNQFromFile(args[0], true);
			else
				conv.uploadNQFromDir(args[0]);
		}
	}

}
