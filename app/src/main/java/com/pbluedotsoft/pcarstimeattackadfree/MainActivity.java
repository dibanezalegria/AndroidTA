package com.pbluedotsoft.pcarstimeattackadfree;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract.LapEntry;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String STATE_CAR = "car";
    private static final int PORT = 5606;   /* Project Cars UDP port */

    boolean mAppOn;     /* Helps pausing the PacketHandlerAsyncTask AsyncTask when onPause() */

    private Timer mTimer;   /* Timer runs the TimeTask that runs the AsyncTasks */

    private WifiManager.MulticastLock mLock;    /* Allows app to receive Wifi multicast packets */

    private boolean mSoundOn;   /* Flag connected to option's menu sound checkbox */

    /**
     * Variables used by PacketHandlerAsyncTask
     */
    private DatagramSocket mSocket;
    private TextView mNLapTV, mRecordTV, mCarTrackComboTV;  // GUI
    private Parser47 mParser47;     // parser for packet 1347 (instantiation in onResume)
    private Parser mParser;         // parser for packet 1367 (instantiation in onResume)
    private Laptime mLaptime;       // current laptime
    private TreeMap<Integer, Laptime> mLapMap;  // laptimes used in Session Adapter

    /**
     * List contains stored laptimes that couldn't be process due to 'waiting for car/track' state
     */
    private ArrayList<Laptime> mBufferedLaps;

    /**
     * References to Fragments
     */
    private SessionFragment mSessionFragment;
    private LiveDbFragment mLiveDbFragment;
    private GhostFragment mGhostFragment;

    // DEBUG
    private int packetCounter = 0;
    private TextView mDebugTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        // String set in AndroidManifest is actually for both activity title and icon name
        // PCarsTA is a good icon name, but here I choose a longer string for activity's title
        setTitle(R.string.main_activity_title);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // ViewPager and TabLayout
        ViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(2); // cached pages to either side of current
        viewPager.setAdapter(new PagerAdapterFragment(getSupportFragmentManager()));
        TabLayout tabLayout = findViewById(R.id.viewpager_tabs);
        tabLayout.setupWithViewPager(viewPager);

        // References to fragments
        mSessionFragment = SessionFragment.getInstance();
        mLiveDbFragment = LiveDbFragment.getInstance();
        mGhostFragment = GhostFragment.getInstance();

        mNLapTV = findViewById(R.id.info_nlap_tv);
        mRecordTV = findViewById(R.id.info_record_tv);
        mCarTrackComboTV = findViewById(R.id.info_car_track_combo_tv);

        // DEBUG TextView
//        mDebugTV = findViewById(R.id.debug_tv);

        // DEBUG: insert dummy laps in session button
