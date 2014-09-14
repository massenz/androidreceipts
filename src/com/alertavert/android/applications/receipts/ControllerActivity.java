// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

import com.alertavert.android.applications.receipts.database.ReceiptsDbOpenHelper;
import com.alertavert.android.applications.receipts.database.SqliteReceiptDao;
import com.alertavert.android.applications.receipts.storage.FileUtils;
import com.alertavert.android.applications.receipts.storage.ReceiptDAO;

/**
 * This is the main activity, and will also act as the application's Controller
 * 
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
public class ControllerActivity extends Activity implements OnClickListener {

  /** Application's Package name */
  public static final String ANDROID_PKG = "com.alertavert.android.applications.receipts";

  /** default tag for debug logs */
  public static String TAG;

  // Dialog IDs for the showDialog/onCreateDialog methods
  // @VisibleForTesting
  public static final int DIALOG_EMAIL_INVALID = 2;
  public static final int DIALOG_NO_RECEIPTS = 3;
  private static final int FILESYSTEM_NOT_AVAIL = 4;
  static final int USER_SETTINGS_ACTIVITY_CODE = 99;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TAG = getRS(R.string.TAG);
    Log.d(TAG, "Controller created, starting activity...");
    // This needs to be called first, or a runtime exception will be thrown:
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    initHomeView();
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.d(TAG, "Controller started...");
    checkFileSystem();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK && requestCode == USER_SETTINGS_ACTIVITY_CODE) {
      Log.d(TAG, "Settings saved");
      // TODO(marco): do we need to notify new settings?
    }
  }

  /**
   * Simple convenience method that retrieves a resource string from the id
   * 
   * @param id
   *          the resource ID
   * @return the corresponding string
   */
  private String getRS(int id) {
    return getResources().getString(id);
  }

  /**
   * Checks that the filesystem is available and ready to be used to
   * store/retrieve receipts' images; if it's not, it shows the user a Dialog
   * box warning.
   */
  public void checkFileSystem() {
    if (!FileUtils.isFilesystemAvailable()) {
      showDialog(FILESYSTEM_NOT_AVAIL);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.d(TAG, "Controller stopped");
  }

  private void initHomeView() {
    setContentView(R.layout.new_main);
    List<Integer> buttonIds = Arrays.asList(R.id.camera_btn, R.id.gallery_btn, R.id.settings_btn,
            R.id.user_btn);

    for (int id : buttonIds) {
      findViewById(id).setOnClickListener(this);
    }
  }

  public void takePicture() {
    Intent intent = new Intent();

    intent.setAction(getResources().getString(R.string.action_take_picture));
    startActivity(intent);
  }

  /**
   * Called to display a gallery of all the pictures taken so far, allowing the
   * user to manipulate them (<Strong>TODO</strong>), to post them to the email
   * address they have chosen at install, or to remove them all.
   */
  public void onDisplayReceipts() {
    ReceiptsDbOpenHelper helper = new ReceiptsDbOpenHelper(this);

    try {
      ReceiptDAO dao = new SqliteReceiptDao(this, helper.getWritableDatabase());

      if (dao.getCount() > 0) {
        Intent intent = new Intent();

        intent.setAction(getResources().getString(R.string.action_show_gallery));
        startActivity(intent);
      } else {
        showDialog(DIALOG_NO_RECEIPTS);
      }
    } finally {
      helper.close();
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    switch (id) {
    case DIALOG_EMAIL_INVALID:
      builder.setMessage(R.string.settings_invalid_msg).setCancelable(false)
              .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  showUserSettings();
                }
              }).setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                }
              });
      return builder.create();

    case DIALOG_NO_RECEIPTS:
      builder.setMessage(R.string.no_receipts).setCancelable(true)
              .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                }
              });
      return builder.create();

    case FILESYSTEM_NOT_AVAIL:
      builder.setMessage(R.string.no_filesystem).setCancelable(true)
              .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                }
              });
      return builder.create();

    default:
      Log.e(TAG, "Illegal dialog ID to show: " + id);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see android.view.View.OnClickListener#onClick(android.view.View)
   */
  @Override
  public void onClick(View btn) {
    switch (btn.getId()) {
    case R.id.camera_btn:
      takePicture();
      break;

    case R.id.gallery_btn:
      onDisplayReceipts();
      break;

    case R.id.settings_btn:
      showUserSettings();
      break;

    default:
      Log.e(TAG, "Unexpected click: " + btn.getId());
    }
  }

  /**
   * Starts the activity that will manage the user's settings
   */
  public void showUserSettings() {
    Intent intent = new Intent();

    intent.setAction(getResources().getString(R.string.action_show_settings));
    startActivityForResult(intent, USER_SETTINGS_ACTIVITY_CODE);
  }
}
