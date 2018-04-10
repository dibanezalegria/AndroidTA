package com.pbluedotsoft.pcarstimeattackadfree;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Daniel Ibanez on 2018-03-18.
 *
 * SessionAdapter - Adapter for mListView in SessionFragment
 */
public class SessionAdapter extends ArrayAdapter<Laptime> {

    private static final String TAG = SessionAdapter.class.getSimpleName();

    private static final int MAX_GAP = 15;    // Differences over MAX_GAP do not show in chart
    private int mMaxGap = MAX_GAP;
    // [laptime, s1, s2, s3]
    private float[] mBest = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
    private float mSlowest = 0; // used for gap calculation
    private Laptime mRecordLap;


    public SessionAdapter(@NonNull Context context, List<Laptime> lapList) {
        super(context, 0, lapList);

        // Extract best sectors and best/worst laptime from list of Laptime objects
        for (Laptime lap : lapList) {
            if (!lap.invalid) {
                // Laptime
                if (lap.time > 0 && lap.time < mBest[0]) {
                    mBest[0] = lap.time;
                }
                if (lap.time > mSlowest) {
                    mSlowest = lap.time;
                }
                // Sectors
                if (lap.s1 > 0 && lap.s1 < mBest[1]) {
                    mBest[1] = lap.s1;
                }
                if (lap.s2 > 0 && lap.s2 < mBest[2]) {
                    mBest[2] = lap.s2;
                }
                if (lap.s3 > 0 && lap.s3 < mBest[3]) {
                    mBest[3] = lap.s3;
                }
            }
        }
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
//        Log.d(TAG, "maxGap: " + mMaxGap);
        if (convertView == null) {
            convertView = (LayoutInflater.from(getContext()))
                    .inflate(R.layout.list_item_session, parent, false);
        }

        // Alternate row background color
        int rowBgColor = (position % 2 == 0) ? R.color.listItemBgDark : R.color.listItemBgLight;
        convertView.setBackgroundColor(ContextCompat.getColor(convertView.getContext(),
                rowBgColor));

        Laptime lap = getItem(position);
        if (lap == null) {
//            Log.d(TAG, "Error: creating view in adapter. Empty list?");
            return convertView;     // should never happen
        }

        TextView lapNumTV = convertView.findViewById(R.id.session_lapnumber_tv);
        TextView s1TV = convertView.findViewById(R.id.session_s1_tv);
        TextView s2TV = convertView.findViewById(R.id.session_s2_tv);
        TextView s3TV = convertView.findViewById(R.id.session_s3_tv);
        TextView lapTimeTV = convertView.findViewById(R.id.session_laptime_tv);
        TextView gapTV = convertView.findViewById(R.id.list_item_gap);

        lapNumTV.setText(String.format("%s", lap.lapNum));


        if (lap.invalid) {
            lapTimeTV.setTextColor(ContextCompat.getColor(getContext(), R.color.invalidLap));
        } else if (mRecordLap != null && lap.time == mRecordLap.time) {
            lapTimeTV.setTextColor(ContextCompat.getColor(getContext(), R.color.record));
        } else if (lap.time == mBest[0]) {
            lapTimeTV.setTextColor(ContextCompat.getColor(getContext(), R.color.bestTime));
        } else {
            lapTimeTV.setTextColor(ContextCompat.getColor(getContext(), R.color.whiteText));
        }

        // Normal font makes "--:--:---" look weird
        if (lap.time == 0) {
            lapTimeTV.setTypeface(Typeface.MONOSPACE);
        } else {
            lapTimeTV.setTypeface(null, Typeface.NORMAL);
        }

        lapTimeTV.setText(Laptime.format(lap.time));

        if (lap.s1 == 0)
            s1TV.setTypeface(Typeface.MONOSPACE);
        else
            s1TV.setTypeface(null, Typeface.NORMAL);

        s1TV.setText(Laptime.format(lap.s1));
        if (lap.s1 == mBest[1] && !lap.invalid) {
            if (mRecordLap != null && lap.s1 > 0 && lap.s1 <= mRecordLap.s1) {
                s1TV.setBackgroundResource(R.color.record);
            } else {
                s1TV.setBackgroundResource(R.color.bestSector);
            }
        } else {
            s1TV.setBackgroundResource(rowBgColor);
        }

        if (lap.s2 == 0)
            s2TV.setTypeface(Typeface.MONOSPACE);
        else
            s2TV.setTypeface(null, Typeface.NORMAL);

        s2TV.setText(Laptime.format(lap.s2));
        if (lap.s2 == mBest[2] && !lap.invalid) {
            if (mRecordLap != null && lap.s2 > 0 && lap.s2 <= mRecordLap.s2) {
                s2TV.setBackgroundResource(R.color.record);
            } else {
                s2TV.setBackgroundResource(R.color.bestSector);
            }
        } else {
            s2TV.setBackgroundResource(rowBgColor);
        }

        if (lap.s3 == 0)
            s3TV.setTypeface(Typeface.MONOSPACE);
        else
            s3TV.setTypeface(null, Typeface.NORMAL);

        s3TV.setText(Laptime.format(lap.s3));
        if (lap.s3 == mBest[3] && !lap.invalid) {
            if (mRecordLap != null && lap.s3 > 0 && lap.s3 <= mRecordLap.s3) {
                s3TV.setBackgroundResource(R.color.record);
            } else {
                s3TV.setBackgroundResource(R.color.bestSector);
            }
        } else {
            s3TV.setBackgroundResource(rowBgColor);
        }

        //
        // Progress bar drawing and gap text
        //
        ProgressBar bar = convertView.findViewById(R.id.progress_bar_gap);
        bar.setProgressDrawable(ContextCompat.getDrawable(getContext(),
                R.drawable.gap_progress_bar));
        gapTV.setGravity(Gravity.END);
        gapTV.setTextColor(ContextCompat.getColor(getContext(), R.color.whiteText));
        float progressPercentage;

        if (lap.time == 0) {
            progressPercentage = 0;
            gapTV.setText("-");
        } else if (lap.invalid) {
            gapTV.setText("invalid");
            bar.setProgressDrawable(ContextCompat.getDrawable(getContext(),
                    R.drawable.gap_progress_bar_invalid));
            gapTV.setGravity(Gravity.CENTER);
            progressPercentage = 100;
        } else if (mRecordLap != null && lap.time == mRecordLap.time) {
            bar.setProgressDrawable(ContextCompat.getDrawable(getContext(),
                    R.drawable.gap_progress_bar_record));
            gapTV.setText("new record");
            gapTV.setGravity(Gravity.CENTER);
            progressPercentage = 100;
        } else if (lap.time == mBest[0]) {
            bar.setProgressDrawable(ContextCompat.getDrawable(getContext(),
                    R.drawable.gap_progress_bar_dark));
            gapTV.setTextColor(ContextCompat.getColor(getContext(), R.color.bestTime));
            gapTV.setText("best in session");
            gapTV.setGravity(Gravity.CENTER);
            progressPercentage = 100;
        } else {
            // This formula calculates the gap we need to draw for the difference between each lap
            // and the best. Note that one very slow lap makes all the rest almost disappear on the
            // chart.
//            gap = (lap.time - mBest[0]) * 100 / (mSlowest - mBest[0]);
            // Instead use the maxGap value from preferences. Differences over maxGap do not show.
            progressPercentage = (Laptime.getGap(lap.time, mBest[0]) * 100) / mMaxGap;
            if (progressPercentage > 100) {
                progressPercentage = 100;
                gapTV.setText(String.format("> %ss", mMaxGap));
            } else {
                gapTV.setText(Laptime.getGapStr(lap.time, mBest[0]));
            }
        }

        bar.setProgress((int) progressPercentage);
        return convertView;
    }

    /**
     * Method called by MainActivity via SessionFragment helps highlighting record laptime.
     */
    public void setRecord(Laptime recordLap) {
        mRecordLap = recordLap;
//        Log.d(TAG, "setRecord: " + Laptime.format(record));
    }

    /**
     * Sets the maximum gap for the chart of time differences in the session tab.
     */
    public void setMaxGap(int gap) {
//        Log.d(TAG, "setMaxGap: " + gap);
        mMaxGap = gap;
    }
}
