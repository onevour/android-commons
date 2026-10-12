package com.onevour.core.form;

import com.onevour.core.form.annotation.Attachment;
import com.onevour.core.form.annotation.Bind;
import com.onevour.core.form.annotation.Email;
import com.onevour.core.form.annotation.Ignore;
import com.onevour.core.form.annotation.Max;
import com.onevour.core.form.annotation.MaxLength;
import com.onevour.core.form.annotation.Min;
import com.onevour.core.form.annotation.MinLength;
import com.onevour.core.form.annotation.Numeric;
import com.onevour.core.form.annotation.Regex;
import com.onevour.core.form.annotation.Required;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The fields of an EvoForm's values class: their names, the view id names they bind to (the name itself, its
 * snake_case, or {@link Bind}), their rules (annotations) and whether they hold draft attachments.
 */
final class FormSchema {

    static final class Item {

        final String name;

        final List<String> viewNames = new ArrayList<>();

        final Rules rules = Rules.of();

        boolean attachment;

        Item(String name) {
            this.name = name;
        }
    }

    private FormSchema() {
    }

    static List<Item> of(Class<?> type) {
        List<Item> items = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) continue;
                if (field.isAnnotationPresent(Ignore.class)) continue;
                items.add(item(field));
            }
        }
        return items;
    }

    private static Item item(Field field) {
        Item item = new Item(field.getName());
        Bind bind = field.getAnnotation(Bind.class);
        if (bind != null) item.viewNames.add(bind.value());
        item.viewNames.add(field.getName());
        String snake = snake(field.getName());
        if (!snake.equals(field.getName())) item.viewNames.add(snake);
        item.attachment = field.isAnnotationPresent(Attachment.class);
        Required required = field.getAnnotation(Required.class);
        if (required != null) {
            Rules rules = required.message() != 0 ? Rules.required(required.message()) : Rules.required();
            item.rules.add(rules);
        }
        Numeric number = field.getAnnotation(Numeric.class);
        if (number != null) item.rules.number(number.message());
        Min min = field.getAnnotation(Min.class);
        if (min != null) item.rules.min(min.value(), min.message());
        Max max = field.getAnnotation(Max.class);
        if (max != null) item.rules.max(max.value(), max.message());
        MinLength minLength = field.getAnnotation(MinLength.class);
        if (minLength != null) item.rules.minLength(minLength.value(), minLength.message());
        MaxLength maxLength = field.getAnnotation(MaxLength.class);
        if (maxLength != null) item.rules.maxLength(maxLength.value(), maxLength.message());
        Email email = field.getAnnotation(Email.class);
        if (email != null) item.rules.email(email.message());
        Regex regex = field.getAnnotation(Regex.class);
        if (regex != null) item.rules.pattern(regex.value(), regex.message());
        return item;
    }

    /** normalPrice → normal_price (view ids are usually snake_case). */
    static String snake(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
