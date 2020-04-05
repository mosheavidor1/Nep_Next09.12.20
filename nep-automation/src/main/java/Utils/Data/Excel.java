package Utils.Data;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import Utils.PropertiesFile.PropertiesFile;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Excel  {

     private static String fileNameAndPath;
     private static XSSFSheet ExcelWSheet;
     private static XSSFWorkbook ExcelWBook;
	public static final String parameterPerEnvironmentIdentifier = "Parameters Per Environment";
	public static final String productionIdentifier = "Production";

 	 
 	 public static boolean setExcelFileAndSheet(String PathAndName,String SheetName) throws Exception  {
   			// Open the Excel file
			FileInputStream ExcelFile = new FileInputStream(PathAndName);
			// Access the required test data sheet
			ExcelWBook = new XSSFWorkbook(ExcelFile);
			if (ExcelWBook != null)
				ExcelWSheet = ExcelWBook.getSheet(SheetName);
			
			if (ExcelWSheet == null || ExcelWBook == null)
				return false;
			return true;

	}
	
	public static void setExcelFileName(String nameAndPath)  {
		fileNameAndPath = nameAndPath;
}

	public static void setExcelSheetName(String sheetName) throws Exception  {
		setExcelFileAndSheet(fileNameAndPath,sheetName);
}

    
    private static int getRowsNumber() {
		return ExcelWSheet.getLastRowNum() +1;
    }
    
	//This method is to read the test data from the Excel cell, in this we are passing parameters as Row num and Col num
    public static String [] getAllCellsOfRow(int RowNum) throws Exception{
	  	
    	int numOfColumn = ExcelWSheet.getRow(RowNum).getLastCellNum();
    	String [] data = new String [numOfColumn];

	  	for(int j=0; j<numOfColumn;j++) {
	  		XSSFCell currentCell = ExcelWSheet.getRow(RowNum).getCell(j);
	  		data[j] =currentCell.getStringCellValue();
	  		}
	  	return data;
    }

    public static Object [] getTestData() throws Exception {
    	int numOfRows = getRowsNumber();
    	if (numOfRows < 2)
    		return null;
    	ArrayList<HashMap<String,String>> testData = new ArrayList<HashMap<String,String>>();
    	HashMap<String,String> keysMap = new HashMap<String,String>();
    	String [] columnsNames= getAllCellsOfRow(0);
    	for (int i=0; i<columnsNames.length; i++) {
    		keysMap.put(columnsNames[i], "" );
    	}

    	boolean isProduction = PropertiesFile.isProduction();
    	boolean isEnvironments = PropertiesFile.isEnvironments();
    	for (int i=1; i<numOfRows; i++) {
    		HashMap<String, String> currentMap = new HashMap<String,String>();
    		currentMap.putAll(keysMap);
    		String [] rowData = getAllCellsOfRow(i);
			if (rowData.length > 0 && columnsNames[0].trim().equalsIgnoreCase(parameterPerEnvironmentIdentifier)) {
				String clusterToTest = PropertiesFile.readProperty("ClusterToTest");
				String currentLineParaSetIdentifier = rowData[0].trim();
				// if it is Environments lines that has "Parameters Per Environment" equals to the environment name will be added
				if( isEnvironments  && ! currentLineParaSetIdentifier.equalsIgnoreCase(clusterToTest.trim())   )
					continue;
				// if it is Production, lines that has "Parameters Per Environment" equals to the production identifier will be added
				if( isProduction  && ! currentLineParaSetIdentifier.equalsIgnoreCase(productionIdentifier)   )
					continue;
				// if it is local cluster, lines that do not contain environments or production identifiers will be added
				if( !(isEnvironments || isProduction ) && ( currentLineParaSetIdentifier.equalsIgnoreCase(productionIdentifier)  || PropertiesFile.isEnvironment(currentLineParaSetIdentifier)  ))
					continue;


			}

    		for (int j=0; j< rowData.length; j++) {
    			currentMap.put(columnsNames[j], rowData[j]);
    		}
    		testData.add(currentMap);
    	}
    	return testData.toArray();
    	
    }


}
