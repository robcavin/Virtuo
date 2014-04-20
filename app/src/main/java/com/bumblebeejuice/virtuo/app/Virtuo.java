package com.bumblebeejuice.virtuo.app;

import android.app.Application;
import android.content.Context;

/**
 * Created by robcavin on 4/19/14.
 */
public class Virtuo extends Application {

    public static Context __context;

    public Virtuo() {
        __context = this;
    }

    public static Context context () {
        return __context;
    }

}
