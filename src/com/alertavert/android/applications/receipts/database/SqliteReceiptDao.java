// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts.database;


import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.alertavert.android.applications.receipts.storage.FileUtils;
import com.alertavert.android.applications.receipts.storage.ReceiptDAO;
import com.alertavert.android.applications.receipts.R;
import com.alertavert.receiptscan.model.Money;
import com.alertavert.receiptscan.model.Receipt;


/**
 * <h1>SqliteReceiptDao</h1>
 * <p>
 * A DAO that operates on the SQLite internal Android database.
 * <p>
 * Currently ignores the IMAGE_NAME column from the database, as we are using the
 * {@link FileUtils#fromName(String)} utility method.
 * 
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
public class SqliteReceiptDao implements ReceiptDAO {
  public static final int INVALID_ID = -1;
  public static final String TAG = "DB";
  private Context context;
  SQLiteDatabase db;
  final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");

  public SqliteReceiptDao(Context context, SQLiteDatabase db) {
    this.context = context;
    this.db = db;
  }

  /**
   * Convenience method that retrieves a string from the application's Resources. No error checking,
   * or even validation, is effected on the id: however, the {@link Resources.NotFoundException
   * NotFoundException} is caught, and simply a null value is returned.
   * 
   * @param id a Resource identifier (typically one of the {@link R.string}.*)
   * @return the string resource, or {@code null} if the id does not identify a valid one
   */
  protected String getQueryString(int id) {
    try {
      return context.getResources().getString(id);
    } catch (NotFoundException ex) {
      Log.e(TAG, "Resource id (" + id + ") not found.", ex);
      return null;
    }
  }

  /**
   * @return
   */
  public List<Receipt> findAll() {
    List<Receipt> results = new ArrayList<Receipt>();
    Cursor c = db.rawQuery(getQueryString(R.string.find_all), null);

    while (c.moveToNext()) {
      int id = c.getInt(0);

      results.add(retrieve(id));
    }
    return results;
  }

  /**
   * Augments the {@link ReceiptDAO} interface by adding the ability to search the receipt simply by
   * name; because NAME is defined as UNIQUE and we do not use wildcards ('%') in the WHERE clause,
   * there can only be one (or zero) matches.
   * 
   * @param name the name associated with the image
   * @return a receipt matching then given name (exactly) or {@code null}
   */
  public Receipt findByName(String name) {
    String sql = getQueryString(R.string.find_by_name);
    Cursor c = db.rawQuery(sql, new String[] { name });

    if (c.moveToNext()) {
      int id = c.getInt(0);

      Log.d(TAG, "Found " + id + " for receipt named " + name);
      return retrieve(id);
    }
    return null;
  }

  /**
   * Creates a receipt from a query result of the find_by_id SQL query:<pre>
   *   SELECT ID, NAME, EXPENSE_DATE, MONEY_AMT, CURRENCY, MERCHANT, NOTES, IMG_URI 
   *   FROM RECEIPTS WHERE ID=?;</pre>
   * 
   * @param c the query cursor, already positioned on the first (or subsequent) row
   * @return a fully-formed Receipt object, with the query result's values initialized
   */
  protected Receipt createFromCursor(Cursor c) {
    Receipt r = new Receipt();

    r.setId(c.getInt(0));
    r.setName(c.getString(1));
    String dateAsString = c.getString(2);

    try {
      r.setTimestamp(fmt.parse(dateAsString));
      float amt = c.getFloat(3);
      String cur = c.getString(4);

      r.setAmount(Money.parse(amt, cur));
    } catch (ParseException e) {
      Log.e(TAG, "Could not parse from DB cursor (" + e.getMessage() + ")", e);
    }
    r.setMerchant(c.getString(5));
    r.setNotes(c.getString(6));
    r.setImageUri(URI.create(c.getString(7)));
    Log.d(TAG, "Cursor at position " + c.getPosition() + " of " + c.getCount() + "--> " + r);
    return r;
  }

  /**
   * Utility method, builds an array of parameters from the passed in Receipt
   * 
   * @param r the Receipt whose internal data we want to store/update
   * @return an array containing the values of the parameters as the SQL queries expect them to be
   */
  protected Object[] constructParametersArrayFromReceipt(Receipt r) {
    return new Object[] {
      r.getName(), fmt.format(r.getTimestamp()), r.getImageUri().toString(),
      r.getAmount().getFloatValue(), r.getAmount().getCurrency(), r.getMerchant(), r.getNotes()
    };
  }
  
  /*
   * (non-Javadoc)
   * @see com.google.android.applications.receiptscan.storage.ReceiptDAO#getAll()
   */
  @Override
  public Map<Integer, Receipt> getAll() {
    List<Receipt> receipts = findAll();
    final Map<Integer, Receipt> result = new HashMap<Integer, Receipt>();

    for (Receipt r : receipts) {
      result.put(r.getId(), r);
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * @see com.google.android.applications.receiptscan.storage.ReceiptDAO#getCount()
   */
  @Override
  public int getCount() {
    return findAll().size();
  }

  /*
   * (non-Javadoc)
   * @see com.google.android.applications.receiptscan.storage.ReceiptDAO#remove(int)
   */
  @Override
  public boolean remove(int id) {
    String sql = getQueryString(R.string.delete_by_id);

    db.execSQL(sql, new Object[] { id });
    if (retrieve(id) == null) {
      return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * @see com.google.android.applications.receiptscan.storage.ReceiptDAO#retrieve(int)
   */
  @Override
  public Receipt retrieve(int id) {
    Cursor c = db.rawQuery(getQueryString(R.string.find_by_id), new String[] { "" + id });

    if (c.moveToNext()) {
      return createFromCursor(c);
    } else {
      return null;
    }
  }
  
  /**
   * Inserts a new Receipt into the database, returning the unique ID associated with the object.
   * 
   * @param receipt
   * @return the database ID for the newly inserted receipt, or {@link #INVALID_ID} if the insertion
   *         failed.
   */
  @Override
  public int store(Receipt receipt) {
    String sql = getQueryString(R.string.insert_receipt);

    db.execSQL(sql, constructParametersArrayFromReceipt(receipt));
    Receipt stored = findByName(receipt.getName());

    if (stored != null) {
      return stored.getId();
    }
    return INVALID_ID;
  }

  /*
   * (non-Javadoc)
   * @see com.google.android.applications.receiptscan.storage.ReceiptDAO#update(int,
   * com.google.android.applications.receiptscan.Receipt)
   */
  @Override
  public void update(int id, Receipt r) {
    String query = getQueryString(R.string.update_by_id);

    Log.d("DB", "Executing " + query + " with " + r + ", ID (" + id + ")");
    Object[] paramsNoId = constructParametersArrayFromReceipt(r);
    Object[] params = new Object[paramsNoId.length + 1];
    int i = 0;

    for (Object o:paramsNoId) {
      params[i++] = o;
    }
    params[paramsNoId.length] = id;
    db.execSQL(query, params);
    
    // TODO (marco) this is only here as I can't access the device's database with sqlite3
    Log.d(TAG, "Post update, this is a findAll() on the DB:");
    for (Receipt r1 : findAll()) { 
      Log.d(TAG, "\t" + r1.toString());
    }
  }
}
