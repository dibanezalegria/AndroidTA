package com.pbluedotsoft.pcarstimeattackadfree;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.MobileAds;
import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.AdRequest;


public class DBActivity extends AppCompatActivity implements LoaderManager
        .LoaderCallbacks<Cursor> {

    private static final String TAG = DBActivity.class.getSimpleName();
    private static final int DB_LAPTIME_LOADER = 10;            // laptimes
    private static final int REQUEST_EXTERNAL_STORAGE = 1000;   // permission

    private DBCursorAdapter mCursorAdapter;

    /**
     * Emulates toggle behaviour for buttons. Toggle between ascending and descending sort order.
     */
    private String mTrackSortOrder = "ASC";
    private String mCarSortOrder = "ASC";
    private String mClassSortOrder = "ASC";
    private String mNLapsSortOrder = "ASC";
    private String mTimeSortOrder = "ASC";

    private String mSortOrder = "nlaps ASC";   // LapEntry.COLUMN_LAP_TIME + " " + mTimeSortOrder

    private boolean mPermissionGranted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ListView listview = findViewById(R.id.laptimes_list_view);
        listview.setEmptyView(findViewById(R.id.layout_laptimes_empty_list));

        // No laptime data until the loader finishes so pass in null for the Cursor.
        mCursorAdapter = new DBCursorAdapter(this, null);
        listview.setAdapter(mCursorAdapter);
        getSupportLoaderManager().initLoader(DB_LAPTIME_LOADER, null, this);

        //
        // Buttons (toggle between ASC and DESC sorting order)
        //
        Button trackBtn = findViewById(R.id.track_btn);
        Button carBtn = findViewById(R.id.car_btn);
        Button classBtn = findViewById(R.id.car_class_button);
        Button nlapsBtn = findViewById(R.id.nlaps_btn);
        Button timeBtn = findViewById(R.id.time_btn);
        // Listeners
        trackBtn.setOnClickListener(new ButtonListener());
        carBtn.setOnClickListener(new ButtonListener());
        classBtn.setOnClickListener(new ButtonListener());
        nlapsBtn.setOnClickListener(new ButtonListener());
        timeBtn.setOnClickListener(new ButtonListener());



        //
        // Verify storage permissions needed for Moto3G (all mobile phones?)
        //
        verifyStoragePermissions();     // modifies mPermissionGranted flag
    }

    /**
     * Menu method implementation
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_main file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_database, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
//            case R.id.action_insert_dummy_data:
//                insertDummyLaptime();
//                return true;

            case R.id.action_delete_all_entries:
                deleteAllLaptimes();
                return true;

            case R.id.action_backup: {
                if (mPermissionGranted) {
                    JsonHelper jsonHelper = new JsonHelper(getApplicationContext());
                    String jsonStr = jsonHelper.backup(getApplicationContext());
                    if (jsonStr != null)
                        Toast.makeText(getApplicationContext(), R.string.backup_ok,
                                Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getApplicationContext(), R.string.backup_fail,
                                Toast.LENGTH_SHORT).show();

//                Log.d(TAG, "json: " + jsonStr);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.allow_access,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }

            case R.id.action_restore: {
                if (!mPermissionGranted) {
                    Toast.makeText(getApplicationContext(), R.string.allow_access,
                            Toast.LENGTH_SHORT).show();
                    break;
                }
                JsonHelper jsonHelper = new JsonHelper(getApplicationContext());
                final JSONArray jsonArray = jsonHelper.restore();
                if (jsonArray != null) {
                    //  Warning dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(DBActivity.this,
                            R.style.MyDialogTheme);
                    builder.setMessage(R.string.restore_warning)
                            .setPositiveButton("Yes, restore from file", new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int
                                                which) {
                                            final ProgressDialog dialog =
                                                    new ProgressDialog(new ContextThemeWrapper
                                                            (DBActivity.this,
                                                                    R.style.MyDialogTheme));
                                            dialog.setMessage("Loading...");
                                            dialog.show();
                                            // Background thread
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    restoreDatabase(jsonArray);
                                                    dialog.dismiss();
                                                }
                                            }).start();
                                        }
                                    })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                }
                            }).show();
                }
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Ask user for permission to read/write external storage and dialog response
     * gets handled by onRequestPermissionsResult()
     */
    public void verifyStoragePermissions() {
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,    // requires api 16+
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        // check if we have write permission
        int permission = ActivityCompat
                .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have automatic permission, so prompt the user.
            // onRequestPermissionsResult handles the answer from user.
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        } else {
            mPermissionGranted = true;
        }
    }

    /**
     * Handles user response to dialog ActivityCompat.requestPermissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissionGranted = true;
                } else {
                    mPermissionGranted = false;
                }
                break;
        }
    }

    /**
     * Restore database
     *
     * @param jsonArray - JSONArray contains array of laptimes in JSON format.
     */
    private boolean restoreDatabase(JSONArray jsonArray) {
        // Delete database
        deleteAllLaptimes();
        try {
            // JSON laptimes array to database
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonData = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put(LapContract.LapEntry.COLUMN_LAP_TRACK, jsonData
                        .getString("track"));
                values.put(LapContract.LapEntry.COLUMN_LAP_CAR, jsonData
                        .getString("car"));
                values.put(LapContract.LapEntry.COLUMN_LAP_CLASS, jsonData
                        .getString("carClass"));
                values.put(LapContract.LapEntry.COLUMN_LAP_NLAPS, jsonData
                        .getInt("nlaps"));
                values.put(LapContract.LapEntry.COLUMN_LAP_S1, Float
                        .parseFloat(jsonData
                                .getString("s1")));
                values.put(LapContract.LapEntry.COLUMN_LAP_S2, Float.parseFloat(jsonData
                        .getString("s2")));
                values.put(LapContract.LapEntry.COLUMN_LAP_S3, Float.parseFloat(jsonData
                        .getString("s3")));
                values.put(LapContract.LapEntry.COLUMN_LAP_TIME, Float
                        .parseFloat(jsonData
                                .getString("time")));

                Uri newUri = getContentResolver().insert(LapContract.LapEntry.CONTENT_URI, values);

//                Log.d(TAG, "Inserted uri: " + newUri);
            }
            return true;

        } catch (JSONException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Delete all laptimes in database.
     */
    private void deleteAllLaptimes() {
        int rowsDeleted = getContentResolver().delete(LapContract.LapEntry.CONTENT_URI, null, null);
//        Log.d(TAG, "rowsDeleted: " + rowsDeleted);
    }

    /**
     * Insert dummy laptime
     */
    private void insertDummyLaptime() {
        String[] tracks = {
                "Dummy track location"
        };

        String[] cars = {
                "Dummy car name"
        };

        String[] carClasses = {
                "Dummy class"
        };

//        // PERFORMANCE TEST: test inserts 10000 laptimes
//        for (int i = 0; i < 1000; i++) {
            String track = tracks[new Random().nextInt(tracks.length)];
            String car = cars[new Random().nextInt(cars.length)];
            String carClass = carClasses[new Random().nextInt(carClasses.length)];
            Random random = new Random();
            int laps = random.nextInt(50000);
            float s1 = random.nextFloat() * 80 + 10;
            float s2 = random.nextFloat() * 80 + 10;
            float s3 = random.nextFloat() * 80 + 10;
            float time = s1 + s2 + s3;

            ContentValues values = new ContentValues();
            values.put(LapContract.LapEntry.COLUMN_LAP_TRACK, track);
            values.put(LapContract.LapEntry.COLUMN_LAP_CAR, car);
            values.put(LapContract.LapEntry.COLUMN_LAP_CLASS, carClass);
            values.put(LapContract.LapEntry.COLUMN_LAP_NLAPS, laps);
            values.put(LapContract.LapEntry.COLUMN_LAP_S1, s1);
            values.put(LapContract.LapEntry.COLUMN_LAP_S2, s2);
            values.put(LapContract.LapEntry.COLUMN_LAP_S3, s3);
            values.put(LapContract.LapEntry.COLUMN_LAP_TIME, time);

            Uri newUri = getContentResolver().insert(LapContract.LapEntry.CONTENT_URI, values);
//            Log.d(TAG, "Inserted uri: " + newUri);
//        }
    }

    /**
     * LoaderManager.LoaderCallbacks method implementations.
     *
     */
    @Override
    @NonNull
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case DB_LAPTIME_LOADER:
                return new CursorLoader(this,
                        LapContract.LapEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        mSortOrder);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case DB_LAPTIME_LOADER:
                mCursorAdapter.swapCursor(cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case DB_LAPTIME_LOADER:
                mCursorAdapter.swapCursor(null);
                break;
        }
    }

    /**
     * Inner class ButtonListener
     */
    public class ButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            int buttonID = view.getId();
            switch (buttonID) {
                case R.id.track_btn:
                    mTrackSortOrder = (mTrackSortOrder.equals("ASC")) ? "DESC" : "ASC";
                    mSortOrder = LapContract.LapEntry.COLUMN_LAP_TRACK + " " + mTrackSortOrder;
                    break;
                case R.id.car_btn:
                    mCarSortOrder = (mCarSortOrder.equals("ASC")) ? "DESC" : "ASC";
                    mSortOrder = LapContract.LapEntry.COLUMN_LAP_CAR + " " + mCarSortOrder;
                    break;
                case R.id.car_class_button:
                    mClassSortOrder = (mClassSortOrder.equals("ASC")) ? "DESC" : "ASC";
                    mSortOrder = LapContract.LapEntry.COLUMN_LAP_CLASS + " " + mClassSortOrder;
                    break;
                case R.id.nlaps_btn:
                    mNLapsSortOrder = (mNLapsSortOrder.equals("ASC")) ? "DESC" : "ASC";
                    mSortOrder = LapContract.LapEntry.COLUMN_LAP_NLAPS + " " + mNLapsSortOrder;
                    break;
                case R.id.time_btn:
                    mTimeSortOrder = (mTimeSortOrder.equals("ASC")) ? "DESC" : "ASC";
                    mSortOrder = LapContract.LapEntry.COLUMN_LAP_TIME + " " + mTimeSortOrder;
                    break;
            }

            getSupportLoaderManager().restartLoader(DB_LAPTIME_LOADER, null, DBActivity.this);
        }
    }
}
