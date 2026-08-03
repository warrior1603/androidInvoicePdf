package com.chouchene.factures.adapter;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintAttributes;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfDocumentAdapter extends PrintDocumentAdapter {
    private Context context;
    private String pdfFilePath;

    public PdfDocumentAdapter(Context context, String pdfFilePath) {
        this.context = context;
        this.pdfFilePath = pdfFilePath;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal, LayoutResultCallback callback,
                         Bundle extras) {
        Log.d("PdfDocumentAdapter", "onLayout started");
        if (cancellationSignal != null && cancellationSignal.isCanceled()) {
            Log.d("PdfDocumentAdapter", "onLayout cancelled");
            callback.onLayoutCancelled();
            return;
        }

        int pages = PrintDocumentInfo.PAGE_COUNT_UNKNOWN;
        try {
            File file = new File(pdfFilePath);
            if (file.exists()) {
                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer renderer = new PdfRenderer(pfd);
                pages = renderer.getPageCount();
                renderer.close();
                pfd.close();
                Log.d("PdfDocumentAdapter", "Detected pages: " + pages);
            } else {
                Log.e("PdfDocumentAdapter", "File not found: " + pdfFilePath);
            }
        } catch (Exception e) {
            Log.e("PdfDocumentAdapter", "Error getting page count", e);
        }

        PrintDocumentInfo info = new PrintDocumentInfo.Builder(new File(pdfFilePath).getName())
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pages)
                .build();

        callback.onLayoutFinished(info, true);
        Log.d("PdfDocumentAdapter", "onLayout finished");
    }


    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {
        Log.d("PdfDocumentAdapter", "onWrite started");
        try (InputStream input = new FileInputStream(pdfFilePath);
             OutputStream output = new FileOutputStream(destination.getFileDescriptor())) {

            byte[] buf = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buf)) > 0) {
                if (cancellationSignal != null && cancellationSignal.isCanceled()) {
                    Log.d("PdfDocumentAdapter", "onWrite cancelled");
                    callback.onWriteCancelled();
                    return;
                }
                output.write(buf, 0, bytesRead);
            }

            Log.d("PdfDocumentAdapter", "onWrite finished successfully");
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

        } catch (Exception e) {
            Log.e("PdfDocumentAdapter", "Error writing PDF", e);
            callback.onWriteFailed(e.toString());
        }
    }
    
}

