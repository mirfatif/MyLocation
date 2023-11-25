package com.mirfatif.mylocation;

import static android.os.Build.VERSION.SDK_INT;
import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT;
import static com.mirfatif.mylocation.BuildConfig.APPLICATION_ID;
import static com.mirfatif.mylocation.util.NotifUtils.PI_FLAGS;
import static com.mirfatif.mylocation.util.Utils.formatLatLng;
import static com.mirfatif.mylocation.util.Utils.formatLocAccuracy;
import static com.mirfatif.mylocation.util.Utils.hasFineLocPerm;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.Builder;
import com.mirfatif.mylocation.util.NotifUtils;
import com.mirfatif.mylocation.util.Utils;
import java.util.concurrent.Future;

public class GpsSvc extends Service implements LocationListener, GpsStatus.Listener {

  public static final String ACTION_STOP_SERVICE = APPLICATION_ID + ".action.STOP_SERVICE";

  static boolean mIsRunning = false;

  private final LocationManager mLocManager =
      (LocationManager) App.getCxt().getSystemService(Context.LOCATION_SERVICE);

  private final PowerManager mPowerManager =
      (PowerManager) App.getCxt().getSystemService(Context.POWER_SERVICE);

  private GnssStatus.Callback mGnssStatusCallback;

  public IBinder onBind(Intent intent) {
    return null;
  }

  public int onStartCommand(Intent intent, int flags, int startId) {
    if (hasFineLocPerm()
        && NotifUtils.hasNotifPerm()
        && (intent == null || !ACTION_STOP_SERVICE.equals(intent.getAction()))) {
      showNotif();
      startGpsLocListener();
      mIsRunning = true;
      return START_STICKY;
    } else {
      stop();
      return START_NOT_STICKY;
    }
  }

  public void onDestroy() {
    stop();
    super.onDestroy();
  }

  static Location mGpsLoc;

  public void onLocationChanged(Location location) {
    mGpsLoc = location;
    updateNotification();
  }

  public void onProviderEnabled(String provider) {
    mLastUpdate = 0;
    updateNotification();
  }

  public void onProviderDisabled(String provider) {
    mLastUpdate = 0;
    updateNotification();
  }

  public void onStatusChanged(String provider, int status, Bundle extras) {
    mLastUpdate = 0;
    updateNotification();
  }

  private void stop() {
    synchronized (NOTIF_UPDATE_LOCK) {
      mIsRunning = false;
      stopGpsLocListener();
      mGpsLoc = null;
      if (mFuture != null) {
        mFuture.cancel(true);
      }
      stopForeground(true);
      stopSelf();
    }
  }

  public void onGpsStatusChanged(int event) {
    updateGpsSats(null);
  }

  private class LocCallback extends GnssStatus.Callback {

    public void onSatelliteStatusChanged(GnssStatus status) {
      Utils.runBg(() -> updateGpsSats(status));
    }
  }

  private WakeLock mWakeLock;
  private Builder mNotifBuilder;

  private static final int NOTIF_ID = Utils.getInteger(R.integer.channel_gps_lock);
  private static final String CHANNEL_ID = "channel_gps_lock";
  private static final String CHANNEL_NAME = Utils.getString(R.string.channel_gps_lock);

  private void showNotif() {
    mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
    mWakeLock.acquire(30 * 60 * 1000L);

    Utils.createNotifChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_DEFAULT);

    Intent intent = new Intent(App.getCxt(), MainActivity.class);
    PendingIntent pi = PendingIntent.getActivity(App.getCxt(), NOTIF_ID, intent, PI_FLAGS);

    mNotifBuilder =
        new Builder(App.getCxt(), CHANNEL_ID)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.notification_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(getString(R.string.channel_gps_lock));

    startForeground(NOTIF_ID, mNotifBuilder.build());

