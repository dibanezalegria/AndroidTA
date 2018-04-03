package com.pbluedotsoft.pcarstimeattackadfree;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by daniel on 10/03/18.
 *
 */

public class Parser47 {

    private static final String TAG = Parser47.class.getSimpleName();

    String car;
    String carClass;
    String track;

    public boolean parse(byte[] packet) {
        try {
            // Car name
            car = new String(Arrays.copyOfRange(packet, 3, 67), "UTF-8");
            int pos = car.indexOf('\0');
            if (pos > 0) {
                car = car.substring(0, pos);
            }
            // Car class
            carClass = new String(Arrays.copyOfRange(packet, 67, 131), "UTF-8");
            pos = carClass.indexOf('\0');
            if (pos > 0) {
                carClass = carClass.substring(0, pos);
            }
            // Track name
            track = new String(Arrays.copyOfRange(packet, 131, 195), "UTF-8");
            pos = track.indexOf('\0');    // find first null in the string
            if (pos > 0) {
                track = track.substring(0, pos);
            }
            // Track variation
            String trackVar = new String(Arrays.copyOfRange(packet, 195, 259), "UTF-8");
            pos = trackVar.indexOf('\0');
            if (pos > 0) {
                trackVar = trackVar.substring(0, pos);
            }
            // Track name + variation
            track = track + " " + trackVar;
            return true;

        } catch (UnsupportedEncodingException ex) {
//            Log.d(TAG, ex.getMessage());
            return false;
        }
    }
}


