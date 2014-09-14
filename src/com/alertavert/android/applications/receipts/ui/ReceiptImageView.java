// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts.ui;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.alertavert.android.applications.receipts.ControllerActivity;
import com.alertavert.android.applications.receipts.ImageViewListener;
import com.alertavert.android.applications.receipts.R;
import com.alertavert.receiptscan.model.Receipt;


/**
 * <h1>ReceiptImageView</h1>
 * <p>
 * Displays a single receipt's image and allows the user to accept or delete the image.
 * 
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
public class ReceiptImageView {
  private Activity activity;
  private Bitmap receiptImage;
  private ImageViewListener listener;
  private Receipt receipt;

  public ReceiptImageView(Activity activity, Receipt receipt) {
    this.activity = activity;
    this.receipt = receipt;
    if (receipt.getImageUri() != null) {
      BitmapFactory.Options opts = new BitmapFactory.Options();

      opts.inSampleSize = 4;
      opts.inScaled = true;
      opts.inTargetDensity = DisplayMetrics.DENSITY_LOW;
      this.receiptImage = BitmapFactory.decodeFile(receipt.getImageUri().getPath(), opts);
    } else {
      Log.e(ControllerActivity.TAG, "No URI for this receipt: " + receipt.getName());
    }
  }

  /**
   * @return the receiptImage
   */
  public Bitmap getReceiptImage() {
    return receiptImage;
  }

  /**
   * @param receiptImage the receiptImage to set
   */
  public void setReceiptImage(Bitmap receiptImage) {
    this.receiptImage = receiptImage;
  }

  /**
   * @return the listener
   */
  public ImageViewListener getListener() {
    return listener;
  }

  /**
   * @param listener the listener to set
   */
  public void setListener(ImageViewListener listener) {
    this.listener = listener;
  }

  public Receipt getReceipt() {
    return receipt;
  }

  public void setReceipt(Receipt r) {
    this.receipt = r;
  }

  public void show() {
    activity.setContentView(R.layout.show_receipt);
    ImageView img = (ImageView) activity.findViewById(R.id.receipt_img);

    img.setScaleType(ImageView.ScaleType.FIT_CENTER);
    img.setImageBitmap(receiptImage);
    Button accept_btn = (Button) activity.findViewById(R.id.accept_btn);

    accept_btn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (getListener() != null) {
          getListener().accept(getReceipt());
        }
      }
    });
    Button cancel_btn = (Button) activity.findViewById(R.id.cancel_btn);

    cancel_btn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (getListener() != null) {
          getListener().cancel();
        }
      }
    });
  }
}
