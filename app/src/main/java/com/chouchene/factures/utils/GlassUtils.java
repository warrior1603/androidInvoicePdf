package com.chouchene.factures.utils;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

public class GlassUtils {

    /**
     * Applies a frosted glass effect to a window (usually a Dialog or BottomSheet).
     * Works on Android 12 (API 31) and above.
     */
    public static void applyGlassEffect(@NonNull Window window, float blurRadius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius((int) blurRadius);
            
            WindowManager.LayoutParams params = window.getAttributes();
            // Optional: Adjust dim amount for better glass visibility
            params.dimAmount = 0.2f; 
            window.setAttributes(params);
        }
    }

    /**
     * Applies a blur effect to a specific view's content.
     */
    public static void applyBlurToView(@NonNull View view, float radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        }
    }
}
