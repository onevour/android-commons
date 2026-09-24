package com.onevour.core;

import com.onevour.core.utilities.format.NFormat;
import com.onevour.core.utilities.input.InputDecimal;
import com.onevour.core.utilities.input.InputNumeric;

import org.junit.Assert;
import org.junit.Test;

import java.text.NumberFormat;
import java.util.Locale;

public class InputNumberUnitTest {

    @Test
    public void test_number_input_double() throws Exception {
        NumberFormat format = NFormat.currency();
        InputDecimal input = new InputDecimal(format, 0.0, Double.MAX_VALUE);
        input.append("1", ".", "5", "6");
        System.out.println("input " + input.getValue().toPlainString());
        input.delete();
        System.out.println("delete " + input.getValue().toPlainString());
        Assert.assertEquals(1.5, input.getValueDouble(), 0.0);
        input.append("9");
        System.out.println("input " + input.getValue().toPlainString());
        Assert.assertEquals(1.59, input.getValueDouble(), 0.0);

        input.append("8");
        System.out.println("input " + input.getValue().toPlainString());
        Assert.assertEquals(1.58, input.getValueDouble(), 0.0);
        input.append("7");
        System.out.println("input " + input.getValue().toPlainString());
        Assert.assertEquals(1.57, input.getValueDouble(), 0.0);
        input.delete();
        System.out.println("delete " + input.getValue().toPlainString());
        Assert.assertEquals(1.50, input.getValueDouble(), 0.0);
        input.delete();
        System.out.println("delete " + input.getValue().toPlainString());
        Assert.assertEquals(1.0, input.getValueDouble(), 0.0);
        input.append("1");
        System.out.println("input " + input.getValue().toPlainString());
        Assert.assertEquals(11.0, input.getValueDouble(), 0.0);
    }

    @Test
    public void test_number_input_double_after_decimal() throws Exception {
        NumberFormat format = NFormat.currency();
        InputDecimal input = new InputDecimal(format, 0.0, Double.MAX_VALUE);
        input.append(".", "5", "6");
        System.out.println("input " + input.getValue().toPlainString());
        input.delete();
        System.out.println("delete " + input.getValue().toPlainString());
        Assert.assertEquals(0.5, input.getValueDouble(), 0.0);
        input.append("9");
        System.out.println("input " + input.getValue().toPlainString());
        Assert.assertEquals(0.59, input.getValueDouble(), 0.0);
        input.append("8");
        System.out.println("input " + input.getValue().toPlainString());
        Assert.assertEquals(0.58, input.getValueDouble(), 0.0);
        input.append("7");
        System.out.println("input " + input.getValue().toPlainString());
        Assert.assertEquals(0.57, input.getValueDouble(), 0.0);
        input.delete();
        System.out.println("delete " + input.getValue().toPlainString());
        Assert.assertEquals(0.50, input.getValueDouble(), 0.0);
        input.delete();
        System.out.println("delete " + input.getValue().toPlainString());
        Assert.assertEquals(0.0, input.getValueDouble(), 0.0);
        input.append("1");
        System.out.println("input " + input.getValue().toPlainString());
        Assert.assertEquals(1.0, input.getValueDouble(), 0.0);
    }

    @Test
    public void test_numeric_input_integer() {
        InputNumeric input = new InputNumeric(0, 1000);
        input.append("1");
        input.append("2");
        input.append("5");
        Assert.assertEquals(125, input.getValueInteger());

        input.delete();
        Assert.assertEquals(12, input.getValueInteger());

        input.setValueToMax();
        Assert.assertEquals(1000, input.getValueInteger());
    }

    @Test
    public void test_number_input_max_value() throws Exception {
        NumberFormat format = NFormat.currency();
        InputDecimal input = new InputDecimal(format, 0.0, 100.0);
        input.setValueToMax();
        Assert.assertEquals(100.0, input.getValueDouble(), 0.0);
    }

    @Test
    public void test_input_decimal_indonesian_locale() throws Exception {
        // Locale Indonesia menggunakan koma ',' sebagai pemisah desimal
        NumberFormat formatID = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        formatID.setMinimumFractionDigits(2);
        formatID.setMaximumFractionDigits(2);

        InputDecimal input = new InputDecimal(formatID, 0.0, Double.MAX_VALUE);
        // Test append dengan koma desimal
        input.append("1", ",", "5", "6");
        Assert.assertEquals(1.56, input.getValueDouble(), 0.0);
        Assert.assertEquals("1,56", input.getValueString());

        // Test delete desimal
        input.delete();
        Assert.assertEquals(1.50, input.getValueDouble(), 0.0);
        Assert.assertEquals("1,50", input.getValueString());

        // Test setValue string terformat Indonesia
        input.setValue("10,75");
        Assert.assertEquals(10.75, input.getValueDouble(), 0.0);
        Assert.assertEquals("10,75", input.getValueString());
    }

    @Test
    public void test_input_decimal_us_locale() throws Exception {
        // Locale US menggunakan titik '.' sebagai pemisah desimal
        NumberFormat formatUS = NumberFormat.getNumberInstance(Locale.US);
        formatUS.setMinimumFractionDigits(2);
        formatUS.setMaximumFractionDigits(2);

        InputDecimal input = new InputDecimal(formatUS, 0.0, Double.MAX_VALUE);
        // Test append dengan titik desimal
        input.append("1", ".", "5", "6");
        Assert.assertEquals(1.56, input.getValueDouble(), 0.0);
        Assert.assertEquals("1.56", input.getValueString());

        // Test delete desimal
        input.delete();
        Assert.assertEquals(1.50, input.getValueDouble(), 0.0);
        Assert.assertEquals("1.50", input.getValueString());

        // Test setValue string terformat US
        input.setValue("10.75");
        Assert.assertEquals(10.75, input.getValueDouble(), 0.0);
        Assert.assertEquals("10.75", input.getValueString());
    }
}
