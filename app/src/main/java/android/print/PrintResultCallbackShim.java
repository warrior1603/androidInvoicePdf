package android.print;

import android.os.Bundle;
import android.print.PrintDocumentAdapter.LayoutResultCallback;
import android.print.PrintDocumentAdapter.WriteResultCallback;

public abstract class PrintResultCallbackShim {

    public static abstract class LayoutResultCallbackShim extends LayoutResultCallback {
        public LayoutResultCallbackShim() {
            super();
        }
    }

    public static abstract class WriteResultCallbackShim extends WriteResultCallback {
        public WriteResultCallbackShim() {
            super();
        }
    }
}
