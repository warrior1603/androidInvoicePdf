package com.chouchene.factures.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.chouchene.factures.R;
import com.google.android.material.card.MaterialCardView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TemplatePreviewFragment extends Fragment {

    private SharedPreferences settingsPrefs;
    private MaterialCardView cardStandard, cardModern;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_template_preview, container, false);
        settingsPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        cardStandard = view.findViewById(R.id.card_standard);
        cardModern = view.findViewById(R.id.card_modern);

        WebView webStandard = view.findViewById(R.id.web_preview_standard);
        WebView webModern = view.findViewById(R.id.web_preview_modern);

        setupPreview(webStandard, "invoice_template.html");
        setupPreview(webModern, "invoice_template_modern.html");

        updateSelectionUI();

        view.findViewById(R.id.btn_select_standard).setOnClickListener(v -> selectTemplate("invoice_template.html"));
        view.findViewById(R.id.btn_select_modern).setOnClickListener(v -> selectTemplate("invoice_template_modern.html"));

        return view;
    }

    private void setupPreview(WebView webView, String templateName) {
        try {
            String html = loadHtmlFromAssets(templateName);
            // Dummy data for preview
            html = html.replace("{{issuerName}}", "Ma Société")
                    .replace("{{invoiceNumber}}", "2026001")
                    .replace("{{invoiceDate}}", "01.01.2026")
                    .replace("{{clientName}}", "Client Démo")
                    .replace("{{description}}", "Description du service...")
                    .replace("{{totalTtc}}", "120.00 €")
                    .replace("{{currency}}", "€");

            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            webView.getSettings().setSupportZoom(false);
            webView.setEnabled(false); // Static preview
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectTemplate(String name) {
        settingsPrefs.edit().putString("invoice_template", name).apply();
        updateSelectionUI();
        Toast.makeText(getContext(), "Modèle sélectionné !", Toast.LENGTH_SHORT).show();
    }

    private void updateSelectionUI() {
        String current = settingsPrefs.getString("invoice_template", "invoice_template.html");
        int primary = ContextCompat.getColor(requireContext(), R.color.primary_light);
        
        cardStandard.setStrokeColor(current.equals("invoice_template.html") ? primary : 0);
        cardStandard.setStrokeWidth(current.equals("invoice_template.html") ? 4 : 0);

        cardModern.setStrokeColor(current.equals("invoice_template_modern.html") ? primary : 0);
        cardModern.setStrokeWidth(current.equals("invoice_template_modern.html") ? 4 : 0);
    }

    private String loadHtmlFromAssets(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
