package com.onevour.sdk.impl.modules.main.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.room.Room;

import com.onevour.core.base.BaseActivity;
import com.onevour.core.utilities.commons.ContextHelper;
import com.onevour.core.utilities.commons.ValueOf;
import com.onevour.sdk.impl.applications.configuration.AppDatabase;
import com.onevour.sdk.impl.databinding.ActivityMainBinding;
import com.onevour.sdk.impl.modules.adapter.controllers.AdapterSampleActivity;
import com.onevour.sdk.impl.modules.bluetooth.BluetoothActivity;
import com.onevour.sdk.impl.modules.location.LocationCaptureActivity;
import com.onevour.sdk.impl.modules.chat.ChatActivity;
import com.onevour.sdk.impl.modules.dinjection.controllers.DInjectionActivity;
import com.onevour.sdk.impl.modules.form.controllers.DeepLinkActivity;
import com.onevour.sdk.impl.modules.form.controllers.FormCopyActivity;
import com.onevour.sdk.impl.modules.form.controllers.FormDatabaseActivity;
import com.onevour.sdk.impl.modules.form.controllers.FormSimpleActivity;
import com.onevour.sdk.impl.modules.formscroll.controllers.FormScrollActivity;
import com.onevour.sdk.impl.modules.fragment.controllers.FragmentActivity;
import com.onevour.sdk.impl.modules.fragmentbottom.controllers.FragmentBottomActivity;
import com.onevour.sdk.impl.modules.fragmentbottomnavigation.controllers.FragmentBottomNavigationActivity;
import com.onevour.sdk.impl.modules.main.components.SampleAdapter;
import com.onevour.sdk.impl.modules.main.models.Sample;
import com.onevour.sdk.impl.modules.main.models.SampleMV;
import com.onevour.sdk.impl.modules.mvvm.views.MVVMActivity;
import com.onevour.sdk.impl.modules.preference.controllers.PreferenceActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * update by Zuliadin at 2023-07-29
 */
public class MainActivity extends BaseActivity implements SampleAdapter.SampleHolder.Listener {

    private String TAG = MainActivity.class.getSimpleName();

    private SampleAdapter adapter = new SampleAdapter();

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextHelper.init(getApplication());
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init(binding.rvSample, adapter);
        adapter.setHolderListener(this);
        List<SampleMV> samples = new ArrayList<>();
        samples.add(new SampleMV("Adapter", AdapterSampleActivity.class));
        samples.add(new SampleMV("Fragment BackStack", FragmentActivity.class));
        samples.add(new SampleMV("Fragment Bottom", FragmentBottomActivity.class));
        samples.add(new SampleMV("Fragment Bottom Navigation", FragmentBottomNavigationActivity.class));
        samples.add(new SampleMV("Form Simple", FormSimpleActivity.class));
        samples.add(new SampleMV("Form Copy", FormCopyActivity.class));
        samples.add(new SampleMV("Form Scroll", FormScrollActivity.class));
        samples.add(new SampleMV("Form Deep Link", DeepLinkActivity.class));
//        samples.add(new SampleMV("Form Database", FormDatabaseActivity.class));
//        samples.add(new SampleMV("MVVM", MVVMActivity.class));
//        samples.add(new SampleMV("Preference", PreferenceActivity.class));
//        samples.add(new SampleMV("Injection", DInjectionActivity.class));
        samples.add(new SampleMV("Bluetooth", BluetoothActivity.class));
        samples.add(new SampleMV("Location Capture", LocationCaptureActivity.class));
//        samples.add(new SampleMV("Chat", ChatActivity.class));
        adapter.setValue(samples);
        session.saveCollection("MENU", samples);
        init();
        initDatabase();
    }

    private void initDatabase() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "onevour-sdk").build();
    }

    private void init() {
        List<SampleMV> samples = session.findCollection("MENU", SampleMV.class);
        if (ValueOf.isNull(samples)) return;
        Log.d(TAG, "list size from ref session " + samples.size());
    }

    @Override
    public void onSelectedHolder(int index, Sample o) {
        startActivity(new Intent(this, o.getClazz()));
    }


}
