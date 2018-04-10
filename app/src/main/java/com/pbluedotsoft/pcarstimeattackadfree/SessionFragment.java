package com.pbluedotsoft.pcarstimeattackadfree;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by Daniel Ibanez on 2018-03-18.
 *
 * SessionFragment - Together with LiveDbFragment, the two fragments for PagerAdapterFragment,
 * the adapter for the ViewPager.
 */
public class SessionFragment extends Fragment {

    private static final String TAG = SessionFragment.class.getSimpleName();
    private static SessionFragment mInstance;
    private ListView mListView;
    private Laptime mRecordLap;     // record lap gets passed to adapter when updating adapter
    private int mMaxGap;            // maximum gap for gap chart

    // Empty constructor
    public SessionFragment() {
//        Log.d(TAG, "SessionFragment constructor.");
    }

    public static SessionFragment getInstance() {
        if (mInstance == null) {
//            Log.d(TAG, "getInstance()");
            mInstance = new SessionFragment();
        }
        return mInstance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.d(TAG, "onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.session_fragment, container, false);
        mListView = view.findViewById(R.id.session_list_view);
        mListView.setEmptyView(view.findViewById(R.id.layout_laptimes_empty_list));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        Log.d(TAG, "OnDestroyView");
    }

    /**
     * Updates record in SessionAdapter and updates mListView's adapter.
     */
    public void update(ArrayAdapter<Laptime> adapter) {
        ((SessionAdapter) adapter).setMaxGap(mMaxGap);
        if (mRecordLap != null) {
            ((SessionAdapter) adapter).setRecord(mRecordLap);
        }
        if (mListView != null) {
            mListView.setAdapter(adapter);
        }
//        Log.d(TAG, "update -> adapter.setRecord and .setMaxGap -> listView.setAdapter");
    }

    /**
     * Updates record in SessionAdapter and invalidates mListView (refresh).
     */
    public void setRecord(Laptime recordLap) {
        mRecordLap = recordLap;
        // Late 1347 will update a buffered list of laptimes, one of them could be a record.
        // If we do not force mListView to refresh, it might continue showing laptime as best in
        // session instead of all time record.
        // Refreshing mLapListView must be done on the UI thread
        if (mListView != null) {
            SessionAdapter adapter = (SessionAdapter) mListView.getAdapter();
            if (adapter != null) {
                adapter.setRecord(recordLap);
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mListView != null)
                                mListView.invalidateViews();
                        }
                    });
                }
            }
//            Log.d(TAG, "setRecord -> adapter.setRecord -> invalidate list view");
        }
    }

    /**
     * Sets the maximum gap for the chart of time differences in the session tab.
     * MainActivity reads value from preferences in onResume.
     */
    public void setMaxGap(int gap) {
        mMaxGap = gap;
    }

}

