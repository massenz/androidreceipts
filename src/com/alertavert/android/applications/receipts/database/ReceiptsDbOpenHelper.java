// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

import com.alertavert.android.applications.receipts.R;


public class ReceiptsDbOpenHelper extends SQLiteOpenHelper {

  private static final int DATABASE_VERSION = 4;
  public static final String DATABASE_NAME = "receipts.db";
  public static final String TAG = "DB";

  protected Context context;

  public ReceiptsDbOpenHelper(Context context) {
    this(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  // @VisibleForTesting
  protected ReceiptsDbOpenHelper(Context ctx, String dbName, CursorFactory factory, int version) {
    super(ctx, dbName, factory, version);
    this.context = ctx;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    Log.d(TAG, "OnCreate");
    createTables(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.d(TAG, "OnUpgrade from rev " + oldVersion + " to rev. " + newVersion);
    // change the version number here, when you want the tables to be re-created
    if (newVersion > 1) {
      dropTables(db);
    }
    createTables(db);
  }

  private void createTables(SQLiteDatabase db) {
    String[] createTables = context.getResources().getStringArray(R.array.create_tables);

    for (String sql : createTables) {
      Log.d(TAG, sql);
      db.execSQL(sql);
    }
  }

  private void dropTables(SQLiteDatabase db) {
    String[] tableNames = context.getResources().getStringArray(R.array.tables);

    for (String table : tableNames) {
      Log.d(TAG, "Dropping table " + table);
      try {
        db.execSQL("DROP TABLE " + table);
      } catch (SQLiteException ex) {
        // most likely it was a table that was not there, should be safe to ignore
        Log.e("DB", "Exception thrown when dropping table " + table, ex);
      }
    }
  }
}
