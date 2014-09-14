/**
 * 
 */
package com.alertavert.android.applications.receipts.connectivity;

import java.net.URI;
import java.util.Collection;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.alertavert.android.applications.receipts.R;
import com.alertavert.receiptscan.model.Receipt;

/**
 * @author marco
 * 
 */
public class GenericLogListener implements ReceiptsSenderListener {

  private Context ctx;

  public GenericLogListener(Activity context) {
    this.ctx = context;
  }

  @Override
  public void onSend(Sender sender, Collection<Receipt> receipt) {
    Toast.makeText(ctx, R.string.on_send, Toast.LENGTH_SHORT);
  }

  @Override
  public void onFailure(Sender sender, Collection<Receipt> receipts, URI destination, Throwable ex) {
    StringBuilder msgBuilder = new StringBuilder();

    msgBuilder.append("Failed when sending ")
            .append((receipts == null ? "" : "" + receipts.size())).append(" receipts to ")
            .append(destination != null ? destination.toString() : "unknown")
            .append(" - original cause was: ")
            .append(ex != null ? ex.getLocalizedMessage() : "unknown");
    Log.e(ctx.getResources().getString(R.string.TAG), msgBuilder.toString(), ex);
    String text = String.format(ctx.getResources().getString(R.string.on_send_fail),
            destination.toString());

    Toast.makeText(ctx, text, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onSuccess(Sender sender, Collection<Receipt> receipt, URI destination) {
    Toast.makeText(ctx, R.string.on_send_ok, Toast.LENGTH_SHORT).show();
  }
}
