package edu.pitt.tcga.httpclient.util;

import java.beans.Introspector;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.mail.MessagingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import sun.misc.BASE64Encoder;

import edu.pitt.tcga.httpclient.exception.QueryException;
import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.ModuleUtil;
import edu.pitt.tcga.httpclient.storage.Storage;
import edu.pitt.tcga.httpclient.storage.StorageFactory;
import edu.pitt.tcga.httpclient.storage.VirtuosoStorage;
import edu.pitt.tcga.httpclient.transfer.Transfer;



public class TCGAHelper {
	public static DecimalFormat fFileSize = new DecimalFormat("#.000");
	public static int B = 0;
	public static int MB = 1;
	public static int GB = 2;
	
	public static Pattern hrefP = Pattern.compile("<a\\b[^>]*href=\"[^>]*>(.*?)</a>");
	public static Pattern dateP = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
	
	private static HttpClient httpclient = null;
	
	//dateTime with zone
	public static SimpleDateFormat df = null ;
	// dateTime for tsv
	public static SimpleDateFormat df2 = null ;
	
	//df.setTimeZone(TimeZone.getTimeZone("UTC"));
	
	// tissue source and disease study tables
	public static List<String[]>tissueSourceSite = null;
	// any codes
	public static Map<String, List<String[]>> codes = new HashMap<String, List<String[]>>();
	
	// maps RDF dataType-links to TCGA data Type report
	private static Map<String, String> dataTypeMapper = null;
	
	private static int ATTEMPT_COUNT = 0;
	private static int MAX_ATTEMPTS = 3;
	
	
	public static HttpClient getHttpClient(){
	
		try {
			HttpClient base = new DefaultHttpClient();
			//SSLContext ctx = SSLContext.getInstance("TLS");
			SSLContext ctx = SSLContext.getInstance("SSL");
			X509TrustManager tm = new X509TrustManager() {
			 
			public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			}
			 
			public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			}
			 
			public X509Certificate[] getAcceptedIssuers() {
			return null;
			}
			};
			//ctx.init(null, new TrustManager[]{tm}, null);
			ctx.init(null, new TrustManager[]{tm}, new java.security.SecureRandom());
			
