package com.mirfatif.mylocation;

import static org.microg.nlp.api.Constants.METADATA_BACKEND_INIT_ACTIVITY;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.mirfatif.mylocation.util.Utils;
import java.util.concurrent.Future;
import org.microg.nlp.api.LocationBackend;
import org.microg.nlp.api.LocationBackend.Stub;
import org.microg.nlp.api.LocationCallback;

public class NlpBackend {

  private static final String TAG = "NlpBackend";

  private final ServiceInfo mInfo;
  private final String mLabel;

  NlpBackend(ServiceInfo info) {
    mInfo = info;
    mLabel = info.loadLabel(App.getCxt().getPackageManager()).toString();
  }

  void start() {
    Utils.runBg(this::bind);
  }

  private SvcConnection mConnection;
  private boolean mBound;
  private Future<?> mInitializedSetter;
  private final Object INITIALIZED_SETTER_WAITER = new Object();

  private void bind() {
    Intent intent = new Intent().setClassName(mInfo.packageName, mInfo.name);
    mConnection = new SvcConnection();
    mBound = App.getCxt().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    if (!mBound) {
      mConnection = null;
    }

    if (mInitializedSetter != null) {
      mInitializedSetter.cancel(true);
    }
    mInitializedSetter =
        Utils.runBg(
            () -> {
              synchronized (INITIALIZED_SETTER_WAITER) {
                try {
                  INITIALIZED_SETTER_WAITER.wait(5000);
                  mInitialized = true;
                } catch (InterruptedException ignored) {
                }
              }
            });
  }

  public String getPkgName() {
    return mInfo.packageName;
  }

  String getLabel() {
    return mLabel;
  }

  private Location mLoc;

  Location getLocation() {
    return mLoc;
  }

  private boolean mInitialized = false;

  boolean failed() {
    return mInitialized && (!mBound || !mConnected);
  }

  private Intent mInitIntent;

  boolean permsRequired() {
    return mInitIntent != null;
  }

  void openInitActivity(MainActivity activity) {
    String initClass = null;
    Bundle metaData = mInfo.metaData;
    if (metaData != null) {
      initClass = metaData.getString(METADATA_BACKEND_INIT_ACTIVITY);
    }
    Intent intent;
    if (initClass != null) {
      intent = new Intent(Intent.ACTION_VIEW);
      intent.setPackage(mInfo.packageName);
      intent.setClassName(mInfo.packageName, initClass);
    } else if (mInitIntent != null) {
      intent = mInitIntent;
    } else {
      return;
    }

    try {
      activity.startActivity(intent);
    } catch (ActivityNotFoundException ignored) {
    }
  }

  private long mLastCall;

  void refresh() {
    long curr = System.currentTimeMillis();
    if (((mLoc == null && (curr - mLastCall) > 5000)
            || (mLoc != null && (curr - mLastCall) > 30000))
        && mSvc != null
        && !failed()
        && !permsRequired()) {
      mLastCall = curr;
      Utils.runBg(this::updateLoc);
    }
  }

  private void updateLoc() {
    try {
      Location loc = mSvc.update();
      if (loc != null) {
        mLoc = loc;
      }
    } catch (Exception e) {
      Log.e(TAG, mLabel + ": " + e);
      cleanUp();
    }
  }

  void stop() {
    cleanUp();
    if (mInitializedSetter != null) {
      mInitializedSetter.cancel(true);
      mInitializedSetter = null;
    }
    mInitialized = false;
  }

  private void cleanUp() {
    if (mSvc != null) {
      try {
        mSvc.close();
      } catch (Exception e) {
        Log.e(TAG, mLabel + ": cleanUp: " + e);
      }
      mSvc = null;
    }
    if (mConnection != null) {
      App.getCxt().unbindService(mConnection);
      mConnection = null;
    }
    mInitialized = true;
    mBound = mConnected = false;
    mLoc = null;
  }

  private LocationBackend mSvc;
  private boolean mConnected = false;

  private class SvcConnection implements ServiceConnection {

    public void onServiceConnected(ComponentName name, IBinder service) {
      mInitialized = true;
      mSvc = Stub.asInterface(service);
      try {
        mSvc.open(new Callback());
        mConnected = true;
        mInitIntent = mSvc.getInitIntent();
        Utils.runBg(NlpBackend.this::updateLoc);
      } catch (Exception e) {
        Log.e(TAG, mLabel + ": onServiceConnected: " + e);
        if (!(e instanceof RemoteException) && !(e instanceof SecurityException)) {
          e.printStackTrace();
        }
        cleanUp();
      }
    }

    public void onServiceDisconnected(ComponentName name) {
      mConnected = false;
    }

    public void onBindingDied(ComponentName name) {
      cleanUp();
    }

    public void onNullBinding(ComponentName name) {
      cleanUp();
    }
  }

  private class Callback extends LocationCallback.Stub {

    public void report(Location location) {
      mLoc = location;
    }
  }
}
