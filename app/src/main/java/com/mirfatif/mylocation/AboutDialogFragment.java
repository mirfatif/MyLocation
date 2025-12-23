package com.mirfatif.mylocation;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentActivity;
import com.mirfatif.mylocation.databinding.AboutDialogBinding;
import com.mirfatif.mylocation.util.Utils;

public class AboutDialogFragment extends AppCompatDialogFragment {

  private AboutDialogFragment() {}

  private MainActivity mA;

  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    mA = (MainActivity) getActivity();
  }

  @NonNull
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AboutDialogBinding b = AboutDialogBinding.inflate(mA.getLayoutInflater());
    b.version.setText(BuildConfig.VERSION_NAME);
    openWebUrl(b.telegram, R.string.telegram_link);
    openWebUrl(b.sourceCode, R.string.source_url);
    b.contact.setOnClickListener(v -> Utils.sendMail(mA, null));
    openWebUrl(b.checkUpdate, R.string.release_url);

    AlertDialog dialog = new Builder(mA).setView(b.getRoot()).create();
    return Utils.setDialogBg(dialog);
  }

  private void openWebUrl(View view, int linkResId) {
    view.setOnClickListener(v -> Utils.openWebUrl(mA, getString(linkResId)));
  }

  public static void show(FragmentActivity activity) {
    new AboutDialogFragment().show(activity.getSupportFragmentManager(), "ABOUT");
  }
}
