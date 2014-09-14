// Copyright AlertAvert.com (c) 2010. All rights reserved.

package com.alertavert.android.applications.receipts;


import java.util.Arrays;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.alertavert.android.applications.receipts.settings.UserSettingsManager;
import com.alertavert.receipts.model.proto.ReceiptsProtos.UserSettings;


/**
 * Manages the User preferences
 *
 * @author m.massenzio@gmail.com (Marco Massenzio)
 */
public class UserPrefsActivity extends Activity implements OnClickListener, OnCheckedChangeListener {

  private static String TAG;
  private static final int COMPRESSION_MAX = 9;
  private static final int DEFAULT_SERVER_PORT = 80;
  
  private UserSettingsManager mgr;
  
  // UI Elements
  EditText emailAddress;
  EditText serverAddress;
  EditText serverPort;
  RadioButton serverRadioBtn;
  RadioButton emailRadioBtn;

  public enum SenderType {
    EMAIL, SERVER_HTTP
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TAG = getRS(R.string.TAG);
    Log.d(TAG, "Starting User Preferences activity...");
    setContentView(R.layout.settings);
    for (int id : Arrays.asList(R.id.SettingsOkBtn, R.id.SettingsCancelBtn,
        R.id.SettingsServerTestBtn)) {
      findViewById(id).setOnClickListener(this);
    }
    emailAddress = (EditText) findViewById(R.id.EmailSettingsText);
    serverAddress = (EditText) findViewById(R.id.ServerNameText);
    serverPort = (EditText) findViewById(R.id.port);
    emailRadioBtn = (RadioButton) findViewById(R.id.EmailSettingsRadio);
    serverRadioBtn = (RadioButton) findViewById(R.id.ServerSettingsRadio);
    
    mgr = UserSettingsManager.getManager();
    mgr.setCtx(this);
    UserSettings settings = mgr.getSettings(); 

    initializeUi(settings);
  }
  
  private String getRS(int id) {
    return getResources().getString(id);
  }

  /* (non-Javadoc)
   * @see android.view.View.OnClickListener#onClick(android.view.View)
   */
  @Override
  public void onClick(View btn) {
    UserSettings settings = getSettingsFromUi();
    int id = btn.getId();

    switch (id) {
    case R.id.SettingsOkBtn:
      mgr.setSettings(settings);
      setResult(Activity.RESULT_OK);
      Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
      finish();
      break;

    case R.id.SettingsCancelBtn:
      setResult(Activity.RESULT_CANCELED);
      finish();
      break;

    case R.id.SettingsServerTestBtn:
      // TODO(marco): test server connectivity
      Toast.makeText(this, "Test to " + settings.getServerAddress() + " in progress...", Toast.LENGTH_LONG).show();
      break;

    default:
      Log.e(TAG, "This was not expected: " + btn.getId() + " view triggered an onClick event");
    }
  }

  private void initializeUi(UserSettings settings) {
    initRadioButtons(settings);
    emailAddress.setText(settings.getEmail());
    serverAddress.setText(settings.getServerAddress());
    serverPort.setText("" + settings.getPort());
    SeekBar compressionLevelBar = ((SeekBar) findViewById(R.id.CompressionLevelBar));

    compressionLevelBar.setMax(COMPRESSION_MAX);
    compressionLevelBar.setProgress(settings.getCompressionLevel());
    // TODO(marco): this MUST be kept disabled until we allow server uploads
    // Commented out for testing, uncomment prior to release, until feature is available
    // serverAddress.setEnabled(false);
    // serverPort.setEnabled(false);
    initFocus();
  }

  private void initRadioButtons(UserSettings settings) {
    emailRadioBtn.setChecked(settings.getDestinationType() == UserSettings.SenderType.EMAIL);
    emailRadioBtn.setOnCheckedChangeListener(this);
    // TODO(marco): this MUST be commented out prior to release until we allow server uploads
    serverRadioBtn.setChecked(settings.getDestinationType() == UserSettings.SenderType.HTTP);
    serverRadioBtn.setOnCheckedChangeListener(this);
    // uncomment line below prior to release
    // serverRadioBtn.setEnabled(false);
  }
  
  private void initFocus() {
    if (emailRadioBtn.isChecked()) {
      emailAddress.requestFocus();
    } else if (serverAddress.isFocusable()) {
      serverAddress.requestFocus();
    } else {
      findViewById(R.id.CompressionLevelBar).requestFocus();
    }
  }

  private UserSettings getSettingsFromUi() {
    UserSettings.Builder settings = UserSettings.newBuilder();
    String portNoStr = serverPort.getText().toString();
    int portNo = DEFAULT_SERVER_PORT;

    if (portNoStr.length() > 0) {
      try {
        portNo = Integer.parseInt(portNoStr);
      } catch (NumberFormatException ex) {
        Log.e(TAG, "Could not parse server port number", ex);
      }
    }
    UserSettings.SenderType type = emailRadioBtn.isChecked()
        ? UserSettings.SenderType.EMAIL
        : UserSettings.SenderType.HTTP;

    return settings.setDestinationType(type).setEmail(emailAddress.getText().toString()).setServerAddress(serverAddress.getText().toString()).setPort(portNo).setCompressionLevel(((SeekBar) findViewById(R.id.CompressionLevelBar)).getProgress()).build();
  }

  /* (non-Javadoc)
   * @see android.widget.CompoundButton.OnCheckedChangeListener#onCheckedChanged(android.widget.CompoundButton, boolean)
   */
  @Override
  public void onCheckedChanged(CompoundButton radioBtn, boolean isChecked) {
    if (radioBtn.getId() == R.id.EmailSettingsRadio) {
      serverRadioBtn.setChecked(!isChecked);
    } else {
      emailRadioBtn.setChecked(!isChecked);
    }
    // TODO(marco): the various fields ought to be enabled/disabled respectively
  }
}
