package com.chouchene.factures.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.chouchene.factures.R;

public class HelpFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        View btnBack = view.findViewById(R.id.btn_back_header);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (isAdded()) {
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            });
        }

        View btnContact = view.findViewById(R.id.btn_contact_support);
        if (btnContact != null) {
            btnContact.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@pdftest.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Support - PdfTest Studio");
                try {
                    startActivity(Intent.createChooser(intent, "Envoyer un email..."));
                } catch (Exception ignored) {}
            });
        }
    }
}