			SSLSocketFactory ssf = new SSLSocketFactory(ctx,SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https",443, ssf));
			return new DefaultHttpClient(ccm, base.getParams());
			} catch (Exception ex) {
		ex.printStackTrace();
		return null;
		}
	}
	
	
		
	
	
	public static void copyURLToFile(URL srcURL, File saveAs, boolean needAuthentication){
		if(!needAuthentication){
			try{
				FileUtils.copyURLToFile(srcURL, saveAs);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		} else{
			try{
				HttpURLConnection uc = (HttpURLConnection)srcURL.openConnection();
				BASE64Encoder enc = new sun.misc.BASE64Encoder();
				String userpassword = MySettings.getStrProperty("tcga.user") + ":" + MySettings.getStrProperty("tcga.pwd");
				String encodedAuthorization = enc.encode( userpassword.getBytes() );
				uc.setRequestProperty("Authorization", "Basic "+encodedAuthorization);
						
				uc.connect();
			
				InputStream raw = uc.getInputStream();
				FileUtils.copyInputStreamToFile(raw, saveAs);
				
				uc.disconnect();
					
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * First column should be compared with  inStoreCodes
	 * DiseaseStudy: abbreviation in UpperCase - convert toLower
	 * @param saveTo
	 * @param inStoreCodes
	 * @param tcgaCodes
	 * @param header
	 */
	public static void storeCodes(String saveTo, String graphName, List<String> inStoreCodes, 
			List<String[]> tcgaCodes, String[] header, boolean lookUpToLowerCase, int lookupCol, boolean updateInRealTime){
		
		try {
			List<String[]> cd = new ArrayList<String[]>();
			PrintWriter out = null;
			String uuid = null, qStr = null;
			String s = "", p = "", o = "",g = "";
			Storage storage = StorageFactory.getStorage();
			Storage virtStorage = VirtuosoStorage.getInstace();
			for (String[] line:tcgaCodes){
				if (!inStoreCodes.contains(line[lookupCol])){
					if(out == null && !Storage.UPDATE_IN_REAL_TIME)	
						out = new PrintWriter(new File(MySettings.NQ_UPLOAD_DIR+saveTo+".nq"));
					uuid = UUID.randomUUID().toString();
					for (int i=0; i<line.length; i++){
						if(i == 0)
							line[i] = lookUpToLowerCase?line[i].toLowerCase():line[i];
						s = storage.nameWithPrefixUUID(MySettings.TCGA_PRE,uuid);
						p = storage.nameWithPrefixPorG(MySettings.TCGA_PRE,header[i]);
						o = storage.literal(line[i]);
						g = storage.nameWithPrefixPorG(MySettings.TCGA_PRE,graphName);
						qStr = virtStorage.nameWithPrefixUUID(MySettings.TCGA_PRE,uuid)+" "+
								virtStorage.nameWithPrefixPorG(MySettings.TCGA_PRE,header[i])+" "+
								virtStorage.literal(line[i])+" "+virtStorage.nameWithPrefixPorG(MySettings.TCGA_PRE,graphName)+" .";
						if(!Storage.UPDATE_IN_REAL_TIME)
							out.println(qStr);
						if(updateInRealTime){
							try {
								storage.insert(s, p, o, g);
							} catch (QueryException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
				cd.add(line);
			}
			codes.put(saveTo, cd);
			
			if(out !=null)
				out.close();
			inStoreCodes.clear();
			inStoreCodes = null;
			tcgaCodes.clear();
			tcgaCodes = null;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static Map<String,String> getDataTypeMapper(){
		if (dataTypeMapper == null){
			//<displayName in report, pgrrName>
			dataTypeMapper = new HashMap<String, String>();
			dataTypeMapper.put("CNV (CN Array)", "CNV_(CN_Array)");
			
			dataTypeMapper.put("CNV (Low Pass DNASeq)", "CNV_(Low_Pass_DNASeq)");
			dataTypeMapper.put("CNV (SNP Array)", "CNV_(SNP_Array)");
			dataTypeMapper.put("Complete Clinical Set", "Clinical");
			dataTypeMapper.put("Minimal Clinical Set", "Clinical");
			dataTypeMapper.put("DNA Methylation", "DNA_Methylation");
			dataTypeMapper.put("Expression-Genes", "Expression_Gene");
			dataTypeMapper.put("Protected Mutations", "Protected_Mutations");
		
			dataTypeMapper.put("Expression-Exon", "Expression_Exon");
			dataTypeMapper.put("Expression-Protein", "Expression_Protein");
			
			dataTypeMapper.put("miRNA Sequence-Variants", "miRNASeq");
			dataTypeMapper.put("miRNASeq", "miRNASeq");
			dataTypeMapper.put("Quantification-miRNA", "miRNASeq");
			dataTypeMapper.put("Quantification-miRNA Isoform", "miRNASeq");
			
			dataTypeMapper.put("Quantification-Exon", "RNASeq");
			dataTypeMapper.put("Quantification-Gene", "RNASeq");
			dataTypeMapper.put("Quantification-Junction", "RNASeq");
			dataTypeMapper.put("RNA Sequence-Variants", "RNASeq");
			dataTypeMapper.put("RNA Structural-Variants", "RNASeq");
			dataTypeMapper.put("RNASeq", "RNASeq");
			dataTypeMapper.put("RNASeqV2", "RNASeqV2");
			dataTypeMapper.put("TotalRNASeqV2", "TotalRNASeqV2");

			dataTypeMapper.put("Expression-miRNA", "Expression_miRNA");
			dataTypeMapper.put("CNV (SNP Array)","CNV_(SNP_Array)");		
			dataTypeMapper.put("Methylation - Bisulfite Sequencing","Methyl_BisulfateSeq");
			dataTypeMapper.put("Fragment Analysis Results","Fragment_Analysis_Results");
			dataTypeMapper.put("Somatic Mutations","Somatic_Mutations");
			
			dataTypeMapper.put("Tissue Slide Images","Tissue_Images");
			
		
			
			dataTypeMapper.put("SNP Copy Number Results","CNV_(CN_Array)");
			dataTypeMapper.put("SNP Frequencies","CNV_(SNP_Array)");
			
		}
		
		return dataTypeMapper;
	}
	
	
	/**
	 * Store only mapped labels (<http://www.w3.org/2000/01/rdf-schema#label>
	 * First column should be compared with  inStoreCodes
	 * DiseaseStudy: abbreviation in UpperCase - convert toLower
	 * @param saveTo
	 * @param inStoreCodes
	 * @param tcgaCodes
	 * @param header
	 * returns true if there a new dataType
	 */
	public static boolean storeDateTypeCodes(String saveTo, String graphName, List<String> inStoreCodes, 
			List<String[]> tcgaCodes, String[] header, boolean lookUpToLowerCase){
		
		boolean found = true;
		String[] dataTypeLinks = {"Clinical","CNV_(CN_Array)",
				"CNV_(Low_Pass_DNASeq)", "CNV_(SNP_Array)",
				"DNA_Methylation", "Expression_Exon",
				"Expression_Genes","Expression_miRNA",
				"Expression_Protein", "Fragment_Analysis_Results",
				"miRNASeq", "Protected_Mutations",
				"RNASeq", "RNASeqV2",
				"Somatic_Mutations", "TotalRNASeqV2"};
		List<String> ignoreDataLinks = Arrays.asList("analyses","Annotations","BAM-file Relationship","Raw Sequence","Reports",
													"Firehose Reports","GCC Reports",
													"Sequencing Trace","Short-Read Relationship",
													"stddata","Trace-Sample Relationship");
		getDataTypeMapper();
		
		//try {
			List<String[]> cd = new ArrayList<String[]>();
			//PrintWriter out = null;
			String uuid = null, qStr = null, pgrrName = null;
			String s = "", p = "", o = "",g = "";
			Storage storage = StorageFactory.getStorage();
			Storage virtStorage = VirtuosoStorage.getInstace();
			for (String[] line:tcgaCodes){
				// if not displayName in codes yet and "available="Yes"
				if (!inStoreCodes.contains(line[1]) && line[3].equals("Yes") && !ignoreDataLinks.contains(line[1])){
					// check name
					pgrrName = dataTypeMapper.get(line[1]);
					if(pgrrName == null){
						found = true;
						// send me email
						try {
							Mailer.postMail(Mailer.MANAGER_SENDER,"New DataType found in TCGA Code Table Reports",  /*subject*/
									 "Sent from TCGAHelper: add to dataTypeMapper new dataType: "+line[1]+" ?", /*message*/
									 new String[] {MySettings.getStrProperty("manager.sendto.address")} /* recipient*/ 
									 );
						} catch (MessagingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					else {
						/*if(out == null)	
							out = new PrintWriter(new File(MySettings.NQ_UPLOAD_DIR+saveTo+".nq"));
						uuid = UUID.randomUUID().toString();
						for (int i=0; i<line.length; i++){
							if(i == 0)
								line[i] = lookUpToLowerCase?line[i].toLowerCase():line[i];
								
								s = storage.nameWithPrefixUUID(MySettings.TCGA_PRE,uuid);
								p = storage.nameWithPrefixPorG(MySettings.TCGA_PRE,header[i]);
								o = storage.literal(line[i]);
								g = storage.nameWithPrefixPorG(MySettings.TCGA_PRE,graphName);
								try {
									storage.insert(s, p, o, g);
								} catch (QueryException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
							qStr = virtStorage.nameWithPrefixUUID(MySettings.TCGA_PRE,uuid)+" "+
							virtStorage.nameWithPrefixPorG(MySettings.TCGA_PRE,header[i])+" "+
							virtStorage.literal(line[i])+" "+virtStorage.nameWithPrefixPorG(MySettings.TCGA_PRE,graphName)+" .";
							out.println(qStr);
						}*/
						cd.add(line);
					}
				}
			}
			codes.put(saveTo, cd);
			
			/*if(out !=null)
				out.close();*/
			inStoreCodes.clear();
			inStoreCodes = null;
			tcgaCodes.clear();
			tcgaCodes = null;
			
		/*} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}*/
		return found;
		
	}
	
	public static void addDataTypeLink(String dataType){
		Storage storage = StorageFactory.getStorage();
		Storage virtStorage = VirtuosoStorage.getInstace();
		
		String pgrrUUID = storage.nameWithPrefixUUID(MySettings.PGRR_PREFIX_URI,UUID.randomUUID().toString());
		String pDT = storage.getRdfsPredicate();
		String oDT = storage.literal(dataType);
		
		String pTS = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"dateCreated");
		String oTS = storage.literal(storage.formatTimeInStorage(storage.formatNowMetaFile()));
		 
		String g = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"dataType-links");
	
		try {
			storage.insert(pgrrUUID, pDT, oDT, g);
			storage.insert(pgrrUUID, pTS, oTS, g);
		} catch (QueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(!Storage.UPDATE_IN_REAL_TIME){
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(MySettings.NQ_UPLOAD_DIR+"dataTypes.nq"));
			out.println(virtStorage.nameWithPrefixUUID(MySettings.TCGA_PRE,pgrrUUID)+" "+
					virtStorage.getRdfsPredicate()+" "+
					virtStorage.literal(dataType)+" "+
					virtStorage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"dataType-links")+" .");
			
			out.println(virtStorage.nameWithPrefixUUID(MySettings.TCGA_PRE,pgrrUUID)+" "+
					virtStorage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"dateCreated")+" "+
					virtStorage.literal(virtStorage.formatTimeInStorage(storage.formatNowMetaFile()))+" "+
					virtStorage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"dataType-links")+" .");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 if(out != null)
			 out.close();
		}
	}
	
	
	/**
	 * Creates List<String[]> studyAbbr, fullStudyName siteCode, siteName
	 */
	public static List<String[]> createTissueSourceSite(){
		if (tissueSourceSite != null) return tissueSourceSite;
		List<String[]> diseaseStudy = getReport("Disease Study"); // studyAbbr (is UPPERCASE), fullStudyName
		List<String[]> tiS = getReport("Tissue Source Site"); // siteCode, siteName, fullStudyName
		
		tissueSourceSite = new ArrayList<String[]>();
		for(String[] tiStr:tiS){
			if(tiStr[2].equals("Cell Line Control"))
				tiStr[2] = "Controls";
			
	
			String[] abbr = lookUp(diseaseStudy, new int[]{1}, new String[]{tiStr[2]}, new int[]{0});
			if(abbr.length > 0){
				String [] line = new String[4];
				line[0] = abbr[0].toLowerCase();
				line[1] = tiStr[2];
				line[2] = tiStr[0];
				line[3] = tiStr[1];
				tissueSourceSite.add(line);
			}
			else 
				System.out.println("TCGAHelper:createTissueSourceSite can't find in DiseaseStudy: "+tiStr[2]);
		}
		
		diseaseStudy = null;
		tiS = null;
		return tissueSourceSite;		
	}
	
	/**
	 * 
	 * @param src - list of String[]
	 * @param lookUpCols - column numbers for lookUp
	 * @param lookUpVals - corresponding values for lookUp
	 * @param returnCols - column numbers to return
	 * @return
	 */
	public static String[] lookUp(List<String[]> src, int[] lookUpCols, 
			String[] lookUpVals, int[] returnCols){
		String[] toret = new String[returnCols.length];
		boolean prevFound = false;
		for(String[] s:src){
			prevFound = s[lookUpCols[0]].equals(lookUpVals[0])?true:false;
			if(lookUpCols.length >1){
				for(int i = 1; i < lookUpCols.length; i++){
					if(prevFound)
						prevFound = s[lookUpCols[i]].equals(lookUpVals[i])?true:false;
				}
			}
			if(prevFound){// all lookUp columns match
				for(int j=0; j<returnCols.length; j++)
					toret[j] = s[returnCols[j]];
				return toret;
			}
		}
		return toret;
	}
	
	/**
	 * 
	 * @param rep report type like "Tissue Source Site"
	 */
	public static List<String[]> getReport(String rep){
		// ATTENTION: if you're getting javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed:
		// install certificate  (see http://www.mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable-to-find-valid-certification-path-to-requested-target/)
		List<String[]>toret = new ArrayList<String[]>();
		
		String urlParameters = "exportType=tab&codeTablesReport="+Introspector.decapitalize(rep.replaceAll(" ", ""));
		String request = "https://tcga-data.nci.nih.gov/datareports/codeTablesExport.htm";
		
		try {
			URL siteUrl = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) siteUrl.openConnection();
			
	//------    
		    /*TrustManager[] trustAllCerts = new TrustManager[] {
		       new X509TrustManager() {
		          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		            return null;
		          }

		          public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

		          public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

		       }
		    };

		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, trustAllCerts,null);
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		    // Create all-trusting host name verifier
		    HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					// TODO Auto-generated method stub
					return true;
				}
		    };
		    // Install the all-trusting host verifier
		    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);*/
	//----------------		
			
			
			
			
			conn.setRequestProperty("referer", "https://tcga-data.nci.nih.gov/datareports/codeTablesReport.htm?codeTable="+rep.replaceAll(" ", "%20"));
			conn.setRequestProperty("content-type","application/x-www-form-urlencoded");
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			
			out.writeBytes(urlParameters);
			out.flush();
			out.close();
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = "";
			boolean isHeader = true;
			
			while((line=in.readLine())!=null) {
				if(!isHeader){
					toret.add(line.split("\t"));
					
				}else  // skip header
					isHeader = false;
			}
			in.close();
			
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
		
		if (toret.size() != 0)
			return toret;
		else{
			System.out.println("TCGAHelper getReport can't access https://tcga-data.nci.nih.gov/datareports/codeTablesReport.htm+"+rep);
			ErrorLog.logFatal("TCGAHelper getReport can't access https://tcga-data.nci.nih.gov/datareports/codeTablesReport.htm+"+rep);
			return null;
		}
		
	}
	

	public static InputStream getGetResponseInputStream(HttpClient httpclientOrig, String url){
		HttpClient httpclient = httpclientOrig;
		if(url.endsWith("gz"))
			httpclient = new DecompressingHttpClient(new DefaultHttpClient());
	  
		try {
			HttpGet request = new HttpGet(url);
			request.setHeader(HTTP.CONN_DIRECTIVE,HTTP.CONN_KEEP_ALIVE);	
			HttpResponse response = httpclient.execute(request);
			HttpEntity entity = response.getEntity();
			InputStream instream = entity.getContent();
			if (entity != null) {
				Header contentType = response.getFirstHeader("Content-Type");
				if (contentType != null
						&& contentType.getValue().indexOf("gzip") != -1) {
					instream = new GZIPInputStream(instream);
					//instream = ungzipInputStream(new GZIPInputStream(instream));
					//InputStream ungzStream = ungzipInputStream2(response.);
					
				}
			}
			
			//response.getEntity().getContent();
			ATTEMPT_COUNT = 0;
			return instream;
		} catch (HttpHostConnectException e){
			if(ATTEMPT_COUNT < MAX_ATTEMPTS){
				// sleep for 3 sec
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				ATTEMPT_COUNT++;
				ErrorLog.log("TCGA site is down? Attempt "+ATTEMPT_COUNT+" to reconnect. "+e.toString());
				getGetResponseInputStream(httpclient, url);
			}
			else {
				//System.out.println("getGetResponseInputStream url: "+url);
				e.printStackTrace();
				ErrorLog.logFatal("TCGA site is down! "+e.toString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e){
			//System.out.println("getGetResponseInputStream url: "+url);
		} 
		
		return null;
		
	}
	
	
	public static InputStream ungzipInputStream(GZIPInputStream instream){
		
		
		PipedInputStream inCopy = null;
	    TeeInputStream inWrapper = null;
	    StringWriter sw = null;
	    ByteArrayOutputStream baot = null;
	    


	            try {
	            	inCopy = new PipedInputStream();
	            	inWrapper = new TeeInputStream(instream, new PipedOutputStream(inCopy));
					baot = new ByteArrayOutputStream();
	            	sw = new StringWriter();
	            	
	            	byte[] buffer = new byte[131072];
	            	int len;
	                while ((len = instream.read(buffer)) > 0) {
	                	baot.write(buffer, 0, len);
	                }
	            	
	                IOUtils.copy(new GZIPInputStream(inCopy),baot);          
	            } catch (IOException e) {
	            	e.printStackTrace();
	            }

	            closeSafely(inCopy);
	            closeSafely(sw);
	            closeSafely(baot);
	    return inWrapper;
	    
	}
	
	private static void closeSafely(Closeable closeable) {
	    if (closeable != null) {
	      try {
	        closeable.close();
	      } catch (IOException e) {
	    	  e.printStackTrace();
	      }
	    }
	  }
	
	public static File urlToFile(String fileURLStr, String saveToDir, boolean hasAuthentication){
		String authStr = " ";
		String saveAsFileName = fileURLStr.substring(fileURLStr.lastIndexOf("/")+1);
		if(!saveToDir.endsWith("/"))
			saveToDir = saveToDir+"/";
		String fileWithPath = saveToDir+saveAsFileName;
		if(hasAuthentication)
			authStr = " --user="+MySettings.getStrProperty("tcga.user")+" --password='"+MySettings.getStrProperty("tcga.pwd")+"' ";
		String comm = "wget -O "+fileWithPath+authStr+fileURLStr;
		Transfer.execBash(comm, false);

		File currFile = new File(fileWithPath);
		// determine if the file is zipped
		if(isGzipped(currFile)){
			InputStream is;
			try {
				is = new GZIPInputStream(new FileInputStream(currFile));
				//TODO make it general
				// considering original file name ends with ".gz"
				File ungzFile = new File(saveToDir+saveAsFileName.replace(".gz", ""));
				FileUtils.copyInputStreamToFile(is, ungzFile);
				closeSafely(is);
				currFile = ungzFile;
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}             
		}		
		return currFile;
	}
	
	public static boolean isGzipped(File f) {
	    InputStream is = null;
	    try {
	        is = new FileInputStream(f);
	        byte [] signature = new byte[2];
	        int nread = is.read( signature ); //read the gzip signature
	        return nread == 2 && signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b;
	    } catch (IOException e) {
	        return false;
	    } finally {
	        closeSafely(is);
	    }
	}
	

	
	/*public static InputStream getGetGZIPResponseInputStream(HttpClient httpclient, String url){/	
		try {
			HttpGet request = new HttpGet(url);
			//request.setHeader(HTTP.CONN_DIRECTIVE,HTTP.CONN_KEEP_ALIVE);	
			request.setHeader("Accept-Encoding", "gzip");
			
			HttpResponse response = httpclient.execute(request);
			
			Header[] headers = response.getAllHeaders();
			for (Header header : headers) {
				System.out.println("Key : " + header.getName() 
				      + " ,Value : " + header.getValue());
			}
			
			
			HttpEntity entity = response.getEntity();
			InputStream instream = null;
			if (entity != null) {
				
//System.out.println("Header: "+entity.getContentType());



				instream = entity.getContent();
				Header contentType = response
						.getFirstHeader("Content-Type");
		//System.out.println("Header: "+contentType);
				if (contentType != null
						&& contentType.getValue().indexOf("gzip") != -1) {
					//instream = new GZIPInputStream(instream);
					instream = ungzipInputStream(new GZIPInputStream(instream));
				}
				
				
				Header contentEncoding = response
						.getFirstHeader("Content-Encoding");
		System.out.println("Header: "+contentEncoding);
				if (contentEncoding != null
						&& contentEncoding.getValue().equalsIgnoreCase("gzip")) {
					instream = new GZIPInputStream(instream);
				}
			}
			ATTEMPT_COUNT = 0;
			return instream;
		} catch (HttpHostConnectException e){
			if(ATTEMPT_COUNT < MAX_ATTEMPTS){
				// sleep for 3 sec
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				ATTEMPT_COUNT++;
				ErrorLog.log("TCGA site is down? Attempt "+ATTEMPT_COUNT+" to reconnect. "+e.toString());
				getGetResponseInputStream(httpclient, url);
			}
			else {
				//System.out.println("getGetResponseInputStream url: "+url);
				e.printStackTrace();
				ErrorLog.logFatal("TCGA site is down! "+e.toString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e){
			//System.out.println("getGetResponseInputStream url: "+url);
		} 
		
		return null;
		
	} */
	
	
	public static boolean isFileLevel(List<LineBean> levelBeans){
		for(LineBean lb:levelBeans)
			if(lb.getUrl().endsWith("/")) return false;
		return true;
	}
	
	public static boolean isArchiveLevel(List<LineBean> levelBeans){
		for(LineBean lb:levelBeans)
			if(lb.isArchiveLevel()) return true;
		return false;
	}
	
	public static HttpResponse getPostResponse(String url, String[] hTypes, 
			String[] hValues, String query){
	
		//HttpClient httpclient = new DefaultHttpClient();
		
		HttpClient httpclient = getHttpClient();
		
		HttpPost httppost = new HttpPost(url);
		for(int i=0; i<hTypes.length; i++){
			httppost.setHeader(hTypes[i], hValues[i]);
		}
		try {
	//System.out.println("httpclient: "+httpclient.toString());
	//System.out.println("httppost: "+httppost.toString());
			httppost.setEntity(new StringEntity(query));
	//System.out.println("ENTITY: "+httppost.getEntity().toString());
			HttpResponse response = httpclient.execute(httppost);
			return response;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		return null;
	}
	
	public static List<String[]>changeDCC(String url){
//System.out.println("changeDCC URL: "+url);
		HttpClient httpclient = getHttpClient();
		InputStream is = getGetResponseInputStream(httpclient, url);
		CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(is)), '\t');
		List<String[]>toret = new ArrayList<String[]>();
		boolean foundRevised = false;
		String match = "files revised";
		String[] readLine = null;
		try {
			while ((readLine = reader.readNext()) != null) {
				if(foundRevised)
					toret.add(readLine);
				if(readLine[0].equalsIgnoreCase(match))
					foundRevised = true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
	         try {
	             is.close();
	             reader.close();
	         } catch (IOException e) {
	             e.printStackTrace();
	         }
	 			httpclient.getConnectionManager().shutdown();
	     }
		return toret;
		
	}
	
	/**
	 * for existing list of changes (CHANGES_DCC.txt) returns (I)nitial insert, (D)eleted, (M)odif, (U)nmodif
	 * @param fname
	 * @param statuses
	 * @return
	 */
	public static String getFileStatus(String fname, List<String[]>statuses){
		if(statuses.size()==0)
			return MySettings.INITIAL;
		for(String[] sArr:statuses){
			if(sArr[0].contains(fname))
				return (sArr[1].equals("+"))?MySettings.INITIAL:(sArr[1].equals("-")?MySettings.DELETED:(sArr[1].equals("R")?MySettings.REVISED:MySettings.UNCHANGED));			
		}
		return "U";
		
	}
	
	/**
	 * TCGA-A2-A1FV-01A-01-TSA.svs - tissue image
	 * TCGA-BH-A1EO-01Z-00-DX1.0E624888-D7E4-48DF-B51A-9AD8F75A66B7.svs - slide image
	 * TCGA-AR-A1AR.747FB91B-F523-4FA0-91DD-6014EF55643D.pdf - path report
	 * nationwidechildrens.org_biospecimen.TCGA-AR-A1AR.xml - bio (format: http://tcga-data.nci.nih.gov/docs/xsd/BCR/tcga.nci/bcr/xml/biospecimen/2.5/TCGA_BCR.Biospecimen.xsd)
	 * 
	 * TCGA-A1-A0SE-01A-11R-A085-13.isoform.quantification.txt - cgcc/bcgsc.ca/illuminaga[hi-seq]_mirnaseq/mirnaseq/
	 * 
	 * snp:
	 * cgcc/broad.mit.edu/genome_wide_snp_6/snp/broad.mit.edu_BRCA.Genome_Wide_SNP_6.Level_3.103.2002.0:
	 * GAMMA_p_TCGA_b103_104_SNP_N_GenomeWideSNP_6_B03_755950.hg18.seg.txt 
	 * LOOK into all broad.mit.edu_BRCA.Genome_Wide_SNP_6.mage-tab.***\/   for matching data
	 * 
	 * Methylation:
	 * LOOK for jhu-usc.edu_BRCA.HumanMethylation27.mage-tab.*.*.*\/  
	 * 
	 * mda_rppa_core, unc.edu:
	 * look for "mage-tab" too
	 * 
	 * gsc/genome.wustl.edu/illuminaga_dnaseq/mutations/
	 * parse *.maf
	 * 
	 * SEE:
	 * https://tcga-data.nci.nih.gov/uuid/
	 * web service:
	 * https://wiki.nci.nih.gov/display/TCGA/TCGA+Biospecimen+Metadata+Web+Service+User%27s+Guide
	 * 
	 * tcga toolkit is using:
	 *  TCGA.get("https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/gbm/bcr/biotab/clin/clinical_patient_gbm.txt", function (err, res) {
     *   
     *                SEE:
     *                https://wiki.nci.nih.gov/display/TCGA/TCGA+barcode for ALL codes
     *   CVS to RDF:
     *   http://www.w3.org/wiki/ConverterToRdf - list
     *   http://rdf123.umbc.edu/ - java app
     *   
     *   TCGA data matrix web service
     *   https://wiki.nci.nih.gov/display/TCGA/TCGA+Data+Matrix+Web+Service+User%27s+Guide
	 * 
	 * @param fName
	 */
	public static void parseFileName(String fName){
		
	}
	
	public static String isBinary(String fName){
		String ext = fName.substring(fName.lastIndexOf(".")+1).toLowerCase();
		return (MySettings.binaryList.contains(ext))?"true":"false";
	}
	
	public static List<LineBean> getPageBeans(String url){
		HttpClient httpclient = getHttpClient();
		try{
			InputStream in = getGetResponseInputStream(httpclient, url);
			if(in == null) return null;
			
			List<LineBean> list =  convertStreamToLineBean(in, url);
			Collections.sort(list);
			return list;
		}finally {
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static List<LineBean> getAllPageBeans(String url){
		HttpClient httpclient = getHttpClient();
		try{
			InputStream in = getGetResponseInputStream(httpclient, url);
			if(in == null) return null;
			
			List<LineBean> list =  convertStreamToALLLineBean(in, url);
			Collections.sort(list);
			return list;
		}finally {
			httpclient.getConnectionManager().shutdown();
		}
	}
	
	public static int countDataFiles(String url){
		HttpClient httpclient = getHttpClient();
		 String line = null;
		 InputStream in = null;
		 int toret = 0;
	        try {
	        	in = getGetResponseInputStream(httpclient, url);
	        	BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				if(in == null) return 0;
	            while ((line = reader.readLine()) != null) {
	            	if(line.contains("a href") && !stringContains(line, MySettings.excludedHREFList) &&
	            			!stringContains(line, MySettings.infoFilesList))
	            		toret++;  		
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        } catch (NumberFormatException e){
	        	System.out.println("NFE countDataFiles: "+url);
	        } finally {
	            try {
	                in.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	            httpclient.getConnectionManager().shutdown();
	        }
	        return toret;
	}
		
	
	public static String getSizeInKB(String size) throws NumberFormatException{
		if(size.indexOf("-") != -1 || size.equals("0"))
			return "0";
		String s = size.substring(0,size.length()-1);
		double ds = 0;
		try{
			ds = Double.valueOf(s);
		} catch (NumberFormatException e){ 
			System.out.println("NFE getSizeInKB str: "+size);
			throw e;
		}

		String u = size.substring(size.length()-1);
		int sw = -1;
		if(u.equalsIgnoreCase("B"))
			sw = 0;
		if(u.equalsIgnoreCase("M"))
			sw = 1;
		else if (u.equalsIgnoreCase("G"))
			sw = 2;
		
		switch(sw){
		case 0:
			ds = ds / 1024;
			break;
		case 1:
			ds = ds * 1024;
			break;
		case 2:
			ds = ds * 1024 * 1024;
			break;
		default:
            break;
		}
		return fFileSize.format(ds);
			
	}
	
	public static String getMatch(String line, Pattern p){
		Matcher hrefM = p.matcher(line);
	    if (hrefM.find()) 
	    	return hrefM.group(1);
	    else
	    	return "";
	    
	}
	
	public static String getMatch(String line, Pattern p, int groupNum){
		Matcher hrefM = p.matcher(line);
	    if (hrefM.find()) {
	    	try{
	    		return hrefM.group(groupNum);
	    	}catch (IndexOutOfBoundsException e){
	    		return "";
	    	}
	    }else
	    	return "";
	    
	}
	
	
	public static String extractSize(String ss) throws NumberFormatException{
		/*String s = ss;
		s = s.trim();
//System.out.println("STR:*"+s+"*");
		s = s.substring(s.lastIndexOf(' ')+1);
//System.out.println("STR2:*"+s+"*");
		try{
			s = getSizeInKB(s);
			return s;
		} catch (NumberFormatException e){
			System.out.println("NumberFormatException extractSize str: "+ss);
			throw e;
		}*/
		return "0";
		
		
	}
	
	public static LineBean lineBeansGetChanges(List<LineBean>beans){
		for(LineBean lb:beans)
			if(lb.getName().contains(MySettings.CHANGES))
				return lb;
		return  null;
	}
	
	public static List<LineBean> recentOnly(List<LineBean> beans){
		Map<String, List<LineBean>> sameArchiveNames = new HashMap<String, List<LineBean>>();
		//combine by the names first
		String archName = null;
		String lbName = null;
		List<LineBean> currSet = null;

		for(LineBean lb:beans){
			lbName = lb.getName();
			if(!lbName.endsWith(".tar") && 
					!lbName.endsWith(".tar.md5") &&
					!lbName.endsWith(".tar.gz")){
				archName = lb.archiveNameUpToRevision();
				
				currSet = sameArchiveNames.get(archName);
				if(currSet == null){
	//System.out.println("   NEW set: "+archName);
					currSet = new LinkedList<LineBean>();
					sameArchiveNames.put(archName, currSet);
				}
	//System.out.println("   ADDED TO set: "+archName+"   n: "+lb.getName());			
				currSet.add(lb);
			}
		}

		// now look for the recent ones
		List<LineBean> toadd = null;
		List<LineBean> toret = new LinkedList<LineBean>();
		for (Map.Entry<String, List<LineBean>> entry : sameArchiveNames.entrySet()) {
			toadd = recentOnlyExperiment(entry.getValue());
			
			if(entry.getKey().indexOf(".mage-tab") != -1)
				toret.addAll(0, toadd); // set .mage-tab. to be the first in the list
			else
				toret.addAll(toadd); // else concatenate to the list
		}
		if(currSet != null){
			currSet.clear();
			currSet = null;
		}
		sameArchiveNames.clear();
		sameArchiveNames = null;

		return toret;
	}
	
	/**
	 * Some archives contain a LOT of the same into in different experiments
	 * example: https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/blca/bcr/nationwidechildrens.org/bio/clin/
	 * 
	 *
	 * @param beans
	 * @return
	 */
	public static List<LineBean> recentOnlyExperiment(List<LineBean> beans){
		//check if names end with num1.num2.0 => this is an archive and we need to filter old entries
//System.out.println("===========");	
//System.out.println("===========");
		List<LineBean> toret = new ArrayList<LineBean>();
		LineBean currBean = null;
		int[] curNums = null;
		for(LineBean lb:beans){

			int[] nums = lb.getCenterExperiment();
			if (nums == null  && currBean != null){
//System.out.println("NUMZ+NULL for "+lb.getName());
				toret.add(currBean);
				currBean = null;
				curNums = null;
			}

			if(nums != null){
//System.out.println(" LBN: "+lb.getName()+"  CENTR: "+nums[0]+"   EXXP = "+nums[1]);
				if(currBean == null){
					currBean = lb;
					curNums = nums;
				}
				else {
					//just another experiment
					if(curNums[0] == nums[0] && curNums[1] < nums[1]){
						currBean = lb;
						curNums = nums;
					}
					else if (curNums[0] != nums[0]){
						if(currBean != null)
							toret.add(currBean);
						currBean = lb;
						curNums = nums;
					}
				}
			}
		}
		if(currBean != null) {
//System.out.println("************ SELECTED: "+currBean.getName());
			toret.add(currBean);
		}
		return (toret.size()==0)?beans:toret;
	}
	
	/**
	 * 
	 * @return [centerID, experimentNum]
	 */
	public static int[] getCenterExperimentFromArchive(String tcgaArchivePath){
		String resStr = tcgaArchiveUpToCenter(tcgaArchivePath);
		if(resStr.equals(tcgaArchivePath))
			return null;
		resStr = tcgaArchivePath.substring(resStr.length()+1, tcgaArchivePath.length());
		
		int centEndInd = resStr.indexOf(".");
		int expEndInd = resStr.indexOf(".", centEndInd+1);
		try{
			int center = Integer.valueOf(resStr.substring(0, centEndInd));
			int  exp = Integer.valueOf(resStr.substring(centEndInd+1,expEndInd)); 
			int[] res = new int[2];
			res[0] = center;
			res[1] = exp;
			return res;
		} catch (NumberFormatException e) {return null;}	
	}
	
	public static int getExperimentNumFromArchive(String tcgaArchivePath){
		int[] arr = getCenterExperimentFromArchive(tcgaArchivePath);
		return (arr == null)?-1:arr[1];
	}
	
	/**
	 * returns <..>/hgsc.bcm.edu_ACC.Mixed_DNASeq_curated.Level_2
	 * out of <..>/hgsc.bcm.edu_ACC.Mixed_DNASeq_curated.Level_2.1.1.0/
	 * OR 
	 * just the original path if came from other places like cgHub
	 * @param tcgaArchivePath
	 * @return
	 */
	public static String tcgaArchiveUpToCenter(String tcgaArchivePath){
		String toret = tcgaArchivePath;
		String patt = ".LEVEL_";
		int levIndSt = tcgaArchivePath.toUpperCase().indexOf(patt);
		if(levIndSt == -1)
			return toret;
		int levIndEnd = tcgaArchivePath.indexOf(".", levIndSt+1);
		return tcgaArchivePath.substring(0,levIndEnd);
	}
	
	
	/**
	 * returns <..>/hgsc.bcm.edu_ACC.Mixed_DNASeq_curated.Level_2.1
	 * out of <..>/hgsc.bcm.edu_ACC.Mixed_DNASeq_curated.Level_2.1.1.0/
	 * OR 
	 * just the original path if came from other places like cgHub
	 * @param tcgaArchivePath
	 * @return
	 */
	public static String tcgaArchiveWithCenterID(String tcgaArchivePath){
		String toret = tcgaArchivePath;
		String patt = ".LEVEL_";
		int levIndSt = tcgaArchivePath.toUpperCase().indexOf(patt);
		if(levIndSt == -1)
			return toret;
		int levIndEnd = tcgaArchivePath.indexOf(".", levIndSt+1);
		return tcgaArchivePath.substring(0,tcgaArchivePath.indexOf(".", levIndEnd+1));
	}

	
	public static boolean stringContains(String line, List<String> list){
		for(String s:list)
			if(line.contains(s))
				return true;
		return false;
	}
	
	
	
	public static List<LineBean> convertStreamToLineBean(InputStream is, String parentDir) {
		List<LineBean>list = new LinkedList<LineBean>();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
    
            	if(line.contains("a href") && !stringContains(line, MySettings.excludedHREFList))
            		list.add(new LineBean(parentDir+getMatch(line,hrefP), getMatch(line, dateP),
        					extractSize(line)));
      		
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e){
        	System.out.println("NFE convertStreamToLineBean parentDir: "+parentDir);
        } finally {
            try {
                is.close();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
	
	public static List<LineBean> convertStreamToALLLineBean(InputStream is, String parentDir) {
		List<LineBean>list = new LinkedList<LineBean>();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
    
            	if(line.contains("a href") && !stringContains(line, MySettings.excludedHREFListNoTAR))
            		list.add(new LineBean(parentDir+getMatch(line,hrefP), getMatch(line, dateP),
        					extractSize(line)));
      		
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e){
        	System.out.println("NFE convertStreamToLineBean parentDir: "+parentDir);
        } finally {
            try {
                is.close();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
	
	
	public static List<String[]> getCodeTable(String tableName){
		if(codes.size() == 0)
			updateCodeLinksNoRDF(); 
		return codes.get(tableName);
	}
	
	public static void clearCodes(){
		codes.clear();
	}
	
	
	/**
	 * without RDF store.
	 */
	public static void updateCodeLinksNoRDF(){
		if (codes.size() !=0) return;
		
		Storage storage = StorageFactory.getStorage();
		
		//diseaseStudy
		storeCodes("diseaseStudyCodes", "diseaseStudy-links", new ArrayList<String>(), 
				getReport("Disease Study"), MySettings.diseaseStudyHeader, true,0, false);
		updateDiseaseStudyLinks();

		//tss
		storeCodes("tssCodes", "tss-links",storage.resultAsStrList(storage.getStrProperty("TSS_LINKS"),"value"), 
				getReport("Tissue Source Site"), MySettings.tssHeader, false, 0, true);
		
		updateTSSPrimaryNames();
		
//System.out.println("updateCodeLinksNoRDF 3");
		// center
		storeCodes("centerCodes", "center-links", storage.resultAsStrList(storage.getStrProperty("CENTER_LINKS"),"value"), 
				getReport("centerCode"), MySettings.centerCodesHeader, false,0, true);
//System.out.println("updateCodeLinksNoRDF 4");
		//sample type
		storeCodes("sampleTypeCodes", "sampleType-links", storage.resultAsStrList(storage.getStrProperty("SAMPLETYPE_LINKS"),"value"), 
				getReport("sampleType"), MySettings.sampleTypeHeader, false, 0, true);
//System.out.println("updateCodeLinksNoRDF 5");
		
		// portion analyte
		storeCodes("portionAnalyte", "analyteType-links", new ArrayList<String>(), 
				getReport("portionAnalyte"), MySettings.portionHeader, false, 0, false);
//System.out.println("updateCodeLinksNoRDF 6");
		
		// dataType
		storeDateTypeCodes("dataType", "dataType-links", new ArrayList<String>(), 
				getReport("dataType"), MySettings.dataTypeHeader, false);
		
	
	System.out.println("done updateCodeLinks");
	
}
	
	private static void updateDiseaseStudyLinks(){

		Storage storage = StorageFactory.getStorage();
		String g = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"diseaseStudy-links");
		
		List<String> diseasesPGRR = storage.resultAsStrList(storage.getStrProperty("ALL_DISEASES_ABBR_Q"),"studyabbreviation");
		List<String> skipDisList = Arrays.asList("cntl", "misc");
		//get list from tcga
		List<String[]> diseasesTCGA = getCodeTable("diseaseStudyCodes");
		
		for(String[] ss:diseasesTCGA){
			if(!skipDisList.contains(ss[0])) {
				if(!diseasesPGRR.contains(ss[0])) {
					String pgrrUUID = storage.nameWithPrefixUUID(MySettings.PGRR_PREFIX_URI,UUID.randomUUID().toString());
					String dateCreated = storage.literal(storage.formatTimeInStorage(storage.formatNowMetaFile()));

					try {
						storage.insert(pgrrUUID, storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"studyAbbreviation"), storage.literal(ss[0]),g);
						storage.insert(pgrrUUID, storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"studyName"), storage.literal(ss[1]),g);
						storage.insert(pgrrUUID, storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"dateCreated"), dateCreated,g);
					} catch (QueryException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	

	
	/** 
	 * if there are new tssNames or tssAbbr in TCGA, add to map(s) and save to nq
	 */
	public static void updateTSSPrimaryNames(){
		List<String[]> currTable = TCGAHelper.getCodeTable("tssCodes");
		List<String> temoNewAbbr = new ArrayList<String>();
		List<String> temoNewAll = new ArrayList<String>();
		List<String> newTssNames= new ArrayList<String>();
		String tssName = null, tssNamePrimary = null;
		File newTss = new File(MySettings.NEW_TSS_DIR+MySettings.getDayFormat()+"_newTss.nq");
		Storage storage = StorageFactory.getStorage();
		String g = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"tss-synonyms");
		String p_tssAbbr = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"tssAbbr");
		String p_tssName = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"tssName");
		String p_tssNamePrimary = storage.nameWithPrefixPorG(MySettings.PGRR_PREFIX_URI,"tssNamePrimary");
		String s = null;
		Set tssNamePrimarySet = new HashSet(ModuleUtil.tssNamePrimary.values());
		for(String[] sArr:currTable){
			tssName = sArr[1].trim();
			tssName = tssName.replaceAll("  ", "");
			if(!ModuleUtil.tssAbbrPrimary.containsKey(sArr[0])){
				s = storage.nameWithPrefixUUID(MySettings.PGRR_PREFIX_URI, UUID.randomUUID().toString());
				
				//look for existing primary names
				if(!ModuleUtil.tssNamePrimary.containsKey(tssName)) {
					temoNewAll.add("found new code: "+sArr[0]+" NEW tsSite: "+tssName);
					tssNamePrimary = tssName;
					
					if(!newTssNames.contains(tssName) && !tssNamePrimarySet.contains(tssName)){
						newTssNames.add(tssName);
					}
				}
				else {
					tssNamePrimary = ModuleUtil.tssNamePrimary.get(tssName);
				 temoNewAbbr.add("new code: "+sArr[0]+" EXISTIG tsSite: "+tssName+"  primary: "+tssNamePrimary);
				}
				
				try {
					storage.insert(s, p_tssAbbr, storage.literal(sArr[0]), g);
					storage.insert(s, p_tssName, storage.literal(tssName), g);
					storage.insert(s, p_tssNamePrimary, storage.literal(tssNamePrimary), g);
				} catch (QueryException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				ErrorLog.log(s+" "+p_tssAbbr+" "+storage.literal(sArr[0])+" "+g+" .", newTss);
				ErrorLog.log(s+" "+p_tssName+" "+storage.literal(tssName)+" "+g+" .", newTss);
				ErrorLog.log(s+" "+p_tssNamePrimary+" "+storage.literal(tssNamePrimary)+" "+g+" .", newTss);
			}
		}
	
		for (String ss:temoNewAbbr)
			System.out.println("**** NEW CODES: "+ss);
		
	
		for (String ss:temoNewAll)
			System.out.println("****************** ALL NEW:  ***** "+ss);
		
	
		String[] newTssNamesArr = newTssNames.toArray(new String[newTssNames.size()]);
		Arrays.sort(newTssNamesArr);
		for (String ss:newTssNamesArr)
			System.out.println("*** NAMES NEW:  ***** "+ss);
		
	}
	
	public static void main(String[] args) {
		
		
		/*addDataTypeLink("RNA-Seq_(BAM)");
		addDataTypeLink("miRNA-Seq_(BAM)");
		addDataTypeLink("WGS_(BAM)");
		addDataTypeLink("WXS_(BAM)");*/
		//addDataTypeLink("Bisulfite-Seq_(cgHub)");
		//addDataTypeLink("Mass_Spectrometry");
		addDataTypeLink("CN_Level4");
	System.out.println("Done");	
		
	}

}
