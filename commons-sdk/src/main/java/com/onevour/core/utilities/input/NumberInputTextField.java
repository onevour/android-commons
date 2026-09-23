package com.onevour.core.utilities.input;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import com.onevour.core.R;
import com.onevour.core.utilities.format.NFormat;

import java.text.NumberFormat;

public class NumberInputTextField extends AppCompatEditText {

    private final NumberInput numberInput = new NumberInput();

    public NumberInputTextField(Context context) {
        super(context);
        init(null);
    }

    public NumberInputTextField(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public NumberInputTextField(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        if (isInEditMode()) return;
        double min = 0;
        double max = Integer.MAX_VALUE;
        boolean decimal = false;
        boolean showMax = false;
        String title = null;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.NumberInputTextField);
            try {
                min = parseDouble(a.getString(R.styleable.NumberInputTextField_minValue), min);
                max = parseDouble(a.getString(R.styleable.NumberInputTextField_maxValue), max);
                decimal = a.getBoolean(R.styleable.NumberInputTextField_isDecimal, false);
                showMax = a.getBoolean(R.styleable.NumberInputTextField_isShowMax, false);
                title = a.getString(R.styleable.NumberInputTextField_titleText);
            } finally {
                a.recycle();
            }
        }
        NumberFormat numberFormat = decimal ? NFormat.currency() : null;
        numberInput.setup(this, numberFormat, min, max);
        if (title != null) numberInput.setTitle(title);
        if (showMax) numberInput.showMaxValue();
    }

    private double parseDouble(@Nullable String value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void setListener(NumberInput.Listener listener) {
        numberInput.setListener(listener);
    }

    public void updateMinMax(double min, double max) {
        numberInput.updateMinMax(min, max);
    }

    public void updateMinMax(double min, double max, boolean showMax) {
        numberInput.updateMinMax(min, max, showMax);
    }

    public void inputValue(double value) {
        numberInput.inputValue(value);
    }

    public void showMaxValue() {
        numberInput.showMaxValue();
    }

    public String getValueString() {
        return numberInput.getValueString();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        numberInput.destroy();
    }
}
