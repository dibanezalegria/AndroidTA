package com.pbluedotsoft.pcarstimeattackadfree;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract;

/**
 * Created by daniel on 13/03/18.
 */

public class DBCursorAdapter extends CursorAdapter {

    private static final String TAG = DBCursorAdapter.class.getSimpleName();

    public DBCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_database, parent, false);
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
        TextView timeTv = view.findViewById(R.id.list_item_lap_time);

        // Find the columns we are interested in
        int idColIndex = cursor.getColumnIndex(LapContract.LapEntry._ID);
        int trackColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_TRACK);
        int carColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_CAR);
        int classColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_CLASS);
        int nlapsColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_NLAPS);
        int timeColIndex = cursor.getColumnIndex(LapContract.LapEntry.COLUMN_LAP_TIME);

        // Read laptime attributes from the cursor
        int id = cursor.getInt(idColIndex);
        String track = cursor.getString(trackColIndex);
        String car = cursor.getString(carColIndex);
        String carClass = cursor.getString(classColIndex);
        int nlaps = cursor.getInt(nlapsColIndex);
        float time = cursor.getFloat(timeColIndex);

        // Update TextViews
        idTv.setText(String.valueOf(id));
        trackTv.setText(track);
        carTv.setText(car);
        classTv.setText(carClass);
        nlapsTv.setText(String.valueOf(nlaps));
        timeTv.setText(Laptime.format(time));

        // Delete button
        Button btn = view.findViewById(R.id.delete_btn);
        final Context fcontext = context;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView idTV = ((LinearLayout)view.getParent()).findViewById(R.id.list_item_lap_id);
//                Log.d(TAG, "view: " + idTV.getText().toString());
                String selection = LapContract.LapEntry._ID + "=?";
                String[] selArgs = { idTV.getText().toString() };
                fcontext.getContentResolver().delete(LapContract.LapEntry.CONTENT_URI,
                        selection, selArgs);
            }
        });
    }

}
