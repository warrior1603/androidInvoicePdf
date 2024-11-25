package com.chouchene.factures.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.print.PrintAttributes;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_webview_pdf_sendmail, container, false);
        Bundle bundle = this.getArguments();
        String filePath = bundle.getString("file_path");
        String mailClient = bundle.getString("mail_client");

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
                        PrintManager printManager = (PrintManager) getActivity().getSystemService(Context.PRINT_SERVICE);

                        try {
                            PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(getContext(), filePath);
                            printManager.print("Document Print Job", printAdapter, new PrintAttributes.Builder().build());
                        } catch (Exception e) {
                            e.printStackTrace();
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
                                                    intent.putExtra(Intent.EXTRA_TEXT, "Bonjour.\n\n" +
                                                        "Veuillez trouver ci-joint la facture demandée.\n\n" +
                                                        "Bien cordialement.\n\n" +
                                                        "\n\n"+
                                                        "IBAN : FR7616958000016908274069822 \n"+
                                                        "BIC : QNTOFRP1XXX \n"+
                                                        "Adresse du titulaire : CM3-VTC, 5 RUE AMBOURGET, Chez M chouchene moez, 93600, AULNAY-SOUS-BOIS - FR"
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
                        shareIntent.setType("text/plain"); // Adjust type if sharing different content
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Partage de fichier");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        startActivity(Intent.createChooser(shareIntent, "Partager sur"));
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
}