//        Button button = findViewById(R.id.debug_button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "Insert dummies in session");
//                insertDummyInSession(20);
//            }
//        });
    }

    @Override
    protected void onPause() {
        super.onPause();
//        Log.d(TAG, "onPause()");
        mAppOn = false;     // enables/disables PacketHandlerAsyncTask in TimerTask onResume()
        mTimer.cancel();
        mTimer.purge();
        mTimer = null;
        if (mSocket != null && !mSocket.isClosed()) {
            mSocket.close();
//            Log.d(TAG, "Closing socket onPause");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        //
        // Reading settings preferences
        //
        // Set default in case user has never entered menu option before
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSoundOn = sharedPref.getBoolean(getResources().getString(R.string.pref_sound_key),
                true);
        boolean auto = sharedPref.getBoolean(getResources().getString(R.string.pref_auto_key),
                true);
        String manual = sharedPref.getString(getResources().getString(R.string.pref_broadcast_key),
                "");
        String maxStr = sharedPref.getString(getResources().getString(R.string.pref_session_gap_key)
                , "15");
        int maxGap = (Integer.parseInt(maxStr) < 5) ? 5 : Integer.parseInt(maxStr);

        // Sets max length for the gap's graph chart in fragment
        mSessionFragment.setMaxGap(maxGap);     // SessionAdapter will get it when first update()

        //
        // Network connection
        //
        String broadcast;
        if (auto) {
            broadcast = getBroadcastAddress();
            if (broadcast == null) {
                Toast.makeText(getApplicationContext(), "Is your WiFi on? Try changing your " +
                        "broadcast address in settings and restart the app.", Toast.LENGTH_LONG)
                        .show();
            }
        } else
            broadcast = manual;

        try {
            mSocket = new DatagramSocket(PORT, InetAddress.getByName(broadcast));
            mSocket.setBroadcast(true);
            mAppOn = true;
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(getApplicationContext(), "Is your WiFi on? Try changing your " +
                    "broadcast address in settings and restart.", Toast.LENGTH_LONG).show();
        }

//        Log.d(TAG, "Broadcast: " + broadcast);
        if (broadcast != null)
            Toast.makeText(getApplicationContext(), "Broadcast address: " + broadcast,
                    Toast.LENGTH_LONG).show();

        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null)
            mLock = wifiManager.createMulticastLock("lock");

        //
        // TimerTask run by Timer creates one PacketHandlerAsyncTask object every 50 ms
        //
        TimerTask packetHandlerTask = new TimerTask() {
            @Override
            public void run() {
                if (mAppOn)
                    new PacketHandlerAsyncTask().execute();  // AsyncTask class
            }
        };
        mTimer = new Timer();
        mTimer.schedule(packetHandlerTask, 0, 50); // run task every 50 ms
        hardReset();
        // POTENTIAL BUG if commented out: read about it on storeLapForLateProcessing()
//            if (mBufferedLaps != null)
//                mBufferedLaps.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Log.d(TAG, "onDestroy()");
        if (mSocket != null && !mSocket.isClosed()) {
            mSocket.close();
        }
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed(); commented this line in order to disable back press
        Toast.makeText(getApplicationContext(), "Back button is disabled.", Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * This is an INNER CLASS.
     * Note: It cannot be static since it needs access to enclosing activities's members.
     */
    public class PacketHandlerAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private static final int PACKET_1347 = 0;
        private static final int PACKET_1367 = 1;
        private static final int PACKET_OTHER = 2;

        private static final int STATE_RESTART = 4;     // restart session
        private static final int STATE_MENU = 1;        // main menu

        private static final int UDP_MAX_SIZE = 5000;

        private byte[] byteBuffer, dataBytes;
        private int packetType;                         // 0: 1347, 1: 1367 or 2: other

        @Override
        protected Boolean doInBackground(Void... voids) {
            //
            //  Important: Do not touch MainActivity's members inside this method.
            //  Decoupling doInBackground from nesting activity avoids problems
            //  with orientation changes.
            //
            mLock.acquire();
            byteBuffer = new byte[UDP_MAX_SIZE];
            DatagramPacket dataPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
            try {
                if (mAppOn)
                    mSocket.receive(dataPacket);
            } catch (IOException ex) {
//                Log.e(TAG, ex.getMessage());
            } finally {
                if (mLock.isHeld())
                    mLock.release();
            }

            dataBytes = dataPacket.getData();

            if (dataPacket.getLength() == 1367) {
                packetType = PACKET_1367;
                mParser = new Parser();
                mParser.parse(dataBytes);
                return true;
            }

            if (dataPacket.getLength() == 1347 && dataBytes[3] != '\0') {
                packetType = PACKET_1347;
                mParser47 = new Parser47();
                if (!mParser47.parse(dataBytes)) {
                    packetType = PACKET_OTHER;
                    return false;   // bad packet
                }
                processBufferedLaps();   // process buffered laps now that we got car/track info
                return true;
            }

            packetType = PACKET_OTHER;
            return false;
        }

        @Override
        protected void onPostExecute(Boolean validPacket) {
            super.onPostExecute(validPacket);

            if (!validPacket)
                return;

            // DEBUG
//            packetCounter++;
//            mDebugTV.setText("Packet counter: " + packetCounter);
//            Log.d(TAG, "Packets handled: " + packetCounter);


            switch (packetType) {
                case PACKET_1347:
                    // Update UI car/track (only if needed)
                    String label = getResources().getString(R.string.waiting_car_info);
                    if (mCarTrackComboTV.getText().toString().equals(label)) {
                        mCarTrackComboTV.setText(String.format(Locale.ENGLISH, "%s @ %s",
                                mParser47.car, mParser47.track));
                        mCarTrackComboTV.setBackgroundColor(ContextCompat.getColor
                                (getApplicationContext(), R.color.black));
                        mCarTrackComboTV.setTextColor(ContextCompat.getColor
                                (getApplicationContext(), R.color.whiteText));
//                        mCarTrackComboTV.startAnimation(AnimationUtils
//                                .loadAnimation(getApplicationContext(), R.anim.blink));
                        if (mSoundOn)
                            new ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                                    .startTone(ToneGenerator.TONE_PROP_BEEP);

                        // Get record for car/track combo from DB
                        Cursor cursor = getContentResolver().query(LapEntry.CONTENT_URI,
                                null,
                                LapEntry.COLUMN_LAP_TRACK + " LIKE ? AND " +
                                        LapEntry.COLUMN_LAP_CAR + " LIKE ?",
                                new String[]{mParser47.track, mParser47.car},
                                null);

                        if (cursor != null && cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            float[] ftimes = new float[] {
                                    cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME)),
                                    cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_S1)),
                                    cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_S2)),
                                    cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_S3)),
                            };
                            Laptime recordLap = new Laptime(0, ftimes, false);

                            // Back to black. Buffered laptimes processing changes bgLayout.color
                            LinearLayout bgLayout = findViewById(R.id.record_bg_layout);
                            bgLayout.setBackgroundColor(ContextCompat
                                    .getColor(getApplicationContext(), R.color.black));
                            mRecordTV.setTypeface(null, Typeface.NORMAL);
                            mRecordTV.setText(Laptime.format(recordLap.time));
                            // Inform SessionFragment that SessionAdapter can highlight record.
                            mSessionFragment.setRecord(recordLap);
                        }
                        if (cursor != null && !cursor.isClosed())
                            cursor.close();

                        // Pass car/track info to LapCursorAdapters via LiveDbFragment
                        mLiveDbFragment.informCursorAdapters(mParser47.car, mParser47.track,
                                mParser.lastLap);
                        // Pass car/track to GhostFragment
                        mGhostFragment.setCarTrackCombo(mParser47.car, mParser47.track);
                    }
                    break;

                case PACKET_1367:
