package com.pbluedotsoft.pcarstimeattackadfree;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract;

/**
 * Created by daniel on 18/03/18.
 *
 */

public class LiveDbFragment extends Fragment implements LoaderManager
        .LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener {

    private static final String TAG = LiveDbFragment.class.getSimpleName();

    /**
     * Identifiers for the data loaders.
     */
    private static final int LAPTIME_LOADER = 0;    // laptimes
    private static final int TRACK_LOADER = 1;      // track spinner
    private static final int CAR_CLASS_LOADER = 2;  // car class spinner

    private static LiveDbFragment mInstance;

    /**
     * ListView swaps between cursor adapters depending on track selection (trackSpinner).
     * <p>
     * mLapCursorAdapter for "all tracks"
     * mLapPlusCursorAdapter with best sectors and gaps for when one specific track is selected.
     */
    private ListView mLapListView;

    /**
     * CursorAdapters
     */
    private LapCursorAdapter mLapCursorAdapter;             // all tracks no time differences
    private LapPlusCursorAdapter mLapPlusCursorAdapter;     // one track with time differences
    private TrackSpinnerCursorAdapter mTrackSpinnerCursorAdapter;   // track spinner
    private ClassSpinnerCursorAdapter mClassSpinnerCursorAdapter;   // car class spinner

    /**
     * Emulates toggle behaviour for buttons. Toggle between ascending and descending sort order.
     */
    private String mCarSortOrder = "ASC";
    private String mNLapsSortOrder = "ASC";
    private String mTimeSortOrder = "ASC";

    /**
     * Spinners
     */
    private AppCompatSpinner mCarClassSpinner;
    private AppCompatSpinner mTrackSpinner;

    /**
     * Buttons and Spinners (when clicked) change selection, selectionArgs and sortOrder.
     * CursorLoaders use these parameters to perform different queries.
     */
    private String mSelection = null;
    private String[] mSelectionArgs = null;
    private String mSortOrder = null;   // ex. LapEntry.COLUMN_LAP_TIME + " " + mTimeSortOrder


    public LiveDbFragment() {
    }

    public static LiveDbFragment getInstance() {
        if (mInstance == null) {
//            Log.d(TAG, "getInstance()");
            mInstance = new LiveDbFragment();
        }
        return mInstance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
//        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.live_db_fragment, container, false);
        mLapListView = view.findViewById(R.id.laptimes_list_view);

        //
        //  ListView (laptimes)
        //
        mLapListView = view.findViewById(R.id.laptimes_list_view);
        mLapListView.setEmptyView(view.findViewById(R.id.layout_laptimes_empty_list));
        // no laptime data until the loader finishes so pass in null for the Cursor.
        mLapCursorAdapter = new LapCursorAdapter(getContext(), null);
        mLapPlusCursorAdapter = new LapPlusCursorAdapter(getContext(), null);
        mLapListView.setAdapter(mLapCursorAdapter);
        // start loader
        getActivity().getSupportLoaderManager().initLoader(LAPTIME_LOADER, null, this);

        //
        // Spinners
        //
        mTrackSpinner = view.findViewById(R.id.track_spinner);
        mTrackSpinnerCursorAdapter = new TrackSpinnerCursorAdapter(getContext(), null);
        mTrackSpinner.setAdapter(mTrackSpinnerCursorAdapter);
        mTrackSpinner.setOnItemSelectedListener(this);
        // start loader
        getActivity().getSupportLoaderManager().initLoader(TRACK_LOADER, null, this);

        mCarClassSpinner = view.findViewById(R.id.car_class_spinner);
        mClassSpinnerCursorAdapter = new ClassSpinnerCursorAdapter(getContext(), null);
        mCarClassSpinner.setAdapter(mClassSpinnerCursorAdapter);
        mCarClassSpinner.setOnItemSelectedListener(this);
        // start loader
        getActivity().getSupportLoaderManager().initLoader(CAR_CLASS_LOADER, null, this);

        //
        // Buttons (toggle between ASC and DESC sorting order)
        //
        Button carBtn = view.findViewById(R.id.car_btn);
        carBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCarSortOrder = (mCarSortOrder.equals("ASC")) ? "DESC" : "ASC";
                mSortOrder = LapContract.LapEntry.COLUMN_LAP_CAR + " " + mCarSortOrder;
                getActivity().getSupportLoaderManager().restartLoader(LAPTIME_LOADER, null,
                        LiveDbFragment.this);
            }
        });

        Button nlapsBtn = view.findViewById(R.id.nlaps_btn);
        nlapsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNLapsSortOrder = (mNLapsSortOrder.equals("ASC")) ? "DESC" : "ASC";
                mSortOrder = LapContract.LapEntry.COLUMN_LAP_NLAPS + " " + mNLapsSortOrder;
                getActivity().getSupportLoaderManager().restartLoader(LAPTIME_LOADER, null,
                        LiveDbFragment.this);
            }
        });

        Button timeBtn = view.findViewById(R.id.time_btn);
        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTimeSortOrder = (mTimeSortOrder.equals("ASC")) ? "DESC" : "ASC";
                mSortOrder = LapContract.LapEntry.COLUMN_LAP_TIME + " " + mTimeSortOrder;
                getActivity().getSupportLoaderManager().restartLoader(LAPTIME_LOADER, null,
                        LiveDbFragment.this);
            }
        });

        return view;
    }

    /**
     * Pass CursorAdapters info coming from MainActivity
     */
    public void informCursorAdapters(String car, String track, float last) {
        if (mLapCursorAdapter != null) {
            mLapCursorAdapter.setCurrentCarTrackCombo(car, track);
//            mLapCursorAdapter.setLastLaptime(last);
        }

        if (mLapPlusCursorAdapter != null) {
            mLapPlusCursorAdapter.setCurrentCar(car);
//            mLapPlusCursorAdapter.setLastLaptime(last);
        }

        // Refresh mLapListView must be done on the UI thread

//            Log.d(TAG, "invalidate list view - runOnUIThread");
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mLapListView != null)
                        mLapListView.invalidateViews();
                }
            });
        }
    }

    /**
     * Methods for the LoaderCallBacks
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        switch (id) {
            case LAPTIME_LOADER: {
//                Log.d(TAG, "------------------------------------- start onCreateLoader");
                String[] projection = {
                        LapContract.LapEntry._ID,
                        LapContract.LapEntry.COLUMN_LAP_TRACK,
                        LapContract.LapEntry.COLUMN_LAP_CAR,
                        LapContract.LapEntry.COLUMN_LAP_CLASS,
                        LapContract.LapEntry.COLUMN_LAP_NLAPS,
                        LapContract.LapEntry.COLUMN_LAP_S1,
                        LapContract.LapEntry.COLUMN_LAP_S2,
                        LapContract.LapEntry.COLUMN_LAP_S3,
                        LapContract.LapEntry.COLUMN_LAP_TIME};

//                Log.d(TAG, "------------------------------------- end OnCreateLoader");

                // This loader will execute the ContentProvider's query on a background thread
                return new CursorLoader(getContext(),
                        LapContract.LapEntry.CONTENT_URI,
                        projection,
                        mSelection,
                        mSelectionArgs,
                        mSortOrder);
            }
            case TRACK_LOADER: {
                String[] projection = {LapContract.LapEntry._ID, LapContract.LapEntry
                        .COLUMN_LAP_TRACK};
                return new CursorLoader(getContext(),
                        LapContract.LapEntry.CONTENT_URI,
                        projection,
                        null,
                        null,
                        LapContract.LapEntry.COLUMN_LAP_TRACK + " ASC");
            }
            case CAR_CLASS_LOADER: {
                String[] projection = {LapContract.LapEntry._ID, LapContract.LapEntry
                        .COLUMN_LAP_CLASS};
                return new CursorLoader(getContext(),
                        LapContract.LapEntry.CONTENT_URI,
                        projection,
                        null,
                        null,
                        LapContract.LapEntry.COLUMN_LAP_CLASS + " ASC");
            }
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LAPTIME_LOADER: {
//                Log.d(TAG, "------------------------------------- start onLoadFinished");
                // Update these adapters with this new cursor with updated laptime data
                mLapCursorAdapter.swapCursor(cursor);
                mLapPlusCursorAdapter.swapCursor(cursor);

                // Get fastest/slowest lap and sectors from displayed laptimes
                // Pass the times to LapPlusCursorAdapter
                String[] projection = {LapContract.LapEntry._ID,
                        LapContract.LapEntry.COLUMN_LAP_S1,
                        LapContract.LapEntry.COLUMN_LAP_S2,
                        LapContract.LapEntry.COLUMN_LAP_S3,
                        LapContract.LapEntry.COLUMN_LAP_TIME
                };
                Cursor c = null;
                try {
                    c = getActivity().getContentResolver().query(LapContract.LapEntry
                                    .CONTENT_URI,
                            projection,
                            mSelection,
                            mSelectionArgs,
                            LapContract.LapEntry.COLUMN_LAP_TIME + " ASC");

                    if (c != null && c.getCount() > 0 && mLapPlusCursorAdapter != null) {
                        c.moveToFirst();
                        float fastest = c.getFloat(c.getColumnIndex(LapContract.LapEntry
                                .COLUMN_LAP_TIME));
                        float bestS1 = getFastestSector(c, LapContract.LapEntry.COLUMN_LAP_S1);
                        float bestS2 = getFastestSector(c, LapContract.LapEntry.COLUMN_LAP_S2);
                        float bestS3 = getFastestSector(c, LapContract.LapEntry.COLUMN_LAP_S3);
                        mLapPlusCursorAdapter.setBestSectors(bestS1, bestS2, bestS3);
                        mLapPlusCursorAdapter.setFastestLap(fastest);
                        if (c.getCount() == 1) {
                            mLapPlusCursorAdapter.setSlowestLap(fastest);
                        } else {
                            c.moveToLast();
                            float slowest = c.getFloat(c.getColumnIndex(LapContract.LapEntry
                                    .COLUMN_LAP_TIME));
                            mLapPlusCursorAdapter.setSlowestLap(slowest);
                        }
                    }
                } catch (Exception ex) {
//                    Log.d(TAG, "Exception in onLoadFinished(): ");
                } finally {
                    if (c != null && !c.isClosed())
                        c.close();
                }
//                Log.d(TAG, "------------------------------------- end onLoadFinished");
                break;
            }
            case TRACK_LOADER: {
                if (cursor == null)
                    break;
                // Eliminate duplicates
                String[] projection = {LapContract.LapEntry._ID, LapContract.LapEntry
                        .COLUMN_LAP_TRACK};
                MatrixCursor newCursor = new MatrixCursor(projection); // Same projection used in
                // loader
                newCursor.addRow(new Object[]{0, "All tracks"});

                if (cursor.moveToFirst()) {
                    String trackName = "";
                    do {
                        if (cursor.getString(cursor.getColumnIndex(LapContract.LapEntry
                                .COLUMN_LAP_TRACK))
                                .compareToIgnoreCase(trackName) != 0) {
                            newCursor.addRow(new Object[]{
                                    cursor.getInt(cursor.getColumnIndex(LapContract.LapEntry._ID)),
                                    cursor.getString(cursor.getColumnIndex(LapContract.LapEntry
                                            .COLUMN_LAP_TRACK))
                            });
                            trackName = cursor.getString(cursor.getColumnIndex(LapContract.LapEntry
                                    .COLUMN_LAP_TRACK));
                        }
                    } while (cursor.moveToNext());
                }
                mTrackSpinnerCursorAdapter.swapCursor(newCursor);
                break;
            }
            case CAR_CLASS_LOADER: {
                if (cursor == null)
                    break;
                // Eliminate duplicates
                String[] projection = {LapContract.LapEntry._ID, LapContract.LapEntry
                        .COLUMN_LAP_CLASS};
                MatrixCursor newCursor = new MatrixCursor(projection); // Same projection used in
                // loader
                newCursor.addRow(new Object[]{0, "All classes"});
                if (cursor.moveToFirst()) {
                    String carClass = "";
                    do {
                        if (cursor.getString(cursor.getColumnIndex(LapContract.LapEntry
                                .COLUMN_LAP_CLASS))
                                .compareToIgnoreCase(carClass) != 0) {
                            newCursor.addRow(new Object[]{
                                    cursor.getInt(cursor.getColumnIndex(LapContract.LapEntry._ID)),
                                    cursor.getString(cursor.getColumnIndex(LapContract.LapEntry
                                            .COLUMN_LAP_CLASS))
                            });
                            carClass = cursor.getString(cursor.getColumnIndex(LapContract.LapEntry
                                    .COLUMN_LAP_CLASS));
                        }
                    } while (cursor.moveToNext());
                }
                mClassSpinnerCursorAdapter.swapCursor(newCursor);
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        switch (loader.getId()) {
            case LAPTIME_LOADER:
//                Log.d(TAG, "------------------------------------- start onLoaderReset");
                mLapCursorAdapter.swapCursor(null);
                mLapPlusCursorAdapter.swapCursor(null);
//                Log.d(TAG, "------------------------------------- end onLoaderReset");
                break;
            case TRACK_LOADER:
                mTrackSpinnerCursorAdapter.swapCursor(null);
                break;
            case CAR_CLASS_LOADER:
                mClassSpinnerCursorAdapter.swapCursor(null);
                break;
        }
    }

    /**
     * Spinner listener methods implementation.
     */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()) {
            case R.id.track_spinner: {
                if (i > 0) {
                    // Sort with gaps from fastest to slowest car
                    mTimeSortOrder = "ASC";
                    mSortOrder = LapContract.LapEntry.COLUMN_LAP_TIME + " " + mTimeSortOrder;
                    mLapListView.setAdapter(mLapPlusCursorAdapter);
                } else {
                    mLapListView.setAdapter(mLapCursorAdapter);
                }
                break;
            }
            case R.id.car_class_spinner: {
                // nothing
                break;
            }
        }

        // Spinners determine selection and selectionArgs
        String trackSelected = "%";
        if (mTrackSpinner.getSelectedItem() != null) {
            if (mTrackSpinner.getSelectedItemPosition() > 0)
                trackSelected = ((MatrixCursor) mTrackSpinner.getSelectedItem())
                        .getString(1);
        }

        String classSelected = "%";
        if (mCarClassSpinner.getSelectedItem() != null) {
            if (mCarClassSpinner.getSelectedItemPosition() > 0)
                classSelected = ((MatrixCursor) mCarClassSpinner.getSelectedItem())
                        .getString(1);
        }

        //
        // Use LIKE instead of '=' so '%' works as wildcard
        //
        mSelection = LapContract.LapEntry.COLUMN_LAP_TRACK + " LIKE ? AND " +
                LapContract.LapEntry.COLUMN_LAP_CLASS + " LIKE ?";
        mSelectionArgs = new String[]{trackSelected, classSelected};

        getActivity().getSupportLoaderManager().restartLoader(LAPTIME_LOADER, null, LiveDbFragment
                .this);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    /**
     * @param db_column Column name in database for sector
     * @return Fastest time for given sector
     */
    private float getFastestSector(Cursor cursor, String db_column) {
        cursor.moveToFirst();
        float best = Float.MAX_VALUE;
        do {
            float time = cursor.getFloat(cursor.getColumnIndex(db_column));
            if (time < best) {
                best = time;
            }
        } while (cursor.moveToNext());

        return best;
    }
}
