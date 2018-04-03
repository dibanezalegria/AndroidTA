package com.pbluedotsoft.pcarstimeattackadfree;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.JsonReader;
import android.util.Log;

import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by daniel on 14/03/18.
 *
 */

public class JsonHelper {

    private static final String TAG = JsonHelper.class.getSimpleName();
    private static String PUBLIC_DIR = "pcarstimeattack";
    private static String FILENAME = "data";

    private Context mContext;

    public JsonHelper(Context context) {
        mContext = context;
    }

    /**
     *  Backup database into a file in the downloads public directory.
     *
     */
    public String backup(Context context) {
        Cursor cursor = context.getContentResolver().query(LapContract.LapEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (cursor == null || cursor.getCount() == 0)
            return "There are not laptimes to backup.";

        JSONObject jsonLaptimes = new JSONObject();     // root key for the array
        JSONArray jsonArray = new JSONArray();          // array containing all the laps

        cursor.moveToFirst();
        do {
            // Find the columns we are interested in
            int trackColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_TRACK);
            int carColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_CAR);
            int classColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_CLASS);
            int nlapsColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_NLAPS);
            int s1ColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_S1);
            int s2ColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_S2);
            int s3ColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_S3);
            int timeColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_TIME);

            // Read laptime attributes from the cursor
            String track = cursor.getString(trackColIndex);
            String car = cursor.getString(carColIndex);
            String carClass = cursor.getString(classColIndex);
            int nlaps = cursor.getInt(nlapsColIndex);
            float s1 = cursor.getFloat(s1ColIndex);
            float s2 = cursor.getFloat(s2ColIndex);
            float s3 = cursor.getFloat(s3ColIndex);
            float time = cursor.getFloat(timeColIndex);

            // Parse one laptime from database to a string in JSON format
            JSONObject jsonObj = new JSONObject();

            try {
                jsonObj.put("track", track);
                jsonObj.put("car", car);
                jsonObj.put("carClass", carClass);
                jsonObj.put("nlaps", nlaps);
                jsonObj.put("s1", s1);
                jsonObj.put("s2", s2);
                jsonObj.put("s3", s3);
                jsonObj.put("time", time);
                jsonArray.put(jsonObj);

            } catch (JSONException ex) {
                ex.printStackTrace();
                return null;
            }
        } while (cursor.moveToNext());

        // Add array with all the laps to 'laptimes' root key
        try {
            jsonLaptimes.put("laptimes", jsonArray);
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }

        if (!cursor.isClosed())
            cursor.close();

        String data = jsonLaptimes.toString();

        // Get the directory for the user's public downloads directory
        File publicDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), PUBLIC_DIR);
        publicDir.mkdirs();
//        if (!publicDir.mkdirs())
//            Log.d(TAG, "Directory has NOT been created.");
        File outFile = new File(publicDir, FILENAME);
        try {
            OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(outFile));
            os.write(data);
            os.flush();
            os.close();
        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return data;
    }

    /**
     *  Restore file from downloads public directory and into database.
     *
     * @return
     */
    public JSONArray restore() {
        // Get the directory for the user's public downloads directory
        File publicDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), PUBLIC_DIR);
        File inFile = new File(publicDir, FILENAME);
        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(inFile));
            BufferedReader reader = new BufferedReader(is);
            String json = reader.readLine();
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("laptimes");
            reader.close();
            return jsonArray;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
