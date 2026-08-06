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

import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.chouchene.factures.R;
import com.chouchene.factures.adapter.PdfDocumentAdapter;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class WebViewPdfFragment extends Fragment {

    private PDFView pdfWebView;
    private Button emailButton;
    private Button shareButton;
    private Button printButton;

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

        pdfWebView = view.findViewById(R.id.pdfView);
        emailButton = view.findViewById(R.id.emailButton);

        shareButton = view.findViewById(R.id.shareButton);
        printButton = view.findViewById(R.id.printButton);

        File file = new File(filePath);
        Uri fileUri = FileProvider.getUriForFile(
                getActivity().getApplicationContext(), "com.chouchene.factures.provider", file
        );

        printButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (filePath == null || filePath.isEmpty()) {
                            android.widget.Toast.makeText(getContext(), "Fichier non trouvé", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!isAdded()) {
                            android.widget.Toast.makeText(v.getContext(), "Écran non prêt pour l'impression", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Activity activity;
                        try {
                            activity = requireActivity();
                        } catch (IllegalStateException e) {
                            android.util.Log.e("WebViewPdfFragment", "Print host activity unavailable", e);
                            android.widget.Toast.makeText(v.getContext(), "Impossible d'imprimer depuis cet écran", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (activity.isFinishing() || activity.isDestroyed()) {
                            android.widget.Toast.makeText(activity, "Fenêtre fermée, impression annulée", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }

                        PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                        if (printManager == null) {
                            android.widget.Toast.makeText(activity, "Service d'impression non disponible", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            // Use a clean job name
                            String fileName = new File(filePath).getName();
                            String jobName = getString(R.string.app_name) + "_" + fileName;

                            PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(activity, filePath);
                            printManager.print(jobName, printAdapter, null);
                        } catch (IllegalStateException e) {
                            android.util.Log.w("WebViewPdfFragment", "Direct print unavailable, opening PDF viewer instead");
                            openPdfForPrinting(activity, fileUri);
                        } catch (Exception e) {
                            android.util.Log.e("WebViewPdfFragment", "Print error", e);
                            android.widget.Toast.makeText(activity, "Erreur lors de l'impression: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        emailButton.setOnClickListener(new View.OnClickListener() {
                                           @Override
                                           public void onClick(View v) {
                                               ArrayList<Uri> uris = new ArrayList<>();
                                               uris.add(fileUri);
                                               Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                                       "mailto", mailClient, null));
                                               emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Mail subject");
                                               List<ResolveInfo> resolveInfos = getActivity().getPackageManager().queryIntentActivities(emailIntent, 0);
                                               List<LabeledIntent> intents = new ArrayList<>();
                                               for (ResolveInfo info : resolveInfos) {
                                                   Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                                                   intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                                                   intent.putExtra(Intent.EXTRA_EMAIL, new String[]{mailClient});
                                                   intent.putExtra(Intent.EXTRA_SUBJECT, "Facture");

                                                   SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                                                   String iban = sharedPreferences.getString("iban", "FR7616958000016908274069822");
                                                   String bic = sharedPreferences.getString("bic", "QNTOFRP1XXX");
                                                   String bankAddress = sharedPreferences.getString("bankAddress", "CM3-VTC, 5 RUE AMBOURGET, Chez M chouchene moez, 93600, AULNAY-SOUS-BOIS - FR");

                                                   intent.putExtra(Intent.EXTRA_TEXT, "Bonjour.\n\n" +
                                                        "Veuillez trouver ci-joint la facture demandée.\n\n" +
                                                        "Bien cordialement.\n\n" +
                                                        "\n\n"+
                                                        "IBAN : " + iban + " \n" +
                                                        "BIC : " + bic + " \n" +
                                                        "Adresse du titulaire : " + bankAddress
                                                        );
                                                   intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); //ArrayList<Uri> of attachment Uri's
                                                   intents.add(new LabeledIntent(intent, info.activityInfo.packageName, info.loadLabel(getActivity().getPackageManager()), info.icon));
                                               }
                                               Intent chooser = Intent.createChooser(intents.remove(intents.size() - 1), "Send email with attachments...");
                                               chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new LabeledIntent[intents.size()]));
                                               startActivity(chooser);
                                           }
                                       }

        );

        shareButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("application/pdf");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Partage de facture");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(shareIntent, "Partager avec"));
                    }
                }
        );

        pdfWebView.fromUri(fileUri)
                .enableSwipe(true) // allows to block changing pages using swipe
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true) // render annotations (such as comments, colors or forms)
                .password(null)
                .scrollHandle(null)
                .enableAntialiasing(true) // improve rendering a little bit on low-res screens
                // spacing between pages in dp. To define spacing color, set view background
                .spacing(0)
                .autoSpacing(false) // add dynamic spacing to fit each page on its own on the screen
                .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view
                .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.
                .pageSnap(false) // snap pages to screen boundaries
                .pageFling(false) // make a fling change only a single page like ViewPager
                .nightMode(false) // toggle night mode
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