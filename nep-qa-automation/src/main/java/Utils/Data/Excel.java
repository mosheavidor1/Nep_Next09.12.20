package Utils.Data;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import Utils.PropertiesFile.PropertiesFile;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Excel  {

     private String fileNameAndPath;
	 private XSSFSheet ExcelWSheet;
     private XSSFWorkbook ExcelWBook;
	 public static final String parameterPerEnvironmentIdentifier = "Parameters Per Environment";
	 public static final String productionIdentifier = "Production";
	 public static final String defaultIdentifier = "Default";

 	 public boolean setExcelFileAndSheet(String PathAndName,String SheetName) throws Exception  {
   			// Open the Excel file
			FileInputStream ExcelFile = new FileInputStream(PathAndName);
			// Access the required test data sheet
			ExcelWBook = new XSSFWorkbook(ExcelFile);
			if (ExcelWBook != null) {
				ExcelWSheet = ExcelWBook.getSheet(SheetName);
			}
			
			if (ExcelWSheet == null || ExcelWBook == null)
				return false;
			return true;

	}
    
    private int getRowsNumber() {
		return ExcelWSheet.getLastRowNum() +1;
    }
    
	//This method is to read the test data from the Excel cell, in this we are passing parameters as Row num and Col num
    public String [] getAllCellsOfRow(int RowNum) throws Exception{
	  	
    	int numOfColumn = ExcelWSheet.getRow(RowNum).getLastCellNum();
    	String [] data = new String [numOfColumn];

	  	for(int j=0; j<numOfColumn;j++) {
	  		XSSFCell currentCell = ExcelWSheet.getRow(RowNum).getCell(j);
	  		if(currentCell != null)
	  			data[j] =currentCell.getStringCellValue();
	  		else
	  			data[j] = "";
	  		}
	  	return data;
    }

    public Object [] getTestData() throws Exception {
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
		String clusterToTest = PropertiesFile.readProperty("ClusterToTest");

		String [] secondRowData = getAllCellsOfRow(1);
		String [] defaultData = null;
		int startDataRow=1;

		if(secondRowData[0].compareToIgnoreCase(defaultIdentifier) ==0 ) {
			defaultData = secondRowData;
			startDataRow =2;
		}
		boolean parametersArePerEnvironment=false;
		if (columnsNames[0].trim().equalsIgnoreCase(parameterPerEnvironmentIdentifier))
			parametersArePerEnvironment=true;

		for (int i=startDataRow; i<numOfRows; i++) {
    		HashMap<String, String> currentMap = new HashMap<String,String>();
    		currentMap.putAll(keysMap);
    		String [] rowData = getAllCellsOfRow(i);
			if (rowData.length > 0 && parametersArePerEnvironment) {
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
    			String current;
    			if (defaultData != null && defaultData.length-1>=j && rowData[j].trim().isEmpty())
    				current = defaultData[j];
    			else
    				current = rowData[j];

    			currentMap.put(columnsNames[j], current);
    		}

    		if(defaultData != null && defaultData.length > rowData.length){
    			for(int j = rowData.length; j < defaultData.length ; j++){
					currentMap.put(columnsNames[j], defaultData[j]);
				}
			}
    		testData.add(currentMap);
    	}

		if (parametersArePerEnvironment && testData.isEmpty() && defaultData != null) {
			HashMap<String, String> currentMap = new HashMap<String,String>();
			currentMap.putAll(keysMap);
			for (int j=0; j< defaultData.length; j++) {
				currentMap.put(columnsNames[j], defaultData[j]);
			}
			testData.add(currentMap);
		}
    	return testData.toArray();
    	
    }


}
