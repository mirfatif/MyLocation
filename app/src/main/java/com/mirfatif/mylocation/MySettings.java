package com.mirfatif.mylocation;

import static com.mirfatif.mylocation.util.Utils.getString;

import android.content.SharedPreferences;
import com.mirfatif.mylocation.util.Utils;

public enum MySettings {
  SETTINGS;

  private final SharedPreferences mPrefs = Utils.getDefPrefs();

  public boolean getBoolPref(int keyResId, boolean defValue) {
    String prefKey = getString(keyResId);
    return mPrefs.getBoolean(prefKey, defValue);
  }

  public int getIntPref(int keyResId, int defValue) {
    String prefKey = getString(keyResId);
    return mPrefs.getInt(prefKey, defValue);
  }

  public void savePref(int key, boolean bool) {
    String prefKey = getString(key);
    mPrefs.edit().putBoolean(prefKey, bool).apply();
  }

  public void savePref(int key, int integer) {
    String prefKey = getString(key);
    mPrefs.edit().putInt(prefKey, integer).apply();
  }

  public boolean getGpsEnabled() {
    return mPrefs.getBoolean(getString(R.string.pref_main_gps_enabled_key), true);
  }

  public void setGpsEnabled(boolean enabled) {
    mPrefs.edit().putBoolean(getString(R.string.pref_main_gps_enabled_key), enabled).apply();
  }

  public boolean getNetworkEnabled() {
    return mPrefs.getBoolean(getString(R.string.pref_main_network_enabled_key), true);
  }

  public void setNetworkEnabled(boolean enabled) {
    mPrefs.edit().putBoolean(getString(R.string.pref_main_network_enabled_key), enabled).apply();
  }

  public boolean getNlpEnabled() {
    return mPrefs.getBoolean(getString(R.string.pref_main_nlp_enabled_key), true);
  }

  public void setNlpEnabled(boolean enabled) {
    mPrefs.edit().putBoolean(getString(R.string.pref_main_nlp_enabled_key), enabled).apply();
  }

  public boolean getForceDarkMode() {
    return getBoolPref(R.string.pref_main_dark_theme_key, true);
  }

  public void setForceDarkMode(boolean force) {
    savePref(R.string.pref_main_dark_theme_key, force);
  }

  public String getLocale() {
    return mPrefs.getString(getString(R.string.pref_main_locale_key), "");
  }

  public void setLocale(String langCode) {
    mPrefs.edit().putString(getString(R.string.pref_main_locale_key), langCode).apply();
  }

  public void plusAppLaunchCount() {
    int appLaunchCountId = R.string.pref_main_app_launch_count_for_feedback_key;
    savePref(appLaunchCountId, getIntPref(appLaunchCountId, 0) + 1);
  }
}
