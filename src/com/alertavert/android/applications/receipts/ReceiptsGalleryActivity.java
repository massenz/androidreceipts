// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts;


import java.io.File;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.Spinner;
import android.widget.Toast;

import com.alertavert.android.applications.receipts.connectivity.GenericLogListener;
import com.alertavert.android.applications.receipts.connectivity.HttpSender;
import com.alertavert.android.applications.receipts.connectivity.MailSender;
import com.alertavert.android.applications.receipts.connectivity.Sender;
import com.alertavert.android.applications.receipts.database.ReceiptsDbOpenHelper;
import com.alertavert.android.applications.receipts.database.SqliteReceiptDao;
import com.alertavert.android.applications.receipts.settings.UserSettingsManager;
import com.alertavert.android.applications.receipts.storage.FileUtils;
import com.alertavert.android.applications.receipts.storage.ReceiptDAO;
import com.alertavert.android.applications.receipts.ui.ReceiptImageView;
import com.alertavert.android.applications.receipts.ui.ReceiptsImagesAdapter;
import com.alertavert.receipts.model.proto.ReceiptsProtos.UserSettings;
import com.alertavert.receiptscan.model.Money;
import com.alertavert.receiptscan.model.Receipt;


/**
 * <h1>ReceiptsGalleryActivity</h1>
 * <p>
 * Activity to manage the gallery of receipts' images and the associated metadata.
 * 
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
public class ReceiptsGalleryActivity extends Activity implements OnItemSelectedListener,
    OnClickListener, ImageViewListener {

  private static final int DIALOG_DATE_PICKER = 1;
  private static final int DIALOG_CONFIRM_DELETE = 2;
  private static final int DIALOG_CONFIRM_CLEAR = 3;
  private static final int DIALOG_FILESYSTEM_NOT_AVAIL = 4;
  private static final int DIALOG_SETTINGS_INVALID = 5;

  private static final int TAKE_PICTURE = 1;
  private static String TAG;
  
  private static final int MENU_PREVIEW = 1;
  private static final int MENU_UPLOAD = 2;
  
  private Toast updateNotify;

  Gallery mGallery;
  EditText receiptNameTxt;
  Button receiptDateBtn;
  EditText receiptValueTxt;
  EditText receiptMerchantTxt;
  EditText receiptNotesTxt;
  Spinner currenciesSelector;
  ArrayAdapter<CharSequence> currenciesAdapter;
  
  Receipt mSelectedReceipt;
  final DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
  // @VisibleForTesting
  ReceiptDAO mDao;
  // @VisibleForTesting
  ReceiptsDbOpenHelper mHelper;

  boolean wasChanged;

  /**
   * 
   * <h1>OnDateTouchListener</h1>
   *
   * <p>
   * Instance private class to handle a date change UI event (user taps on the
   * date control to change the date).
   * It is made non-static by design, so as to have access to all the outer class instance
   * attributes.
   *
   */
  private class OnDateTouchListener implements View.OnClickListener,
      DatePickerDialog.OnDateSetListener {

    /**
     * When the user 'touches' the timestamp's textbox this listener will display a Date picker
     * dialog.
     * <p>
     * If the user instead touched on the image's name, we need to mark this, as it may require an
     * update of the receipt's metadata, if the name was changed.
     * 
     * @see DateChangeListener
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public void onClick(View v) {
      if (v == receiptDateBtn) {
        Log.d(TAG, "Changing date from " + fmt.format(mSelectedReceipt.getTimestamp()));
        showDialog(DIALOG_DATE_PICKER);
      }
    }

    /**
     * When the date textbox (disabled in the view) receives a 'touch event' it will display a date
     * picker dialog which, when saved, will in turn call this listener to update the receipt's
     * timestamp.
     * 
     * @see DatePickerDialog.OnDateSetListener#onDateSet(android.widget.DatePicker, int, int, int)
     */
    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
      Calendar cal = Calendar.getInstance();

      cal.set(year, monthOfYear, dayOfMonth);
      Log.d(TAG, "onDateSet -- date set to: " + cal.getTime().toString());
      mSelectedReceipt.setTimestamp(cal.getTime());
      wasChanged = true;
      refreshTimestampText();
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TAG = getResources().getString(R.string.TAG);
    Log.d(TAG, "Gallery created");
    currenciesAdapter = ArrayAdapter.createFromResource(this, R.array.currencies,
        android.R.layout.simple_spinner_item);
    currenciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    updateNotify = Toast.makeText(this, R.string.metadata_saved, Toast.LENGTH_SHORT);
  }
  
  @Override
  protected void onStart() {
    super.onStart();
    Log.d(TAG, "Gallery activity started...");
    if (!FileUtils.isFilesystemAvailable()) {
      showDialog(DIALOG_FILESYSTEM_NOT_AVAIL);
    }
    Intent i = getIntent();

    Log.d(TAG, "Intent action: " + i.getAction());
    if (i.getAction().equals(getResources().getString(R.string.action_take_picture))) {
      takePicture();
    }
    mDao = getDao();
  }
  
  @Override
  protected void onNewIntent(Intent intent) {
    Log.d(TAG, "New Intent -- action: " + intent.getAction());
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(Menu.NONE, MENU_PREVIEW, 0, R.string.menu_preview).setIcon(R.drawable.orange_gallery);
    menu.add(Menu.NONE, MENU_UPLOAD, 1, R.string.menu_upload).setIcon(R.drawable.orange_email);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case MENU_PREVIEW:
      previewCurrentReceipt();
      return true;

    case MENU_UPLOAD:
      uploadReceipts();
      return true;

    default:
      Log.e(TAG, "Selected menu item unrecognized: " + item.getTitle() + ", " + item.getItemId());
      return false;
    }
  }

  private void previewCurrentReceipt() {
    if (wasChanged) {
      update();
    }
    wasChanged = false;
    ReceiptImageView imageView = new ReceiptImageView(this, mSelectedReceipt);

    imageView.setListener(this);
    imageView.show();
  }
  
  /**
   * Lazily initialized DAO to retrieve/store receipt's metadata
   * 
   * @return the DAO, newly creating it, if necessary 
   */
  // @VisibleForTesting
  ReceiptDAO getDao() {
    if (mDao == null) {
      mDao = new SqliteReceiptDao(this, getHelper().getWritableDatabase());
    }
    return mDao;
  }

  /**
   * @return lazily initialized DB Open Helper, to open the SQLite db for the receipts
   */
  // @VisibleForTesting
  SQLiteOpenHelper getHelper() {
    if (mHelper == null) {
      mHelper = new ReceiptsDbOpenHelper(this);
    }
    return mHelper;
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    Log.d(TAG, "RGA on pause");
    if (wasChanged) {
      update();
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "Gallery resumed");
    initGallery();    
  }

  @Override
  protected void onStop() {
    super.onStop();
    mDao = null;
    mHelper.close();
    mHelper = null;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);

    switch (id) {
    case DIALOG_DATE_PICKER:
      Calendar cal = Calendar.getInstance();

      cal.setTime(mSelectedReceipt.getTimestamp());
      return new DatePickerDialog(this, new OnDateTouchListener(), cal.get(Calendar.YEAR),
          cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

    case DIALOG_CONFIRM_DELETE:
      builder.setMessage(getResources().getString(R.string.gallery_delete_confirm)).setCancelable(false).setPositiveButton(getResources().getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          delete();
        }
      }).setNegativeButton(
          getResources().getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
        }
      });
      return builder.create();

    case DIALOG_CONFIRM_CLEAR:
      builder.setMessage(getResources().getString(R.string.gallery_delete_clear)).setCancelable(false).setPositiveButton(getResources().getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          clearAll();
          finish();
        }
      }).setNegativeButton(
          getResources().getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
        }
      });
      return builder.create();

    case DIALOG_FILESYSTEM_NOT_AVAIL:
      builder.setMessage(R.string.no_filesystem).setCancelable(true).setPositiveButton(
          R.string.dialog_ok, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
        }
      });
      return builder.create();

    case DIALOG_SETTINGS_INVALID:
      builder.setMessage(R.string.settings_invalid_msg).setCancelable(false).setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          showUserSettings();
        }
      }).setNegativeButton(
          R.string.dialog_no, new DialogInterface.OnClickListener() {
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

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    if (id == DIALOG_DATE_PICKER) {
      if (dialog instanceof DatePickerDialog) {
        DatePickerDialog dlg = (DatePickerDialog) dialog;
        Calendar cal = Calendar.getInstance();

        cal.setTime(mSelectedReceipt.getTimestamp());
        dlg.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH));
      }
    }
  }

  protected void initGallery() {
    Log.d(TAG, "Initialising Receipts images Gallery");
    setContentView(R.layout.receipts_gallery);
    mGallery = (Gallery) findViewById(R.id.gallery);
    if (mGallery == null) {
      Log.e(TAG, "Could not create a gallery of images");
      return;
    }
    mGallery.setAdapter(new ReceiptsImagesAdapter(this, getDao()));
    mGallery.setOnItemSelectedListener(this);

    receiptNameTxt = (EditText) findViewById(R.id.ReceiptNameTxt);
    receiptDateBtn = (Button) findViewById(R.id.ReceiptDateBtn);
    receiptDateBtn.setOnClickListener(new OnDateTouchListener());
    receiptValueTxt = (EditText) findViewById(R.id.ReceiptValueTxt);
    setListenerNextFocus(receiptNameTxt, receiptValueTxt);
    currenciesSelector = (Spinner) findViewById(R.id.CurrenciesSpinner);
    currenciesSelector.setAdapter(currenciesAdapter);
    receiptMerchantTxt = (EditText) findViewById(R.id.ReceiptMerchantTxt);
    setListenerNextFocus(receiptValueTxt, receiptMerchantTxt);
    receiptNotesTxt = (EditText) findViewById(R.id.ReceiptNotesTxt);
    setListenerNextFocus(receiptMerchantTxt, receiptNotesTxt);
    setListenerNextFocus(receiptNotesTxt, receiptNameTxt);

    int ids[] = { R.id.gallery_delete, R.id.gallery_clear, R.id.gallery_snap };

    for (int id : ids) {
      Button btn = (Button) findViewById(id);

      btn.setOnClickListener(this);
    }
  }
  
  private void setListenerNextFocus(View w, final View next) {
    w.setOnKeyListener(new OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        wasChanged = true;
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
          next.setSelected(true);
          return true;
        }
        return false;
      }
    });
  }

  /**
   * Updates the displayed value for the receipt's timestamp
   */
  private void refreshTimestampText() {
    Date d = mSelectedReceipt.getTimestamp();

    if (d == null) {
      d = new Date();
    }
    receiptDateBtn.setText(fmt.format(d));
  }

  /**
   * When an image is selected, it is made the 'current' receipt, and its metadata can be edited by
   * the user. Also, when the selection changes, all modifications made (if any) are persisted
   * 
   * @see OnItemSelectedListener#onItemSelected(android.widget.AdapterView, android.view.View, int,
   *      long)
   */
  @Override
  public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
    if (wasChanged) {
      update();
    }
    mSelectedReceipt = (Receipt) adapter.getItemAtPosition(position);
    wasChanged = false;
    Log.d(TAG,
        "Selected receipt: " + mSelectedReceipt.getName() + "(" + mSelectedReceipt.getId()
        + ") created on: " + mSelectedReceipt.getTimestamp());
    receiptNameTxt.setText(mSelectedReceipt.getName());
    receiptValueTxt.setText(mSelectedReceipt.getAmount().toString());
    String currency = mSelectedReceipt.getAmount().getCurrency();

    currenciesSelector.setSelection(currenciesAdapter.getPosition(currency));
    refreshTimestampText();
    receiptMerchantTxt.setText(mSelectedReceipt.getMerchant());
    receiptNotesTxt.setText(mSelectedReceipt.getNotes());
    view.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        previewCurrentReceipt();
      }
    });
  }

  /**
   *  Updates the currently selected receipt with the new values (metadata) entered by the user 
   */
  private void update() {
    Log.d(TAG, "Updating " + (mSelectedReceipt == null ? "nothing" : mSelectedReceipt.getName()));
    if (mSelectedReceipt != null) {
      mSelectedReceipt.setName(receiptNameTxt.getText().toString());
      float amt = 0.0f;

      try {
        amt = Float.parseFloat(receiptValueTxt.getText().toString());
      } catch (NumberFormatException ex) {
        Log.e(TAG,
            "Could not parse this: " + receiptValueTxt.getText().toString()
            + " - Original cause was: " + ex.getLocalizedMessage());
      }
      CharSequence currency = currenciesAdapter.getItem(currenciesSelector.getSelectedItemPosition());

      mSelectedReceipt.setAmount(Money.parse(amt, currency));
      mSelectedReceipt.setNotes(receiptNotesTxt.getText().toString());
      mSelectedReceipt.setMerchant(receiptMerchantTxt.getText().toString());
      getDao().update(mSelectedReceipt.getId(), mSelectedReceipt);
      updateNotify.show();
    }
    wasChanged = false;
  }

  /**
   * Invoked when nothing is selected, currently does nothing
   * 
   * @see OnItemSelectedListener
   */
  @Override
  public void onNothingSelected(AdapterView<?> adapter) {
    // TODO (marco) do we need to do anything when nothing is selected?
    Log.d("test", "Nothing selected");
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
    case R.id.gallery_delete:
      showDialog(DIALOG_CONFIRM_DELETE);
      break;

    case R.id.gallery_clear:
      showDialog(DIALOG_CONFIRM_CLEAR);
      break;

    case R.id.gallery_snap:
      takePicture();
      break;

    default:
      Log.e(TAG, "Unexpected view sent a click to ReceiptsGalleryActivity: " + v.getId());
    }
  }

  /** the URI for the picture */
  // TODO (marco) do something decent here, will yer?
  private Uri imageUri;
  
  /**
   * Takes a new receipt's picture using the Camera Intent
   */
  private void takePicture() {
    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
    File photo = FileUtils.fromName(FileUtils.getNextValidName());

    Log.d(TAG, "Picture file: " + photo.getAbsolutePath());
    imageUri = Uri.fromFile(photo);
    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
    startActivityForResult(intent, TAKE_PICTURE);
  }
  
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
    case TAKE_PICTURE:
      if (resultCode == Activity.RESULT_OK) {
        Log.d(TAG, "Picture taken: " + imageUri.toString());
        Receipt newReceipt = new Receipt();

        newReceipt.setImageUri(URI.create(imageUri.toString()));
        newReceipt.setName(FileUtils.fromFile(new File(imageUri.getPath())));
        getDao().store(newReceipt);
      } else {
        Log.e(TAG, "Error taking picture, result: " + resultCode);
      }
      break;

    default:
      Log.e(TAG, "Invalid request. Request code: " + requestCode + ", result: " + resultCode);
    }
    // restore the intent, if it was set to take_picture, so as to avoid entering an infinite loop
    Intent i = new Intent();

    i.setAction(getResources().getString(R.string.action_show_gallery));
    setIntent(i);
  }

  /**
   * Removes all currently saved receipts and clears all the image files.
   */
  void clearAll() {
    Map<Integer, Receipt> receipts = getDao().getAll();

    Log.d(TAG, "Clearing all receipts, found: " + receipts.size());
    for (Integer id : receipts.keySet()) {
      Receipt r = receipts.get(id);
      File imageFile = new File(r.getImageUri().getPath());

      imageFile.delete();
      getDao().remove(id);
      Log.d(TAG, "Removing: " + r.getName() + " (" + id + ")");
    }
  }

  /**
   * Removes the currently selected receipt and either regenerates the Gallery view, or closes this
   * Activity, if no receipts are left to display.
   */
  void delete() {
    getDao().remove(mSelectedReceipt.getId());
    File imageFile = new File(mSelectedReceipt.getImageUri().getPath());

    imageFile.delete();
    mSelectedReceipt = null;
    if (getDao().getCount() == 0) {
      finish();
    } else {
      initGallery();
    }
  }
  
  /**
   * Starts the activity that will manage the user's settings
   */
  private void showUserSettings() {
    Intent intent = new Intent();

    intent.setAction(getResources().getString(R.string.action_show_settings));
    startActivityForResult(intent, ControllerActivity.USER_SETTINGS_ACTIVITY_CODE);
  }
  
  /**
   * Sends all receipts to the destination set in the user settings.
   */
  private void uploadReceipts() {
    if (wasChanged) {
      update();
    }
    UserSettings settings = getSettingsFromPrefs();
    Sender sender;

    switch (settings.getDestinationType()) {
    case EMAIL:
      if (!settings.hasEmail() || settings.getEmail() == null) {
        showDialog(DIALOG_SETTINGS_INVALID);
        return;
      }
      sender = new MailSender(settings.getEmail(), this);
      ((MailSender) sender).setReceiptsFormatter(new EmailBodyFormatter(this));
      break;

    case HTTP:
      if (!settings.hasServerAddress()) {
        showUserSettings();
        return;
      }
      sender = new HttpSender();
      try {
        URL url = new URL("http", settings.getServerAddress(), settings.getPort(),
            getRS(R.string.server_context) + "/" + getRS(R.string.action_upload));

        sender.setDestination(new URI(url.toExternalForm()));
      } catch (Exception ex) {
        Log.e(TAG, "Could not create destination URL", ex);
        return;
      }
      break;

    default:
      Log.e(TAG, "Unknown sender type: " + settings.getDestinationType());
      String text = String.format(getRS(R.string.on_send_fail), settings.getServerAddress(),
          "Unknown sender type");

      Toast.makeText(this, text, Toast.LENGTH_SHORT);
      return;
    }
    sender.addSenderListener(new GenericLogListener(this));
    sender.setSenderOption("Custom-Property", ControllerActivity.ANDROID_PKG);
    if (mDao.getCount() > 0) {
      sender.send(mDao.getAll().values());
    }
  }
  
  /**
   * Simple convenience method to retrieve a resource string, from its id
   * 
   * @param id the resource's ID (from the R generated file)
   * @return the corresponding string
   */
  private String getRS(int id) {
    return getResources().getString(id);
  }
  
  /**
   * Retrieves the user settings from the app-wide preferences store
   * 
   * @return the user settings
   */
  private UserSettings getSettingsFromPrefs() {
    UserSettingsManager mgr = UserSettingsManager.getManager();

    mgr.setCtx(this);
    return mgr.getSettings();
  }

  // ----------- ImageViewListener implementation
  @Override
  public void accept(Receipt r) {
    initGallery();
  }

  @Override
  public void cancel() {
    showDialog(DIALOG_CONFIRM_DELETE);
  }
}
