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

public class ClassSpinnerCursorAdapter extends CursorAdapter {

    public ClassSpinnerCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.class_spinner_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView carClassTv = view.findViewById(R.id.car_class_tv);
        int carClassColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_CLASS);
        String name = cursor.getString(carClassColIndex);
        carClassTv.setText(name);
    }
}
