package com.chouchene.factures.utils;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.airbnb.lottie.LottieAnimationView;

import java.io.IOException;

public class LottieUtils {

    public static void loadLottieWithFallback(LottieAnimationView lottie, ImageView fallback, String fileName) {
        if (lottie == null || fallback == null) return;
        
        // Prevent crash on state restoration if file is missing
        lottie.setSaveEnabled(false);

        if (assetExists(lottie.getContext(), fileName)) {
            lottie.setFailureListener(result -> {
                lottie.setVisibility(View.GONE);
                fallback.setVisibility(View.VISIBLE);
            });
            try {
                lottie.setAnimation(fileName);
                lottie.playAnimation();
                lottie.setVisibility(View.VISIBLE);
                fallback.setVisibility(View.GONE);
            } catch (Exception e) {
                lottie.setVisibility(View.GONE);
                fallback.setVisibility(View.VISIBLE);
            }
        } else {
            lottie.setVisibility(View.GONE);
            fallback.setVisibility(View.VISIBLE);
        }
    }

    private static boolean assetExists(Context context, String fileName) {
        if (fileName == null) return false;
        try {
            context.getAssets().open(fileName).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
