/**
 * 
 */
package com.alertavert.android.applications.receipts.settings;


import android.content.Context;
import android.content.SharedPreferences;

import com.alertavert.android.applications.receipts.R;
import com.alertavert.android.applications.receipts.UserPrefsActivity.SenderType;
import com.alertavert.receipts.model.proto.ReceiptsProtos.UserSettings;


/**
 * Singleton class to manage user settings, and their retrieval / storage into the
 * application Preferences store (see {@link SharedPreferences}).
 * 
 * <p>To access the application settings, just obtain the singleton instance of this
 * class via the {@link #getManager()} invocation, and then use the getter/setter to manage them.
 * 
 * <p>There is no need for the activities to actively manage the settings' storage, this is done
 * transparently by this class: they are retrieved from the local storage the first time they
 * are requested, and saved there subsequently at every update.
 * 
 * @author marco
 *
 */
public class UserSettingsManager {
  private static final int DEFAULT_COMPRESSION_LEVEL = 6;
  
  private static UserSettingsManager manager = new UserSettingsManager();
  
  private UserSettingsManager() {}
  
  public static UserSettingsManager getManager() {
    return manager;
  }
  
  private Context ctx;
  private UserSettings settings;

  public void setCtx(Context ctx) {
    this.ctx = ctx;
  }
  
  public UserSettings getSettings() {
    if (settings == null) {
      retrieveSettingsFromPrefs();
    }
    return settings;
  }

  public void setSettings(UserSettings settings) {
    this.settings = settings;
    storeSettingsInPrefs();
  }
  
  private void retrieveSettingsFromPrefs() {
    if (ctx == null) {
      throw new IllegalStateException("Could not retrieve user preferences, set Context first");
    }    
    SharedPreferences prefs = ctx.getSharedPreferences(getRS(R.string.PREFS), Context.MODE_PRIVATE);
    String senderType = prefs.getString(getRS(R.string.ACCOUNT_TYPE), SenderType.EMAIL.toString());
    
    settings = UserSettings.newBuilder().setDestinationType(UserSettings.SenderType.valueOf(senderType)).setEmail(prefs.getString(getRS(R.string.EMAIL), "")).setCompressionLevel(prefs.getInt(getRS(R.string.COMPRESSION_LVL), DEFAULT_COMPRESSION_LEVEL))// Server:Port are retrieved from the installed default configuration if not already set
        // by the user
        .setServerAddress(prefs.getString(getRS(R.string.SERVER), getRS(R.string.server_ip))).setPort(prefs.getInt(getRS(R.string.PORT), ctx.getResources().getInteger(R.integer.server_port))).build();
  }

  /**
   * @param settings
   */
  private void storeSettingsInPrefs() {
    if (ctx == null) {
      throw new IllegalStateException("Could not retrieve user preferences, set Context first");
    }    
    SharedPreferences prefs = ctx.getSharedPreferences(getRS(R.string.PREFS), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();

    editor.putString(getRS(R.string.ACCOUNT_TYPE), settings.getDestinationType().name());
    editor.putString(getRS(R.string.EMAIL), settings.getEmail());
    editor.putInt(getRS(R.string.COMPRESSION_LVL), settings.getCompressionLevel());
    editor.putString(getRS(R.string.SERVER), settings.getServerAddress());
    editor.putInt(getRS(R.string.PORT), settings.getPort());
    editor.commit();
  }
  
  private String getRS(int id) {
    return ctx.getResources().getString(id);
  }
}
