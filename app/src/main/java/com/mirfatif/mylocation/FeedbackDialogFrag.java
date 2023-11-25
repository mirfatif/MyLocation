package com.mirfatif.mylocation;

import static com.mirfatif.mylocation.MySettings.SETTINGS;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mirfatif.mylocation.databinding.FeedbackDialogBinding;
import com.mirfatif.mylocation.util.Utils;

public class FeedbackDialogFrag extends BottomSheetDialogFragment {

  private MainActivity mA;

  public void onAttach(Context context) {
    super.onAttach(context);
    mA = (MainActivity) getActivity();
  }

  public Dialog onCreateDialog(Bundle savedInstanceState) {
    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
    dialog.setDismissWithAnimation(true);
    return dialog;
  }

  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    boolean isYes = requireArguments().getBoolean(YES);
    FeedbackDialogBinding b =
        FeedbackDialogBinding.inflate(getLayoutInflater(), container, container != null);

    b.msgV.setText(
        isYes
            ? (Utils.isPsProVersion() ? R.string.rate_the_app : R.string.purchase_and_rate_the_app)
            : R.string.ask_to_provide_feedback);
    b.neutralButton.setText(R.string.do_not_ask);
    b.posButton.setText(
        isYes ? (Utils.isPsProVersion() ? R.string.rate : R.string.i_will) : R.string.contact);

    b.neutralButton.setOnClickListener(
        v -> {
          SETTINGS.setAskForFeedbackTs(DateUtils.WEEK_IN_MILLIS * 8);
          dismiss();
        });

    b.posButton.setOnClickListener(
        v -> {
          dismiss();
          if (isYes) {
            if (Utils.isPsProVersion()) {
              Utils.openWebUrl(mA, Utils.getString(R.string.play_store_url));
            } else {
              DonateDialogFragment.show(mA);
            }
          } else {
            AboutDialogFragment.show(mA);
          }
          Utils.showToast(R.string.thank_you);
        });

    b.negButton.setOnClickListener(v -> dismiss());

    return b.getRoot();
  }

  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    ((View) view.getParent()).setBackgroundResource(R.drawable.bottom_sheet_background);
  }

  private static final String YES = "IS_YES";

  public static void show(FragmentActivity activity, boolean isYes) {
    FeedbackDialogFrag frag = new FeedbackDialogFrag();
    Bundle args = new Bundle();
    args.putBoolean(YES, isYes);
    frag.setArguments(args);
    frag.show(activity.getSupportFragmentManager(), "FEEDBACK_RATING");
  }
}