    updateGpsSats(null);
    mLastUpdate = 0;
    updateNotification();
  }

  public static final long MIN_DELAY = 5000;

  private void startGpsLocListener() {
    mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_DELAY, 0, this);
    if (SDK_INT >= VERSION_CODES.N) {
      mGnssStatusCallback = new LocCallback();
      mLocManager.registerGnssStatusCallback(mGnssStatusCallback);
    } else {
      mLocManager.addGpsStatusListener(this);
    }
  }

  private void stopGpsLocListener() {
    if (mWakeLock != null) {
      if (mWakeLock.isHeld()) {
        mWakeLock.release();
      }
      mWakeLock = null;
    }
    mLocManager.removeUpdates(this);
    if (SDK_INT >= VERSION_CODES.N) {
      mLocManager.unregisterGnssStatusCallback(mGnssStatusCallback);
    } else {
      mLocManager.removeGpsStatusListener(this);
    }
  }

  private final Object UPDATE_GPS_SATS_LOCK = new Object();
  private int mTotalSats, mSatsStrongSig, mUsedSats;

  private void updateGpsSats(GnssStatus status) {
    synchronized (UPDATE_GPS_SATS_LOCK) {
      if (!hasFineLocPerm()) {
        stop();
        return;
      }

      GpsStatus gpsStatus;
      if (SDK_INT >= VERSION_CODES.N) {
        if (status != null) {
          mTotalSats = mSatsStrongSig = mUsedSats = 0;
          for (int i = 0; i < status.getSatelliteCount(); i++) {
            mTotalSats++;
            if (status.getCn0DbHz(i) != 0) {
              mSatsStrongSig++;
            }
            if (status.usedInFix(i)) {
              mUsedSats++;
            }
          }
        }
      } else if ((gpsStatus = mLocManager.getGpsStatus(null)) != null) {
        mTotalSats = mSatsStrongSig = mUsedSats = 0;
        for (GpsSatellite gpsSat : gpsStatus.getSatellites()) {
          mTotalSats++;
          if (gpsSat.getSnr() != 0) {
            mSatsStrongSig++;
          }
          if (gpsSat.usedInFix()) {
            mUsedSats++;
          }
        }
      }
      updateNotification();
    }
  }

  private Future<?> mFuture;

  private synchronized void updateNotification() {
    if (mFuture != null) {
      mFuture.cancel(true);
    }
    mFuture = Utils.runBg(this::updateNotifBg);
  }

  private final Object NOTIF_UPDATE_LOCK = new Object();
  private long mLastUpdate;

  private void updateNotifBg() {
    synchronized (NOTIF_UPDATE_LOCK) {
      if (!mIsRunning) {
        return;
      }

      long sleep = 5000 + mLastUpdate - System.currentTimeMillis();
      if (sleep > 0) {
        try {
          NOTIF_UPDATE_LOCK.wait(sleep);
        } catch (InterruptedException e) {
          return;
        }
      }
      mLastUpdate = System.currentTimeMillis();

      String sText, bText;
      long when = 0;
      if (!mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        sText = bText = getString(R.string.turned_off);
      } else {
        sText = bText = getString(R.string.satellites_count, mTotalSats, mSatsStrongSig, mUsedSats);
        double lat, lng;
        Location gpsLoc = mGpsLoc;
        if (gpsLoc != null
            && (lat = gpsLoc.getLatitude()) != 0
            && (lng = gpsLoc.getLongitude()) != 0) {
          sText =
              getString(
                  R.string.location,
                  formatLatLng(lat),
                  formatLatLng(lng),
                  formatLocAccuracy(gpsLoc.getAccuracy()));
          bText += "\n" + sText;
        }
        if (gpsLoc != null && gpsLoc.getTime() != 0) {
          when = gpsLoc.getTime();
        }
      }
      mNotifBuilder.setContentText(sText);
      mNotifBuilder.setStyle(new BigTextStyle().bigText(bText));
      if (when != 0) {
        mNotifBuilder.setWhen(when);
        mNotifBuilder.setShowWhen(true);
      } else {
        mNotifBuilder.setShowWhen(false);
      }
      NotifUtils.notify(NOTIF_ID, mNotifBuilder.build());
    }
  }
}
