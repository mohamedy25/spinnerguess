package com.example.spinnerguess;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ClientDatabase extends SQLiteOpenHelper {
    public ClientDatabase(Context context) {
        super(context, "clients.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE clients (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, balance REAL NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public long addClient(String name, double balance) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("balance", balance);
        return getWritableDatabase().insert("clients", null, values);
    }

    public List<ClientRecord> getClients() {
        List<ClientRecord> clients = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("clients",
                new String[]{"_id", "name", "balance"}, null, null, null, null, "_id ASC")) {
            while (cursor.moveToNext()) {
                clients.add(new ClientRecord(cursor.getLong(0), cursor.getString(1), cursor.getDouble(2)));
            }
        }
        return clients;
    }

    public ClientRecord getClient(long id) {
        try (Cursor cursor = getReadableDatabase().query("clients",
                new String[]{"_id", "name", "balance"}, "_id = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return new ClientRecord(cursor.getLong(0), cursor.getString(1), cursor.getDouble(2));
            }
        }
        return null;
    }

    public void updateBalance(long id, double balance) {
        ContentValues values = new ContentValues();
        values.put("balance", balance);
        getWritableDatabase().update("clients", values, "_id = ?", new String[]{String.valueOf(id)});
    }
}