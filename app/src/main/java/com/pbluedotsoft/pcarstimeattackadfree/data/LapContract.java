package com.pbluedotsoft.pcarstimeattackadfree.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by daniel on 3/03/18.
 *
 */
public final class LapContract {

    // Empty constructor prevents accidental instantiation.
    private LapContract() {}

    /**
     * Constant values for Content Provider.
     */
    public static final String CONTENT_AUTHORITY = "com.pbluedotsoft.pcarstimeattackadfree";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_LAPTIMES = "laptimes";

    /**
     * Inner class that defines constant values for the laptimes database table.
     * Each entry in the table represents a single laptime.
     */
    public static final class LapEntry implements BaseColumns {
        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of pets.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LAPTIMES;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single pet.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LAPTIMES;

        /** The content URI to access the laptime data in the provider */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_LAPTIMES);

        /** Table name */
        public static final String TABLE_NAME = "laptimes";

        /** Table column names */
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_LAP_TRACK = "track";
        public static final String COLUMN_LAP_CAR= "car";
        public static final String COLUMN_LAP_CLASS = "class";
        public static final String COLUMN_LAP_NLAPS = "nlaps";
        public static final String COLUMN_LAP_S1 = "s1";
        public static final String COLUMN_LAP_S2 = "s2";
        public static final String COLUMN_LAP_S3 = "s3";
        public static final String COLUMN_LAP_TIME = "time";

    }
}
