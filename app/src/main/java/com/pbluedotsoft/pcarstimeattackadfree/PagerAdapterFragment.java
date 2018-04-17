package com.pbluedotsoft.pcarstimeattackadfree;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;


/**
 * The viewpager's adapter.
 *
 */
public class PagerAdapterFragment extends FragmentPagerAdapter {

    private static final String TAG = PagerAdapterFragment.class.getSimpleName();

    private static final int PAGE_COUNT = 3;
    private final String[] mTabTitles;

    public PagerAdapterFragment(FragmentManager fm) {
        super(fm);
        mTabTitles = new String[] { "Session", "Live Database", "Ghost" };
    }

    @Override
    public Fragment getItem(int position) {
//        Log.d(TAG, "getItem");
        switch (position) {
            case 0:
                return SessionFragment.getInstance();
            case 1:
                return LiveDbFragment.getInstance();
            case 2:
                return GhostFragment.getInstance();
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabTitles[position];
    }

}
