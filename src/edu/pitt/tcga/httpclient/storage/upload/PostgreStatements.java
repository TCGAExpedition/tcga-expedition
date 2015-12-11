package edu.pitt.tcga.httpclient.storage.upload;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;


import edu.pitt.tcga.httpclient.db.DBConnection;
import edu.pitt.tcga.httpclient.db.DBStatements;
import edu.pitt.tcga.httpclient.log.ErrorLog;

public class PostgreStatements extends DBStatements{
	public static DBConnection dbConn = new DBConnection();
	private Map<String, List<String>> tables = new HashMap<String,List<String>>();
	private CopyManager copyManager = null;
	
	
	private static final String UPSERT = "UPDATE <graphName> SET <col_name>='<col_val>' WHERE uuid='<uuid_val>'; "+
			"INSERT INTO <graphName> (uuid, <col_name>) "+
			"SELECT '<uuid_val>', '<col_val>' "+
			"WHERE NOT EXISTS (SELECT 1 FROM <graphName> WHERE uuid='<uuid_val>');";
	
	private static final String INSERT = "INSERT INTO <graphName> (uuid, <col_name>) "+
			"VALUES('<uuid_val>', '<col_val>');";
	
	
	public void upsert(String subj, String predicate, String obj, String graphName){

		StringBuilder sb = new StringBuilder();
		List<String> fieldList = tables.get(graphName);
		if(fieldList == null){
			sb.append("CREATE TABLE IF NOT EXISTS "+graphName+" (); ");
			// don't use 'uuid' data type - it trancates dashes and other symbols
			sb.append("SELECT add_column('public','"+graphName+"','uuid','character(36)'); ");
			fieldList = new ArrayList<String>();
			tables.put(graphName, fieldList);
		}
		boolean hasField = fieldList.contains(predicate);
		if(!hasField){
			if(predicate.startsWith("date"))
				sb.append("SELECT add_column('public','"+graphName+"','"+predicate+"','timestamp'); ");
			else
				sb.append("SELECT add_column('public','"+graphName+"','"+predicate+"','text'); ");
			fieldList.add(predicate);
		}
		
		// subsctiption is a special case - multiple values for the same userId
		String ups = (graphName.equalsIgnoreCase("subscription") || graphName.equalsIgnoreCase("protocol"))?INSERT:UPSERT;
		ups = ups.replaceAll("<graphName>", graphName);
		ups = ups.replaceAll("<col_name>", predicate);
		ups = ups.replaceAll("<col_val>", obj);
		ups = ups.replaceAll("<uuid_val>", subj);
		
		sb.append(ups);
		update(sb.toString());
			
		sb.setLength(0);
		sb = null;
			
	}
	
	public synchronized void createNQTable(String tName){
		String st ="CREATE TABLE IF NOT EXISTS "+tName+" (uuid character(36), predicate text, object text, graph text);";
		update(st);
	}
	
	public synchronized void copyIn(File fPath, String tName, String delim){
		FileReader fr = null;
		
		try {
			if(copyManager == null)
			copyManager = new CopyManager((BaseConnection) dbConn.getConnection());
			fr = new FileReader(fPath);
			copyManager.copyIn("COPY "+tName+" FROM STDIN WITH DELIMITER '"+delim+"'", fr);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ErrorLog.log("*** ERROR: PostgreStatement copyIn: "+e.toString());
		}finally {

            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException ex) {
            	ex.printStackTrace();
    			ErrorLog.log("PostgreStatement copyIn: "+ex.toString());
            }
        }
	}
	
	public void dropTable(String tableName){
		update("DROP TABLE "+tableName+";");
	}
	
	public void tempToDestTable(String tmpTableName, String destTableName){
			update("SELECT insert_from_tempo('"+tmpTableName+"', '"+destTableName+"');");
	}
	
	
	private synchronized void update(String statement){
		try {
			Connection conn = dbConn.getConnection();
			PreparedStatement st = conn
					.prepareStatement(statement);
			st.executeUpdate();
			st.close();
		} catch (Exception ex) {
			//ex.printStackTrace();
			ErrorLog.log("PostgreStatement update: "+ex.toString());

		}	
	}
	
	
	public void disconnect(){
		dbConn.disconnect();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PostgreStatements ps = new PostgreStatements();
		//ps.upsert("01234567890123456789012345678912", "myPrediate", "predicateVal", "test_aaa");
		ps.copyIn(new File("C:/DevSrc/cvs/rdf_to_db/tempo/tmp_dataType-links_out001.tsv"), "datatype_links_out001", "\t");
		ps.disconnect();
		
		System.out.println("Done PostgreStatements");

	}


}
