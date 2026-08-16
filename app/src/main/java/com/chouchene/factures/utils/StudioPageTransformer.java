package com.chouchene.factures.utils;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class StudioPageTransformer implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(@NonNull View page, float position) {
        page.setTranslationX(-position * page.getWidth());
        
        if (position < -1) { // [-Infinity,-1)
            page.setAlpha(0f);
        } else if (position <= 0) { // [-1,0]
            page.setAlpha(1 + position);
            page.setScaleX(1 + 0.05f * position);
            page.setScaleY(1 + 0.05f * position);
        } else if (position <= 1) { // (0,1]
            page.setAlpha(1 - position);
            page.setScaleX(1 - 0.05f * position);
            page.setScaleY(1 - 0.05f * position);
        } else { // (1,+Infinity]
            page.setAlpha(0f);
        }
    }
}
