package com.chouchene.factures.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.PdfDocumentAdapter;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class WebViewPdfFragment extends Fragment {

    public WebViewPdfFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementEnterTransition(new com.google.android.material.transition.MaterialContainerTransform());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_webview_pdf_sendmail, container, false);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey("transition_name")) {
            view.setTransitionName(bundle.getString("transition_name"));
        }
        final String filePath = (bundle != null) ? bundle.getString("file_path", "") : "";
        final String mailClient = (bundle != null) ? bundle.getString("mail_client", "") : "";

        PDFView pdfWebView = view.findViewById(R.id.pdfView);
        Button emailButton = view.findViewById(R.id.emailButton);
        Button shareButton = view.findViewById(R.id.shareButton);
        Button printButton = view.findViewById(R.id.printButton);
        TextView txtPageCount = view.findViewById(R.id.txt_page_count);
        View btnBack = view.findViewById(R.id.btn_back_pdf);
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        // Wire up containers
        view.findViewById(R.id.btn_email_container).setOnClickListener(v -> emailButton.performClick());
        view.findViewById(R.id.btn_share_container).setOnClickListener(v -> shareButton.performClick());
        view.findViewById(R.id.btn_print_container).setOnClickListener(v -> printButton.performClick());

        File file = new File(filePath);
        Uri fileUri = FileProvider.getUriForFile(
                requireContext().getApplicationContext(), "com.chouchene.factures.provider", file
        );

        printButton.setOnClickListener(v -> {
            Activity activity = getActivity();
            if (activity != null) {
                openPdfForPrinting(activity, fileUri);
            }
        });

        emailButton.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            String iban = sharedPreferences.getString("Iban", "");
            String bic = sharedPreferences.getString("Bic", "");
            String bankAddress = sharedPreferences.getString("Bank_address", "");

            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", mailClient, null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Facture / Bon de commande");
            List<ResolveInfo> resolveInfos = getActivity().getPackageManager().queryIntentActivities(emailIntent, 0);
            
            if (!resolveInfos.isEmpty()) {
                List<Intent> intents = new ArrayList<>();
                for (ResolveInfo info : resolveInfos) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setPackage(info.activityInfo.packageName);
                    intent.setType("application/pdf");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mailClient});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Facture / Bon de commande");
                    intent.putExtra(Intent.EXTRA_TEXT, "Bonjour, \n\n Veuillez trouver ci-joint votre document. \n\n" +
                            "Coordonnées bancaires : \n" +
                            "IBAN : " + iban + " \n" +
                            "BIC : " + bic + " \n" +
                            "Adresse du titulaire : " + bankAddress);
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    intents.add(new LabeledIntent(intent, info.activityInfo.packageName, info.loadLabel(getActivity().getPackageManager()), info.icon));
                }
                Intent chooser = Intent.createChooser(intents.remove(intents.size() - 1), "Envoyer par email...");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new LabeledIntent[intents.size()]));
                startActivity(chooser);
            }
        });

        shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Partage de facture");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Partager avec"));
        });

        pdfWebView.fromUri(fileUri)
                .onLoad(nbPages -> {
                    if (txtPageCount != null) {
                        txtPageCount.setText(String.format(Locale.getDefault(), "1/%d", nbPages));
                    }
                })
                .onPageChange((page, pageCount) -> {
                    if (txtPageCount != null) {
                        txtPageCount.setText(String.format(Locale.getDefault(), "%d/%d", page + 1, pageCount));
                    }
                })
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .password(null)
                .scrollHandle(null)
                .enableAntialiasing(true)
                .spacing(10)
                .autoSpacing(true)
                .pageFitPolicy(FitPolicy.WIDTH)
                .fitEachPage(false)
                .pageSnap(false)
                .pageFling(false)
                .nightMode(false)
                .load();

        return view;
    }

    private void openPdfForPrinting(Activity activity, Uri fileUri) {
        Intent viewPdfIntent = new Intent(Intent.ACTION_VIEW);
        viewPdfIntent.setDataAndType(fileUri, "application/pdf");
        viewPdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(viewPdfIntent, "Ouvrir pour imprimer"));
        } catch (ActivityNotFoundException e) {
            android.widget.Toast.makeText(activity, "Aucune application PDF disponible", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
