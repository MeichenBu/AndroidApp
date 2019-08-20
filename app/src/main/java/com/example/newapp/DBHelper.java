package com.example.newapp;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(MainActivity context){
        super(context,"DATA",null,5);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + Point.TABLE + "("
                +Point.ID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                +Point.BUILDING + " TEXT, "
                +Point.FLOOR + " TEXT, "
                +Point.LOC_X + " TEXT, "
                +Point.LOC_Y + " TEXT, "
                +Point.MAG_X + " TEXT, "
                +Point.MAG_Y + " TEXT, "
                +Point.MAG_Z + " TEXT, "
                +Point.G + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }


    public void insert(ContentValues values)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.insert("DATA", null, values);
        db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        //db.execSQL("DROP TABLE IF EXISTS "+ Point.TABLE);
        //onCreate(db);
    }
}
