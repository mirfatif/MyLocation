package com.mirfatif.mylocation;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import com.mirfatif.mylocation.util.Utils;

public class App extends Application {

  private static Context mAppContext;

  public void onCreate() {
    super.onCreate();
    mAppContext = getApplicationContext();
    updateContext();
  }

  public static void updateContext() {
    mAppContext = Utils.setLocale(mAppContext);
  }

  public static Context getCxt() {
    return mAppContext;
  }

  public static Resources getRes() {
    return mAppContext.getResources();
  }
}
