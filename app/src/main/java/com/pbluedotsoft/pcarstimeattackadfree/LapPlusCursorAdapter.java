package com.pbluedotsoft.pcarstimeattackadfree;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract.LapEntry;

/**
 * Created by daniel on 4/03/18.
 *
 * LapPlusCursorAdapter is an adapter for a ListView that uses a cursor of laptime data as its
 * source.
 * This adapter knows how to create list items for each row of laptime data in the cursor.
 *
 * Note: It is the same as LapCursorAdapter but includes a column with gaps instead of track names.
 *
 */
public class LapPlusCursorAdapter extends CursorAdapter {

    private static final String TAG = LapPlusCursorAdapter.class.getSimpleName();

    private float mFastestLap, mSlowestLap;
    private float mFastS1, mFastS2, mFastS3;
    private float mLastInSession;
    private String mCurrentCar;

    public LapPlusCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_livedb_plus, parent, false);
    }

    /**
     * This method binds the laptime data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current laptime can be set on the name
     * TextView in the list item layout.
     *
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Alternate background color
        int position = cursor.getPosition();
        int rowBgColor = (position % 2 == 0) ? R.color.listItemBgDark : R.color.listItemBgLight;
        view.setBackgroundColor(ContextCompat.getColor(view.getContext(), rowBgColor));

        TextView idTv = view.findViewById(R.id.list_item_lap_id);
        TextView carTv = view.findViewById(R.id.list_item_lap_car);
        TextView classTv = view.findViewById(R.id.list_item_lap_class);
        TextView nlapsTv = view.findViewById(R.id.list_item_lap_nlaps);
        TextView s1Tv = view.findViewById(R.id.list_item_lap_s1);
        TextView s2Tv = view.findViewById(R.id.list_item_lap_s2);
        TextView s3Tv = view.findViewById(R.id.list_item_lap_s3);
        final TextView timeTv = view.findViewById(R.id.list_item_lap_time);
        TextView diffTv = view.findViewById(R.id.list_item_lap_gap);

        // Find the columns we are interested in
        int idColIndex = cursor.getColumnIndex(LapEntry._ID);
//        int trackColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TRACK);
        int carColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_CAR);
        int classColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_CLASS);
        int nlapsColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_NLAPS);
        int s1ColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_S1);
        int s2ColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_S2);
        int s3ColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_S3);
        int timeColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME);

        // Read laptime attributes from the cursor
        int id = cursor.getInt(idColIndex);
//        String track = cursor.getString(trackColIndex);
        String car = cursor.getString(carColIndex);
        String carClass = cursor.getString(classColIndex);
        int nlaps = cursor.getInt(nlapsColIndex);
        float s1 = cursor.getFloat(s1ColIndex);
        float s2 = cursor.getFloat(s2ColIndex);
        float s3 = cursor.getFloat(s3ColIndex);
        float time = cursor.getFloat(timeColIndex);

        // Update TextViews
        idTv.setText(String.valueOf(id));
//        trackTv.setText(track);
        carTv.setText(car);
        classTv.setText(carClass);
        nlapsTv.setText(String.valueOf(nlaps));
        s1Tv.setText(Laptime.format(s1));
        s2Tv.setText(Laptime.format(s2));
        s3Tv.setText(Laptime.format(s3));
        timeTv.setText(Laptime.format(time));
        diffTv.setText(Laptime.getGapStr(time, mFastestLap));

        //
        // Best sectors drawing
        //
        if (getCount() > 1) {
            int colorId = (s1 == mFastS1) ? R.color.bestSector : rowBgColor;
            s1Tv.setBackgroundColor(ContextCompat.getColor(context, colorId));
            colorId = (s2 == mFastS2) ? R.color.bestSector : rowBgColor;
            s2Tv.setBackgroundColor(ContextCompat.getColor(context, colorId));
            colorId = (s3 == mFastS3) ? R.color.bestSector : rowBgColor;
            s3Tv.setBackgroundColor(ContextCompat.getColor(context, colorId));
        }

        //
        // Progress bar drawing
        //
        float gap = (time - mFastestLap) * 100 / (mSlowestLap - mFastestLap);
        ProgressBar bar = view.findViewById(R.id.progress_bar_gap);
        if (cursor.getCount() == 1) {
            bar.setProgress(0);
        } else {
            bar.setProgress((int)gap);
        }

        //
        //  Highlight current car
        //
        if (car.equals(mCurrentCar)) {
            carTv.setBackgroundColor(ContextCompat.getColor(view.getContext(),
                    R.color.record));
        } else {
            carTv.setBackgroundColor(ContextCompat.getColor(view.getContext(), rowBgColor));
        }

        // Blink laptime when best in session is fastest in database
//        if (car.equals(mCurrentCar) && time == mLastInSession) {
//            Log.d(TAG, "blink!");
//            timeTv.startAnimation(AnimationUtils.loadAnimation(context, R.anim.blink));
//        }
    }

    /**
     * Set fastest lap from the currently displayed laptimes after onLoadFinished in MainActivity.
     */
    public void setFastestLap(float time) {
        mFastestLap = time;
//        Log.d(TAG, "fastest UPDATED: " + Laptime.format(mFastestLap));
    }

    /**
     * Set slowest lap from the currently displayed laptimes after onLoadFinished in MainActivity.
     */
    public void setSlowestLap(float time) {
        mSlowestLap = time;
//        Log.d(TAG, "slowest UPDATED: " + Laptime.format(mSlowestLap));
    }

    /**
     * Set fastest sectors from currently displayed laptimes after onLoadFinished in MainActivity.
     * Sectors can belong to different laptimes.
     */
    public void setBestSectors(float s1, float s2, float s3) {
//        Log.d(TAG, "setBestSectors: " + s1 + " " + s2 +  " " + s3);
        mFastS1 = s1;
        mFastS2 = s2;
        mFastS3 = s3;
    }

    /**
     *
     * @param carName - car being driven right now
     */
    public void setCurrentCar(String carName) {
        mCurrentCar = carName;
    }

    /**
     * Used to run animation laptime blinking when last in session is as well fastest lap in
     * database for car/track combo.
     *
     * @param last - last laptime in session
     */
//    public void setLastLaptime(float last) {
//        Log.d(TAG, "last set: " + last);
//        mLastInSession = last;
//    }
}

