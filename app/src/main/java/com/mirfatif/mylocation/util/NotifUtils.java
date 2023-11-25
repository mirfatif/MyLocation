package com.mirfatif.mylocation.util;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.Manifest;
import android.app.Notification;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;
import com.mirfatif.mylocation.App;

public class NotifUtils {

  public static final int PI_FLAGS = FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE;

  private NotifUtils() {}

  public static boolean hasNotifPerm() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        || App.getCxt().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
  }

  public static void notify(int id, Notification notif) {
    if (hasNotifPerm()) {
      NotificationManagerCompat.from(App.getCxt()).notify(id, notif);
    }
  }
}
