package com.onevour.sdk.impl.modules.fragment.controllers;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;

import com.onevour.core.components.fragment.FragmentNavigation;
import com.onevour.sdk.impl.R;

public class FragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_fragment);
        FragmentNavigation.addBackStack(this, R.id.container, PageOneFragment.newInstance(), "A");
        FragmentNavigation.addBackStack(this, R.id.container, PageTwoFragment.newInstance(), "B");
        FragmentNavigation.addBackStack(this, R.id.container, PageThreeFragment.newInstance(), "C");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (FragmentNavigation.onBackPressed(this, "A")) {
            super.onBackPressed();
        } else {
            finish();
        }
    }
}
