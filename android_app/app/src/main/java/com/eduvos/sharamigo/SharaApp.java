package com.eduvos.sharamigo;

import android.app.Application;

public class SharaApp extends Application {
    private static SharaApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static SharaApp get() {
        return instance;
    }
}
