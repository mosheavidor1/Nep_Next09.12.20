package Utils;

import java.util.Vector;

import Utils.Logs.JLog;

public class Utils {
	
	public static Vector<String> extractFileNames(String lines, String startPattern, String endPattern) {
    	
    	JLog.logger.info("Going to extract 2 file names from log lines: {}", lines);
    	
        Vector<String> fileNames = new Vector<String>();
        int file_start = 0;
        for (int i = 0;i < 2; i++) {
            int start = lines.indexOf(startPattern, file_start);
            int stop = lines.indexOf(endPattern, start);           
            org.testng.Assert.assertTrue(start != -1 , "Failed to find '" + startPattern + "' in log lines");
            org.testng.Assert.assertTrue(stop != -1, "Failed to find '" + endPattern + "' in log lines");
            String zipFileMane = lines.substring(start, stop + endPattern.length());
            fileNames.add(zipFileMane);
            file_start = stop;
            JLog.logger.info("Found file name {}", zipFileMane);
        }
        
        return fileNames;
    }
	
}
