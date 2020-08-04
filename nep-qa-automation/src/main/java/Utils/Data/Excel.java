package Utils.Data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class Excel  {

	 private HSSFSheet ExcelWSheet;
     private HSSFWorkbook ExcelWBook;
	 public static final String parameterPerEnvironmentIdentifier = "Parameters Per Environment";
	 public static final String productionIdentifier = "Production";
	 public static final String defaultIdentifier = "Default";
	 public static final String populateMethod = "EP_Values_Populate_Method";
	 public static final String rowForEveryEP = "Row_For_Every_EP";
	 public static final String allEPInOneRow = "All_EP_In_One_Row";
	 public static final String keepSheetData = "Keep_Excel_Data";
	 public static final String hostNameKey = "EP_HostName_";
	 public static final String userNameKey = "EP_UserName_";
	 public static final String passwordKey = "EP_Password_";
	 public static final String typeKey = "EP_Type_";
	 public static List<Endpoint> epList;


 	 public boolean setExcelFileAndSheet(String PathAndName,String SheetName)  {
 	 	try {
			// Open the Excel file
			FileInputStream ExcelFile = new FileInputStream(PathAndName);
			// Access the required test data sheet
			ExcelWBook = new HSSFWorkbook(ExcelFile);

			if (ExcelWBook != null) {
				ExcelWSheet = ExcelWBook.getSheet(SheetName);
			}

			if (ExcelWSheet == null || ExcelWBook == null)
				return false;
			return true;
		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not open the following sheet: " + SheetName + "  At file: " + PathAndName + "\n" + e.toString());
			return false;
		}


	 }
    
    private int getRowsNumber() {
 	 	try {
			return ExcelWSheet.getLastRowNum() +1;
		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not get Excel rows number" + "\n" + e.toString());
			return -1;
		}

	}
    
	//This method is to read the test data from the Excel cell, in this we are passing parameters as Row num and Col num
    public String [] getAllCellsOfRow(int RowNum) {
	  	try {
			int numOfColumn = ExcelWSheet.getRow(RowNum).getLastCellNum();
			String[] data = new String[numOfColumn];

			for (int j = 0; j < numOfColumn; j++) {
				HSSFCell currentCell = ExcelWSheet.getRow(RowNum).getCell(j);
				if (currentCell != null)
					data[j] = currentCell.getStringCellValue();
				else
					data[j] = "";
			}
			return data;
		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not get Excel row data" + "\n" + e.toString());
			return null;
		}

	}

    public Object [] getTestData()  {
 	 	try {
			int numOfRows = getRowsNumber();
			if (numOfRows < 2)
				return null;
			ArrayList<HashMap<String, String>> testData = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> keysMap = new HashMap<String, String>();
			String[] columnsNames = getAllCellsOfRow(0);
			for (int i = 0; i < columnsNames.length; i++) {
				keysMap.put(columnsNames[i], "");
			}

			boolean isProduction = PropertiesFile.isProduction();
			boolean isEnvironments = PropertiesFile.isEnvironments();
			String clusterToTest = PropertiesFile.readProperty("ClusterToTest");

			String[] secondRowData = getAllCellsOfRow(1);
			String[] defaultData = null;
			int startDataRow = 1;

			if (secondRowData[0].compareToIgnoreCase(defaultIdentifier) == 0) {
				defaultData = secondRowData;
				startDataRow = 2;
			}
			boolean parametersArePerEnvironment = false;
			if (columnsNames[0].trim().equalsIgnoreCase(parameterPerEnvironmentIdentifier))
				parametersArePerEnvironment = true;

			for (int i = startDataRow; i < numOfRows; i++) {
				HashMap<String, String> currentMap = new HashMap<String, String>();
				currentMap.putAll(keysMap);
				String[] rowData = getAllCellsOfRow(i);
				if (rowData.length > 0 && parametersArePerEnvironment) {
					String currentLineParaSetIdentifier = rowData[0].trim();
					// if it is Environments lines that has "Parameters Per Environment" equals to the environment name will be added
					if (isEnvironments && !currentLineParaSetIdentifier.equalsIgnoreCase(clusterToTest.trim()))
						continue;
					// if it is Production, lines that has "Parameters Per Environment" equals to the production identifier will be added
					if (isProduction && !currentLineParaSetIdentifier.equalsIgnoreCase(productionIdentifier))
						continue;
					// if it is local cluster, lines that do not contain environments or production identifiers will be added
					if (!(isEnvironments || isProduction) && (currentLineParaSetIdentifier.equalsIgnoreCase(productionIdentifier) || PropertiesFile.isEnvironment(currentLineParaSetIdentifier)))
						continue;

				}

				for (int j = 0; j < rowData.length; j++) {
					String current;
					if (defaultData != null && defaultData.length - 1 >= j && rowData[j].trim().isEmpty())
						current = defaultData[j];
					else
						current = rowData[j];

					currentMap.put(columnsNames[j], current);
				}

				if (defaultData != null && defaultData.length > rowData.length) {
					for (int j = rowData.length; j < defaultData.length; j++) {
						currentMap.put(columnsNames[j], defaultData[j]);
					}
				}
				testData.add(currentMap);
			}

			if (parametersArePerEnvironment && testData.isEmpty() && defaultData != null) {
				HashMap<String, String> currentMap = new HashMap<String, String>();
				currentMap.putAll(keysMap);
				for (int j = 0; j < defaultData.length; j++) {
					currentMap.put(columnsNames[j], defaultData[j]);
				}
				testData.add(currentMap);
			}


			PopulateEpData(testData);
			return testData.toArray();
		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not get Excel test data" + "\n" + e.toString());
			return null;
		}

	}

	private void PopulateEpData (ArrayList<HashMap<String, String>> list){
 	 	for(int i = 0; i<list.size();i++) {
			if (! list.get(i).containsKey(populateMethod)) {
				continue;
			}
			String method = list.get(i).get(populateMethod);
			if(method.compareToIgnoreCase(keepSheetData)==0) {
				continue;
			}
			//if ep data not found keep excel values
			if (epList==null || epList.isEmpty()) {
				JLog.logger.error("Could not populate ep data as no EP data found. Keeping Excel values.");
				continue;
			}

			if (method.compareToIgnoreCase(rowForEveryEP)==0){
				ArrayList<HashMap<String, String>> addedRows = new ArrayList<HashMap<String, String>>();
				for(int j=0; j<epList.size();j++){
					HashMap<String, String> newLine = (HashMap<String, String>) list.get(i).clone();
					newLine.put(hostNameKey+"1",epList.get(j).hostName);
					newLine.put(userNameKey+"1",epList.get(j).userName);
					newLine.put(passwordKey+"1",epList.get(j).password);
					newLine.put(typeKey+"1",epList.get(j).type);
					addedRows.add(newLine);
				}
				list.remove(i);
				list.addAll(0,addedRows);
				//skip newly added lines
				i+=addedRows.size()-1;
			}

			else if (method.compareToIgnoreCase(allEPInOneRow)==0){
				for(int j=0; j<epList.size();j++){
					list.get(i).put(hostNameKey+ (j+1),epList.get(j).hostName);
					list.get(i).put(userNameKey+ (j+1),epList.get(j).userName);
					list.get(i).put(passwordKey+ (j+1),epList.get(j).password);
					list.get(i).put(typeKey+ (j+1),epList.get(j).type);
				}
			}

			else {
				JLog.logger.error("Could not find legal value at column: " + populateMethod + " Keeping Excel defaults");

			}

 	 	}

	}


}
