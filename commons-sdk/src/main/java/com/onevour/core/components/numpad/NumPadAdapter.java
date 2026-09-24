package com.onevour.core.components.numpad;

import java.text.ParseException;

public interface NumPadAdapter {

    void validateInit();

    String getValueString();

    void setValue(Double doubleValue);

    void setValue(String value) throws ParseException;

    void setValueToMax() throws ParseException;

    void append(String valueChar) throws ParseException;

    void delete();

    double getValueDouble();

    int getValueInteger();



    boolean isAfterPoint();

    void updateMinMax(double min, double max);



}
