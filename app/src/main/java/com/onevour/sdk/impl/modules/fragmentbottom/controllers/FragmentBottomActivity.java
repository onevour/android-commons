package com.onevour.sdk.impl.modules.fragmentbottom.controllers;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.onevour.core.rest.components.RestRequest;
import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.models.HttpResponse;
import com.onevour.sdk.impl.databinding.ActivityFragmentBottomBinding;


public class FragmentBottomActivity extends AppCompatActivity {

//    @BindView(R.id.btn_show)
//    Button btnShow;


    ActivityFragmentBottomBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFragmentBottomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //ButterKnife.bind(this);
        binding.btnShow.setOnClickListener(this::onBtnShowClicked);
        binding.btnShowAsync.setOnClickListener(this::onBtnShowLoaderClicked);
    }

    //@OnClick(R.id.btn_show)
    public void onBtnShowClicked(View view) {
        RestRequest.post("https://api.digitalrecordcard.com/index.php/api_v1/login", new String(""), new HttpListener<String>() {
            @Override
            public void onSuccess(HttpResponse response, String responseBody) {
                Log.d("API-TEST", response);
            }

            @Override
            public void onError(HttpErrorResponse httpErrorResponse) {
                Log.e("API-TEST", httpErrorResponse.getMessage());
            }
        });
    }

//    @OnClick(R.id.btn_show_async)
    public void onBtnShowLoaderClicked(View view) {
        BottomLoaderFragment fragment = BottomLoaderFragment.newInstance();
        fragment.show(getSupportFragmentManager(), BottomLoaderFragment.TAG);
    }
}
