package com.onevour.sdk.impl.modules.mvvm.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import com.onevour.sdk.impl.R;
import com.onevour.sdk.impl.databinding.ActivityMvvmBinding;
import com.onevour.sdk.impl.modules.mvvm.models.LoginUser;
import com.onevour.sdk.impl.modules.mvvm.viewmodels.LoginViewModel;

public class MVVMActivity extends AppCompatActivity {

    LoginViewModel viewModel;

    ActivityMvvmBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_mvvm);
        binding.setLifecycleOwner(this);
        binding.setLoginViewModel(viewModel);
        viewModel.getUser().observe(this, this::onLoginSuccess);
        viewModel.init();
//        viewModel.getUser().observe(this, new Observer<String>() {
//            @Override
//            public void onChanged(String s) {
//                Log.d("MVVM", "text change " + s);
//            }
//        });
    }

    public void onLoginSuccess(LoginUser loginUser) {
//        Log.d("MVVM", "model change " + loginUser.getUserDTO().getUsername());
    }
}