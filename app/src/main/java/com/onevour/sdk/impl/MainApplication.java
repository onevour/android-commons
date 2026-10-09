package com.onevour.sdk.impl;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.onevour.core.location.LocationCapture;
import com.onevour.core.utilities.commons.ContextHelper;
import com.onevour.sdk.impl.modules.bluetooth.PrinterManager;
import com.onevour.sdk.impl.modules.bluetooth.services.v1.BluetoothSDKService;
import com.onevour.sdk.impl.modules.dinjection.injections.AppComponent;
import com.onevour.sdk.impl.modules.dinjection.injections.AppModule;
import com.onevour.sdk.impl.modules.dinjection.injections.DaggerAppComponent;
import com.onevour.sdk.impl.modules.location.LocationSample;

public class MainApplication extends Application {

    public static AppComponent component;

    //private final PrinterManager printerManager = PrinterManager.newInstance();

    @Override
    public void onCreate() {
        super.onCreate();
        component = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
        startService(new Intent(this, BluetoothSDKService.class));
        // android-commons location capture: when to capture and what to do with a location is the sample's
        ContextHelper.init(this);
        LocationCapture.init(this, LocationSample.config(this));
        LocationCapture.startWatchdog();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        printerManager.setContext(this);
//        printerManager.proceedDiscovery();
//        printerManager.startServer();

    }

    // @Override protected void attachBaseContext(Context context) {}

}
