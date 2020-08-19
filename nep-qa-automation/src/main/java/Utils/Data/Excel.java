package Utils.Data;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.apache.commons.io.FileUtils;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


public class Excel  {

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
	 private String sheetName;
	 private String fileName;
	 private File xmlFile;


	public Excel(String pathAndName, String sheetName) {
		this.sheetName = sheetName;
		fileName = pathAndName;
		xmlFile = new File(fileName);
		if (!xmlFile.exists()) {
			org.testng.Assert.fail("Could not find Excel file at: " + pathAndName);
		}

	}

    public Object [] getTestData()  {
 	 	try {

			String fileContent = FileUtils.readFileToString(xmlFile, Charset.defaultCharset());

			//remove 2 first lines of the XML - currently not needed
			//fileContent = fileContent.substring(fileContent.indexOf('\n')+1);
			//fileContent = fileContent.substring(fileContent.indexOf('\n')+1);
			//replace the xml header - currently not needed
			/*fileContent = "<?xml version=\"1.0\"?>\n" +
					"<!DOCTYPE some_name [ \n" +
					"<!ENTITY nbsp \"&#160;\"> \n" +
					"<!ENTITY acute \"&#180;\"> \n" +
					"]>" + fileContent;*/

			SAXParserFactory parserFactor = SAXParserFactory.newInstance();
			SAXParser parser = parserFactor.newSAXParser();
			SAXHandler handler = new SAXHandler(sheetName);

			ByteArrayInputStream bis = new ByteArrayInputStream(fileContent.getBytes());

			parser.parse(bis, handler);

			int numOfRows = handler.xmlRowList.size();
			if (numOfRows < 2){
				JLog.logger.error("Could not get valid data from sheet: " + sheetName + " Number of valid rows found: " + numOfRows);
				//org.testng.Assert.fail("Could not get valid data from sheet: " + sheetName + " Number of valid rows found: " + numOfRows);
			}
			ArrayList<HashMap<String, String>> testData = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> keysMap = new HashMap<String, String>();
			XmlRow columnsNames = handler.xmlRowList.get(0);
			for (int i = 0; i < columnsNames.cellList.size(); i++) {
				keysMap.put(columnsNames.cellList.get(i), "");
			}

			boolean isProduction = PropertiesFile.isProduction();
			boolean isEnvironments = PropertiesFile.isEnvironments();
			String clusterToTest = PropertiesFile.readProperty("ClusterToTest");

			XmlRow secondRowData = handler.xmlRowList.get(1);
			XmlRow defaultData = null;
			int startDataRow = 1;

			if (secondRowData.cellList.get(0).compareToIgnoreCase(defaultIdentifier) == 0) {
				defaultData = secondRowData;
				startDataRow = 2;
			}
			boolean parametersArePerEnvironment = false;
			if (columnsNames.cellList.get(0).trim().equalsIgnoreCase(parameterPerEnvironmentIdentifier))
				parametersArePerEnvironment = true;

			for (int i = startDataRow; i < numOfRows; i++) {
				HashMap<String, String> currentMap = new HashMap<String, String>();
				currentMap.putAll(keysMap);
				XmlRow rowData = handler.xmlRowList.get(i);
				if (rowData.cellList.size() > 0 && parametersArePerEnvironment) {
					String currentLineParaSetIdentifier = rowData.cellList.get(0).trim();
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

				for (int j = 0; j < rowData.cellList.size(); j++) {
					String current;
					if (defaultData != null && defaultData.cellList.size() - 1 >= j && rowData.cellList.get(j).trim().isEmpty())
						current = defaultData.cellList.get(j);
					else
						current = rowData.cellList.get(j);

					currentMap.put(columnsNames.cellList.get(j), current);
				}

				if (defaultData != null && defaultData.cellList.size() > rowData.cellList.size()) {
					for (int j = rowData.cellList.size(); j < defaultData.cellList.size(); j++) {
						currentMap.put(columnsNames.cellList.get(j), defaultData.cellList.get(j));
					}
				}
				testData.add(currentMap);
			}

			if (parametersArePerEnvironment && testData.isEmpty() && defaultData != null) {
				HashMap<String, String> currentMap = new HashMap<String, String>();
				currentMap.putAll(keysMap);
				for (int j = 0; j < defaultData.cellList.size(); j++) {
					currentMap.put(columnsNames.cellList.get(j), defaultData.cellList.get(j));
				}
				testData.add(currentMap);
			}


			PopulateEpData(testData);
			return testData.toArray();
		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not get Excel test data. WorkSheet name: " + sheetName + "\n" + e.toString());
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
