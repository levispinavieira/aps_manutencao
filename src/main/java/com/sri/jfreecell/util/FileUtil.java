package com.sri.jfreecell.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FileUtil {

    public static final String STATE_FILE = "state.ser";
    public static final String STAT_FILE = "usrstat.ser";

    public static String getAppDirectory() {
        String userDir = System.getProperty("user.home");
        String appDir = userDir.concat(File.separator).concat(".jfreecell").concat(File.separator);

        File directory = new File(appDir);
        if (!directory.exists()) {
            directory.mkdir();
        }
        return appDir;
    }

    public static void saveObjecttoFile(Object obj, String fileName) {
        String fullpath = getAppDirectory().concat(fileName);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fullpath))) {
            oos.writeObject(obj);
            System.out.println("Object saved to file: " + fileName);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Unable to save file: " + fileName);
        }
    }

    public static Object getObjectfromFile(String fileName) {
        String fullpath = getAppDirectory().concat(fileName);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fullpath))) {
            System.out.println("Retriving Object from file: " + fileName);
            return ois.readObject();
        } catch (Exception ex) {
            System.err.println("File not found or corrupted: " + fileName);
        }
        return null;
    }

    public static void deleteFile(String fileName) {
        String fullpath = getAppDirectory().concat(fileName);
        try {
            File file = new File(fullpath);
            if (file.delete()) {
                System.out.println(file.getName() + " is deleted!");
            } else {
                System.out.println("Delete operation is failed.");
            }
        } catch (Exception e) {
            System.out.println("Delete operation is failed.");
            e.printStackTrace();
        }
    }

}
