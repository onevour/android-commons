package com.onevour.sdk.impl.modules.form.controllers;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import com.onevour.core.utilities.format.NFormat;
import com.onevour.core.utilities.input.NumberInput;
import com.onevour.core.utilities.input.NumberInputStyle;
import com.onevour.sdk.impl.databinding.ActivityFormSimpleBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class FormSimpleActivity extends AppCompatActivity {

    private final NumberInput numPadText = new NumberInput();
    private final NumberInput numPadIndo = new NumberInput();
    private final NumberInput numPadUs = new NumberInput();

    private ActivityFormSimpleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFormSimpleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.inputNumber.setText(String.valueOf(Integer.MAX_VALUE));
        binding.inputDecimal.setText(NFormat.currencyFormat(5603169.26));
        binding.inputDecimal2.setText(NFormat.currencyFormat(100000.0));

        // 1. Custom Light Mode Input
        binding.inputCustomLight.setText(NFormat.currencyFormat(50000.0));

        // 2. Custom Dark Mode Input
        NumberInputStyle darkStyle = new NumberInputStyle.Builder()
                .setDialogBackgroundColor(Color.parseColor("#121212"))
                .setTitleTextColor(Color.parseColor("#BB86FC"))
                .setResultTextColor(Color.parseColor("#03DAC6"))
                .setKeyTextColor(Color.parseColor("#E0E0E0"))
                .build();
        binding.inputCustomDark.setStyle(darkStyle);
        binding.inputCustomDark.setText(NFormat.currencyFormat(75000.0));

        // 3. Locale Indonesia (Pemisah Desimal: Koma ',')
        NumberFormat formatIndo = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        formatIndo.setMinimumFractionDigits(2);
        formatIndo.setMaximumFractionDigits(2);
        numPadIndo.setup(binding.inputLocaleIndo, formatIndo, 0, 10000000.0);
        binding.inputLocaleIndo.setText(formatIndo.format(1250000.50));

        // 4. Locale US (Pemisah Desimal: Titik '.')
        NumberFormat formatUs = NumberFormat.getNumberInstance(Locale.US);
        formatUs.setMinimumFractionDigits(2);
        formatUs.setMaximumFractionDigits(2);
        numPadUs.setup(binding.inputLocaleUs, formatUs, 0, 10000000.0);
        binding.inputLocaleUs.setText(formatUs.format(1250000.50));

        // 5. Input Trigger via Text
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
        numPadIndo.destroy();
        numPadUs.destroy();
    }
}
