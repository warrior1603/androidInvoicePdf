package com.chouchene.factures.adapter;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintAttributes;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo info = new PrintDocumentInfo.Builder("document")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build();

        callback.onLayoutFinished(info, true);
    }


    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal, WriteResultCallback callback) {
        try (FileInputStream input = new FileInputStream(new File(pdfFilePath));
             FileOutputStream output = new FileOutputStream(destination.getFileDescriptor())) {
            byte[] buffer = new byte[1024];
            int size;
            while ((size = input.read(buffer)) != -1 && !cancellationSignal.isCanceled()) {
                output.write(buffer, 0, size);
            }

            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
            } else {
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            }
        } catch (Exception e) {
            callback.onWriteFailed(e.toString());
            e.printStackTrace();
        }
    }
    
}

