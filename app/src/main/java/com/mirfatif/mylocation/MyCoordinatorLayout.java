package com.mirfatif.mylocation;

import android.animation.LayoutTransition;
import android.content.Context;
import android.util.AttributeSet;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

public class MyCoordinatorLayout extends CoordinatorLayout {

  public MyCoordinatorLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    LayoutTransition transition = new LayoutTransition();
    transition.enableTransitionType(LayoutTransition.CHANGING);
    setLayoutTransition(transition);
  }
}
