package com.onevour.sdk.impl.modules.form.controllers;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import com.onevour.core.utilities.format.NFormat;
import com.onevour.core.utilities.input.NumberInput;
import com.onevour.sdk.impl.databinding.ActivityFormSimpleBinding;


public class FormSimpleActivity extends AppCompatActivity {

    private final NumberInput numPadText = new NumberInput();

    private ActivityFormSimpleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFormSimpleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.inputNumber.setText(String.valueOf(Integer.MAX_VALUE));
        binding.inputDecimal.setText(NFormat.currencyFormat(5603169.26));
        binding.inputDecimal2.setText(NFormat.currencyFormat(100000.0));
        numPadText.setup(this, NFormat.currency(), 0, Double.MAX_VALUE);
        binding.inputFromText.setOnClickListener(this::updateValue);
    }

    private void updateValue(View view) {
        numPadText.setTitle("Maximum payment");
        numPadText.updateMinMax(0, 30000, true);
        numPadText.inputValue(2000.98);
        numPadText.setListener(new NumberInput.Listener() {
            @Override
            public void onSubmitValue() {

            }

            @Override
            public void onValue(@IdRes int id, boolean isDecimal, int intValue, double doubleValue) {
                binding.inputFromText.setText(numPadText.getValueString());
            }

        });
        numPadText.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        numPadText.destroy();
    }
}
