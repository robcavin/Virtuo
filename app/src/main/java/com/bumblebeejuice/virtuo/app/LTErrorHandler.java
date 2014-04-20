package com.bumblebeejuice.virtuo.app;

import android.util.Log;

/**
 * Created by robcavin on 12/17/13.
 */
public class LTErrorHandler {

    public static void handleException(Exception e, boolean reportToCrashlytics) {
        Log.e("LIGHTT ERROR", "------------------------------------------");
        Log.e("LIGHTT ERROR", e.toString());
        Log.e("LIGHTT ERROR", Log.getStackTraceString(e));
        Log.e("LIGHTT ERROR", "------------------------------------------");
    }

    public static void handleException(Exception e) {
        handleException(e,true);
    }
}
