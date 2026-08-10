package com.chouchene.factures.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.chouchene.factures.R;
import com.google.android.material.button.MaterialButton;

public class UIUtils {

    public static void showSuccessDialog(Context context, String title, String message, Runnable onDone) {
        if (context == null) return;
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_success_dialog, null);
        dialog.setContentView(view);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            GlassUtils.applyGlassEffect(dialog.getWindow(), 50f);
        }

        com.airbnb.lottie.LottieAnimationView lottie = view.findViewById(R.id.lottie_success);
        TextView txtTitle = view.findViewById(R.id.txt_success_title);
        TextView txtMessage = view.findViewById(R.id.txt_success_message);
        MaterialButton btnDone = view.findViewById(R.id.btn_done);

        if (title != null) txtTitle.setText(title);
        if (message != null) txtMessage.setText(message);
        
        LottieUtils.loadLottieWithFallback(lottie, new android.widget.ImageView(context), "anim_onboarding_1.json");

        btnDone.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDone != null) onDone.run();
        });

        dialog.show();
    }
}
