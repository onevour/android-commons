package com.onevour.core.utilities.input;

import android.util.Log;

import com.onevour.core.utilities.commons.ValueOf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class InputDouble implements NumberInputAdapter {

    private static final String TAG = InputDouble.class.getSimpleName();

    private final int decrease = 10;

    private AtomicReference<BigDecimal> value = new AtomicReference<>();

    private NumberFormat numberFormat;

    private volatile int cursor = 0;

    private volatile boolean isAfterPoint = false;

    private String decimalSeparator = ".";

    private double min = 0.00;

    private double max = Double.MAX_VALUE;

    public InputDouble(NumberFormat numberFormat, double min, double max) {
        value.set(BigDecimal.valueOf(0.00));
        if (!(numberFormat instanceof DecimalFormat)) {
            throw new IllegalArgumentException("numberFormat must be a DecimalFormat instance");
        }
        this.numberFormat = numberFormat;
        this.min = min;
        this.max = max;
        DecimalFormatSymbols d = ((DecimalFormat) numberFormat).getDecimalFormatSymbols();
        decimalSeparator = String.valueOf(d.getDecimalSeparator());
    }

    @Override
    public void validateInit() {
        BigDecimal fractionalPart = value.get().remainder(BigDecimal.ONE);
        isAfterPoint = fractionalPart.compareTo(BigDecimal.valueOf(0.00)) > 0;
        if (!isAfterPoint) this.cursor = 0;
//        Log.d(TAG, "fraction validate is " + fractionalPart.toPlainString() + " decimal" + isAfterPoint);
    }

    @Override
    public String getValueString() {
        return numberFormat.format(value.get());
    }

    @Override
    public void setValue(Double doubleValue) {
        try {
            reset();
            double clamped = Math.max(min, Math.min(doubleValue, max));
            char[] format = BigDecimal.valueOf(clamped).toPlainString().toCharArray();
            for (char c : format) {
                String value = String.valueOf(c);
                if (value.equalsIgnoreCase(".")) { // check native separator decimal
                    append(decimalSeparator);
                } else {
                    append(value);
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "failed to set value " + doubleValue, e);
        }
    }

    @Override
    public void setValueToMax() throws ParseException {
        reset();
        char[] format = BigDecimal.valueOf(max).toPlainString().toCharArray();
        for (char c : format) {
            String value = String.valueOf(c);
            if (value.equalsIgnoreCase(".")) { // check native separator decimal
                append(decimalSeparator);
            } else {
                append(value);
            }
        }
    }

    @Override
    public void setValue(String valueStr) throws ParseException {
        reset();
        if (ValueOf.isEmpty(valueStr)) {
            value.set(BigDecimal.valueOf(0.00));
            return;
        }
        double parsedValue = Objects.requireNonNull(numberFormat.parse(valueStr.trim())).doubleValue();
        double clamped = Math.max(min, Math.min(parsedValue, max));
        BigDecimal decimal = BigDecimal.valueOf(clamped);
        if (decimal.compareTo(BigDecimal.valueOf(0.00)) == 0) {
            value.set(BigDecimal.valueOf(0.00));
            return;
        }
        char[] chars = decimal.toPlainString().toCharArray();
        for (char c : chars) {
            String value = String.valueOf(c);
            if (value.equalsIgnoreCase(".")) { // check native separator decimal
                append(decimalSeparator);
            } else append(value);
        }
    }

    public void append(String... valueChars) throws ParseException {
        for (String o : valueChars) {
            append(o);
        }
    }

    @Override
    public void append(String valueChar) throws ParseException {
        Log.d(TAG, "input ".concat(valueChar));
        if (valueChar.equalsIgnoreCase(decimalSeparator)) {
            if (isAfterPoint) return;
            isAfterPoint = true;
            cursor = 0;
            return;
        }
        if (isAfterPoint) {
            appendAfterDecimal(valueChar);
        } else {
            BigDecimal decimal = value.get().multiply(BigDecimal.valueOf(decrease)).add(new BigDecimal(valueChar));
            if (decimal.compareTo(BigDecimal.valueOf(max)) > 0) return;
            value.set(decimal);
        }
    }

    @Override
    public void delete() {
        if (isAfterPoint) {
            BigDecimal fractionalPart = value.get().remainder(BigDecimal.ONE);
            deleteDecimal(fractionalPart);
            return;
        }
        BigDecimal decimal = value.get().divide(BigDecimal.valueOf(decrease), 2, RoundingMode.CEILING);
        BigDecimal diff = value.get().remainder(BigDecimal.valueOf(decrease));
        if (diff.compareTo(BigDecimal.valueOf(0.00)) > 0) {
            decimal = value.get().subtract(diff).divide(BigDecimal.valueOf(decrease), 2, RoundingMode.CEILING);
        }
        if (decimal.compareTo(BigDecimal.valueOf(max)) > 0) return;
        if (decimal.compareTo(BigDecimal.valueOf(min)) < 0) return;
        value.set(decimal);
    }

    /**
     * cursor tracks how many of the 2 decimal places are filled (0, 1 or 2). Appending
     * fills the next empty place (or overwrites the last one once both are full);
     * deleting clears the last-filled place and steps back one. This keeps append and
     * delete acting on the same slot, so undo-by-delete actually reverses the last append.
     */
    private void appendAfterDecimal(String valueChar) {
        BigDecimal fractionalPart = value.get().remainder(BigDecimal.ONE);
        String fractionalPartStr = fractionalPart.toPlainString();
        if (fractionalPartStr.length() == 3) fractionalPartStr = fractionalPartStr.concat("0");
        char[] values = fractionalPartStr.toCharArray();
        int fillIndex = Math.min(cursor, 1) + 2;
        values[fillIndex] = valueChar.charAt(0);
        BigDecimal newDecimal = new BigDecimal(new String(values));
        BigDecimal decimalValue = value.get().subtract(fractionalPart).add(newDecimal);
        if (decimalValue.compareTo(BigDecimal.valueOf(max)) > 0) {
            return;
        }
        cursor = Math.min(cursor + 1, 2);
        value.set(decimalValue);
    }

    private void deleteDecimal(BigDecimal fractionalPart) {
        if (cursor <= 0) return;
        String fractionalPartStr = fractionalPart.toPlainString();
        if (fractionalPartStr.length() == 3) fractionalPartStr = fractionalPartStr.concat("0");
        char[] values = fractionalPartStr.toCharArray();
        int eraseIndex = cursor + 1;
        values[eraseIndex] = '0';
        BigDecimal decimal = value.get().subtract(fractionalPart).add(new BigDecimal(new String(values)));
        if (decimal.compareTo(BigDecimal.valueOf(min)) < 0) return;
        cursor--;
        value.set(decimal);
        isAfterPoint = cursor > 0;
    }

    @Override
    public double getValueDouble() {
        return value.get().doubleValue();
    }

    @Override
    public int getValueInteger() {
        return value.get().intValue();
    }

    @Override
    public boolean isAfterPoint() {
        return isAfterPoint;
    }

    @Override
    public void updateMinMax(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public BigDecimal getValue() {
        return value.get();
    }

    /**
     * @noinspection UnpredictableBigDecimalConstructorCall
     */
    public void reset() {
        this.value.set(BigDecimal.valueOf(0.00));
        this.cursor = 0;
        this.isAfterPoint = false;
    }

}