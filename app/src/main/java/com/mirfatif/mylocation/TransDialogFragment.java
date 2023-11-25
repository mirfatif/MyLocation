package com.mirfatif.mylocation;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialogFragment;
import com.mirfatif.mylocation.databinding.TranslationDialogBinding;
import com.mirfatif.mylocation.util.Utils;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;

public class TransDialogFragment extends AppCompatDialogFragment {

  public TransDialogFragment() {}

  private MainActivity mA;

  public void onAttach(Context context) {
    super.onAttach(context);
    mA = (MainActivity) getActivity();
  }

  public Dialog onCreateDialog(Bundle savedInstanceState) {
    TranslationDialogBinding b = TranslationDialogBinding.inflate(getLayoutInflater());
    b.langCreditsV.setText(Utils.htmlToString(R.string.language_credits));
    BetterLinkMovementMethod method = BetterLinkMovementMethod.newInstance();
    method.setOnLinkClickListener((tv, url) -> Utils.openWebUrl(mA, url));
    b.langCreditsV.setMovementMethod(method);
    AlertDialog dialog =
        new Builder(mA).setTitle(R.string.translations).setView(b.getRoot()).create();
    return Utils.setDialogBg(dialog);
  }
}
