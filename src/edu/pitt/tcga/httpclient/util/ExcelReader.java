package edu.pitt.tcga.httpclient.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelReader {
	
	public static DataMatrix excelSheetToDataMatrix(String filePath, String sheetName, int skipTopRows, boolean hasHeader) {
        List<String[]> data = new LinkedList<String[]>();
        DataMatrix dm = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
 
            // Using XSSF for xlsx format, for xls use HSSF
            Workbook workbook = new XSSFWorkbook(fis);    
            Sheet sheet = workbook.getSheet(sheetName);
            FormulaEvaluator formulaEval = workbook.getCreationHelper().createFormulaEvaluator();
            //int noOfColumns = sheet.getRow(0).getPhysicalNumberOfCells();
            int rownum = 0; 
            String cellValue = "";
            for(Row row: sheet){
            	if(rownum >= skipTopRows){
	            	String[] rowdata = new String[row.getPhysicalNumberOfCells()];
	            	int pos = 0;
	     
	            	for(Cell cell:row){
	            		cellValue = "";
	            		if (cell != null) {
	            	        switch(cell.getCellType()) {
	            	        case Cell.CELL_TYPE_NUMERIC:
	            	        	cellValue = String.valueOf(cell.getNumericCellValue());
	            	            break;
	            	        case Cell.CELL_TYPE_FORMULA:
	            	            CellValue objCellValue = formulaEval.evaluate(cell);
	            	            if (objCellValue.getCellType() == Cell.CELL_TYPE_NUMERIC) {
	            	            	cellValue = String.valueOf(objCellValue.getNumberValue());
	            	            }
	            	            else if (objCellValue.getCellType() == Cell.CELL_TYPE_STRING)
	            	            	cellValue = objCellValue.getStringValue();
	            	            break;
	            	        case Cell.CELL_TYPE_STRING:
	            	        	cellValue = cell.getStringCellValue();
	            	            break;
	            	        }
	            		
	            		}
	            		 rowdata[pos] = cellValue;
		                 pos++;
	            	 }
	            	 data.add(rowdata);  
            	}
            	 rownum++;
            }
            fis.close();
           
            dm = new DataMatrix("DATAM", data, hasHeader);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dm;
    }

}
