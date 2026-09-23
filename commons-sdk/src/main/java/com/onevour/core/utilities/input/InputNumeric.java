package com.onevour.core.utilities.input;

import com.onevour.core.utilities.commons.ValueOf;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicInteger;

public class InputNumeric implements NumberInputAdapter {

    private static final String TAG = "NID-INT";

    private AtomicInteger value = new AtomicInteger(0);

    private int decrease = 10;

    private boolean isAfterPoint = false;

    private int min, max;

    public InputNumeric(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public void validateInit() {
        // ignore
    }

    @Override
    public String getValueString() {
        return String.valueOf(value);
    }

    @Override
    public void setValue(Double doubleValue) {
        value.set(clamp(doubleValue.intValue()));
    }

    @Override
    public void setValue(String valueStr) throws ParseException {
        if (ValueOf.isEmpty(valueStr)) {
            value.set(0);
            return;
        }
        try {
            value.set(clamp(Integer.parseInt(valueStr.trim())));
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid integer value: " + valueStr, 0);
        }
    }

    private int clamp(int value) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    @Override
    public void append(String valueChar) {
        BigInteger integer = BigInteger.valueOf(value.intValue()).multiply(BigInteger.valueOf(decrease)).add(new BigInteger(valueChar));
        if (integer.compareTo(BigInteger.valueOf(max)) > 0) return;
        value.set(integer.intValue());
    }

    @Override
    public void delete() {
        int integer = (value.get() / decrease);
        int diff = value.get() % decrease;
        if (diff > 0) integer = (value.get() - diff) / decrease;
        if (integer < min) return;
        value.set(integer);
    }

    @Override
    public double getValueDouble() {
        return value.get();
    }

    @Override
    public int getValueInteger() {
        return value.get();
    }

    @Override
    public void setValueToMax() {
        value.set(max);
    }

    @Override
    public boolean isAfterPoint() {
        return false;
    }

    @Override
    public void updateMinMax(double min, double max) {
        this.min = (int) min;
        this.max = (int) max;
    }
}