//                    Log.d(TAG, "GS: " + mParser.gameState);
                    boolean updateSession = false;
                    if (mParser.gameState == STATE_RESTART) {
                        softReset();
                        updateSession = true;
                    }

                    if (mParser.gameState == STATE_MENU) {
                        if (!mLapMap.isEmpty() || mParser47.car != null) {
                            hardReset();
                            updateSession = true;
                        }
                    }

                    Laptime currLap = mLapMap.get(mParser.lapNum);
                    Laptime prevLap = mLapMap.get(mParser.lapNum - 1);

                    if (mParser.sectorNum == 1) {
                        if (mParser.lapNum > 1 && prevLap != null && prevLap.time <= 0) {
                            // update previous lap s3 and time (only once)
                            prevLap.s3 = mParser.lastSec;
                            prevLap.time = mParser.lastLap;

                            // database update
                            if (mParser47.car == null) {
//                                Toast.makeText(getApplicationContext(),
//                                        R.string.waiting_car_info_long,
//                                        Toast.LENGTH_LONG).show();
                                storeLapForLateProcessing(mLaptime);
                                mCarTrackComboTV.startAnimation(AnimationUtils
                                        .loadAnimation(getApplicationContext(), R.anim.blink));
                            } else {
                                // real-time db update: returns true if laptime is a record
                                if (databaseUpdate(mLaptime)) {
                                    // beep crossing finish line (record)
                                    if (mSoundOn)
                                        new ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                                                .startTone(ToneGenerator.TONE_PROP_BEEP2);
                                    // pass record to SessionFragment (highlight)
                                    mSessionFragment.setRecord(mLaptime);
                                }
                            }
                            updateSession = true;
//                            Log.d(TAG, "s3/time update: " + mLaptime);
                        }

                        if (currLap == null) {
//                            Log.d(TAG, "--------------- NEW Laptime object: " + mParser.lapNum);
                            mLaptime = new Laptime(mParser.lapNum);
                            mLapMap.put(mParser.lapNum, mLaptime);
                            updateSession = true;
                        }

                    } else if (mParser.sectorNum == 2 && currLap != null && mParser.lastSec > 0 &&
                            currLap.s1 <= 0) {

                        // update s1 (check for null in case starting app while player on s2)
                        currLap.s1 = mParser.lastSec;
                        updateSession = true;
//                        Log.d(TAG, "Update s1: " + mLaptime);

                    } else if (mParser.sectorNum == 3 && mParser.lastSec > 0 &&
                            currLap != null && currLap.s2 <= 0) {

                        // check on null in case user starting app while car already on s3
                        // update s2: check on lastSec avoids updates when in starting grid,
                        currLap.s2 = mParser.lastSec;
                        updateSession = true;
//                        Log.d(TAG, "Update s2: " + mLaptime);
                    }

                    // set invalid if flagged (only once)
                    if (mParser.invalidLap == 1 && currLap != null && !currLap.invalid) {
                        currLap.invalid = true;
                        updateSession = true;
                    }
