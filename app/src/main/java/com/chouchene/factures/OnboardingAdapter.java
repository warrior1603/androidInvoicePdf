package com.chouchene.factures;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import com.chouchene.factures.utils.LottieUtils;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnboardingViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_onboarding, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.setOnboardingData(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {

        private final LottieAnimationView imageOnboarding;
        private final TextView textTitle;
        private final TextView textDescription;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageOnboarding = itemView.findViewById(R.id.img_onboarding);
            textTitle = itemView.findViewById(R.id.txt_title);
            textDescription = itemView.findViewById(R.id.txt_desc);
        }

        void setOnboardingData(OnboardingItem onboardingItem) {
            textTitle.setText(onboardingItem.getTitle());
            textDescription.setText(onboardingItem.getDescription());
            
            ImageView imgFallback = itemView.findViewById(R.id.img_fallback);
            if (imgFallback != null && onboardingItem.getImage() != 0) {
                imgFallback.setImageResource(onboardingItem.getImage());
            }
            LottieUtils.loadLottieWithFallback(imageOnboarding, imgFallback, onboardingItem.getLottieRes());
        }
    }

    public static class OnboardingItem {
        private final int image;
        private String lottieRes;
        private final String title;
        private final String description;

        public OnboardingItem(int image, String title, String description) {
            this.image = image;
            this.title = title;
            this.description = description;
        }

        public OnboardingItem(String lottieRes, int fallbackImage, String title, String description) {
            this.image = fallbackImage;
            this.lottieRes = lottieRes;
            this.title = title;
            this.description = description;
        }

        public int getImage() { return image; }
        public String getLottieRes() { return lottieRes; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }
}
