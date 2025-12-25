package com.mirfatif.mylocation.util;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.mirfatif.mylocation.MySettings.SETTINGS;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.TooltipCompat;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;
import com.mirfatif.mylocation.App;
import com.mirfatif.mylocation.R;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Utils {

  private Utils() {}

  public static final String GITHUB_URL = "https://github.com/mirfatif/MyLocation";

  public static String getString(int resId, Object... args) {
    return App.getCxt().getString(resId, args);
  }

  public static int getInteger(int resId) {
    return App.getCxt().getResources().getInteger(resId);
  }

  public static boolean isNaN(double d) {
    return d != d;
  }

  public static void copyLoc(Location location) {
    if (location != null) {
      ClipboardManager clipboard =
          (ClipboardManager) App.getCxt().getSystemService(Context.CLIPBOARD_SERVICE);
      String loc = location.getLatitude() + "," + location.getLongitude();
      ClipData data = ClipData.newPlainText("location", loc);
      clipboard.setPrimaryClip(data);
      Utils.showShortToast(R.string.copied);
    }
  }

  public static void openMap(Activity act, Location location) {
    if (location != null) {
      String loc = location.getLatitude() + "," + location.getLongitude();
      try {
        act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + loc + "?q=" + loc)));
      } catch (ActivityNotFoundException ignored) {
        Utils.showToast(R.string.no_maps_installed);
      }
    }
  }

  public static void openAppSettings(Activity act, String pkg) {
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.parse("package:" + pkg));
    try {
      act.startActivity(intent);
    } catch (ActivityNotFoundException ignored) {
      Utils.showToast(R.string.failed_open_app_settings);
    }
  }

  public static boolean openWebUrl(Activity activity, String url) {
    Intent intent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
    PackageManager pm = App.getCxt().getPackageManager();
    int flags = VERSION.SDK_INT >= VERSION_CODES.M ? PackageManager.MATCH_ALL : 0;
    List<ResolveInfo> infoList = pm.queryIntentServices(intent, flags);
    boolean customTabsSupported = !infoList.isEmpty();

    if (customTabsSupported) {
      CustomTabColorSchemeParams colorSchemeParams =
          new CustomTabColorSchemeParams.Builder()
              .setToolbarColor(App.getRes().getColor(R.color.primary))
              .build();
      CustomTabsIntent customTabsIntent =
          new CustomTabsIntent.Builder()
              .setShareState(CustomTabsIntent.SHARE_STATE_ON)
              .setDefaultColorSchemeParams(colorSchemeParams)
              .build();
      customTabsIntent.launchUrl(activity, Uri.parse(url));
      return true;
    }

    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    intent.addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try {
      activity.startActivity(intent);
      return true;
    } catch (ActivityNotFoundException ignored) {
    }

    if (VERSION.SDK_INT >= VERSION_CODES.R) {
      intent.setFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER);
      try {
        activity.startActivity(intent);
        return true;
      } catch (ActivityNotFoundException ignored) {
      }
    }

    showToast(R.string.no_browser_installed);
    return true;
  }

  private static final DecimalFormat sLatLngFormat = new DecimalFormat();

  static {
    sLatLngFormat.setMaximumFractionDigits(5);
  }

  public static String formatLatLng(double coordinate) {
    return sLatLngFormat.format(coordinate);
  }

  public static String formatLocAccuracy(float accuracy) {
    return String.format(Locale.getDefault(), "%.0f", accuracy);
  }

  public static boolean hasFineLocPerm() {
    return hasPerm(ACCESS_FINE_LOCATION);
  }

  public static boolean hasCoarseLocPerm() {
    return hasPerm(ACCESS_COARSE_LOCATION);
  }

  public static boolean hasPerm(String perm) {
    return ActivityCompat.checkSelfPermission(App.getCxt(), perm) == PERMISSION_GRANTED;
  }

  public static SharedPreferences getDefPrefs() {
    return App.getCxt().getSharedPreferences("def_prefs", Context.MODE_PRIVATE);
  }

  public static Context setLocale(Context context) {
    String lang = SETTINGS.getLocale();
    Locale locale;
    if (TextUtils.isEmpty(lang)) {
      if (VERSION.SDK_INT >= VERSION_CODES.N) {
        locale = Resources.getSystem().getConfiguration().getLocales().get(0);
      } else {
        locale = Resources.getSystem().getConfiguration().locale;
      }
    } else {
      String[] langSpecs = lang.split("\\|");
      if (langSpecs.length == 2) {
        locale = new Locale(langSpecs[0], langSpecs[1]);
      } else {
        locale = new Locale(lang);
      }
    }
    Locale.setDefault(locale);
    Configuration config = context.getResources().getConfiguration();
    config.setLocale(locale);
    return context.createConfigurationContext(config);
  }

  private static final Handler UI_EXECUTOR = new Handler(Looper.getMainLooper());

  public static UiRunnable runUi(LifecycleOwner lifecycleOwner, Runnable runnable) {
    if (lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(State.INITIALIZED)) {
      return runUi(runnable);
    }
    return new UiRunnable();
  }

  public static UiRunnable runUi(Runnable runnable) {
    UiRunnable uiRunnable = new UiRunnable(runnable);
    UI_EXECUTOR.post(uiRunnable);
    return uiRunnable;
  }

  public static class UiRunnable implements Runnable {

    private final Runnable mRunnable;

    UiRunnable(Runnable runnable) {
      mRunnable = runnable;
    }

    UiRunnable() {
      mRunnable = null;
    }

    public void run() {
      Objects.requireNonNull(mRunnable).run();
    }
  }

  private static final ExecutorService BG_EXECUTOR = Executors.newCachedThreadPool();

  public static Future<?> runBg(Runnable runnable) {
    return BG_EXECUTOR.submit(runnable);
  }

  public static void showToast(String msg) {
    if (msg != null) {
      runUi(() -> showToast(msg, Toast.LENGTH_LONG));
    }
  }

  public static void showToast(int resId, Object... args) {
    if (resId != 0) {
      showToast(getString(resId, args));
    }
  }

  public static void showShortToast(int resId, Object... args) {
    if (resId != 0) {
      runUi(() -> showToast(getString(resId, args), Toast.LENGTH_SHORT));
    }
  }

  private static void showToast(String msg, int duration) {
    Toast toast = Toast.makeText(App.getCxt(), msg, duration);
    toast.show();
  }

  public static void createNotifChannel(String id, String name, int importance) {
    NotificationManagerCompat nm = NotificationManagerCompat.from(App.getCxt());
    NotificationChannelCompat ch = nm.getNotificationChannelCompat(id);
    if (ch == null) {
      ch = new NotificationChannelCompat.Builder(id, importance).setName(name).build();
      nm.createNotificationChannel(ch);
    }
  }

  public static void setTooltip(ImageView v) {
    TooltipCompat.setTooltipText(v, v.getContentDescription());
  }

  public static boolean isNightMode(Activity activity) {
    int uiMode = activity.getResources().getConfiguration().uiMode;
    return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

  public static boolean setNightTheme(Activity activity) {
    if (!SETTINGS.getForceDarkMode()) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
      return false;
    }

    if (isNightMode(activity)) {
      return false;
    }

    int defMode = AppCompatDelegate.getDefaultNightMode();
    if (defMode == AppCompatDelegate.MODE_NIGHT_YES) {
      return false;
    }

    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    return true;
  }

  public static AlertDialog setDialogBg(AlertDialog dialog) {
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawableResource(R.drawable.alert_dialog_bg_bordered);
      window.setWindowAnimations(android.R.style.Animation_Dialog);
    }
    return dialog;
  }
}
