package com.onevour.sdk.impl.modules.formscroll.controllers;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.onevour.core.components.numpad.NumPad;
import com.onevour.sdk.impl.databinding.ActivityFormScrollBinding;


import java.util.ArrayList;
import java.util.List;

//import butterknife.BindView;
//import butterknife.ButterKnife;

public class FormScrollActivity extends AppCompatActivity {

    private static final String TAG = "F-SCROLL";

//    @BindView(R.id.input_number_1)
//    EditText inputNumber1;
//    @BindView(R.id.input_number_2)
//    EditText inputNumber2;
//    @BindView(R.id.input_number_3)
//    EditText inputNumber3;
//    @BindView(R.id.input_number_4)
//    EditText inputNumber4;
//    @BindView(R.id.input_number_5)
//    EditText inputNumber5;
//    @BindView(R.id.input_number_6)
//    EditText inputNumber6;
//    @BindView(R.id.input_number_7)
//    EditText inputNumber7;
//    @BindView(R.id.input_number_8)
//    EditText inputNumber8;
//    @BindView(R.id.input_number_9)
//    EditText inputNumber9;
//    @BindView(R.id.input_number_10)
//    EditText inputNumber10;
//    @BindView(R.id.input_number_11)
//    EditText inputNumber11;
//    @BindView(R.id.input_number_12)
//    EditText inputNumber12;
//    @BindView(R.id.input_number_13)
//    EditText inputNumber13;
//    @BindView(R.id.input_number_14)
//    EditText inputNumber14;
//    @BindView(R.id.input_number_15)
//    EditText inputNumber15;
//
//    @BindView(R.id.scroll_view)
//    ScrollView scrollView;

    private long lastScrollUpdate = -1;

    private boolean isScroll = false;

    private final List<NumPad> numPads = new ArrayList<>();

    ActivityFormScrollBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_form_scroll);
//        ButterKnife.bind(this);
        binding = ActivityFormScrollBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        List<EditText> editTextList = new ArrayList<>();
        editTextList.add(binding.inputNumber1);
        editTextList.add(binding.inputNumber2);
        editTextList.add(binding.inputNumber3);
        editTextList.add(binding.inputNumber4);
        editTextList.add(binding.inputNumber5);
        editTextList.add(binding.inputNumber6);
        editTextList.add(binding.inputNumber7);
        editTextList.add(binding.inputNumber8);
        editTextList.add(binding.inputNumber9);
        editTextList.add(binding.inputNumber10);
        editTextList.add(binding.inputNumber11);
        editTextList.add(binding.inputNumber12);
        editTextList.add(binding.inputNumber13);
        editTextList.add(binding.inputNumber14);
        editTextList.add(binding.inputNumber15);
        for (EditText e : editTextList) {
            e.setText(String.valueOf(0));
            NumPad numPad = new NumPad(e);
//            numPad.setScrollFlag(isScroll);
            numPads.add(numPad);
        }
//        ListenScrollChangesHelper listener = new ListenScrollChangesHelper();
//        listener.addViewToListen(scrollView, (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
//            Log.d(TAG, scrollX + "|" + oldScrollX + "|" + scrollY + "|" + oldScrollY);
//            if (lastScrollUpdate == -1) {
//                scrollView.postDelayed(new ScrollStateHandler(), 100);
//            }
//            lastScrollUpdate = System.currentTimeMillis();
//        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (NumPad numPad : numPads) {
            numPad.destroy();
        }
    }

    private class ScrollStateHandler implements Runnable {

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastScrollUpdate) > 100) {
                lastScrollUpdate = -1;
                isScroll = true;
                Log.d(TAG, "scroll stop");
            } else {
                binding.scrollView.postDelayed(this, 100);
            }
        }
    }
}
