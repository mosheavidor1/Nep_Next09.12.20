package Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
            org.testng.Assert.fail("Could delete directory: " + path + "\n" + e.toString());
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


}
