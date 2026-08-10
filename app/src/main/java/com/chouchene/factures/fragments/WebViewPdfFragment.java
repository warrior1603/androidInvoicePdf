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
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.transition.TransitionInflater;
import androidx.transition.TransitionSet;
import androidx.transition.ChangeBounds;
import androidx.transition.ChangeTransform;
import androidx.transition.ChangeImageTransform;

import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.chouchene.factures.R;
import com.chouchene.factures.adapter.PdfDocumentAdapter;
import com.chouchene.factures.database.AppDatabase;
import com.chouchene.factures.database.DatabaseClient;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;


public class WebViewPdfFragment extends Fragment {

    private String mailClient = "";

    public WebViewPdfFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(new ChangeBounds());
        transitionSet.addTransition(new ChangeTransform());
        transitionSet.addTransition(new ChangeImageTransform());
        transitionSet.setDuration(1000);
        
        setSharedElementEnterTransition(transitionSet);
        postponeEnterTransition();
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
        final String clientName = (bundle != null) ? bundle.getString("client_name", "") : "";
        final String transitionName = (bundle != null) ? bundle.getString("transition_name", "") : "";

        if (bundle != null && bundle.containsKey("mail_client")) {
            mailClient = bundle.getString("mail_client", "");
        }

        if (!clientName.isEmpty()) {
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = DatabaseClient.getInstance(requireContext().getApplicationContext()).getAppDatabase();
                com.chouchene.factures.entity.Client client = db.clientDao().getClientByName(clientName);
                if (client != null) {
                    mailClient = client.getEmail();
                }
            });
        }

        PDFView pdfWebView = view.findViewById(R.id.pdfView);
        Button emailButton = view.findViewById(R.id.emailButton);
        Button shareButton = view.findViewById(R.id.shareButton);
        Button printButton = view.findViewById(R.id.printButton);
        TextView txtPageCount = view.findViewById(R.id.txt_page_count);
        View btnBack = view.findViewById(R.id.btn_back_pdf);
        ImageView headerIcon = view.findViewById(R.id.img_header_icon);

        if (headerIcon != null) {
            if (!transitionName.isEmpty()) {
                ViewCompat.setTransitionName(headerIcon, transitionName);
            }

            // Update icon and tint based on document type
            String docType = (bundle != null) ? bundle.getString("doc_type", "INVOICE") : "INVOICE";
            int iconRes;
            int tintColor;

            switch (docType) {
                case "ORDER":
                    iconRes = R.drawable.ic_shopping_cart_outline;
                    tintColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.icon_dashboard);
                    break;
                case "BOOKING":
                    iconRes = R.drawable.ic_calendar_event_outline;
                    tintColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.icon_agenda);
                    break;
                case "INVOICE":
                default:
                    iconRes = R.drawable.ic_receipt_outline;
                    tintColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary);
                    break;
            }

            headerIcon.setImageResource(iconRes);
            headerIcon.setColorFilter(tintColor);
        }
        
        com.airbnb.lottie.LottieAnimationView lottieLoading = view.findViewById(R.id.lottie_loading_pdf);
        View cardZoom = view.findViewById(R.id.card_zoom_indicator);
        TextView txtZoom = view.findViewById(R.id.txt_zoom_level);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        }

        // Load Lottie if available
        if (lottieLoading != null) {
            com.chouchene.factures.utils.LottieUtils.loadLottieWithFallback(lottieLoading, new android.widget.ImageView(getContext()), "anim_onboarding_1.json");
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
                    startPostponedEnterTransition();
                })
                .onRender(nbPages -> {
                    if (lottieLoading != null) {
                        lottieLoading.setVisibility(View.GONE);
                    }
                })
                .onPageChange((page, pageCount) -> {
                    if (txtPageCount != null) {
                        txtPageCount.setText(String.format(Locale.getDefault(), "%d/%d", page + 1, pageCount));
                    }
                })
                .onPageScroll((page, positionOffset) -> {
                    updateZoomIndicator(pdfWebView, cardZoom, txtZoom);
                })
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .password(null)
                .scrollHandle(new DefaultScrollHandle(getContext()))
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

    private final Runnable hideZoomRunnable = () -> {
        View v = getView();
        if (v != null) {
            View card = v.findViewById(R.id.card_zoom_indicator);
            if (card != null) card.setVisibility(View.GONE);
        }
    };

    private void updateZoomIndicator(PDFView pdfView, View cardZoom, TextView txtZoom) {
        if (pdfView == null || cardZoom == null || txtZoom == null) return;
        
        float zoom = pdfView.getZoom();
        if (zoom > 1.0f) {
            cardZoom.setVisibility(View.VISIBLE);
            txtZoom.setText(String.format(Locale.getDefault(), "%d%%", (int) (zoom * 100)));
            
            cardZoom.removeCallbacks(hideZoomRunnable);
            cardZoom.postDelayed(hideZoomRunnable, 1500);
        } else {
            cardZoom.setVisibility(View.GONE);
        }
    }
}
