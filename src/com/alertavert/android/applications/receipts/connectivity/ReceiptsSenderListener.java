// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts.connectivity;


import java.net.URI;
import java.util.Collection;

import com.alertavert.receiptscan.model.Receipt;


public interface ReceiptsSenderListener {

  /**
   * Invoked by the {@link Sender} just before sending the {@link Receipt}
   * 
   * @param sender 
   * @param receipt
   */
  public void onSend(Sender sender, Collection<Receipt> receipts);

  /**
   * Invoked if there is an error either whilst sending, or an error value is returned by
   * the destination server, or if an exception was thrown.
   * 
   * @param sender
   * @param receipt
   * @param destination
   * @param exception optional, can be {@code null}
   */
  public void onFailure(Sender sender, Collection<Receipt> receipts, URI destination,
      Throwable exception);
	
  /**
   * Invokde after successful completion of send
   * 
   * @param sender
   * @param receipt
   * @param destination
   */
  public void onSuccess(Sender sender, Collection<Receipt> receipts, URI destination);
}
