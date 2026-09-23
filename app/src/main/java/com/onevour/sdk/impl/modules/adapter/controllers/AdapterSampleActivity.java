package com.onevour.sdk.impl.modules.adapter.controllers;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.onevour.core.utilities.beans.BeanCopy;
import com.onevour.core.utilities.helper.UIHelper;
import com.onevour.core.components.recycleview.AdapterGeneric;
import com.onevour.core.components.recycleview.RecyclerViewScrollListener;

import com.onevour.sdk.impl.databinding.ActivityMasterSampleBinding;
import com.onevour.sdk.impl.modules.adapter.components.AdapterSampleData;
import com.onevour.sdk.impl.SampleData;
import com.onevour.sdk.impl.modules.adapter.model.SampleDataMV;

import java.util.ArrayList;
import java.util.List;


public class AdapterSampleActivity extends AppCompatActivity implements AdapterGeneric.AdapterListener<SampleData>, RecyclerViewScrollListener.PaginationListener<SampleData>, AdapterSampleData.HolderSampleData.Listener {

    private String TAG = "MA-APP-AD";

    AdapterSampleData adapter = new AdapterSampleData();

    ActivityMasterSampleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMasterSampleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIHelper.initRecyclerView(binding.rvSampleData, adapter, this, true);
        adapter.setHolderListener(this);
//        init();
        binding.add.setOnClickListener(this::addNewSampleData);
    }

    private void addNewSampleData(View view) {
        int size = adapter.getAdapterList().size();
        SampleDataMV sampleDataMV = new SampleDataMV(1, new SampleData(size, "next : ".concat(String.valueOf(size))));
        adapter.addMore(sampleDataMV);
//        init();
    }

    private void init() {
//        binding.rvSampleData.postDelayed(() -> {
//            for (int i = 0; i < 100; i++) {
//                adapter.addMore(new SampleDataMV(2, new SampleData(i, "next : ".concat(String.valueOf(i)))));
//            }
//        }, 500);
        binding.rvSampleData.postDelayed(() -> {
            int i = 0;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
            i++;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
            i++;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
            i++;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
            i++;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
            i++;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
            i++;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
            i++;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
            i++;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
            i++;
            adapter.addMore(new SampleDataMV(1, new SampleData(i, "next : ".concat(String.valueOf(i)))));
        }, 500);
    }

    private void requestNextPage(boolean resultSuccess) {
        if (Looper.myLooper() == null) Looper.prepare();
        new Handler().postDelayed(() -> {
            List<SampleDataMV> list = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                list.add(new SampleDataMV(new SampleData("next : ".concat(String.valueOf(i)))));
            }
            if (resultSuccess) {
                adapter.setValue(list);
            } else {
//                adapter.er("HttpErrorResponse, tap for reload");
            }

        }, 2000);
    }

    @Override
    public void onLoadRetry(int index, SampleData o) {
        requestNextPage(true);
        Log.d(TAG, "retry load data from last :  from index: ".concat(String.valueOf(index)));
    }

    @Override
    public void loadMoreItems(SampleData sampleData) {
        requestNextPage(false);
        Log.d(TAG, "load more from last value: ".concat(sampleData.getName()));

    }

    @Override
    public void updateAge(int index, SampleDataMV o) {
        // SampleDataMV o = BeanCopy.gson(sampleDataMV, SampleDataMV.class);
        int currentAge = o.getModel().getAge();
        o.getModel().setAge(currentAge + 1);
        adapter.updateItem(index, o);
    }

}
