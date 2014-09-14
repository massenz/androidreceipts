// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts.storage;


import com.alertavert.receiptscan.model.Receipt;

import java.util.Map;


/**
 * DAO for the Receipt objects, implements the CRUD pattern with the 
 * {@Link #store(Receipt)}, {@link #retrieve(int)}, {@link #remove(int)} methods ("Update" can
 * be achieved by either modifying the {@link java.util. Map Map<Integer, Receipt>} returned by
 * {@link #getAll()} or more simply, using the "{@code remove &amp; store} pattern.
 *  
 * @author m.massenzio@gmail.com (Marco Massenzio)
 *
 */
public interface ReceiptDAO {

  /**
   * Saves a receipt's image on semi-permanent storage (this will be lost when the user
   * closes the app).
   * 
   * @param receipt to be stored (image and metadata)
   * @return a unique id (for the session) identifying this particular item in storage
   */
  public abstract int store(Receipt receipt);

  /**
   * Retrieves a previously stored receipt.
   * 
   * @param id the unique ID returned by a previous call to {@link #store(Receipt)}
   * @return the previously stored receipt, or null if this is an invalid ID, or the receipt had
   *    been previously removed
   */
  public abstract Receipt retrieve(int id);

  /**
   * 
   * @return the total count of receipts currently stored
   */
  public abstract int getCount();

  /**
   * @return all the receipts stored, indexed by their unique IDs
   */
  public abstract Map<Integer, Receipt> getAll();
  
  /**
   * Removes the receipt corresponding to the unique ID from storage
   * 
   * @param id the unique ID returned by a previous call to {@link #store(Receipt)}
   * @return {@code true} if successful
   */
  public abstract boolean remove(int id);
  
  /**
   * Updates the receipt whose ID is {@code id} with the new values from {@code r}, if it exists.
   * No-op otherwise.
   * 
   * @param id
   * @param r
   */
  public abstract void update(int id, Receipt r);
}
