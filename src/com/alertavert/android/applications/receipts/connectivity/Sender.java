// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts.connectivity;


import com.alertavert.receiptscan.model.Receipt;

import java.net.URI;
import java.util.Collection;


/**
 * Generic sender interface, allows setting up a destination URL, and additional
 * extra options
 * 
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
public interface Sender {

  /**
   * The core of this interface, will send the given list of Receipts to the destination
   * that was previously set via {@link #setDestination(java.net.URI)}
   * 
   * @param receipts
   * @return {@code true} if successful
   */
  public abstract boolean send(Collection<Receipt> receipts);
  
  /**
   * Sets the destination for this sender, in URI form: see
   * http://java.sun.com/javase/6/docs/api/java/net/URI.html
   * 
   * @param dest a fully-formed URI, with optional query and fragment parameters, if required
   *     by the concrete sender to specify the final destination for the package
   */
  public abstract void setDestination(URI dest);
  
  /**
   * Generic option-setting method, for senders who require further configuration options: this
   * is optional, and if not implemented, may simply return {@code false}
   * <p>
   * Options are simply represented by a (name, value) pairs.
   * 
   * @param name
   * @param value
   * @return {@code true} if the option was set successfully.
   */
  public abstract boolean setSenderOption(String name, String value);
  
  /**
   * Optional convenience method
   * 
   * @return a user-friendly description of the failure
   * @see #send(Collection)
   */
  public abstract String getFailureReason();
  
  public void addSenderListener(ReceiptsSenderListener listener);
  public void removeSenderListener(ReceiptsSenderListener listener);
}
