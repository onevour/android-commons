package com.onevour.core.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.onevour.core.form.annotation.Attachment;
import com.onevour.core.form.annotation.Bind;
import com.onevour.core.form.annotation.Ignore;
import com.onevour.core.form.annotation.Min;
import com.onevour.core.form.annotation.Numeric;
import com.onevour.core.form.annotation.Required;
import org.junit.Test;

import java.util.List;

public class FormSchemaTest {

    static class PromoValues {
        @Required @Numeric @Min(1)
        Double normalPrice;

        @Bind("cio_display")
        String display;

        @Attachment
        List<Long> photoIds;

        @Ignore
        String computedTotal;

        static final String CONSTANT = "x";

        transient int cache;
    }

    @Test
    public void fieldsWithTheirViewNamesRulesAndAttachments() {
        List<FormSchema.Item> items = FormSchema.of(PromoValues.class);
        assertEquals(3, items.size());

        FormSchema.Item price = items.get(0);
        assertEquals("normalPrice", price.name);
        assertEquals("normalPrice", price.viewNames.get(0));
        assertEquals("normal_price", price.viewNames.get(1));
        assertFalse(price.rules.isEmptyRules());

        FormSchema.Item display = items.get(1);
        assertEquals("cio_display", display.viewNames.get(0));
        assertTrue(display.rules.isEmptyRules());

        assertTrue(items.get(2).attachment);
    }

    @Test
    public void snakeCaseOfCamelCase() {
        assertEquals("normal_price", FormSchema.snake("normalPrice"));
        assertEquals("qty_sos_gsk", FormSchema.snake("qtySosGsk"));
        assertEquals("note", FormSchema.snake("note"));
    }
}
