package com.onevour.sdk.impl;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.onevour.sdk.impl.modules.bluetooth.PrinterManager;
import com.onevour.sdk.impl.modules.bluetooth.services.v1.BluetoothSDKService;
import com.onevour.sdk.impl.modules.dinjection.injections.AppComponent;
import com.onevour.sdk.impl.modules.dinjection.injections.AppModule;
import com.onevour.sdk.impl.modules.dinjection.injections.DaggerAppComponent;

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
