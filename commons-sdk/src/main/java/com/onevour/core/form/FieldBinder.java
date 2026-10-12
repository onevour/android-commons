package com.onevour.core.form;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** How a field of an {@link EvoForm} reads, writes, watches and shows its error, by kind of input. */
abstract class FieldBinder {

    @Nullable
    abstract Object get();

    abstract void set(@Nullable Object value);

    /** {@code changed} after every edit by the user. */
    abstract void listen(Runnable changed);

    void showError(@Nullable String error) {
    }

    /** The view to scroll to / focus for an error, null for a value without view. */
    @Nullable
    View view() {
        return null;
    }

    static boolean supports(View view) {
        return view instanceof EditText || view instanceof RadioGroup || view instanceof CompoundButton || view instanceof AdapterView;
    }

    static FieldBinder of(View view) {
        if (view instanceof EditText) return new Text((EditText) view);
        if (view instanceof RadioGroup) return new Radio((RadioGroup) view);
        if (view instanceof CompoundButton) return new Checked((CompoundButton) view);
        if (view instanceof AdapterView) return new Selection((AdapterView<?>) view);
        throw new IllegalArgumentException("EvoForm cannot register " + view.getClass().getSimpleName()
                + ": use register(name, getter, setter)");
    }

    static FieldBinder of(Supplier<Object> getter, Consumer<Object> setter) {
        return new Value(getter, setter);
    }

    /** A field of the values class without view: the form holds it (setValue / getValue, reset, draft). */
    static FieldBinder held() {
        Object[] box = new Object[1];
        return new Value(() -> box[0], value -> box[0] = value);
    }

    /** A text field: its text; the error on its TextInputLayout when it has one, else on the field. */
    static final class Text extends FieldBinder {

        private final EditText field;

        Text(EditText field) {
            this.field = field;
        }

        @Override
        Object get() {
            return Objects.isNull(field.getText()) ? "" : field.getText().toString();
        }

        @Override
        void set(@Nullable Object value) {
            field.setText(Objects.isNull(value) ? "" : String.valueOf(value));
        }

        @Override
        void listen(Runnable changed) {
            field.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    changed.run();
                }
            });
        }

        @Override
        void showError(@Nullable String error) {
            TextInputLayout layout = layout();
            if (Objects.nonNull(layout)) {
                layout.setError(error);
            } else {
                field.setError(error);
            }
        }

        @Nullable
        private TextInputLayout layout() {
            ViewParent parent = field.getParent();
            for (int i = 0; i < 2 && Objects.nonNull(parent); i++) {
                if (parent instanceof TextInputLayout) return (TextInputLayout) parent;
                parent = parent.getParent();
            }
            return null;
        }

        @Override
        View view() {
            return field;
        }
    }

    /** A check box / switch: true or false. */
    static final class Checked extends FieldBinder {

        private final CompoundButton button;

        Checked(CompoundButton button) {
            this.button = button;
        }

        @Override
        Object get() {
            return button.isChecked();
        }

        @Override
        void set(@Nullable Object value) {
            button.setChecked(Boolean.TRUE.equals(value));
        }

        @Override
        void listen(Runnable changed) {
            button.setOnCheckedChangeListener((b, checked) -> changed.run());
        }

        @Override
        void showError(@Nullable String error) {
            button.setError(error);
        }

        @Override
        View view() {
            return button;
        }
    }

    /**
     * A radio group: the value of the checked button, its {@code android:tag} when it has one, else its id name;
     * null when none is checked.
     */
    static final class Radio extends FieldBinder {

        private final RadioGroup group;

        Radio(RadioGroup group) {
            this.group = group;
        }

        @Override
        Object get() {
            int id = group.getCheckedRadioButtonId();
            if (id == View.NO_ID) return null;
            View button = group.findViewById(id);
            // the checked row was removed (the options were built again)
            return Objects.isNull(button) ? null : valueOf(button);
        }

        @Override
        void set(@Nullable Object value) {
            if (Objects.isNull(value)) {
                group.clearCheck();
                return;
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof RadioButton && String.valueOf(value).equals(valueOf(child))) {
                    group.check(child.getId());
                    return;
                }
            }
        }

        @Override
        void listen(Runnable changed) {
            group.setOnCheckedChangeListener((g, id) -> changed.run());
        }

        @Override
        void showError(@Nullable String error) {
            int last = group.getChildCount() - 1;
            if (last >= 0 && group.getChildAt(last) instanceof TextView) ((TextView) group.getChildAt(last)).setError(error);
        }

        @Override
        View view() {
            return group;
        }

        private static String valueOf(View button) {
            if (Objects.nonNull(button.getTag())) return String.valueOf(button.getTag());
            try {
                return button.getResources().getResourceEntryName(button.getId());
            } catch (Exception e) {
                return String.valueOf(button.getId());
            }
        }
    }

    /** A spinner / list: the position selected. */
    static final class Selection extends FieldBinder {

        private final AdapterView<?> view;

        Selection(AdapterView<?> view) {
            this.view = view;
        }

        @Override
        Object get() {
            return view.getSelectedItemPosition();
        }

        @Override
        void set(@Nullable Object value) {
            if (!(value instanceof Number)) return;
            int position = ((Number) value).intValue();
            if (Objects.nonNull(view.getAdapter()) && position >= 0 && position < view.getAdapter().getCount()) {
                view.setSelection(position);
            }
        }

        @Override
        void listen(Runnable changed) {
            AdapterView.OnItemSelectedListener own = view.getOnItemSelectedListener();
            // a spinner reports its first position by itself after layout: not an edit
            int[] last = {view.getSelectedItemPosition()};
            view.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View item, int position, long id) {
                    if (Objects.nonNull(own)) own.onItemSelected(parent, item, position, id);
                    if (position == last[0]) return;
                    last[0] = position;
                    changed.run();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    if (Objects.nonNull(own)) own.onNothingSelected(parent);
                    changed.run();
                }
            });
        }

        @Override
        View view() {
            return view;
        }
    }

    /** A value without view (a product id, a date in millis, a list of photo ids): the form's getter and setter. */
    static final class Value extends FieldBinder {

        private final Supplier<Object> getter;

        private final Consumer<Object> setter;

        Value(Supplier<Object> getter, Consumer<Object> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        Object get() {
            return getter.get();
        }

        @Override
        void set(@Nullable Object value) {
            setter.accept(value);
        }

        @Override
        void listen(Runnable changed) {
            // the form tells: form.setValue(name, value) or form.changed(name)
        }
    }
}