//                    Log.d(TAG, "laptime: " + mLaptime);

                    //
                    // Update GUI
                    //
                    if (!mNLapTV.getText().toString().equals(String.valueOf(mParser.lapNum)) &&
                            mParser.lapNum > 0) {
                        mNLapTV.setText(String.valueOf(mParser.lapNum));
                    }

                    //
                    // Update Session Fragment
                    //
                    if (updateSession) {
                        updateSession();
                    }
                    break;
                default:
                    break;
            }
        }

        /**
         * Buffered list contains laptimes that where driven while waiting for car/track info.
         */
        private void storeLapForLateProcessing(Laptime laptime) {
            // TODO: Important note about a potential bug.
            // Laps stored for late processing do not have information about car/track.
            // These laps will be entered in database when car/track information is available.
            // Potential bug: when app resumes after being paused, stored laptimes remain intact.
            // If the user has changed track/car configuration while paused, stored laptimes will
            // be saved with the new car/track combo! A possible solution is to clear the list of
            // stored laptimes in onResume when pausing the app, but then it is not buffering of
            // laps when the app loses focus even for one second.
            if (mBufferedLaps == null) {
                mBufferedLaps = new ArrayList<>();
            }

            mBufferedLaps.add(laptime);
//            Log.d(TAG, "Laptime has been buffered for later processing: " +
//                    Laptime.format(laptime.time));
        }

        /**
         * Process laptimes stored while waiting for car/track info.
         */
        private void processBufferedLaps() {
            if (mBufferedLaps == null) {
//                Log.d(TAG, "NO laps for late processing.");
                return;
            }

            // This flag makes sure even if several buffered laps are improving the record,
            // SessionFragment.setRecord gets called only once.
            Laptime newRecordLap = null;
            while (!mBufferedLaps.isEmpty()) {
                Laptime laptime = mBufferedLaps.remove(0);
                if (databaseUpdate(laptime)) {
                    newRecordLap = laptime;
                }
//                Log.d(TAG, "Late processing: " + Laptime.format(laptime.time));
            }

            if (newRecordLap != null) {
                // pass record to SessionFragment (highlight)
                mSessionFragment.setRecord(newRecordLap);
            }
//            Log.d(TAG, "Buffered laps newRecord: " + Laptime.format(newRecord));
        }

        /**
         * Update laptimes in database.
         *
         * @param laptime - needed for processBufferedLaps for delayed laptime processing.
         */
        private boolean databaseUpdate(Laptime laptime) {
            // Crash fix released version 1.0.2: check mParser47
            if (!laptime.isGood() || mParser47.track == null || mParser47.car == null)
                return false;

            String selection = LapEntry.COLUMN_LAP_TRACK + " LIKE ? AND " +
                    LapEntry.COLUMN_LAP_CAR + " LIKE ?";
            String[] selArgs = new String[]{mParser47.track, mParser47.car};

            Cursor cursor = getContentResolver().query(LapEntry.CONTENT_URI,
                    null,
                    selection,
                    selArgs,
                    null);

            if (cursor == null) {
//                Log.d(TAG, "Error: database access.");
                return false;
            }

            // Pass car/track/last to LiveDbFragment -> LapCursorAdapter and LapCursorPlusAdapter
            // This makes the laptime blink in the adapters. It works fine. Disabled for now.
//            mLiveDbFragment.informCursorAdapters(mParser47.car, mParser47.track, mParser.lastLap);

            final String showNow;
            final String showAfter;
            final int gapColor;
            boolean isNewRecord = false;

            ContentValues values = new ContentValues();
            if (cursor.getCount() == 0) {
                // Insert
                values.put(LapEntry.COLUMN_LAP_TRACK, mParser47.track);
                values.put(LapEntry.COLUMN_LAP_CAR, mParser47.car);
                values.put(LapEntry.COLUMN_LAP_CLASS, mParser47.carClass);
                values.put(LapEntry.COLUMN_LAP_NLAPS, 1);
                values.put(LapEntry.COLUMN_LAP_S1, laptime.s1);
                values.put(LapEntry.COLUMN_LAP_S2, laptime.s2);
                values.put(LapEntry.COLUMN_LAP_S3, laptime.s3);
                values.put(LapEntry.COLUMN_LAP_TIME, laptime.time);
                Uri newUri = getContentResolver().insert(LapEntry.CONTENT_URI, values);
                showNow = Laptime.format(laptime.time);
                showAfter = Laptime.format(laptime.time);
                gapColor = R.color.black;
                isNewRecord = true;
            } else {
                // Add one to lap counter
                cursor.moveToFirst();
                int nlaps = cursor.getInt(cursor.getColumnIndex(LapEntry.COLUMN_LAP_NLAPS));
                float recordFromDB = cursor.getFloat(cursor.getColumnIndex(LapEntry
                        .COLUMN_LAP_TIME));
                values.put(LapEntry.COLUMN_LAP_NLAPS, ++nlaps);

                // Record?
                if (recordFromDB > laptime.time) {
                    values.put(LapEntry.COLUMN_LAP_S1, laptime.s1);
                    values.put(LapEntry.COLUMN_LAP_S2, laptime.s2);
                    values.put(LapEntry.COLUMN_LAP_S3, laptime.s3);
                    values.put(LapEntry.COLUMN_LAP_TIME, laptime.time);
                    showAfter = Laptime.format(laptime.time);
                    isNewRecord = true;
                } else {
                    showAfter = Laptime.format(recordFromDB);
                }
                getContentResolver().update(LapEntry.CONTENT_URI, values, selection, selArgs);
                showNow = Laptime.getGapStr(laptime.time, recordFromDB);
                gapColor = (showNow.charAt(0) == '-') ? R.color.bestSector : R.color.gap_bg_color;
            }

            cursor.close();

            //
            // Show gap for n seconds on the lower/right corner of the screen
            //
            final LinearLayout bgLayout = findViewById(R.id.record_bg_layout);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bgLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                            gapColor));
                    mRecordTV.setTypeface(null, Typeface.NORMAL);
                    mRecordTV.setText(showNow);
                    bgLayout.startAnimation(AnimationUtils
                            .loadAnimation(getApplicationContext(), R.anim.blink));
                }
            });

            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bgLayout.setBackgroundColor(ContextCompat.getColor
                                    (getApplicationContext(),
                                    R.color.black));
                            mRecordTV.setText(showAfter);
                        }
                    });
                }
            }, 8000);   // 8s

            return isNewRecord;
        }
    }

    /**
     * Update session and ghost fragments.
     */
    private void updateSession() {
        List<Laptime> laps = new ArrayList<>(mLapMap.values());
        SessionAdapter adapter = new SessionAdapter(getApplicationContext(), laps);
        mSessionFragment.update(adapter);

        // Inform GhostFragment
        mGhostFragment.updateLapList(mParser.lapNum, mLapMap);
    }

    /**
     * Reset when exiting to main menu. Resets parsers and GUI.
     */
    private void hardReset() {
        mParser47 = new Parser47();
        mParser = new Parser();
        mLaptime = new Laptime(0);
        mLapMap = new TreeMap<>();
        // Inform adapters that car/track is unknown (highlight purposes)
        mLiveDbFragment.informCursorAdapters(null, null, 0);
        // Inform SessionFragment that record is unknown (highlight purposes)
        mSessionFragment.setRecord(null);
        // Reset GhostFragment session
        mGhostFragment.hardReset();
        // GUI reset
        mNLapTV.setText("-");
        mCarTrackComboTV.setText(getResources().getString(R.string.waiting_car_info));
        mCarTrackComboTV.setTextColor(ContextCompat.getColor
                (getApplicationContext(), R.color.grey_900));
        mCarTrackComboTV.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                R.color.waiting_car_track_info));
        mRecordTV.setTypeface(Typeface.MONOSPACE);
        mRecordTV.setText("--:--:---");
