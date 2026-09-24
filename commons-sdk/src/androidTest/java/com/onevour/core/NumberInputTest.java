package com.onevour.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.graphics.Color;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.onevour.core.utilities.input.NumberInput;
import com.onevour.core.utilities.input.NumberInputStyle;
import com.onevour.core.utilities.input.NumberInputTextField;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NumberInputTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testNumberInputTextFieldInitialization() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            NumberInputTextField textField = new NumberInputTextField(context);
            assertNotNull(textField);

            textField.inputValue(150.0);
            assertNotNull(textField.getText());
            assertEquals("150", textField.getText().toString());
        });
    }

    @Test
    public void testNumberInputStyleApplication() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            EditText editText = new EditText(context);
            NumberInput numberInput = new NumberInput();
            numberInput.setup(editText, null, 0, 1000);

            NumberInputStyle style = new NumberInputStyle.Builder()
                    .setDialogBackgroundColor(Color.BLACK)
                    .setTitleTextColor(Color.WHITE)
                    .setResultTextColor(Color.GREEN)
                    .setKeyTextColor(Color.YELLOW)
                    .build();

            numberInput.setStyle(style);

            numberInput.inputValue(250.0);
            assertNotNull(editText.getText());
            assertEquals("250", editText.getText().toString());
        });
    }

    @Test
    public void testUpdateMinMax() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            NumberInputTextField textField = new NumberInputTextField(context);
            textField.updateMinMax(10.0, 500.0);

            textField.inputValue(100.0);
            assertNotNull(textField.getText());
            assertEquals("100", textField.getText().toString());
        });
    }
}
