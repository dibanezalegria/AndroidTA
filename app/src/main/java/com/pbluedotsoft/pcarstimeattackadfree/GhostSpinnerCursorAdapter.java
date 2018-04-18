package com.pbluedotsoft.pcarstimeattackadfree;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
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
public class GhostSpinnerCursorAdapter extends CursorAdapter {

    private String mActualCar;


    public GhostSpinnerCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.ghost_spinner_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView ghostNameTv = view.findViewById(R.id.ghost_name_tv);
        int carColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_CAR);
        String name = cursor.getString(carColIndex);
        ghostNameTv.setText(name);
        if (name.equals(mActualCar)) {
            ghostNameTv.setTextColor(ContextCompat.getColor(context, R.color.indigo_50));
        }
     }

    /**
     *
     * @param car - actual car
     */
    public void setActualCar(String car) {
        mActualCar = car;
    }
}
