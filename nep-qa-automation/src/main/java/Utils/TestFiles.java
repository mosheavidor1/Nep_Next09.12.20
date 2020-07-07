package Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

public class TestFiles {



        public static void DeleteFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()){
                boolean result = file.delete();
                if(result == false || file.exists())
                    org.testng.Assert.fail("Could delete file: " + path);
            }

        }
        catch (Exception e){
            org.testng.Assert.fail("Could delete file: " + path + "\n" + e.toString());
        }

    }

    public static void DeleteDirectory(String path){
        try {
            File dir = new File(path);
            if (dir.exists()) {
                FileUtils.forceDelete(dir);
                if (dir.exists())
                    org.testng.Assert.fail("Could delete directory: " + path);
            }
        }
        catch (IOException e) {
            org.testng.Assert.fail("Could not delete directory: " + path + "\n" + e.toString());
        }

    }


    public static void CreateFolder(String folder){
        try {
            File file = new File(folder);
            if (!file.exists() || !file.isDirectory()) {
                boolean bool = file.mkdir();
                if (!bool)
                    org.testng.Assert.fail("Could not create directory: " + folder);
            }
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not create directory: " + folder + "\n" + e.toString());
        }

    }


    public static void Copy(String source, String destination){
        try {
            File from = new File(source);
            File to = new File(destination);
            Files.copy(from.toPath(),to.toPath());
            if(!to.exists())
                org.testng.Assert.fail("Could not copy file from:" + source + "  to: " + destination);

        }
        catch (Exception e){
            org.testng.Assert.fail("Could not copy file from:" + source + "  to: " + destination + "\n" + e.toString());
        }

    }


    public static boolean Exists(String path) {
        File file = new File(path);
        if (file.exists())
            return true;
        else
            return false;
    }



    public static void AppendToFile(String path, String addThisString , boolean checkIfStringAlreadyExists){
        try {
            if (!Exists(path))
                org.testng.Assert.fail("Could not find file:" + path + " needed to append the following: " + addThisString);

            File file = new File(path);


            Scanner scanner = new Scanner(file);

            boolean found = false;

            if ( checkIfStringAlreadyExists) {
                while (scanner.hasNextLine()) {
                    String lineFromFile = scanner.nextLine();
                    if (addThisString.trim().equalsIgnoreCase(lineFromFile.trim())) {
                        found = true;
                        break;
                    }
                }
            }


            if(! found) {
                FileWriter fr = new FileWriter(file, true);
                fr.write( addThisString);
                fr.close();
            }
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not append to file:" + path + " needed to append the following: " + addThisString + "\n" + e.toString());
        }


    }
}
