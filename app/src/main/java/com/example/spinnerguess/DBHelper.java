package com.example.spinnerguess;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASENAME = "result.db";
    private static final String TABLE_RECORD = "tblresult";
    private static final int DATABASEVERSION = 2; // تم رفع الإصدار لتحديث الجدول وإضافة كلمة المرور

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_SCORE = "score";
    private static final String COLUMN_PASSWORD = "password"; // عمود كلمة المرور الجديد

    private static final String[] allColumns = {COLUMN_ID, COLUMN_NAME, COLUMN_SCORE, COLUMN_PASSWORD};

    private static final String CREATE_TABLE_USER = "CREATE TABLE IF NOT EXISTS " +
            TABLE_RECORD + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_NAME + " TEXT," +
            COLUMN_SCORE + " INTEGER," +
            COLUMN_PASSWORD + " TEXT);";

    private SQLiteDatabase database; // access to table

    public DBHelper(@Nullable Context context) {
        super(context, DATABASENAME, null, DATABASEVERSION);
    }

    // creating the database
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE_USER);
    }

    // in case of version upgrade -> new schema
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORD);
        onCreate(sqLiteDatabase);
    }

    // Insert user with password
    public ModelUser insert(ModelUser user) {
        database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, user.getUserName());
        values.put(COLUMN_SCORE, user.getScore());
        values.put(COLUMN_PASSWORD, user.getPassword());
        long id = database.insert(TABLE_RECORD, null, values);
        user.setId(id);
        database.close();
        return user;
    }

    public void deleteUser(ModelUser user) {
    }

    public void deleteById(long id) {
        database = getWritableDatabase();
        database.delete(TABLE_RECORD, COLUMN_ID + " = " + id, null);
        database.close();
    }

    // Update user including password
    public void update(ModelUser user) {
        database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, user.getId());
        values.put(COLUMN_NAME, user.getUserName());
        values.put(COLUMN_SCORE, user.getScore());
        values.put(COLUMN_PASSWORD, user.getPassword());
        database.update(TABLE_RECORD, values, COLUMN_ID + "=" + user.getId(), null);
        database.close();
    }

    // Return all rows in table
    public ArrayList<ModelUser> selectAll() {
        database = getReadableDatabase();
        ArrayList<ModelUser> users = new ArrayList<>();
        String sortOrder = COLUMN_SCORE + " DESC";
        Cursor cursor = database.query(TABLE_RECORD, allColumns, null, null, null, null, sortOrder);
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
                int score = cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE));
                String password = cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD));
                long id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
                ModelUser user = new ModelUser(name, score, id, password);
                users.add(user);
            }
        }
        cursor.close();
        database.close();
        return users;
    }

    public ArrayList<ModelUser> genericSelectByUserName(String userName) {
        String[] vals = { userName };
        String column = COLUMN_NAME;
        return select(column, vals);
    }

    public ArrayList<ModelUser> select(String column, String[] values) {
        database = getReadableDatabase();
        ArrayList<ModelUser> users = new ArrayList<>();
        Cursor cursor = database.query(TABLE_RECORD, allColumns, column + " = ? ", values, null, null, null);
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
                int score = cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE));
                String password = cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD));
                long id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
                ModelUser user = new ModelUser(name, score, id, password);
                users.add(user);
            }
        }
        cursor.close();
        database.close();
        return users;
    }
    public ArrayList<ModelUser> getTop10Users() {
        database = getReadableDatabase();
        ArrayList<ModelUser> users = new ArrayList<>();
        String sortOrder = COLUMN_SCORE + " DESC LIMIT 10";
        Cursor cursor = database.query(TABLE_RECORD, allColumns, null, null, null, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
                int score = cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE));
                String password = cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD));
                long id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
                ModelUser user = new ModelUser(name, score, id, password);
                users.add(user);
            }
            cursor.close();
        }
        database.close();
        return users;
    }
}