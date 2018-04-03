package com.pbluedotsoft.pcarstimeattackadfree;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract.LapEntry;

/**
 * Created by daniel on 8/03/18.
 *
 */
public class TrackSpinnerCursorAdapter extends CursorAdapter {


    public TrackSpinnerCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.track_spinner_item, parent, false);

    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView trackNameTv = view.findViewById(R.id.track_name_tv);
        int trackColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TRACK);
        String name = cursor.getString(trackColIndex);
        trackNameTv.setText(name);
    }
}
