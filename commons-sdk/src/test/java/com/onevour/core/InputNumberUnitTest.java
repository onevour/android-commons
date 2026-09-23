package com.onevour.core;

import android.util.Log;

import com.onevour.core.utilities.format.NFormat;
import com.onevour.core.rest.components.RestRequest;
import com.onevour.core.utilities.input.InputDecimal;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.text.NumberFormat;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({Log.class, RestRequest.class})
public class InputNumberUnitTest {

    @Mock
    Log log;

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

}