package com.onevour.core.draft;

import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The input of a one-screen form read from its views: every text field, check box / switch, radio group and spinner
 * with an id, by id name (stable across builds, unlike the int). A view tagged {@link #IGNORE} is left out, with
 * its children. Only text fields are watched (watchers add up); the others keep the form's own listeners.
 */
public final class LayoutState {

    /** Tag a view with this ({@code android:tag="draft:ignore"}) to keep it out of the draft (e.g. a computed total). */
    public static final String IGNORE = "draft:ignore";

    private final View root;

    LayoutState(View root) {
        this.root = root;
    }

    Map<String, Object> capture() {
        Map<String, Object> values = new LinkedHashMap<>();
        walk(root, (name, view) -> {
            if (view instanceof EditText) {
                values.put(name, String.valueOf(((EditText) view).getText()));
            } else if (view instanceof RadioGroup) {
                int checked = ((RadioGroup) view).getCheckedRadioButtonId();
                values.put(name, checked == View.NO_ID ? null : entryName(view.getResources(), checked));
            } else if (view instanceof CompoundButton && !(view instanceof RadioButton)) {
                values.put(name, ((CompoundButton) view).isChecked());
            } else if (view instanceof AdapterView) {
                values.put(name, (double) ((AdapterView<?>) view).getSelectedItemPosition());
            }
        });
        return values;
    }

    void restore(Map<String, Object> values) {
        if (Objects.isNull(values)) return;
        walk(root, (name, view) -> {
            if (!values.containsKey(name)) return;
            Object value = values.get(name);
            if (view instanceof EditText) {
                ((EditText) view).setText(Objects.isNull(value) ? "" : String.valueOf(value));
            } else if (view instanceof RadioGroup) {
                if (Objects.isNull(value)) {
                    ((RadioGroup) view).clearCheck();
                } else {
                    int id = view.getResources().getIdentifier(String.valueOf(value), "id", view.getContext().getPackageName());
                    if (id != 0) ((RadioGroup) view).check(id);
                }
            } else if (view instanceof CompoundButton && !(view instanceof RadioButton)) {
                ((CompoundButton) view).setChecked(Boolean.TRUE.equals(value));
            } else if (view instanceof AdapterView && value instanceof Number) {
                int position = ((Number) value).intValue();
                AdapterView<?> adapterView = (AdapterView<?>) view;
                if (position >= 0 && Objects.nonNull(adapterView.getAdapter()) && position < adapterView.getAdapter().getCount()) {
                    adapterView.setSelection(position);
                }
            }
        });
    }

    /** {@code changed} after every edit of a text field. */
    void watch(Runnable changed) {
        walk(root, (name, view) -> {
            if (!(view instanceof EditText)) return;
            ((EditText) view).addTextChangedListener(new TextWatcher() {
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
        });
    }

    private interface Visitor {
        void visit(String name, View view);
    }

    private static void walk(View view, Visitor visitor) {
        if (IGNORE.equals(view.getTag())) return;
        if (view.getId() != View.NO_ID && isInput(view)) {
            String name = entryName(view.getResources(), view.getId());
            if (Objects.nonNull(name)) visitor.visit(name, view);
        }
        // a radio group's buttons are its value; a spinner's rows are not inputs
        if (view instanceof ViewGroup && !(view instanceof RadioGroup) && !(view instanceof AdapterView)) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) walk(group.getChildAt(i), visitor);
        }
    }

    private static boolean isInput(View view) {
        return view instanceof EditText || view instanceof RadioGroup || view instanceof CompoundButton
                || view instanceof AdapterView;
    }

    private static String entryName(Resources resources, int id) {
        try {
            return resources.getResourceEntryName(id);
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }
}
