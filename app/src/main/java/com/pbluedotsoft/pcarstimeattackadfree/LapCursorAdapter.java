package com.pbluedotsoft.pcarstimeattackadfree;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract.LapEntry;

/**
 * Created by daniel on 4/03/18.
 *
 * LapCursorAdapter is an adapter for a ListView that uses a cursor of laptime data as its source.
 * This adapter knows how to create list items for each row of laptime data in the cursor.
 *
 */
public class LapCursorAdapter extends CursorAdapter {

    private static final String TAG = LapCursorAdapter.class.getSimpleName();

    private String mCurrentCar, mCurrentTrack;
    private float mLastInSession;

    public LapCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_livedb, parent, false);
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
        TextView trackTv = view.findViewById(R.id.list_item_lap_track);
        TextView carTv = view.findViewById(R.id.list_item_lap_car);
        TextView classTv = view.findViewById(R.id.list_item_lap_class);
        TextView nlapsTv = view.findViewById(R.id.list_item_lap_nlaps);
        TextView s1Tv = view.findViewById(R.id.list_item_lap_s1);
        TextView s2Tv = view.findViewById(R.id.list_item_lap_s2);
        TextView s3Tv = view.findViewById(R.id.list_item_lap_s3);
        TextView timeTv = view.findViewById(R.id.list_item_lap_time);

        // Find the columns we are interested in
        int idColIndex = cursor.getColumnIndex(LapEntry._ID);
        int trackColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TRACK);
        int carColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_CAR);
        int classColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_CLASS);
        int nlapsColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_NLAPS);
        int s1ColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_S1);
        int s2ColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_S2);
        int s3ColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_S3);
        int timeColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME);

        // Read laptime attributes from the cursor
        int id = cursor.getInt(idColIndex);
        String track = cursor.getString(trackColIndex);
        String car = cursor.getString(carColIndex);
        String carClass = cursor.getString(classColIndex);
        int nlaps = cursor.getInt(nlapsColIndex);
        float s1 = cursor.getFloat(s1ColIndex);
        float s2 = cursor.getFloat(s2ColIndex);
        float s3 = cursor.getFloat(s3ColIndex);
        float time = cursor.getFloat(timeColIndex);

        // Update TextViews
        idTv.setText(String.valueOf(id));
        trackTv.setText(track);
        carTv.setText(car);
        classTv.setText(carClass);
        nlapsTv.setText(String.valueOf(nlaps));
        s1Tv.setText(Laptime.format(s1));
        s2Tv.setText(Laptime.format(s2));
        s3Tv.setText(Laptime.format(s3));
        timeTv.setText(Laptime.format(time));

        if (car.equals(mCurrentCar) && track.equals(mCurrentTrack)) {
            carTv.setBackgroundColor(ContextCompat.getColor(view.getContext(),
                    R.color.record));
        } else {
            carTv.setBackgroundColor(ContextCompat.getColor(view.getContext(),
                    rowBgColor));
        }

        // Make laptime blink when best in session is an all time record
//        if (car.equals(mCurrentCar) && time == mLastInSession) {
//            timeTv.startAnimation(AnimationUtils.loadAnimation(context, R.anim.blink));
//        }
    }

    /**
     *
     * @param carName - car/track combo being driven right now (highlighting purposes)
     */
    public void setCurrentCarTrackCombo(String carName, String track) {
        mCurrentCar = carName;
        mCurrentTrack = track;
    }

    /**
     * Used to run animation laptime blinking when last in session is as well fastest lap in
     * database for car/track combo.
     *
     * @param last - last laptime in session
     */
//    public void setLastLaptime(float last) {
//        mLastInSession = last;
//    }
}
