// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts.ui;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import com.alertavert.android.applications.receipts.ControllerActivity;
import com.alertavert.android.applications.receipts.storage.ReceiptDAO;
import com.alertavert.android.applications.receipts.R;
import com.alertavert.receiptscan.model.Receipt;


/**
 * <h1>ReceiptsImagesAdapter</h1>
 * <p>
 * Required to enable the Gallery view for Receipts' images, each item is provided as a
 * {@link #THUMB_WIDTH} x {@link #THUMB_HEIGHT} thumbnail, obtained scaling appropriately the
 * original bitmap image.
 * 
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
public class ReceiptsImagesAdapter extends BaseAdapter {

  /**
   * Default thumbnail height
   */
  private static final int THUMB_HEIGHT = 100;

  /**
   * Default thumbnail width
   */
  protected static final int THUMB_WIDTH = 150;

  ReceiptDAO dao;
  List<Integer> imageIds;
  Context ctx;

  /**
   * @param ctx the application context that this adapter will running under
   * @param dao the Receipts' DAO that allow this adapter to retrieve for each item the associated
   *          receipt
   */
  public ReceiptsImagesAdapter(Context ctx, ReceiptDAO dao) {
    this.dao = dao;
    this.ctx = ctx;
  }

  /**
   * @see android.widget.Adapter#getCount()
   */
  @Override
  public int getCount() {
    return dao.getCount();
  }

  /**
   * @see android.widget.Adapter#getItem(int)
   */
  @Override
  public Object getItem(int position) {
    Log.d(ControllerActivity.TAG, "Adapter -- getItem at position " + position);
    int id = (int) getItemId(position);

    Log.d(ControllerActivity.TAG, "Adapter -- with ID: " + id);
    if (id == -1) {
      return null;
    }
    Receipt r = dao.retrieve(id);

    Log.d(ControllerActivity.TAG, "Adapter -- found receipt: " + (r == null ? "none" : r.getName()));
    return r;
  }

  /*
   * (non-Javadoc)
   * @see android.widget.Adapter#getItemId(int)
   */
  @Override
  public long getItemId(int position) {
    // lazy initialization for the IDs list
    if (imageIds == null) {
      imageIds = new ArrayList<Integer>(dao.getAll().keySet());
    }
    if (position < imageIds.size()) {
      return imageIds.get(position);
    }
    // throw new IllegalArgumentException("No images available for position " + position);
    return -1;
  }

  /*
   * (non-Javadoc)
   * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
   */
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    Receipt r = (Receipt) getItem(position);

    if (r == null) {
      Log.e(ControllerActivity.TAG, "Cannot find receipt for position " + position);
      return convertView;
    }
    ImageView view;

    if (convertView instanceof ImageView) {
      view = (ImageView) convertView;
    } else {
      view = new ImageView(ctx);
    }
    view.setLayoutParams(new Gallery.LayoutParams(THUMB_WIDTH, THUMB_HEIGHT));
    view.setScaleType(ImageView.ScaleType.CENTER_CROP);
    // we must compress the original bitmap to create a reasonably-sized thumbnail
    String filePath;

    if ((r.getImageUri() != null) && ((filePath = r.getImageUri().getPath()) != null)) {
      Log.d(ControllerActivity.TAG, "Trying to open image file: " + r.getImageUri().getPath());
      BitmapFactory.Options opts = new BitmapFactory.Options();

      opts.inSampleSize = 5;
      opts.inTargetDensity = DisplayMetrics.DENSITY_LOW;
      opts.inScaled = true;
      Bitmap original = BitmapFactory.decodeFile(filePath, opts);

      if (original == null) {
        Log.e(ControllerActivity.TAG, "File does not exist");
        view.setImageResource(R.drawable.no_pic);
        return view;
      }
      float aspectRatio = original.getWidth() / original.getHeight();

      Log.d(ControllerActivity.TAG, "Aspect ratio: " + aspectRatio);
      if (aspectRatio == 0) {
        aspectRatio = 16.0f / 9.0f;
      }
      Bitmap thumbnail = Bitmap.createScaledBitmap(original, (int) (THUMB_HEIGHT * aspectRatio),
          THUMB_HEIGHT, true);

      view.setImageBitmap(thumbnail);
    } else {
      Log.e(ControllerActivity.TAG, "Found no valid file URI for " + r.getName());
      view.setImageResource(R.drawable.no_pic);
    }
    return view;
  }
}
