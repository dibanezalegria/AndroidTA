package com.pbluedotsoft.pcarstimeattackadfree.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pbluedotsoft.pcarstimeattackadfree.data.LapContract.LapEntry;

import java.util.Arrays;

/**
 * Created by daniel on 3/03/18.
 *
 */
public class LapProvider extends ContentProvider {

    public static final String TAG = LapProvider.class.getSimpleName();

    /** URI matcher codes */
    private static final int LAPTIMES = 100;    // all rows from table
    private static final int LAPTIME_ID = 101;  // single row

    /** UriMatcher object to match a content URI to a corresponding code */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // Here there are all the URI patterns that the provider should recognize. All paths
        // added to the UriMatcher have a corresponding code to return when a match is found.
        sUriMatcher.addURI(LapContract.CONTENT_AUTHORITY, LapContract.PATH_LAPTIMES, LAPTIMES);
        sUriMatcher.addURI(LapContract.CONTENT_AUTHORITY, LapContract.PATH_LAPTIMES + "/#",
                LAPTIME_ID);
    }

    /** Database helper object */
    private LapDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new LapDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;
        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case LAPTIMES:
                cursor = database.query(LapEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case LAPTIME_ID:
                selection = LapEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                cursor = database.query(LapEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LAPTIMES:
                return LapEntry.CONTENT_LIST_TYPE;
            case LAPTIME_ID:
                return LapEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // todo: sanity check for values before inserting in database
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LAPTIMES:
                // Get writeable database
                SQLiteDatabase database = mDbHelper.getWritableDatabase();
                // Insert the new pet with the given values
                long id = database.insert(LapEntry.TABLE_NAME, null, values);
                // If the ID is -1, then the insertion failed. Log an error and return null.
                if (id == -1) {
                    Log.e(TAG, "Failed to insert row for " + uri);
                    return null;
                }

                // Notify all listeners that the data has changed for the pet content URI
                getContext().getContentResolver().notifyChange(uri, null);

                // Return the new URI with the ID (of the newly inserted row) appended at the end
                return ContentUris.withAppendedId(uri, id);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsDeleted;
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LAPTIMES:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(LapEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LAPTIME_ID:
                // Delete a single row given by the ID in the URI
                selection = LapEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(LapEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LAPTIMES:
                return updateLaptime(uri, values, selection, selectionArgs);
            case LAPTIME_ID:
                // Extract out the ID from the URI, so we know which row to update.
                // Selection will be "_id=?" and selection arguments will be a String array
                // containing the actual ID.
                selection = LapEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateLaptime(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update laptimes in the database with the given content values. Apply the changes to the
     * rows specified in the selection and selection arguments (which could be 0 or 1 or more pets).
     * Return the number of rows that were successfully updated.
     */
    private int updateLaptime(Uri uri, ContentValues values, String selection, String[]
            selectionArgs) {
        // todo: Sanity checks for all data. Example:
        if (values.containsKey(LapEntry.COLUMN_LAP_TRACK)) {
            String name = values.getAsString(LapEntry.COLUMN_LAP_TRACK);
            if (name == null) {
                throw new IllegalArgumentException("Laptime requires a track");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Returns the number of database rows affected by the update statement
        int rowsUpdated = database.update(LapEntry.TABLE_NAME, values, selection, selectionArgs);

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }


}
