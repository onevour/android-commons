package com.onevour.sdk.impl.modules.fragmentbottomnavigation.controllers;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.onevour.core.components.fragment.FragmentBottomNavigationManager;
import com.onevour.sdk.impl.R;
import com.onevour.sdk.impl.databinding.ActivityFragmentBottomNavigationBinding;
import com.onevour.sdk.impl.modules.fragment.controllers.PageOneFragment;
import com.onevour.sdk.impl.modules.fragment.controllers.PageThreeFragment;
import com.onevour.sdk.impl.modules.fragment.controllers.PageTwoFragment;

//import butterknife.BindView;
//import butterknife.ButterKnife;

public class FragmentBottomNavigationActivity extends AppCompatActivity implements
        BottomNavigationView.OnNavigationItemSelectedListener {

//    @BindView(R.id.navigation)
//    BottomNavigationView navigation;

    private FragmentBottomNavigationManager manager = new FragmentBottomNavigationManager();

    ActivityFragmentBottomNavigationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFragmentBottomNavigationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        ButterKnife.bind(this);
        manager.onCreateStateHandler(savedInstanceState);
        manager.setup(this, R.id.container, getSupportFragmentManager(), binding.navigation, this);
        manager.register(R.id.navigation_f_a, PageOneFragment.newInstance(), "A");
        manager.register(R.id.navigation_f_b, PageTwoFragment.newInstance(), "B");
        manager.register(R.id.navigation_f_c, PageThreeFragment.newInstance(), "C");
        manager.active(R.id.navigation_f_a);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        return manager.onNavigationItemSelected(menuItem);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        manager.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

}
