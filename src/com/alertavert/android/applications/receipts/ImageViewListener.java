// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts;


import com.alertavert.receiptscan.model.Receipt;


/**
 * <h1>ImageViewListener</h1>
 * 
 * <h3>Copyright AlertAvert.com (c) 2009. All rights reserved.</h3>
 *
 * @author m.massenzio@gmail.com (Marco Massenzio)
 *
 */
public interface ImageViewListener {
  public void accept(Receipt r);
  public void cancel();
}
