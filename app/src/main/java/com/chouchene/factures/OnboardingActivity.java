package com.chouchene.factures;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutDots;
    private MaterialButton btnNext, btnSkip, btnGetStarted;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean("dynamic_colors", false)) {
            com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        layoutDots = findViewById(R.id.layout_dots);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);
        btnGetStarted = findViewById(R.id.btn_get_started);
        viewPager = findViewById(R.id.viewPager);

        setupOnboardingItems();

        viewPager.setAdapter(onboardingAdapter);
        
        // Add Advanced Page Transformer
        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            
            // 1. Zoom/Scale Effect
            float scale = absPos > 1 ? 0.8f : 1 - (absPos * 0.2f);
            page.setScaleX(scale);
            page.setScaleY(scale);
            
            // 2. Parallax Effect on internal elements
            View title = page.findViewById(R.id.txt_title);
            View desc = page.findViewById(R.id.txt_desc);
            View lottie = page.findViewById(R.id.img_onboarding);
            
            if (title != null) title.setTranslationX(position * 500);
            if (desc != null) desc.setTranslationX(position * 300);
            if (lottie != null) {
                lottie.setTranslationX(position * -200);
                lottie.setAlpha(1 - absPos);
            }
        });

        setupDots();

        setCurrentDot(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentDot(position);
                if (position == onboardingAdapter.getItemCount() - 1) {
                    btnNext.setVisibility(View.GONE);
                    btnSkip.setVisibility(View.GONE);
                    btnGetStarted.setVisibility(View.VISIBLE);
                    layoutDots.setVisibility(View.GONE);
                } else {
                    btnNext.setVisibility(View.VISIBLE);
                    btnSkip.setVisibility(View.VISIBLE);
                    btnGetStarted.setVisibility(View.GONE);
                    layoutDots.setVisibility(View.VISIBLE);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
        btnGetStarted.setOnClickListener(v -> finishOnboarding());
    }

    private void setupOnboardingItems() {
        List<OnboardingAdapter.OnboardingItem> items = new ArrayList<>();

        // Page 1: Bienvenue & Professionnalisme
        items.add(new OnboardingAdapter.OnboardingItem(
                "anim_onboarding_1.json",
                R.drawable.rounded_business_24,
                getString(R.string.onboarding_title_welcome),
                getString(R.string.onboarding_desc_welcome)
        ));

        // Page 2: Gestion de factures et bon
        items.add(new OnboardingAdapter.OnboardingItem(
                "anim_onboarding_1.json", // Reusing standard doc animation
                R.drawable.invoice,
                getString(R.string.onboarding_title_docs),
                getString(R.string.onboarding_desc_docs)
        ));

        // Page 3: Gestion de clients
        items.add(new OnboardingAdapter.OnboardingItem(
                "anim_onboarding_3.json",
                R.drawable.rounded_people_24,
                getString(R.string.onboarding_title_clients),
                getString(R.string.onboarding_desc_clients)
        ));

        // Page 4: Gestion d'agenda
        items.add(new OnboardingAdapter.OnboardingItem(
                "anim_agenda.json",
                R.drawable.ic_nav_calendar_outline,
                getString(R.string.onboarding_title_agenda),
                getString(R.string.onboarding_desc_agenda)
        ));

        // Page 5: Dashboard des bénéfices et dépenses
        items.add(new OnboardingAdapter.OnboardingItem(
                "anim_onboarding_2.json",
                R.drawable.graph_svgrepo_com,
                getString(R.string.onboarding_title_stats),
                getString(R.string.onboarding_desc_stats)
        ));

        onboardingAdapter = new OnboardingAdapter(items);
    }

    private void setupDots() {
        ImageView[] dots = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(getApplicationContext());
            dots[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.rounded_circle_24
            ));
            dots[i].setLayoutParams(params);
            layoutDots.addView(dots[i]);
        }
    }

    private void setCurrentDot(int index) {
        int childCount = layoutDots.getChildCount();
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorActive = typedValue.data;

        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutDots.getChildAt(i);
            if (i == index) {
                imageView.setColorFilter(colorActive);
            } else {
                imageView.setColorFilter(ContextCompat.getColor(getApplicationContext(), android.R.color.darker_gray));
            }
        }
    }

    private void finishOnboarding() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean("first_run", false).apply();
        startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
        finish();
    }
}
