package com.onevour.core.components.numpad;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.res.ResourcesCompat;

import com.onevour.core.R;
import com.onevour.core.utilities.format.NFormat;

import java.text.NumberFormat;

public class NumPadField extends AppCompatEditText {

    private final NumPad numPad = new NumPad();

    public NumPadField(Context context) {
        super(context);
        init(null);
    }

    public NumPadField(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public NumPadField(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        if (isInEditMode()) return;
        double min = 0;
        double max = Integer.MAX_VALUE;
        boolean decimal = false;
        boolean showMax = false;
        boolean useBottomSheet = false;
        String title = null;
        NumPadStyle style = null;

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.NumberInputTextField);
            try {
                min = parseDouble(a.getString(R.styleable.NumberInputTextField_minValue), min);
                max = parseDouble(a.getString(R.styleable.NumberInputTextField_maxValue), max);
                decimal = a.getBoolean(R.styleable.NumberInputTextField_isDecimal, false);
                showMax = a.getBoolean(R.styleable.NumberInputTextField_isShowMax, false);
                useBottomSheet = a.getBoolean(R.styleable.NumberInputTextField_useBottomSheet, false);
                title = a.getString(R.styleable.NumberInputTextField_titleText);

                NumPadStyle.Builder styleBuilder = new NumPadStyle.Builder();
                boolean styleConfigured = false;

                int fontResId = a.getResourceId(R.styleable.NumberInputTextField_dialogFontFamily, 0);
                if (fontResId != 0) {
                    Typeface tf = ResourcesCompat.getFont(getContext(), fontResId);
                    if (tf != null) {
                        styleBuilder.setTypeface(tf);
                        styleConfigured = true;
                    }
                }

                if (a.hasValue(R.styleable.NumberInputTextField_dialogBackground)) {
                    styleBuilder.setDialogBackgroundColor(a.getColor(R.styleable.NumberInputTextField_dialogBackground, 0));
                    styleConfigured = true;
                }

                if (a.hasValue(R.styleable.NumberInputTextField_dialogTitleColor)) {
                    styleBuilder.setTitleTextColor(a.getColor(R.styleable.NumberInputTextField_dialogTitleColor, 0));
                    styleConfigured = true;
                }

                if (a.hasValue(R.styleable.NumberInputTextField_dialogTitleTextSize)) {
                    styleBuilder.setTitleTextSizePx(a.getDimension(R.styleable.NumberInputTextField_dialogTitleTextSize, 0));
                    styleConfigured = true;
                }

                if (a.hasValue(R.styleable.NumberInputTextField_dialogResultColor)) {
                    styleBuilder.setResultTextColor(a.getColor(R.styleable.NumberInputTextField_dialogResultColor, 0));
                    styleConfigured = true;
                }

                if (a.hasValue(R.styleable.NumberInputTextField_dialogResultTextSize)) {
                    styleBuilder.setResultTextSizePx(a.getDimension(R.styleable.NumberInputTextField_dialogResultTextSize, 0));
                    styleConfigured = true;
                }

                if (a.hasValue(R.styleable.NumberInputTextField_dialogKeyTextColor)) {
                    styleBuilder.setKeyTextColor(a.getColor(R.styleable.NumberInputTextField_dialogKeyTextColor, 0));
                    styleConfigured = true;
                }

                if (a.hasValue(R.styleable.NumberInputTextField_dialogKeyTextSize)) {
                    styleBuilder.setKeyTextSizePx(a.getDimension(R.styleable.NumberInputTextField_dialogKeyTextSize, 0));
                    styleConfigured = true;
                }

                if (a.hasValue(R.styleable.NumberInputTextField_dialogKeyBackground)) {
                    Drawable bgDrawable = a.getDrawable(R.styleable.NumberInputTextField_dialogKeyBackground);
                    if (bgDrawable != null) {
                        styleBuilder.setKeyBackgroundDrawable(bgDrawable);
                    } else {
                        styleBuilder.setKeyBackgroundColor(a.getColor(R.styleable.NumberInputTextField_dialogKeyBackground, 0));
                    }
                    styleConfigured = true;
                }

                if (styleConfigured) {
                    style = styleBuilder.build();
                }

            } finally {
                a.recycle();
            }
        }
        NumberFormat numberFormat = decimal ? NFormat.currency() : null;
        if (useBottomSheet) {
            numPad.setUseBottomSheet(true);
        }
        numPad.setup(this, numberFormat, min, max);
        if (style != null) {
            numPad.setStyle(style);
        }
        if (title != null) numPad.setTitle(title);
        if (showMax) numPad.showMaxValue();
    }

    private double parseDouble(@Nullable String value, double fallback) {
        if (value == null) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void setStyle(NumPadStyle style) {
        numPad.setStyle(style);
    }

    public void setUseBottomSheet(boolean useBottomSheet) {
        numPad.setUseBottomSheet(useBottomSheet);
    }

    public void setListener(NumPad.Listener listener) {
        numPad.setListener(listener);
    }

    public void updateMinMax(double min, double max) {
        numPad.updateMinMax(min, max);
    }

    public void updateMinMax(double min, double max, boolean showMax) {
        numPad.updateMinMax(min, max, showMax);
    }

    public void inputValue(double value) {
        numPad.inputValue(value);
    }

    public void showMaxValue() {
        numPad.showMaxValue();
    }

    public String getValueString() {
        return numPad.getValueString();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        numPad.destroy();
    }
}
