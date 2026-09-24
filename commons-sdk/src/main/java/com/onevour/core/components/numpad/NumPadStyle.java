package com.onevour.core.components.numpad;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.Px;

public class NumPadStyle {

    private Typeface typeface;
    private Integer dialogBackgroundColor;
    private Integer titleTextColor;
    private Float titleTextSizePx;
    private Integer resultTextColor;
    private Float resultTextSizePx;
    private Integer keyTextColor;
    private Float keyTextSizePx;
    private Drawable keyBackgroundDrawable;
    private Integer keyBackgroundColor;

    public NumPadStyle() {
    }

    public Typeface getTypeface() {
        return typeface;
    }

    public Integer getDialogBackgroundColor() {
        return dialogBackgroundColor;
    }

    public Integer getTitleTextColor() {
        return titleTextColor;
    }

    public Float getTitleTextSizePx() {
        return titleTextSizePx;
    }

    public Integer getResultTextColor() {
        return resultTextColor;
    }

    public Float getResultTextSizePx() {
        return resultTextSizePx;
    }

    public Integer getKeyTextColor() {
        return keyTextColor;
    }

    public Float getKeyTextSizePx() {
        return keyTextSizePx;
    }

    public Drawable getKeyBackgroundDrawable() {
        return keyBackgroundDrawable;
    }

    public Integer getKeyBackgroundColor() {
        return keyBackgroundColor;
    }

    public static class Builder {

        private final NumPadStyle style = new NumPadStyle();

        public Builder setTypeface(Typeface typeface) {
            style.typeface = typeface;
            return this;
        }

        public Builder setDialogBackgroundColor(@ColorInt int color) {
            style.dialogBackgroundColor = color;
            return this;
        }

        public Builder setTitleTextColor(@ColorInt int color) {
            style.titleTextColor = color;
            return this;
        }

        public Builder setTitleTextSizePx(@Px float sizePx) {
            style.titleTextSizePx = sizePx;
            return this;
        }

        public Builder setResultTextColor(@ColorInt int color) {
            style.resultTextColor = color;
            return this;
        }

        public Builder setResultTextSizePx(@Px float sizePx) {
            style.resultTextSizePx = sizePx;
            return this;
        }

        public Builder setKeyTextColor(@ColorInt int color) {
            style.keyTextColor = color;
            return this;
        }

        public Builder setKeyTextSizePx(@Px float sizePx) {
            style.keyTextSizePx = sizePx;
            return this;
        }

        public Builder setKeyBackgroundDrawable(Drawable drawable) {
            style.keyBackgroundDrawable = drawable;
            return this;
        }

        public Builder setKeyBackgroundColor(@ColorInt int color) {
            style.keyBackgroundColor = color;
            return this;
        }

        public NumPadStyle build() {
            return style;
        }
    }
}
