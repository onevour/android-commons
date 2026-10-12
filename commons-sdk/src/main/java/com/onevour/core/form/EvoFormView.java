package com.onevour.core.form;

import android.content.Context;
import android.graphics.Rect;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.onevour.core.R;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The UI of an {@link EvoForm}, made by {@link EvoForm#bind}: binds the views of a screen to the form's fields, both
 * ways, by id name (the field's name, its snake_case, or {@code @Bind}), shows the errors at their fields (the first
 * one focused after a validation), offers a stored draft ("Continue the draft?") and asks before leaving with unsaved
 * input. It belongs to the Activity / Fragment (it holds views); the form stays in the ViewModel.
 *
 * <pre>
 * EvoFormView&lt;LoginValues&gt; form = viewModel.form.bind(this, binding.getRoot());
 * binding.btnSave.setOnClickListener(v -&gt; viewModel.save());
 * onBackPressed: form.leave(this::finish);
 * </pre>
 */
public final class EvoFormView<T> {

    /** The values passed every rule. */
    public interface OnValid<T> {
        void submit(T values);
    }

    private final Context context;

    private final LifecycleOwner owner;

    private final EvoForm<T> model;

    private final Map<String, FieldBinder> fields = new LinkedHashMap<>();

    /** Fields whose error shows in a text of their own (e.g. under a radio group), not on the field. */
    private final Map<String, TextView> errorViews = new LinkedHashMap<>();

    /** Writing a model value into a view: not an edit to send back. */
    private boolean applying;

    EvoFormView(Context context, LifecycleOwner owner, View root, EvoForm<T> model) {
        this.context = context;
        this.owner = owner;
        this.model = model;
        for (FormSchema.Item item : FormSchema.of(model.type())) {
            View view = findView(root, item.viewNames);
            if (Objects.nonNull(view) && FieldBinder.supports(view)) attach(item.name, FieldBinder.of(view));
        }
        model.errors().observe(owner, this::showErrors);
        model.validations().observe(owner, count -> focusFirstError());
        model.draftOffer().observe(owner, offer -> {
            if (Objects.nonNull(offer) && model.claimOffer()) offerDraft(offer);
        });
    }

    public EvoForm<T> model() {
        return model;
    }

    /** A view that is not found by its id name, bound to the field {@code name}. */
    public EvoFormView<T> register(@NonNull View view, @NonNull String name) {
        attach(name, FieldBinder.of(view));
        return this;
    }

    /** The error of {@code name} shown in {@code view} (visible with the text, gone without), not on the field. */
    public EvoFormView<T> errorView(@NonNull String name, @NonNull TextView view) {
        errorViews.put(name, view);
        showErrors(model.errors().getValue());
        return this;
    }

    /**
     * Shows the model's values again in the views of these fields (all when none given): after the options of a radio
     * group or a spinner were built (again), so the value can be checked / selected.
     */
    public void refresh(@NonNull String... names) {
        Iterable<String> which = names.length == 0 ? fields.keySet() : Arrays.asList(names);
        for (String name : which) {
            FieldBinder binder = fields.get(name);
            if (Objects.nonNull(binder)) apply(binder, model.getValue(name));
        }
    }

    /** A click listener (e.g. Save): validates the model, then gives the values to {@code onValid}. */
    public View.OnClickListener handleSubmit(@NonNull OnValid<T> onValid) {
        return view -> {
            if (model.validate()) onValid.submit(model.values());
        };
    }

    /** Back / close: nothing unsaved leaves at once; else "continue later / discard / stay" (with a draft) or confirm. */
    public void leave(@NonNull Runnable onLeave) {
        if (!model.isDirty()) {
            onLeave.run();
            return;
        }
        if (model.hasDraft()) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.commons_draft_leave_title)
                    .setMessage(R.string.commons_draft_leave_message)
                    .setPositiveButton(R.string.commons_draft_later, (d, w) -> {
                        model.flushDraft();
                        onLeave.run();
                    })
                    .setNegativeButton(R.string.commons_draft_discard, (d, w) -> {
                        model.discard();
                        onLeave.run();
                    })
                    .setNeutralButton(R.string.commons_draft_stay, null)
                    .show();
            return;
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.commons_form_leave_title)
                .setMessage(R.string.commons_form_leave_message)
                .setPositiveButton(R.string.commons_form_leave, (d, w) -> onLeave.run())
                .setNegativeButton(R.string.commons_draft_stay, null)
                .show();
    }

    /* ---------- internals ---------- */

    private void attach(String name, FieldBinder binder) {
        fields.put(name, binder);
        // view → model: the user's edits
        binder.listen(() -> {
            if (!applying) model.setValue(name, binder.get());
        });
        // model → view: a value set by the ViewModel, a reset, a draft continued
        model.value(name).observe(owner, value -> apply(binder, value));
    }

    private void apply(FieldBinder binder, Object value) {
        if (Objects.equals(EvoForm.canonical(binder.get()), EvoForm.canonical(value))) return;
        applying = true;
        try {
            binder.set(value);
        } finally {
            applying = false;
        }
    }

    private void showErrors(Map<String, FormError> errors) {
        for (Map.Entry<String, FieldBinder> entry : fields.entrySet()) {
            if (errorViews.containsKey(entry.getKey())) continue;
            FormError error = Objects.isNull(errors) ? null : errors.get(entry.getKey());
            entry.getValue().showError(Objects.isNull(error) ? null : error.text(context));
        }
        for (Map.Entry<String, TextView> entry : errorViews.entrySet()) {
            FormError error = Objects.isNull(errors) ? null : errors.get(entry.getKey());
            TextView view = entry.getValue();
            if (Objects.nonNull(error)) view.setText(error.text(context));
            view.setVisibility(Objects.isNull(error) ? View.GONE : View.VISIBLE);
        }
    }

    private void focusFirstError() {
        Map<String, FormError> errors = model.errors().getValue();
        if (Objects.isNull(errors) || errors.isEmpty()) return;
        for (Map.Entry<String, FieldBinder> entry : fields.entrySet()) {
            if (!errors.containsKey(entry.getKey())) continue;
            // the field itself (typing goes on there), else its error text
            View view = Objects.nonNull(entry.getValue().view()) ? entry.getValue().view() : errorViews.get(entry.getKey());
            if (Objects.nonNull(view)) {
                view.requestFocus();
                // a view that takes no focus (a radio group, a text) is still scrolled to
                view.requestRectangleOnScreen(new Rect(0, 0, view.getWidth(), view.getHeight()), false);
                return;
            }
        }
    }

    private void offerDraft(EvoForm.DraftOffer offer) {
        String when = Objects.isNull(offer.updatedAt) ? "" : DateFormat.getTimeFormat(context).format(offer.updatedAt);
        // the screen goes with the offer unanswered (a rotation): the dialog goes, the next binder offers again
        AlertDialog[] shown = new AlertDialog[1];
        owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner source) {
                if (Objects.nonNull(shown[0]) && shown[0].isShowing()) shown[0].dismiss();
                if (Objects.nonNull(model.draftOffer().getValue())) model.releaseOffer();
            }
        });
        shown[0] = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.commons_draft_restore_title)
                .setMessage(context.getString(R.string.commons_draft_restore_message, when))
                .setCancelable(false)
                .setPositiveButton(R.string.commons_draft_continue, (d, w) -> model.continueDraft())
                .setNegativeButton(R.string.commons_draft_discard, (d, w) -> model.discardOfferedDraft())
                .show();
    }

    @Nullable
    private View findView(View root, List<String> names) {
        String pkg = context.getPackageName();
        for (String name : names) {
            int id = context.getResources().getIdentifier(name, "id", pkg);
            if (id == 0) continue;
            View view = root.findViewById(id);
            if (Objects.nonNull(view)) return view;
        }
        return null;
    }
}
