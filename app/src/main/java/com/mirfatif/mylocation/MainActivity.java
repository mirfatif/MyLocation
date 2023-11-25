package com.mirfatif.mylocation;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static android.os.Build.VERSION.SDK_INT;
import static com.mirfatif.mylocation.GpsSvc.ACTION_STOP_SERVICE;
import static com.mirfatif.mylocation.GpsSvc.MIN_DELAY;
import static com.mirfatif.mylocation.MySettings.SETTINGS;
import static com.mirfatif.mylocation.util.Utils.copyLoc;
import static com.mirfatif.mylocation.util.Utils.hasCoarseLocPerm;
import static com.mirfatif.mylocation.util.Utils.hasFineLocPerm;
import static com.mirfatif.mylocation.util.Utils.isNaN;
import static com.mirfatif.mylocation.util.Utils.openMap;
import static com.mirfatif.mylocation.util.Utils.setNightTheme;
import static org.microg.nlp.api.Constants.ACTION_LOCATION_BACKEND;

import android.Manifest.permission;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.mirfatif.mylocation.NlpAdapter.NlpClickListener;
import com.mirfatif.mylocation.databinding.ActivityMainBinding;
import com.mirfatif.mylocation.util.NotifUtils;
import com.mirfatif.mylocation.util.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

  private ActivityMainBinding mB;

  private final LocationManager mLocManager =
      (LocationManager) App.getCxt().getSystemService(Context.LOCATION_SERVICE);

  private LicenseVerifier mLicenseVerifier;
  private boolean mGpsProviderSupported = false;
  private boolean mNetProviderSupported = false;

  protected void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.AppTheme);
    super.onCreate(savedInstanceState);
    if (setNightTheme(this)) {
      return;
    }
    mB = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(mB.getRoot());

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayUseLogoEnabled(true);
      actionBar.setDisplayShowHomeEnabled(true);
      actionBar.setIcon(R.drawable.action_bar_icon);
    }

    for (String provider : mLocManager.getAllProviders()) {
      if (provider.equals(GPS_PROVIDER)) {
        mGpsProviderSupported = true;
      }
      if (provider.equals(NETWORK_PROVIDER)) {
        mNetProviderSupported = true;
      }
    }

    setupGps();
    updateGpsUi();
    setupNetwork();
    updateNetUi();
    setupUnifiedNlp();
    checkPerms();

    mB.grantPerm.setOnClickListener(v -> Utils.openAppSettings(this, getPackageName()));

    mLicenseVerifier = new LicenseVerifier(this);

    if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
      SETTINGS.plusAppLaunchCount();
    }
  }

  protected void onStart() {
    super.onStart();
    startLocListeners();
    setTimer();
    setGrantPermButtonState();
  }

  protected void onStop() {
    stopTimer();
    stopLocListeners();
    super.onStop();
  }

  protected void onResume() {
    super.onResume();
    checkLicense();
  }

  protected void onDestroy() {
    if (mLicenseVerifier != null) {
      mLicenseVerifier.onDestroy();
    }
    super.onDestroy();
  }

  protected void onSaveInstanceState(Bundle outState) {
    FragmentManager fm = getSupportFragmentManager();
    Fragment frag = fm.findFragmentByTag(SATS_DIALOG_TAG);
    if (frag != null) {
      fm.beginTransaction().remove(frag).commitNowAllowingStateLoss();
    }
    super.onSaveInstanceState(outState);
  }

  public void onBackPressed() {
    if (VERSION.SDK_INT == VERSION_CODES.Q) {

      finishAfterTransition();
    } else {
      super.onBackPressed();
    }
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_overflow, menu);
    MenuCompat.setGroupDividerEnabled(menu, true);
    if (menu instanceof MenuBuilder) {
      ((MenuBuilder) menu).setOptionalIconsVisible(true);
    }
    String locale = SETTINGS.getLocale();
    if (locale.equals(getString(R.string.lang_code_en))) {
      menu.findItem(R.id.action_locale_en).setChecked(true);
    } else if (locale.equals(getString(R.string.lang_code_pt_rBr))) {
      menu.findItem(R.id.action_locale_pt_rBr).setChecked(true);
    } else if (locale.equals(getString(R.string.lang_code_el))) {
      menu.findItem(R.id.action_locale_el).setChecked(true);
    } else {
      menu.findItem(R.id.action_locale_system).setChecked(true);
    }
    menu.findItem(R.id.action_dark_theme).setChecked(SETTINGS.getForceDarkMode());
    if (Utils.isPsProVersion()) {
      menu.findItem(R.id.action_donate).setVisible(false);
    }
    return true;
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.action_loc_settings) {
      try {
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
      } catch (ActivityNotFoundException ignored) {
        Utils.showToast(R.string.failed_open_loc_settings);
      }
      return true;
    }
    if (item.getGroupId() == R.id.action_locale_group) {
      if (!item.isChecked()) {
        String locale = null;
        if (itemId == R.id.action_locale_system) {
          locale = "";
        } else if (itemId == R.id.action_locale_en) {
          locale = getString(R.string.lang_code_en);
        } else if (itemId == R.id.action_locale_pt_rBr) {
          locale = getString(R.string.lang_code_pt_rBr);
        } else if (itemId == R.id.action_locale_el) {
          locale = getString(R.string.lang_code_el);
        }
        if (locale != null) {
          SETTINGS.setLocale(locale);

          stopNlpBackends();
          App.updateContext();
          recreate();
        }
      }
      return true;
    }
    if (itemId == R.id.action_dark_theme) {
      SETTINGS.setForceDarkMode(!item.isChecked());
      setNightTheme(this);
      return true;
    }
    if (itemId == R.id.action_donate) {
      DonateDialogFragment.show(this);
      return true;
    }
    if (itemId == R.id.action_about) {
      AboutDialogFragment.show(this);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  protected void attachBaseContext(Context context) {
    super.attachBaseContext(Utils.setLocale(context));
  }

  private void setupGps() {
    if (!mGpsProviderSupported) {
      return;
    }

    mB.clearAgps.setOnClickListener(v -> clearAGPSData());
    mB.lockGps.setOnClickListener(
        v -> {
          if (mB.lockGps.isChecked()) {
            if (NotifUtils.hasNotifPerm()) {
              Intent intent = new Intent(App.getCxt(), GpsSvc.class);

              if (SDK_INT >= VERSION_CODES.O) {
                startForegroundService(intent);
              } else {
                startService(intent);
              }
            } else {
              Utils.showToast(R.string.perm_not_granted);
            }
          } else {
            startService(new Intent(App.getCxt(), GpsSvc.class).setAction(ACTION_STOP_SERVICE));
          }
        });

    mB.gpsCont.map.setOnClickListener(v -> openMap(this, mGpsLocation));
    mB.gpsCont.copy.setOnClickListener(v -> copyLoc(mGpsLocation));

    mB.gpsCont.switchV.setOnClickListener(
        v -> {
          if (SETTINGS.getGpsEnabled() != mB.gpsCont.switchV.isChecked()) {
            SETTINGS.setGpsEnabled(mB.gpsCont.switchV.isChecked());
            startGpsLocListener();
            setTimer();
          }
        });

    if (GpsSvc.mIsRunning) {
      mB.lockGps.setChecked(true);
    }

    mB.gpsCont.satDetail.setOnClickListener(v -> showSatsDialog());

    Utils.setTooltip(mB.gpsCont.map);
    Utils.setTooltip(mB.gpsCont.copy);
    Utils.setTooltip(mB.gpsCont.satDetail);
  }

  private void setupNetwork() {
    if (!mNetProviderSupported) {
      return;
    }

    mB.netCont.map.setOnClickListener(v -> openMap(this, mNetLocation));
    mB.netCont.copy.setOnClickListener(v -> copyLoc(mNetLocation));

    mB.netCont.switchV.setOnClickListener(
        v -> {
          if (SETTINGS.getNetworkEnabled() != mB.netCont.switchV.isChecked()) {
            SETTINGS.setNetworkEnabled(mB.netCont.switchV.isChecked());
            startNetLocListener();
            setTimer();
          }
        });

    Utils.setTooltip(mB.netCont.map);
    Utils.setTooltip(mB.netCont.copy);
  }

  private final List<NlpBackend> mBackends = new ArrayList<>();
  private NlpAdapter mNlpAdapter;

  private void setupUnifiedNlp() {
    Intent intent = new Intent(ACTION_LOCATION_BACKEND);
    List<ResolveInfo> infoList = getPackageManager().queryIntentServices(intent, 0);
    synchronized (mBackends) {
      mBackends.clear();
      for (ResolveInfo info : infoList) {
        mBackends.add(new NlpBackend(info.serviceInfo));
      }
    }

    Utils.setTooltip(mB.nlpCont.download);
    mB.nlpCont.download.setOnClickListener(
        v -> Utils.openWebUrl(this, "https://github.com/microg/UnifiedNlp/wiki/Backends"));

    mB.nlpCont.switchV.setOnClickListener(
        v -> {
          if (SETTINGS.getNlpEnabled() != mB.nlpCont.switchV.isChecked()) {
            SETTINGS.setNlpEnabled(mB.nlpCont.switchV.isChecked());
            startNlpBackends();
            setTimer();
          }
        });

    mNlpAdapter =
        new NlpAdapter(
            new NlpClickListener() {

              public void mapClicked(Location loc) {
                openMap(MainActivity.this, loc);
              }

              public void copyClicked(Location loc) {
                copyLoc(loc);
              }

              public void settingsClicked(NlpBackend backend) {
                Utils.openAppSettings(MainActivity.this, backend.getPkgName());
                backend.openInitActivity(MainActivity.this);
              }
            },
            mBackends);
    mB.nlpCont.rv.setAdapter(mNlpAdapter);
    mB.nlpCont.rv.setLayoutManager(new LinearLayoutManager(this));
    mB.nlpCont.rv.addItemDecoration(
        new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
  }

  private final Object LOC_LISTENER_LOCK = new Object();

  private void startLocListeners() {
    startGpsLocListener();
    startNetLocListener();
    startNlpBackends();
  }

  private LocListener mGpsLocListener;
  private GpsStatus.Listener mGpsStatusListener;
  private GnssStatus.Callback mGnssStatusCallback;

  private void startGpsLocListener() {
    synchronized (LOC_LISTENER_LOCK) {
      stopGpsLocListener();
      if (SETTINGS.getGpsEnabled() && mGpsProviderSupported && hasFineLocPerm()) {
        mGpsLocListener = new LocListener(true);
        mLocManager.requestLocationUpdates(GPS_PROVIDER, MIN_DELAY, 0, mGpsLocListener);
        mGpsStatusListener = new GpsStatusListener();
        if (SDK_INT >= VERSION_CODES.N) {
          mGnssStatusCallback = new LocCallback();
          mLocManager.registerGnssStatusCallback(mGnssStatusCallback);
        } else {
          mLocManager.addGpsStatusListener(mGpsStatusListener);
        }
      }
    }
  }

  private LocListener mNetLocListener;

  private void startNetLocListener() {
    synchronized (LOC_LISTENER_LOCK) {
      stopNetLocListener();
      if (SETTINGS.getNetworkEnabled()
          && mNetProviderSupported
          && (hasCoarseLocPerm() || hasFineLocPerm())) {
        mNetLocListener = new LocListener(false);
        mLocManager.requestLocationUpdates(NETWORK_PROVIDER, MIN_DELAY, 0, mNetLocListener);
      }
    }
  }

  private void startNlpBackends() {
    synchronized (mBackends) {
      stopNlpBackends();
      if (SETTINGS.getNlpEnabled()) {
        for (NlpBackend backend : mBackends) {
          backend.start();
        }
      }
    }
  }

  private void stopLocListeners() {
    stopGpsLocListener();
    stopNetLocListener();
    stopNlpBackends();
  }

  private void stopGpsLocListener() {
    synchronized (LOC_LISTENER_LOCK) {
      if (mGpsLocListener != null) {
        mLocManager.removeUpdates(mGpsLocListener);
        mGpsLocListener = null;
      }
      if (mGpsStatusListener != null) {
        if (SDK_INT >= VERSION_CODES.N) {
          mLocManager.unregisterGnssStatusCallback(mGnssStatusCallback);
        } else {
          mLocManager.removeGpsStatusListener(mGpsStatusListener);
        }
        mGpsStatusListener = null;
        mGnssStatusCallback = null;
      }
      clearGpsData();
    }
  }

  private void clearGpsData() {
    mGpsLocation = null;
    synchronized (mSats) {
      mSats.clear();
    }
  }

  private void stopNetLocListener() {
    synchronized (LOC_LISTENER_LOCK) {
      if (mNetLocListener != null) {
        mLocManager.removeUpdates(mNetLocListener);
        mNetLocListener = null;
      }
      mNetLocation = null;
    }
  }

  private void stopNlpBackends() {
    synchronized (mBackends) {
      for (NlpBackend backend : mBackends) {
        backend.stop();
      }
    }
  }

  private final List<Sat> mSats = new ArrayList<>();
  private final ReentrantLock UPDATE_SATS_LOCK = new ReentrantLock();

  private void updateGpsSats(GnssStatus status) {
    if (!UPDATE_SATS_LOCK.tryLock()) {
      return;
    }
    if (hasFineLocPerm()) {
      synchronized (mSats) {
        GpsStatus gpsStatus;
        if (SDK_INT >= VERSION_CODES.N) {
          mSats.clear();
          for (int i = 0; i < status.getSatelliteCount(); i++) {
            mSats.add(new Sat(status.getSvid(i), status.usedInFix(i), status.getCn0DbHz(i)));
          }
        } else if ((gpsStatus = mLocManager.getGpsStatus(null)) != null) {
          mSats.clear();
          for (GpsSatellite gpsSat : gpsStatus.getSatellites()) {
            mSats.add(new Sat(gpsSat.getPrn(), gpsSat.usedInFix(), gpsSat.getSnr()));
          }
        }
        Collections.sort(mSats, (s1, s2) -> Float.compare(s2.mSnr, s1.mSnr));
      }
    }
    UPDATE_SATS_LOCK.unlock();
  }

  private Timer mTimer;
  private long mPeriod = 1000;
  private int mTickCount;

  private void setTimer() {
    mPeriod = 1000;
    mTickCount = 0;
    startTimer();
  }

  private void startTimer() {
    stopTimer();
    mTimer = new Timer();
    mTimer.scheduleAtFixedRate(
        new TimerTask() {

          public void run() {
            Utils.runUi(MainActivity.this, () -> updateUi());
            mTickCount++;
            if (mTickCount == 5) {
              mPeriod = 5000;
              startTimer();
            }
          }
        },
        0,
        mPeriod);
  }

  private void stopTimer() {
    if (mTimer != null) {
      mTimer.cancel();
      mTimer = null;
    }
  }

  private Location mGpsLocation, mNetLocation;

  private void updateUi() {
    if (mB != null && mLicenseVerifier != null && mLicenseVerifier.isVerified()) {
      updateGpsUi();
      updateNetUi();
      updateNlpUi();
    }
  }

  private void updateGpsUi() {
    String state = null, lat = "--", lng = "--", acc = "--", time = "--";
    boolean hasFineLocPerm = false, showSats = false, locAvailable = false;
    if (!mGpsProviderSupported) {
      state = getString(R.string.not_supported);
    } else {
      hasFineLocPerm = hasFineLocPerm();
      if (!hasFineLocPerm) {
        state = getString(R.string.perm_not_granted);
      } else if (!mLocManager.isProviderEnabled(GPS_PROVIDER)) {
        state = getString(R.string.turned_off);
      } else {
        showSats = SETTINGS.getGpsEnabled() && !mSats.isEmpty();
        if (mGpsLocation == null) {
          mGpsLocation = GpsSvc.mGpsLoc;
        }
        Location gpsLoc = mGpsLocation;
        if (gpsLoc != null && !isNaN(gpsLoc.getLatitude()) && !isNaN(gpsLoc.getLongitude())) {
          locAvailable = true;
          lat = Utils.formatLatLng(gpsLoc.getLatitude());
          lng = Utils.formatLatLng(gpsLoc.getLongitude());
          if (!isNaN(gpsLoc.getAccuracy()) && gpsLoc.getAccuracy() != 0) {
            acc = getString(R.string.acc_unit, Utils.formatLocAccuracy(gpsLoc.getAccuracy()));
          }
          long curr = System.currentTimeMillis();
          long t = gpsLoc.getTime();
          t = t - Math.max(0, t - curr);
          time = DateUtils.getRelativeTimeSpanString(t).toString();
        }
      }
    }
    mB.clearAgps.setEnabled(hasFineLocPerm);
    mB.lockGps.setEnabled(hasFineLocPerm);
    mB.gpsCont.map.setEnabled(locAvailable);
    mB.gpsCont.copy.setEnabled(locAvailable);
    mB.gpsCont.switchV.setEnabled(hasFineLocPerm);
    mB.gpsCont.switchV.setChecked(hasFineLocPerm && SETTINGS.getGpsEnabled());
    mB.gpsCont.stateV.setText(state);
    mB.gpsCont.latV.setText(lat);
    mB.gpsCont.lngV.setText(lng);
    mB.gpsCont.accV.setText(acc);
    mB.gpsCont.timeV.setText(time);
    mB.gpsCont.satDetail.setEnabled(showSats);

    int total, good = 0, used = 0;
    synchronized (mSats) {
      total = mSats.size();
      for (Sat sat : mSats) {
        if (sat.mSnr != 0) {
          good++;
        }
        if (sat.mUsed) {
          used++;
        }
      }
    }

    mB.gpsCont.totalSatV.setText(String.valueOf(total));
    mB.gpsCont.goodSatV.setText(String.valueOf(good));
    mB.gpsCont.usedSatV.setText(String.valueOf(used));

    synchronized (SATS_DIALOG_TAG) {
      if (mSatsDialog != null) {
        if (showSats) {
          mSatsDialog.submitList(mSats);
        } else {
          mSatsDialog.dismissAllowingStateLoss();
        }
      }
    }
  }

  private void updateNetUi() {
    String state = null, lat = "--", lng = "--", acc = "--", time = "--";
    boolean hasLocPerm = false, locAvailable = false;
    if (!mNetProviderSupported) {
      state = getString(R.string.not_supported);
    } else {
      hasLocPerm = hasCoarseLocPerm() || hasFineLocPerm();
      Location netLoc;
      if (!hasLocPerm) {
        state = getString(R.string.perm_not_granted);
      } else if (!mLocManager.isProviderEnabled(NETWORK_PROVIDER)) {
        state = getString(R.string.turned_off);
      } else if ((netLoc = mNetLocation) != null
          && !isNaN(netLoc.getLatitude())
          && !isNaN(netLoc.getLongitude())) {
        locAvailable = true;
        lat = Utils.formatLatLng(netLoc.getLatitude());
        lng = Utils.formatLatLng(netLoc.getLongitude());
        if (!isNaN(netLoc.getAccuracy()) && netLoc.getAccuracy() != 0) {
          acc = getString(R.string.acc_unit, Utils.formatLocAccuracy(netLoc.getAccuracy()));
        }
        long curr = System.currentTimeMillis();
        long t = netLoc.getTime();
        t = t - Math.max(0, t - curr);
        time = DateUtils.getRelativeTimeSpanString(t).toString();
      }
    }
    mB.netCont.map.setEnabled(locAvailable);
    mB.netCont.copy.setEnabled(locAvailable);
    mB.netCont.switchV.setEnabled(hasLocPerm);
    mB.netCont.switchV.setChecked(hasLocPerm && SETTINGS.getNetworkEnabled());
    mB.netCont.stateV.setText(state);
    mB.netCont.latV.setText(lat);
    mB.netCont.lngV.setText(lng);
    mB.netCont.accV.setText(acc);
    mB.netCont.timeV.setText(time);
  }

  private void updateNlpUi() {
    boolean hasLocPerm = hasCoarseLocPerm();
    mB.nlpCont.switchV.setEnabled(hasLocPerm);
    mB.nlpCont.switchV.setChecked(hasLocPerm && SETTINGS.getNlpEnabled());
    synchronized (mBackends) {
      for (NlpBackend backend : mBackends) {
        mB.nlpCont.download.setVisibility(mBackends.isEmpty() ? View.VISIBLE : View.GONE);
        backend.refresh();
        if (mNlpAdapter != null) {
          mNlpAdapter.notifyDataSetChanged();
        }
      }
    }
  }

  private void setGrantPermButtonState() {
    if (mB != null) {
      if (hasFineLocPerm() && hasCoarseLocPerm()) {
        mB.grantPerm.setVisibility(View.GONE);
      } else {
        mB.grantPerm.setVisibility(View.VISIBLE);
      }
    }
  }

  private SatsDialogFragment mSatsDialog;
  private static final String SATS_DIALOG_TAG = "SATELLITES_DETAIL";

  private void showSatsDialog() {
    mSatsDialog = new SatsDialogFragment();
    mSatsDialog.setOnDismissListener(
        d -> {
          synchronized (SATS_DIALOG_TAG) {
            mSatsDialog = null;
          }
        });
    mSatsDialog.showNow(getSupportFragmentManager(), SATS_DIALOG_TAG);
    mSatsDialog.submitList(mSats);
  }

  private void checkPerms() {
    List<String> perms = new ArrayList<>();
    if (!hasFineLocPerm()) {
      perms.add(permission.ACCESS_FINE_LOCATION);
    }
    if (!hasCoarseLocPerm()) {
      perms.add(permission.ACCESS_COARSE_LOCATION);
    }
    if (!NotifUtils.hasNotifPerm()) {
      perms.add(permission.POST_NOTIFICATIONS);
    }
    if (!perms.isEmpty()) {
      ActivityCompat.requestPermissions(this, perms.toArray(new String[] {}), 0);
    }
  }

  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    List<Integer> results = new ArrayList<>();
    for (int i : grantResults) {
      results.add(i);
    }
    if (results.contains(PERMISSION_GRANTED)) {
      startLocListeners();
      setGrantPermButtonState();
      setTimer();
    }
  }

  void checkLicense() {
    if (mLicenseVerifier != null) {
      mLicenseVerifier.check();
    }
  }

  private void clearAGPSData() {
    if (hasFineLocPerm()) {
      mLocManager.sendExtraCommand(GPS_PROVIDER, "delete_aiding_data", null);
      mLocManager.sendExtraCommand(GPS_PROVIDER, "force_time_injection", null);
      String command = SDK_INT >= VERSION_CODES.Q ? "force_psds_injection" : "force_xtra_injection";
      mLocManager.sendExtraCommand(GPS_PROVIDER, command, null);
      Utils.showShortToast(R.string.cleared);
    }
  }

  private class LocListener implements LocationListener {

    private final boolean mIsGps;

    private LocListener(boolean isGps) {
      mIsGps = isGps;
    }

    public void onLocationChanged(Location location) {
      if (mIsGps) {
        mGpsLocation = location;
      } else {
        mNetLocation = location;
      }
    }

    public void onProviderEnabled(String provider) {
      setTimer();
    }

    public void onProviderDisabled(String provider) {
      if (mIsGps) {
        clearGpsData();
      } else {
        mNetLocation = null;
      }
      setTimer();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
      setTimer();
    }
  }

  private class GpsStatusListener implements GpsStatus.Listener {

    public void onGpsStatusChanged(int event) {
      Utils.runBg(() -> MainActivity.this.updateGpsSats(null));
    }
  }

  private class LocCallback extends GnssStatus.Callback {

    public void onSatelliteStatusChanged(GnssStatus status) {
      Utils.runBg(() -> updateGpsSats(status));
    }
  }

  static class Sat {

    final int mPrn;
    final boolean mUsed;
    final float mSnr;

    Sat(int prn, boolean used, float snr) {
      mPrn = prn;
      mUsed = used;
      mSnr = snr;
      if (snr > maxSnr) {
        maxSnr = snr + correction;
      } else if (snr < minSnr) {
        minSnr = snr;
        if (minSnr < 0) {
          correction = -minSnr;
        }
      }
    }

    static float maxSnr;
    private static float minSnr, correction;
  }

  ActivityMainBinding getRootView() {
    return mB;
  }
}
