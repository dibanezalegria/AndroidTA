package com.pbluedotsoft.pcarstimeattackadfree;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract;
import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract.LapEntry;

import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by daniel on 10/04/18.
 *
 * GhostFragment
 */

public class GhostFragment extends Fragment implements LoaderManager
        .LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener {

    private static final String TAG = GhostFragment.class.getSimpleName();

    private String mActualCar, mActualTrack;

    /**
     * Identifier for data loader
     */
    private static final int GHOST_LOADER = 200;

    /**
     * Cursor adapter
     */
    private GhostSpinnerCursorAdapter mGhostSpinnerCursorAdapter;

    /**
     * TextViews
     */
    TextView mLapNumTV;
    TextView[] mCur, mGhostA, mGhostB;
    TextView[] mGapA, mGapB;

    /**
     * Spinner
     */
    private AppCompatSpinner mGhostSpinnerA;
    private AppCompatSpinner mGhostSpinnerB;

    /**
     * Fragment
     */
    private static GhostFragment mInstance;

    /**
     * TreeMap
     */
    TreeMap<Integer, Laptime> mLapMap;

    /**
     * LAST LAP needs to know the actual lap
     */
    private int mActualLap;

    /**
     * GhostB selects actual car only the first time the spinner gets populated
     */
    private boolean mFirstTimeSelectionDone;



    public GhostFragment() {
        mCur = new TextView[4];
        mGhostA = new TextView[4];
        mGhostB = new TextView[4];
        mGapA = new TextView[4];
        mGapB = new TextView[4];
    }

    public static GhostFragment getInstance() {
        if (mInstance == null) {
            mInstance = new GhostFragment();
        }
        return mInstance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * RestrictedApi removes warning from setSupportBackgroundTintList
     *
     */
    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ghost_fragment, container, false);

        // SpinnerA
        mGhostSpinnerA = view.findViewById(R.id.ghost_spinnerA);
        mGhostSpinnerA.setSupportBackgroundTintList(ContextCompat.getColorStateList(getContext(),
                R.color.indigo_300));
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_values, R.layout.ghost_spinner_item);
        mGhostSpinnerA.setAdapter(adapter);
        mGhostSpinnerA.setOnItemSelectedListener(this);

        // SpinnerB
        mGhostSpinnerB = view.findViewById(R.id.ghost_spinnerB);
        mGhostSpinnerB.setSupportBackgroundTintList(ContextCompat.getColorStateList(getContext(),
                R.color.indigo_300));
        mGhostSpinnerCursorAdapter = new GhostSpinnerCursorAdapter(getContext(), null);
        mGhostSpinnerB.setAdapter(mGhostSpinnerCursorAdapter);
        mGhostSpinnerB.setOnItemSelectedListener(this);

        // Current Lap
        mLapNumTV = view.findViewById(R.id.lap_num_tv);
        mCur[0] = view.findViewById(R.id.current_laptime_tv);
        mCur[1] = view.findViewById(R.id.current_s1_tv);
        mCur[2] = view.findViewById(R.id.current_s2_tv);
        mCur[3] = view.findViewById(R.id.current_s3_tv);

        // Ghosts
        mGhostA[0] = view.findViewById(R.id.ghostA_laptime_tv);
        mGhostA[1] = view.findViewById(R.id.ghostA_s1_tv);
        mGhostA[2] = view.findViewById(R.id.ghostA_s2_tv);
        mGhostA[3] = view.findViewById(R.id.ghostA_s3_tv);

        mGhostB[0] = view.findViewById(R.id.ghostB_laptime_tv);
        mGhostB[1] = view.findViewById(R.id.ghostB_s1_tv);
        mGhostB[2] = view.findViewById(R.id.ghostB_s2_tv);
        mGhostB[3] = view.findViewById(R.id.ghostB_s3_tv);

        // Gaps
        mGapA[0] = view.findViewById(R.id.gapA_laptime_tv);
        mGapA[1] = view.findViewById(R.id.gapA_s1_tv);
        mGapA[2] = view.findViewById(R.id.gapA_s2_tv);
        mGapA[3] = view.findViewById(R.id.gapA_s3_tv);

        mGapB[0] = view.findViewById(R.id.gapB_laptime_tv);
        mGapB[1] = view.findViewById(R.id.gapB_s1_tv);
        mGapB[2] = view.findViewById(R.id.gapB_s2_tv);
        mGapB[3] = view.findViewById(R.id.gapB_s3_tv);

        // Start loader
        getActivity().getSupportLoaderManager().initLoader(GHOST_LOADER, null, this);

        // Reset TextViews
        hardReset();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /**
     * Methods for the LoaderCallBacks
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case GHOST_LOADER:
                if (mActualCar == null)
                    mActualTrack = "noTrack";
                String sel = LapContract.LapEntry.COLUMN_LAP_TRACK + " LIKE ?";
                String[] selArgs = new String[]{mActualTrack};
                String order = LapContract.LapEntry.COLUMN_LAP_CAR + " ASC";
                return new CursorLoader(getContext(),
                        LapEntry.CONTENT_URI,
                        null,
                        sel,
                        selArgs,
                        order);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case GHOST_LOADER:
                int counter = 0;
                int index = -1;
                // MatrixCursor seems to need a projection with id
                String[] projection = {LapContract.LapEntry._ID, LapContract.LapEntry
                        .COLUMN_LAP_CAR};
                MatrixCursor newCursor = new MatrixCursor(projection);
                if (cursor.getCount() > 0) {
                    if (cursor.moveToFirst()) {
                        mGhostSpinnerCursorAdapter.setActualCar(mActualCar);    // highlight
                        do {
                            String car = cursor.getString(cursor.getColumnIndex(LapContract.LapEntry
                                    .COLUMN_LAP_CAR));
                            if (car.equals(mActualCar))
                                index = counter;

                            newCursor.addRow(new Object[]{
                                    cursor.getInt(cursor.getColumnIndex(LapContract.LapEntry._ID)),
                                    car
                            });
                            counter++;
                        } while (cursor.moveToNext());
                    }
                } else {
                    newCursor.addRow(new Object[]{0, getString(R.string.waiting)});
                }

                mGhostSpinnerCursorAdapter.swapCursor(newCursor);

                // Highlight actual car
                if (index > -1 && !mFirstTimeSelectionDone) {
                    mGhostSpinnerB.setSelection(index);
                    mFirstTimeSelectionDone = true;
                }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case GHOST_LOADER:
                mGhostSpinnerCursorAdapter.swapCursor(null);
                break;
        }
    }

    /**
     * Spinner listener methods implementation.
     */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()) {
            case R.id.ghost_spinnerA:
                updateGhost(0);
                updateGaps(0);
                break;
            case R.id.ghost_spinnerB:
                updateGhost(1);
                updateGaps(1);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    /**
     * Update s1, s2, s3 and laptime TextViews for Ghosts A and B.
     *
     * @param ghostID - 0 updates ghostA TextViews, 1 updates ghostB TextViews
     */
    private void updateGhost(int ghostID) {
        // Go ahead only if fragment initialized
        if (getActivity() == null)
            return;

        switch(ghostID) {
            case 0:
                // Spinner A
                if (mGhostSpinnerA == null || mGhostSpinnerA.getSelectedItem() == null)
                    return;

                switch (mGhostSpinnerA.getSelectedItem().toString()) {
                    case "LAST LAP":
                        // Last lap
                        if (mLapMap == null)
                            return;

                        Laptime last = mLapMap.get(mActualLap - 1);
                        if (last == null)
                            return;

                        if (last.time > 0)
                            for (int i = 0; i < 4; i++)
                                mGhostA[i].setTypeface(null, Typeface.NORMAL);

                        mGhostA[0].setText(Laptime.format(last.time));
                        mGhostA[1].setText(Laptime.format(last.s1));
                        mGhostA[2].setText(Laptime.format(last.s2));
                        mGhostA[3].setText(Laptime.format(last.s3));
                        break;
                    case "BEST IN SESSION":
                        // Best lap
                        if (mLapMap == null)
                            return;

                        float bestLap = Float.MAX_VALUE;
                        float[] ftimes = new float[4];
                        for (Laptime lap : mLapMap.values()) {
                            if (!lap.invalid && lap.time < bestLap && lap.time > 0) {
                                bestLap = lap.time;
                                ftimes[0] = lap.time;
                                ftimes[1] = lap.s1;
                                ftimes[2] = lap.s2;
                                ftimes[3] = lap.s3;
                            }
                        }

                        if (bestLap < Float.MAX_VALUE)
                            for (int i = 0; i < 4; i++)
                                mGhostA[i].setTypeface(null, Typeface.NORMAL);

                        mGhostA[0].setText(Laptime.format(ftimes[0]));
                        mGhostA[1].setText(Laptime.format(ftimes[1]));
                        mGhostA[2].setText(Laptime.format(ftimes[2]));
                        mGhostA[3].setText(Laptime.format(ftimes[3]));
                        break;
                    case "ALL TIME BEST":
                        // Record lap
                        if (mActualCar == null) {
                            for (int i = 0; i < 4; i++) {
                                mGhostA[i].setTypeface(Typeface.MONOSPACE);
                                mGhostA[i].setText("--:--.---");
                            }
                            return;
                        }

                        String selection = LapEntry.COLUMN_LAP_TRACK + " LIKE ? AND " +
                                LapEntry.COLUMN_LAP_CAR + " LIKE ?";
                        String[] selArgs = new String[]{mActualTrack, mActualCar};
                        Cursor cursor = getActivity().getContentResolver().query(LapEntry.CONTENT_URI,
                                null,
                                selection,
                                selArgs,
                                null);

                        ftimes = new float[4];
                        if (cursor != null && cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            ftimes[0] = cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME));
                            ftimes[1] = cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_S1));
                            ftimes[2] = cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_S2));
                            ftimes[3] = cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_S3));
                            for (int i = 0; i < 4; i++)
                                mGhostA[i].setTypeface(null, Typeface.NORMAL);
                        }
                        if (cursor != null && !cursor.isClosed())
                            cursor.close();

                        mGhostA[0].setText(Laptime.format(ftimes[0]));
                        mGhostA[1].setText(Laptime.format(ftimes[1]));
                        mGhostA[2].setText(Laptime.format(ftimes[2]));
                        mGhostA[3].setText(Laptime.format(ftimes[3]));
                        break;
                }
                break;

            case 1:
                // Spinner B
                if (mGhostSpinnerB == null || mGhostSpinnerB.getSelectedItem() == null ||
                        mGhostSpinnerB.getSelectedItem().toString()
                                .equals(getString(R.string.waiting))) {
                    return;
                }

                String carSelected = ((MatrixCursor) mGhostSpinnerB.getSelectedItem())
                        .getString(1);

                String selection = LapEntry.COLUMN_LAP_TRACK + " LIKE ? AND " +
                        LapEntry.COLUMN_LAP_CAR + " LIKE ?";
                String[] selArgs = new String[]{mActualTrack, carSelected};
                Cursor cursor = getActivity().getContentResolver().query(LapEntry.CONTENT_URI,
                        null,
                        selection,
                        selArgs,
                        null);

                float[] ftimes = new float[4];
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    ftimes[0] = cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME));
                    ftimes[1] = cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_S1));
                    ftimes[2] = cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_S2));
                    ftimes[3] = cursor.getFloat(cursor.getColumnIndex(LapEntry.COLUMN_LAP_S3));
                    for (int i = 0; i < 4; i++)
                        mGhostB[i].setTypeface(null, Typeface.NORMAL);
                }
                if (cursor != null && !cursor.isClosed())
                    cursor.close();

                mGhostB[0].setText(Laptime.format(ftimes[0]));
                mGhostB[1].setText(Laptime.format(ftimes[1]));
                mGhostB[2].setText(Laptime.format(ftimes[2]));
                mGhostB[3].setText(Laptime.format(ftimes[3]));

                break;

            case 2:
                updateGhost(0);
                updateGhost(1);
                break;
        }
    }

    /**
     * Set car/track combo. If car/track null then reset views.
     *
     * @param car - current car
     * @param track - current track
     */
    public void setCarTrackCombo(String car, String track) {
        mActualCar = car;
        mActualTrack = track;
        // getActivity null after main activity's hard reset (?)
        if (getActivity() != null) {
            getActivity().getSupportLoaderManager().restartLoader(GHOST_LOADER, null, this);
        }

        updateGhost(2);
    }

    /**
     * Soft reset when MainActivity softReset
     */
    public void softReset() {
        mActualLap = 0;
        mLapMap = null;

        // Go ahead only if fragment initialized
        if (getActivity() == null)
            return;

        mLapNumTV.setText("CURRENT LAP");

        for (int i = 0; i < 4; i++) {
            // Soft reset keeps RECORD. Reset only LAST and BEST
            if (!mGhostSpinnerA.getSelectedItem().toString().equals("ALL TIME BEST")) {
                mGhostA[i].setTypeface(Typeface.MONOSPACE);
                mGhostA[i].setText("--:--.---");
            }

            mCur[i].setTypeface(Typeface.MONOSPACE);
            mCur[i].setText("--:--.---");

            mGapA[i].setText("");
            mGapB[i].setText("");

            mGapA[i].setBackgroundColor(ContextCompat.getColor(getContext(), R.color.black));
            mGapB[i].setBackgroundColor(ContextCompat.getColor(getContext(), R.color.black));
        }
    }

    /**
     * Hard reset when MainActivity hardReset
     */
    public void hardReset() {
        mActualCar = null;
        mActualTrack = null;
        mFirstTimeSelectionDone = false;
        softReset();
        // Go ahead only if fragment initialized
        if (getActivity() != null) {
            for (int i = 0; i < 4; i++) {
                mGhostA[i].setTypeface(Typeface.MONOSPACE);
                mGhostB[i].setTypeface(Typeface.MONOSPACE);
                mGhostA[i].setText("--:--.---");
                mGhostB[i].setText("--:--.---");
            }

            getActivity().getSupportLoaderManager().restartLoader(GHOST_LOADER, null, this);
        }
    }

    /**
     * Update current lap, ghosts and gaps
     *
     * @param lapNum - current lap number
     * @param lapMap - laps in current session
     */
    public void updateLapList(int lapNum, TreeMap<Integer, Laptime> lapMap) {
        // Go ahead only if fragment initialized
        if (getActivity() == null)
            return;

        mActualLap = lapNum;
        mLapMap = lapMap;
        final Laptime currLap = lapMap.get(lapNum);
        final Laptime prevLap = lapMap.get(lapNum - 1);

        //
        // Update current lap
        //
        if (currLap == null)
            return;

        if (currLap.s1 > 0 && currLap.s2 == 0) {
            mCur[1].setTypeface(null, Typeface.NORMAL);
            mCur[1].setText(Laptime.format(currLap.s1));

        } else if (currLap.s2 > 0 && currLap.s3 == 0) {
            mCur[2].setTypeface(null, Typeface.NORMAL);
            mCur[2].setText(Laptime.format(currLap.s2));

        } else if (currLap.s1 == 0) {
            if (prevLap == null) {
                mLapNumTV.setText(String.format(Locale.ENGLISH, "LAP  %s",
                        currLap.lapNum));
                return;
            }
            // When a lap gets invalidated, sessionUpdate() in MainActivity calls this method.
            // That is a call that we are not interested in handling here.
            // fix: update s3 and time only when s2 is already set
            if (!mCur[2].getText().toString().equals("--:--.---")) {
                mCur[3].setTypeface(null, Typeface.NORMAL);
                mCur[0].setTypeface(null, Typeface.NORMAL);
                mCur[3].setText(Laptime.format(prevLap.s3));
                mCur[0].setText(Laptime.format(prevLap.time));
                // Show sectors, laptime and gaps for 10s before clearing TextViews
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mLapNumTV.setText(String.format(Locale.ENGLISH, "LAP  %s",
                                        currLap.lapNum));
                                for (int i = 0; i < 4; i++) {
                                    mCur[i].setTypeface(Typeface.MONOSPACE);
                                    mCur[i].setText("--:--.---");
                                }
                                updateGhost(2);
                                updateGaps(2);      // reset gaps
                            }
                        });
                    }
                }, 8000);   // 8s
            }
        }

        //
        // Gaps
        //
        updateGaps(2);
    }

    /**
     * Update gap 0 (ghostA-current) and gap 1 (current-ghostB).
     *
     * @param gapID - 0 update ghostA-current, 1 update current-ghostB and 2 update both
     */
    private void updateGaps(int gapID) {
        // Go ahead only if fragment initialized
        if (getActivity() == null)
            return;

        switch (gapID) {
            case 0:
                // Update gap ghostA/current
                for (int i = 0; i < 4; i++) {
                    if (!mCur[i].getText().equals("--:--.---") && !mGhostA[i]
                            .getText().equals("--:--.---")) {

                        float gap = Laptime.getGap(timeToFloat(mCur[i].getText().toString()),
                                timeToFloat(mGhostA[i].getText().toString()));
                        if (gap > 0) {
                            mGapA[i].setBackgroundColor(ContextCompat.getColor(getContext(),
                                    R.color.gap_bg_color));
                        } else if (gap < 0){
                            mGapA[i].setBackgroundColor(ContextCompat.getColor(getContext(),
                                    R.color.bestSector));
                        } else {
                            mGapA[i].setBackgroundColor(ContextCompat.getColor(getContext(),
                                    R.color.black));
                        }

                        if (gap != 0) {
                            mGapA[i].setText(Laptime.getGapStr(
                                    timeToFloat(mCur[i].getText().toString()),
                                    timeToFloat(mGhostA[i].getText().toString())));
                        } else
                            mGapA[i].setText("-");

                    } else {
                        mGapA[i].setText("");
                        mGapA[i].setBackgroundColor(ContextCompat.getColor(getContext(),
                                R.color.black));
                    }
                }
                break;
            case 1:
                // Update gap current/ghostB
                for (int i = 0; i < 4; i++) {
                    if (!mCur[i].getText().equals("--:--.---") && !mGhostB[i]
                            .getText().equals("--:--.---")) {

                        float gap = Laptime.getGap(timeToFloat(mCur[i].getText().toString()),
                                timeToFloat(mGhostB[i].getText().toString()));
                        if (gap > 0) {
                            mGapB[i].setBackgroundColor(ContextCompat.getColor(getContext(),
                                    R.color.gap_bg_color));
                        } else if (gap < 0){
                            mGapB[i].setBackgroundColor(ContextCompat.getColor(getContext(),
                                    R.color.bestSector));
                        } else {
                            mGapB[i].setBackgroundColor(ContextCompat.getColor(getContext(),
                                    R.color.black));
                        }

                        if (gap != 0) {
                            mGapB[i].setText(Laptime.getGapStr(
                                    timeToFloat(mCur[i].getText().toString()),
                                    timeToFloat(mGhostB[i].getText().toString())));
                        } else
                            mGapB[i].setText("-");

                    } else {
                        mGapB[i].setText("");
                        mGapB[i].setBackgroundColor(ContextCompat.getColor(getContext(),
                                R.color.black));
                    }
                }
                break;
            case 2:
                updateGaps(0);
                updateGaps(1);
                break;
        }
    }

    /**
     * Converts time in string format into seconds
     *
     * @param str - string format [--:--.---]
     * @return  time in seconds
     */
    private float timeToFloat(String str) {
        if (str.equals("--:--.---"))
            return 0;

        float time = 0;
        try {
            int min = Integer.parseInt(str.substring(0, 2));
            int sec = Integer.parseInt(str.substring(3, 5));
            int mil = Integer.parseInt(str.substring(6, 9));
            time = TimeUnit.MINUTES.toMillis(min) + TimeUnit.SECONDS.toMillis(sec) + mil;
        } catch (Exception ex) {
            Log.d(TAG, "Parsing error.");
        }

        return time / 1000;
    }

}

