package com.chouchene.factures.utils;

import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class AnimationUtils {

    public static void popView(View view) {
        if (view == null) return;
        
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        
        view.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }
}
