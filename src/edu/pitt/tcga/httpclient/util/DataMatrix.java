package edu.pitt.tcga.httpclient.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.module.ModuleUtil;


public class DataMatrix {
	private List<String[]> data;
	private String[] header = null;
	private String name = null;
	
	
	public DataMatrix(String name){
		setName(name);
		data = new LinkedList<String[]>();
	}
	
	public DataMatrix(String name, List<String[]> data){
		this(name);
		setData(data);
	}
	
	public DataMatrix(String name, List<String[]> data, boolean hasHeader){
		this(name);
		if(!hasHeader)
			this.data = data;
		else
			setData(data);
	}
	
	public String[] getHeader(){
		return header;
	}
	
	public void setHeader(String[] header){
		this.header = header;
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	
	public void setData(List<String[]> d){
		clear();
		header = d.get(0);
		d.remove(0);
		data = d; // no header
	}
	
	public void setDataOnly(List<String[]> d){
		clear();
		data = d;
	}
	
	public List<String[]> getData(){
		return data;
	}
	
	public int numRows(){
		return data.size();
	}
	
	public int numColumns(){
		return data.get(0).length;
	}
	
	public void clear(){
		header = null;
		data.clear();
	}
	
	public void destroy(){
		clear();
		data = null;
	}
	
	/**
	 * 
	 * @param rowSt
	 * @param rowEnd - excluded
	 * @param colSt
	 * @param colEnd - excluded
	 * @return
	 */
	public List<String[]> getSubMatrix(int rowSt, int rowEnd, int colSt, int colEnd){
		List<String[]>subM = new LinkedList<String[]>();
		int numCols = colEnd-colSt;
		for(int i = rowSt; i < rowEnd; i++){
			String[] row = new String[numCols];
			System.arraycopy(data.get(i), colSt, row, 0, numCols);
			subM.add(row);
		}
		return subM;
	}
	
	public List<String> getColumnValues(int colNum, boolean uniqueValues){
		List<String> colValues = new LinkedList<String>();
		String val = "";
		for(int i=0; i<numRows(); i++){
			val = data.get(i)[colNum];
			if(!uniqueValues || (uniqueValues && !colValues.contains(val)))
				colValues.add(val);		
		}
		return colValues;
			
	}
	
	/**
	 * 
	 * @param origMatrixData - no empty
	 * @param newHeader
	 * @param dataToAdd - no empty rows, must have the same num of rows
	 * @return
	 */
	public static List<String[]> addColumns(List<String[]>  origMatrixData, String[] newHeader, List<String[]> dataToAdd ){
		int newSz = newHeader.length;
		int origNumCols = origMatrixData.get(0).length;
		int numColsToAdd = newSz - origNumCols;
		List<String[]> newM = new LinkedList<String[]>();
		newM.add(newHeader);
		
		for(int i=0; i<origMatrixData.size(); i++){
			String[] newArr = new String[newSz];
			System.arraycopy(origMatrixData.get(i), 0, newArr, 0, origNumCols);
			System.arraycopy(dataToAdd.get(i), 0, newArr, origNumCols, numColsToAdd);
			newM.add(newArr);
		}
		
		return newM;	
	}
	
	public void reorderColumns(int stRow, int[] columnOrder){
		List<String[]>newData = new ArrayList<String[]>();
		int rowSz = columnOrder.length;
		
		String[] oldArr, newArr;
		int i = 0;
		for (int r = stRow; r < data.size(); r++){
			oldArr = data.get(r);
			newArr = new String[rowSz];
			i = 0;
			for(int c:columnOrder){
				newArr[i] = oldArr[c];
				i++;
			}
			newData.add(newArr);
		}
		data.clear();
		data = newData;
		
	}
	
	public String rowToString(int rowNum, String delim, int[] columnOrder){
		String[] rowData = getRowData(rowNum);
		StringBuilder sb = new StringBuilder();
		if(columnOrder != null && rowData.length == columnOrder.length){
			for(int c:columnOrder)
				sb.append(rowData[c]+delim);
			sb.setLength(sb.length() - delim.length());
		}
		else {
			for(int c=0; c < rowData.length; c++)
				sb.append(rowData[c]+delim);
			sb.setLength(sb.length() - delim.length());
			
		}
		sb.append("\n");
		return sb.toString();
	}
	
	public int getColumnNum(String colName){
		int pos = 0;
		colName = colName.toLowerCase();
		for(String s:header){
			if(s.toLowerCase().equals(colName))
				return pos;
			pos++;
		}
		return -1; // if not found
	}
	
	/**
	 * 
	 * @param colName
	 * @param occurence starts with 1
	 * @return
	 */
	public int getColumnNum(String colName, int occurence){
		int pos = 0;
		int numFound = 0;
		colName = colName.toLowerCase();
		for(String s:header){
			if(s.toLowerCase().equals(colName)){
				numFound++;
				if(numFound == occurence)
					return pos;
			}
			pos++;
		}
		return -1; // if not found
	}
	
	public List<Integer> getStartsWithDataRows(String regex, int col){
		List<Integer> toret = new LinkedList<Integer>();
		int pos = 0;
		for (String[] sArr:data){
			if(sArr.length >= col  && sArr[col].startsWith(regex))
				toret.add(pos);
			pos++;
		}
		return toret;		
	}
	
	public List<Integer> getStartsWithIgnoreCaseDataRows(String regex, int col){
		List<Integer> toret = new LinkedList<Integer>();
		int pos = 0;
		for (String[] sArr:data){
			if(sArr.length >= col && sArr[col].toLowerCase().startsWith(regex.toLowerCase()))
				toret.add(pos);
			pos++;
		}
		return toret;		
	}
	
	public int getRowNumStartsWithData(String regex, int col){
		int pos = 0;
		for (String[] sArr:data){
			if(sArr.length >= col && sArr[col].startsWith(regex))
				return pos;
			pos++;
		}
		return -1;		
	}
	
	public int getRowNumStartsWithIgnoreCaseData(String regex, int col){
		int pos = 0;
		for (String[] sArr:data){
			if(sArr.length >= col && sArr[col].toLowerCase().startsWith(regex.toLowerCase()))
				return pos;
			pos++;
		}
		return -1;		
	}
	
	
	/**
	 * 
	 * @param rows
	 * @param lookupValue
	 * @param col
	 * @return All matched rows
	 */
	public List<Integer> matchRowsFromSubset(List<Integer> rows, String lookupValue, int col){
		List<Integer> toret = new ArrayList<Integer>();
		if(rows.size() == 0 ) return toret;
		for(Integer row:rows){
			if(data.get(row).length >= col && data.get(row)[col].equalsIgnoreCase(lookupValue)){
				toret.add(row);
				
			}
		}
		
		rows.clear();
		rows = null;
		return toret;
	}
	
	
	/**
	 * 
	 * @param rows
	 * @param lookupValue
	 * @param col
	 * @return FIRST matched row
	 */
	public int getRowFromSubset(List<Integer> rows, String lookupValue, int col){
		if(rows.size() == 0 ) return -1;
		for(Integer row:rows){
			if(data.get(row).length >= col && data.get(row)[col].equalsIgnoreCase(lookupValue)){
				rows.clear();
				rows = null;
				return row;
			}
		}
		
		rows.clear();
		rows = null;
		return -1;
	}
	
	public int getDataRow(String lookUpVal, int col){
		int pos = 0;
		try{
			for(String[] sArr:data){
				if (sArr[col].equals(lookUpVal))
					return pos;
				pos++;
			}
			return -1;
		} catch (NullPointerException e) {
			return -1;
		} catch (ArrayIndexOutOfBoundsException e){
			return -1;
		}
	}
	
	public String[] getRowData(int rowNum){
		return data.get(rowNum);
	}
	
	public String getRowColValue(String colName, int rowNum){
		return getRowColValue(getColumnNum(colName), rowNum);
	}
	
	public String getRowColValue(int colNum, int rowNum){
		return data.get(rowNum)[colNum];
	}
	
	public void removeRow(int rowNum){
		data.remove(rowNum);
	}
	
	public void removeLastRows(int numLastRows){
		for (int i = 0; i < numLastRows; i++)
			data.remove(data.size()-1);
	}
	
	public void setValue(int row, int col, String value){
		try{
			data.get(row)[col] = value;
		}catch (IndexOutOfBoundsException e){}
	}
	
	public List<String> getUniqueValuesInCol(int colNum){
		List<String> toret = new LinkedList<String>();
		String toAdd = "";
		for(String[] sArr:data){
			toAdd = sArr[colNum];
			if(toAdd !=null && !toAdd.equals("") && !toret.contains(sArr[colNum])){
				toret.add(sArr[colNum]);
			}
		}
		return toret;
	}
	
	public String getLookUpValue(String lookUpColName, String lookUpValue, 
			String returnColName){
		int cPos = getColumnNum(lookUpColName);
		if (cPos == -1){
			String err = "DataMatrinx.getLookUpValue: can't find lookUpColName = "+lookUpColName;
			System.err.println("err");
			ErrorLog.logFatal(err);
			return null;
		}
		int rPos = getDataRow(lookUpValue, cPos);
		if (rPos == -1){
			String err = "DataMatrinx.getLookUpValue: can't find lookUpValue = "+lookUpValue+" in column "+String.valueOf(cPos);
			System.err.println("err");
			ErrorLog.logFatal(err);
			return null;
		}
		
		int retCPos = getColumnNum(returnColName);
		if (retCPos == -1){
			String err = "DataMatrinx.getLookUpValue: can't find returnColName = "+returnColName;
			System.err.println("err");
			ErrorLog.logFatal(err);
			return null;
		}
		
		return data.get(rPos)[retCPos];
	}

	
	public List<String[]> asList(){
		List<String[]>toret = new LinkedList<String[]>();
		if(header != null)
			toret.add(header);
		toret.addAll(data);
		return toret;
	}
	
	public static DataMatrix getDataMatrix(String file, char delim){
		DataMatrix dm = null;
		try {
			CSVReader reader = new CSVReader(new BufferedReader(
					new FileReader(new File(file))), delim);
			dm = new DataMatrix("Metadata", reader.readAllToList());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return dm;
	}
	
	public void saveAs(String filePath, boolean doAppend){
		try {
			List<String[]> list = new LinkedList<String[]>();
			if(header != null )
				list.add(header);
			list.addAll(data);
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(filePath), doAppend));			
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
	
	public static void saveList(List<String[]> listData, String filePath){
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(filePath), false));			
			output.write(ModuleUtil.listArrayToString(listData, "\t"));
			
			listData.clear();
			listData = null;

			output.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sortByColumn(int colNum){
		Map<String, List<String[]>> map = new TreeMap<String, List<String[]>>();
		
    	for (String[] line: data ) {
    		String key = line[colNum];
    		List<String[]> l = map.get(key);
    		if (l == null) {
    			l = new LinkedList<String[]>();
    			map.put(key, l);
    		}
    		l.add(line);

    	}
    	
    	data.clear();
    	for (List<String[]> list : map.values()) 
    		data.addAll(list);

    	map.clear();
    	map = null;

	}
	
	public static void main(String[] args){
		
		List<String[]>origMatrixData = new LinkedList<String[]>();
		List<String[]>dataToAdd = new LinkedList<String[]>();
		
		String[] newHeader = {"REFERENCE", "", "", "", ""};
		
		for(int j = 0; j < 4; j++){
			String[] origRow = {String.valueOf(j+10), String.valueOf(j+20)};
			origMatrixData.add(origRow);
			
			String[] toAddRow = {String.valueOf(j+100), String.valueOf(j+200), String.valueOf(j+300)};
			dataToAdd.add(toAddRow);
		}
		
		List<String[]> newData = addColumns(origMatrixData, newHeader, dataToAdd );
		for(String[] sArr:newData)
			System.out.println("Row: "+Arrays.asList(sArr));
		
		
		System.out.println("DONE DataMatrix");
	}

}
