package Utils;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class TestFiles {

        public static void DeleteFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()){
                boolean result = file.delete();
                if(result == false || file.exists())
                    org.testng.Assert.fail("Could not delete file: " + path);
            }
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not delete file: " + path + "\n" + e.toString());
        }

    }


    //Delete all files of a folder not including sub folder content
    public static void DeleteAllFiles(String path) {
        try {
            File file = new File(path);
            Path nioPath = Paths.get(path);
            if (file.exists()){
                Files.walk(nioPath,1)
                        .filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .forEach(File::delete);

            }
            else {
                org.testng.Assert.fail("Could not delete all files under: " + path + " could not find such folder");
            }
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not delete file: " + path + "\n" + e.toString());
        }

    }

    public static void DeleteDirectory(String path){
        try {
            File dir = new File(path);
            if (dir.exists()) {
                FileUtils.forceDelete(dir);
                if (dir.exists())
                    org.testng.Assert.fail("Could not delete directory: " + path);
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
                boolean bool = file.mkdirs();
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

    public static void RemoveLines(String path, String ifContainThisStringCommentLine , char commentSign)  {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            String currentLine;
            String textToWrite="";

            boolean changeDone =false;
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains(ifContainThisStringCommentLine)) {
                    if (currentLine.length() > 0 && currentLine.trim().charAt(0) != commentSign) {
                        currentLine = "";
                        changeDone = true;
                    }
                }
                textToWrite+= System.getProperty("line.separator") + currentLine;
            }
            reader.close();

            if (changeDone) {
                PrintWriter out = new PrintWriter(path);
                out.print(textToWrite);
                out.close();
            }

        }
        catch (Exception e){
            org.testng.Assert.fail("Could not remove lines from the following file: " + path + "\n" + e.toString());
        }

    }

}
