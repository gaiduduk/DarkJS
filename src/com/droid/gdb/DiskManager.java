package com.droid.gdb;

import com.droid.djs.fs.DataOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;

public class DiskManager {

    private static DiskManager instance;
    public IniFile properties = null;
    public ActionThread mainThread;

    public final static File dbDir = new File("out/SimpleGraphDB");
    public final static File propertiesFile = new File(dbDir, "settings.properties");
    public Integer partSize;
    public Integer cacheSize;
    public Integer device_id;

    public static DiskManager getInstance() {
        if (instance == null) {
            try {
                instance = new DiskManager();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public final static String SECTION = "_manager_";
    public final static String PART_SIZE_KEY = "part_size";
    public final static Integer PART_SIZE_DEFAULT = 4096;
    public final static String CACHE_SIZE_KEY = "cache_size";
    public final static Integer CACHE_SIZE_DEFAULT = 4096;
    public final static String DEVICE_ID_KEY = "device_id";

    private DiskManager() throws FileNotFoundException {
        // TODO double save settings
        // TODO problem when DiskManager getFunctions without saving data rights

        if (!dbDir.isDirectory())
            if (!dbDir.mkdirs())
                throw new FileNotFoundException();
        try {
            properties = new IniFile(propertiesFile);

            loadProperties(properties);
            saveProperties(properties);

            createFtpTempDir();

            mainThread = new ActionThread(cacheSize);
            Thread thread = new Thread(mainThread);
            thread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createFtpTempDir() {
        if (!DataOutputStream.ftpTempDir.exists())
            DataOutputStream.ftpTempDir.mkdirs();
        else {
            File[] files = DataOutputStream.ftpTempDir.listFiles();
            if (files != null)
                for (File file : files)
                    file.delete();
        }
    }

    private void loadProperties(IniFile properties) {
        this.partSize = properties.getInt(SECTION, PART_SIZE_KEY, PART_SIZE_DEFAULT);
        this.cacheSize = properties.getInt(SECTION, CACHE_SIZE_KEY, CACHE_SIZE_DEFAULT);
        this.device_id = properties.getInt(SECTION, DEVICE_ID_KEY, Math.abs(new Random().nextInt()));
    }

    private void saveProperties(IniFile properties) {
        properties.put(SECTION, PART_SIZE_KEY, "" + this.partSize);
        properties.put(SECTION, CACHE_SIZE_KEY, "" + this.cacheSize);
        properties.put(SECTION, DEVICE_ID_KEY, "" + this.device_id);
    }

    public void addDisk(String rootDir) {
    }

    public void diskTesting() {
        // TODO testing of all disks
    }

    public File getFileById(long fileId) {
        return new File(dbDir, fileId + ".data");
    }
}
