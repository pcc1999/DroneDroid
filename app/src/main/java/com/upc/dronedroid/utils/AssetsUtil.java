package com.upc.dronedroid.utils;

import android.app.Activity;
import android.net.Uri;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AssetsUtil {
    public static String loadJSONFromFile(Activity activity, String filename, Uri uriImport) {
        String json = null;
        InputStream is;
        try {
            if (uriImport == null) {
                Uri uri = Uri.fromFile(new File(activity.getExternalFilesDir("FlightPlans").toString() + "/" + filename));
                is = activity.getContentResolver().openInputStream(uri);
            } else {
                is = activity.getContentResolver().openInputStream(uriImport);
            }
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static String loadJSONFromAsset(Activity activity, String filename) {
        String json = null;
        try {
            InputStream is = activity.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static boolean deleteFile(Activity activity, String filename) {
        File fileToDelete = new File(activity.getExternalFilesDir("FlightPlans") + "/" + filename);
        if (fileToDelete.exists()) {
            return fileToDelete.delete();
        } else {
            return false;
        }
    }

    public static void saveJSON(Activity activity, String name, String extDirectory, String JSON) {
        File rootFolder = activity.getExternalFilesDir(extDirectory);
        //Check name contains ".json", if not, add it
        File jsonFile;
        if(!name.contains(".json")){
            jsonFile = new File(rootFolder, name + ".json");
        } else {
            jsonFile = new File(rootFolder, name);
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(jsonFile);
            writer.write(JSON);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