//        Log.d(TAG, "--------------------- HARD reset");
    }

    /**
     * Restart session. Do not delete mParser47. Do not update car and track GUI.
     */
    private void softReset() {
        mParser = new Parser();
        mLaptime = new Laptime(0);
        mLapMap = new TreeMap<>();
        // GUI reset
        mNLapTV.setText("-");
        // Reset GhostFragment session
        mGhostFragment.softReset();
//        Log.d(TAG, "--------------------- SOFT reset");
    }

    /**
     * Get broadcast address.
     *
     * @return String
     */
    private String getBroadcastAddress() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context
                .CONNECTIVITY_SERVICE);
        if (connMgr == null)
            return null;

        String bcast = null;
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            InetAddress inet = getInetAddress();
            if (inet != null) {
                bcast = inet.getHostAddress();
            }
        }

        return bcast;
    }

    /**
     * Get InetAdress.
     *
     * @return InetAddress
     */
    private InetAddress getInetAddress() {
        InetAddress inetAddress;
        try {
            Enumeration<NetworkInterface> nic = NetworkInterface.getNetworkInterfaces();

            while (nic.hasMoreElements()) {
                NetworkInterface singleInterface = nic.nextElement();
                String interfaceName = singleInterface.getName();
                if (interfaceName.contains("wlan0") || interfaceName.contains("eth0")) {
                    for (InterfaceAddress interfaceAddress : singleInterface
                            .getInterfaceAddresses()) {
                        inetAddress = interfaceAddress.getBroadcast();
                        if (inetAddress != null) {
                            return inetAddress;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Menu method implementation
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_mainl file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean sound = sharedPref.getBoolean(getResources().getString(R.string.pref_sound_key),
                true);
        MenuItem soundItem = menu.findItem(R.id.sound_cb);
        soundItem.setChecked(sound);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            case R.id.sound_cb:
                mSoundOn = !item.isChecked();
                item.setChecked(mSoundOn);
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.pref_sound_key), mSoundOn);
                editor.apply();
                if (mSoundOn) {
                    Toast.makeText(getApplicationContext(), "Sound ON", Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(getApplicationContext(), "Sound OFF", Toast.LENGTH_SHORT)
                            .show();
                }
//                Log.d(TAG, "mSound: " + mSoundOn);
                return true;
            case R.id.action_reset_session:
                softReset();
                updateSession();
                return true;
            case R.id.action_edit_database:
                if (mParser.gameState > 1) {
                    Toast.makeText(getApplicationContext(), R.string.db_locked, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Intent intent = new Intent(this, DBActivity.class);
                    startActivity(intent);
                }
                return true;

            case R.id.action_settings:
                if (mParser.gameState > 1) {
                    Toast.makeText(getApplicationContext(), R.string.settings_locked, Toast
                            .LENGTH_SHORT)
                            .show();
                } else {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                }
                return true;
            case R.id.action_help: {
                showInfoDialog(R.string.help);
                return true;
            }
            case R.id.action_about:
                showInfoDialog(R.string.about);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Insert [amount] of dummy laptimes in session (for debugging purposes)
     */
    private void insertDummyInSession(int amount) {
        // Dummy session adapter
        ArrayList<Laptime> list = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < amount; i++) {
            float s1 = random.nextFloat() * 30 + 10;
            float s2 = random.nextFloat() * 30 + 10;
            float s3 = random.nextFloat() * 30 + 10;
            float total = s1 + s2 + s3;
            float[] time = {total, s1, s2, s3};
            Laptime lap = new Laptime(i + 1, time, false);
            list.add(lap);
        }
        SessionAdapter adapter = new SessionAdapter(getApplicationContext(), list);
        mSessionFragment.update(adapter);
    }

    /**
     * Shows HTML text in an Information Dialog.
     */
    private void showInfoDialog(int messageID) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        // fromHtml deprecated for Android N and higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            builder.setMessage(Html.fromHtml(getString(messageID),
                    Html.FROM_HTML_MODE_LEGACY));
        } else {
            builder.setMessage(Html.fromHtml(getString(messageID)));
        }
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing
            }
        });
        builder.create().show();
    }

}


