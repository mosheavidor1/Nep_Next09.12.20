package Tests.SWG;

import Actions.TestActions;
import Utils.Logs.JLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.Test;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


//This is a tool is created to test upload functionality regarding https://jira.trustwave.com/browse/SWGCLOUD-3101
public class UploadFiles {



	 @Test (alwaysRun = true)
	 public void UploadTest () throws IOException {

		 List<File> list = ListOfFiles("\\\\192.168.90.69\\test\\email\\Documents");
		 String copyFailuresFolder = "C:\\temp\\Error Files\\";
		 String curlCmd = "C:\\Selenium\\Utils\\curl-7.69.1-win64-mingw\\bin\\curl -k -F file=@\"XXX\" https://mize.nty.m86.local/Scripts/App/postdatastrict.aspx -x 192.168.142.180:8443";
		 for (File file : list) {

			 String filePath = file.getAbsolutePath();

			 JLog.logger.info("Info: Start uploading of file: " +filePath);


			 String curlWithFile = curlCmd.replace("XXX",filePath);

			 String response = TestActions.execCmd(curlWithFile,false);
			 String output="";
			 if(!response.contains("Data was uploaded successfully")) {
			 	String newFilePath = copyFailuresFolder+file.getName();
				 File localCopy = new File(newFilePath);
				 try {
					 FileUtils.copyFile(file,localCopy);
				 } catch (IOException e) {
					 JLog.logger.error("Access to the following file denied: " + file.getAbsolutePath());
					 continue;
				 }
				 String fileTempName = copyFailuresFolder + "TempU1Test";
				 String extension = FilenameUtils.getExtension(localCopy.getName());
				 if (extension != "")
				 	fileTempName += "." + extension;
				 File tempCopy = new File(fileTempName);
				 tempCopy.delete();
				 FileUtils.copyFile(localCopy, tempCopy);
				 curlWithFile = curlCmd.replace("XXX", fileTempName);

				 response = TestActions.execCmd(curlWithFile, false);
				 tempCopy.delete();
				 if (!response.contains("Data was uploaded successfully")) {
				 	output = "Error: file not uploaded: " + filePath;
				 	String errorMessageIdentifier ="<td id=\"eumtd\" valign=\"top\" class=\"messageCSS\" style=\"width: 100%\">";
				 	if(response.contains(errorMessageIdentifier))
				 		output+= "Error Message: " + response.substring(response.indexOf(errorMessageIdentifier)+errorMessageIdentifier.length(),response.indexOf("<br><br><br>",response.indexOf(errorMessageIdentifier)));
				 	JLog.logger.error(output);

				 } else
				 	localCopy.delete();
			 }


		 }


	 }


	public static List<File> ListOfFiles(String directoryName) {
		File directory = new File(directoryName);

		List<File> resultList = new ArrayList<File>();

		// get all the files from a directory
		File[] fList = directory.listFiles();
		resultList.addAll(Arrays.asList(fList));
		for (File file : fList) {
			if (file.isFile()) {
				System.out.println(file.getAbsolutePath());
			} else if (file.isDirectory()) {
				resultList.remove(file);
				resultList.addAll(ListOfFiles(file.getAbsolutePath()));
			}
		}
		return resultList;
	}


}
