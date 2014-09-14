// Copyright Infinite Bandwidth ltd (c) 2010. All rights reserved.
// Created 11 Oct 2010, by M. Massenzio (marco@alertavert.com)

package com.alertavert.android.applications.receipts;


import java.util.Collection;

import com.alertavert.android.applications.receipts.R;
import com.alertavert.receiptscan.model.Receipt;

import android.content.Context;
import android.text.format.DateFormat;


/**
 * <h1>EmailBodyFormatter</h1>
 *
 * Takes care of formatting the body of the email in an application-transparent way.
 * 
 */
@SuppressWarnings("serial")
public class EmailBodyFormatter implements ReceiptsFormatter {

  private final String nameLbl;
  private final String timestampLbl;
  private final String amtLbl;
  private final String merchantLbl;
  private final String notesLbl;
  private final String separator;
  private final String fmt;

  /**
   * @param scanActivity
   */
  public EmailBodyFormatter(Context ctx) {
    nameLbl = ctx.getResources().getString(R.string.email_lbl_name);
    timestampLbl = ctx.getResources().getString(R.string.email_lbl_timestamp);
    amtLbl = ctx.getResources().getString(R.string.email_lbl_amount);
    merchantLbl = ctx.getResources().getString(R.string.email_lbl_merchant);
    notesLbl = ctx.getResources().getString(R.string.email_lbl_notes);
    separator = ctx.getResources().getString(R.string.email_lbl_separator);
    fmt = ctx.getResources().getString(R.string.email_lbl_timestamp_fmt);
  }

  /* (non-Javadoc)
   * @see com.alertavert.android.applications.receipts.ReceiptsFormatter#format(java.util.Collection)
   */
  @Override
  public String format(Collection<Receipt> receipts) {
    StringBuffer sb = new StringBuffer();

    for (Receipt r:receipts) {
      sb.append(nameLbl).append(": ").append(r.getName()).append(
          System.getProperty("line.separator"));
      sb.append(timestampLbl).append(": ").append(DateFormat.format(fmt, r.getTimestamp())).append(
          System.getProperty("line.separator"));
      sb.append(amtLbl).append(": ").append(r.getAmount().toStringWithCurrency()).append(
          System.getProperty("line.separator"));
      sb.append(merchantLbl).append(": ").append(r.getMerchant()).append(
          System.getProperty("line.separator"));
      if (r.getNotes().length() > 0) {
        sb.append("--\n" + notesLbl + "\n--\n").append(r.getNotes()).append(
            System.getProperty("line.separator"));
      }
      sb.append(separator).append(System.getProperty("line.separator"));
    }
    return sb.toString();
  }

}
