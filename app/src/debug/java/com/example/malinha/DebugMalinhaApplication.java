package com.example.malinha;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public final class DebugMalinhaApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
        appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());

        // Request App Check once so the SDK can print its debug secret before AI is used.
        appCheck.getAppCheckToken(false);
    }
}
