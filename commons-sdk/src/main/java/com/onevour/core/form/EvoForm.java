package com.onevour.core.form;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.onevour.core.R;
import com.onevour.core.draft.Drafts;
import com.onevour.core.draft.FormDraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A form, in the spirit of react-hook-form's {@code useForm}: fields registered by name (views or plain values), their
 * rules, the values as one typed object, dirty state, errors at the fields, a submit that validates first, and the
 * draft built in (the input kept while it is filled, offered back when the form opens again: {@link FormDraft}).
 *
 * <pre>
 * EvoForm&lt;ReasonValues&gt; form = EvoForm.with(this, ReasonValues.class)
 *         .draft("REASON", visitId, customerId)          // optional: the draft
 *         .attachments("photoIds")                      // optional: fields holding draft attachment ids
 *         .build();
 * form.register(binding.reasonGroup, "reasonId", Rules.required());
 * form.register(binding.note, "description", Rules.required().maxLength(200));
 * form.register("photoIds", vm::photoIds, ids -&gt; vm.setPhotoIds(ids));
 * form.watch("reasonId", value -&gt; showReasonHint(value));
 * form.reset(loadedValues);                             // the starting values; offers the draft back
 * binding.save.setOnClickListener(form.handleSubmit(values -&gt; vm.save(values, form::saved)));
 * onBackPressed: form.leave(this::finish);
 * </pre>
 * Field names are the names of {@code T}'s fields (values are converted with Gson; an empty text is null).
 */
public final class EvoForm<T> {

    /** The values passed every rule: save them, then call {@link #saved()}. */
    public interface OnValid<T> {
        void submit(T values);
    }

    private static final Gson GSON = new Gson();

    private final AppCompatActivity activity;

    private final Class<T> type;

    private final Map<String, FieldBinder> fields = new LinkedHashMap<>();

    private final Map<String, Rules> rules = new LinkedHashMap<>();

    private final Map<String, List<Consumer<Object>>> watchers = new LinkedHashMap<>();

    private final List<Runnable> anyWatchers = new ArrayList<>();

    private final Map<String, String> errors = new LinkedHashMap<>();

    private final List<String> attachmentFields = new ArrayList<>();

    @Nullable
    private final FormDraft<Map<String, Object>> draft;

    /** Writing values into the fields (reset, restore): not edits. */
    private boolean silent;

    /** The values the form started with (JSON), when it keeps no draft. */
    @Nullable
    private String baseline;

    private EvoForm(Builder<T> builder) {
        this.activity = builder.activity;
        this.type = builder.type;
        this.attachmentFields.addAll(builder.attachmentFields);
        if (Objects.nonNull(builder.draftForm)) {
            this.draft = new FormDraft<Map<String, Object>>(activity, builder.draftForm, builder.draftKey, builder.draftOwner,
                    mapClass(), new FormDraft.Host<Map<String, Object>>() {
                @Override
                public Map<String, Object> capture() {
                    return values();
                }

                @Override
                public void restore(Map<String, Object> state) {
                    setValues(state);
                }

                @Override
                public Collection<Long> photoIds(Map<String, Object> state) {
                    return attachmentIds(state);
                }

                @Override
                public void onDiscarded() {
                    if (Objects.nonNull(builder.onDiscarded)) builder.onDiscarded.run();
                }
            });
        } else {
            this.draft = null;
        }
        // the values class gives the fields: bound to the view of the same id name under root, else held by the form
        for (FormSchema.Item item : FormSchema.of(type)) {
            View view = Objects.isNull(builder.root) ? null : findView(builder.root, item.viewNames);
            FieldBinder binder = Objects.nonNull(view) && FieldBinder.supports(view) ? FieldBinder.of(view) : FieldBinder.held();
            add(item.name, binder, item.rules.isEmptyRules() ? null : item.rules);
            if (item.attachment && !attachmentFields.contains(item.name)) attachmentFields.add(item.name);
        }
    }

    /** The view under {@code root} with one of the id names, null when none. */
    @Nullable
    private View findView(View root, List<String> names) {
        String pkg = activity.getPackageName();
        for (String name : names) {
            int id = activity.getResources().getIdentifier(name, "id", pkg);
            if (id == 0) continue;
            View view = root.findViewById(id);
            if (Objects.nonNull(view)) return view;
        }
        return null;
    }

    /** A form whose fields are registered by hand (fields of {@code T} not registered are held by the form). */
    public static <T> Builder<T> with(@NonNull AppCompatActivity activity, @NonNull Class<T> type) {
        return new Builder<>(activity, type, null);
    }

    /**
     * A form bound by itself: each field of {@code T} to the view under {@code root} whose id name is the field's name
     * (or its snake_case, or {@link com.onevour.core.form.annotation.Bind}), its rules from its annotations ({@link com.onevour.core.form.annotation.Required}, {@link com.onevour.core.form.annotation.Min}, ...), a field
     * without view held by the form, {@link com.onevour.core.form.annotation.Attachment} fields as the draft's attachments. No register needed.
     */
    public static <T> Builder<T> bind(@NonNull AppCompatActivity activity, @NonNull View root, @NonNull Class<T> type) {
        return new Builder<>(activity, type, root);
    }

    /* ---------- fields ---------- */

    /** A view (text field, check box / switch, radio group, spinner) as the field {@code name}. */
    public EvoForm<T> register(@NonNull View view, @NonNull String name) {
        return register(view, name, null);
    }

    public EvoForm<T> register(@NonNull View view, @NonNull String name, @Nullable Rules fieldRules) {
        return add(name, FieldBinder.of(view), fieldRules);
    }

    /** A value without view (a picked id, a date in millis, photo ids): call {@link #setValue} when it changes. */
    public EvoForm<T> register(@NonNull String name, @NonNull Supplier<Object> getter, @NonNull Consumer<Object> setter) {
        return register(name, getter, setter, null);
    }

    public EvoForm<T> register(@NonNull String name, @NonNull Supplier<Object> getter, @NonNull Consumer<Object> setter,
                               @Nullable Rules fieldRules) {
        return add(name, FieldBinder.of(getter, setter), fieldRules);
    }

    private EvoForm<T> add(String name, FieldBinder binder, @Nullable Rules fieldRules) {
        // a field registered by hand takes the place of the one the values class gave (its rules kept unless given)
        fields.put(name, binder);
        if (Objects.nonNull(fieldRules)) rules.put(name, fieldRules);
        binder.listen(() -> onEdited(name));
        return this;
    }

    /** {@code listener} with the field's new value after every edit (as react-hook-form's watch). */
    public EvoForm<T> watch(@NonNull String name, @NonNull Consumer<Object> listener) {
        watchers.computeIfAbsent(name, key -> new ArrayList<>()).add(listener);
        return this;
    }

    /** {@code listener} after any edit. */
    public EvoForm<T> watch(@NonNull Runnable listener) {
        anyWatchers.add(listener);
        return this;
    }

    /* ---------- values ---------- */

    /** The values as {@code T}. */
    public T getValues() {
        JsonElement tree = GSON.toJsonTree(values());
        return GSON.fromJson(tree, type);
    }

    @Nullable
    public Object getValue(@NonNull String name) {
        FieldBinder binder = fields.get(name);
        return Objects.isNull(binder) ? null : normalize(binder.get());
    }

    /** Sets a field's value as if the user edited it (watchers, draft, its error cleared). */
    public void setValue(@NonNull String name, @Nullable Object value) {
        FieldBinder binder = fields.get(name);
        if (Objects.isNull(binder)) return;
        silent = true;
        binder.set(value);
        silent = false;
        onEdited(name);
    }

    /** A plain value changed outside the form (e.g. a photo taken): the form takes it as an edit. */
    public void changed(@NonNull String name) {
        onEdited(name);
    }

    /**
     * The starting values (the loaded record, or an empty {@code T}): the fields show them, nothing is dirty; then the
     * draft of this form, if any, is offered back.
     */
    public void reset(@Nullable T defaults) {
        if (Objects.nonNull(defaults)) {
            Map<String, Object> map = GSON.fromJson(GSON.toJsonTree(defaults), mapType());
            setValues(map);
        }
        clearErrors();
        if (Objects.nonNull(draft)) {
            draft.offerRestore();
        } else {
            baseline = GSON.toJson(values());
        }
    }

    /** Whether the values differ from the starting ones (or a draft was restored). */
    public boolean isDirty() {
        if (Objects.nonNull(draft)) return draft.isDirty();
        return Objects.nonNull(baseline) && !baseline.equals(GSON.toJson(values()));
    }

    /* ---------- validation ---------- */

    /** Checks every field; the errors are shown at their fields, the first one focused. True when all pass. */
    public boolean trigger() {
        clearErrors();
        Map<String, Object> values = values();
        View first = null;
        for (Map.Entry<String, Rules> entry : rules.entrySet()) {
            String error = entry.getValue().validate(activity, values.get(entry.getKey()), values);
            if (Objects.isNull(error)) continue;
            setError(entry.getKey(), error);
            FieldBinder binder = fields.get(entry.getKey());
            if (Objects.isNull(first) && Objects.nonNull(binder)) first = binder.view();
        }
        if (Objects.nonNull(first)) first.requestFocus();
        return errors.isEmpty();
    }

    /** An error of a field (e.g. from the server), shown at it. */
    public void setError(@NonNull String name, @Nullable String message) {
        if (Objects.isNull(message)) errors.remove(name);
        else errors.put(name, message);
        FieldBinder binder = fields.get(name);
        if (Objects.nonNull(binder)) binder.showError(message);
    }

    public void clearErrors() {
        for (String name : new ArrayList<>(errors.keySet())) setError(name, null);
        errors.clear();
    }

    /** The fields' errors now, by name. */
    public Map<String, String> errors() {
        return Collections.unmodifiableMap(errors);
    }

    /** A click listener (e.g. the Save button): validates, then gives the values to {@code onValid}. */
    public View.OnClickListener handleSubmit(@NonNull OnValid<T> onValid) {
        return view -> {
            if (trigger()) onValid.submit(getValues());
        };
    }

    /* ---------- the end ---------- */

    /** The values were saved: the draft attachments become final, the draft is done with, nothing is dirty. */
    public void saved() {
        Collection<Long> ids = attachmentIds(values());
        if (!ids.isEmpty()) Drafts.run(() -> Drafts.commitAttachments(ids));
        if (Objects.nonNull(draft)) {
            draft.saved();
        } else {
            baseline = GSON.toJson(values());
        }
    }

    /** Back / close: nothing unsaved leaves at once; else, with a draft, "continue later / discard / stay", else confirm. */
    public void leave(@NonNull Runnable onLeave) {
        if (Objects.nonNull(draft)) {
            draft.leave(onLeave);
            return;
        }
        if (!isDirty()) {
            onLeave.run();
            return;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.commons_form_leave_title)
                .setMessage(R.string.commons_form_leave_message)
                .setPositiveButton(R.string.commons_form_leave, (d, w) -> onLeave.run())
                .setNegativeButton(R.string.commons_draft_stay, null)
                .show();
    }

    /* ---------- internals ---------- */

    /** The fields' values by name (empty text as null). */
    Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, FieldBinder> entry : fields.entrySet()) {
            values.put(entry.getKey(), normalize(entry.getValue().get()));
        }
        return values;
    }

    private void setValues(Map<String, Object> values) {
        silent = true;
        for (Map.Entry<String, FieldBinder> entry : fields.entrySet()) {
            if (values.containsKey(entry.getKey())) entry.getValue().set(display(values.get(entry.getKey())));
        }
        silent = false;
    }

    private void onEdited(String name) {
        if (silent) return;
        if (errors.containsKey(name)) setError(name, null);
        Object value = getValue(name);
        List<Consumer<Object>> listeners = watchers.get(name);
        if (Objects.nonNull(listeners)) for (Consumer<Object> listener : listeners) listener.accept(value);
        for (Runnable listener : anyWatchers) listener.run();
        if (Objects.nonNull(draft)) draft.changed();
    }

    private Collection<Long> attachmentIds(Map<String, Object> values) {
        List<Long> ids = new ArrayList<>();
        for (String name : attachmentFields) {
            Object value = values.get(name);
            if (value instanceof Collection) {
                for (Object id : (Collection<?>) value) {
                    Double number = Rules.toNumber(id);
                    if (Objects.nonNull(number)) ids.add(number.longValue());
                }
            } else {
                Double number = Rules.toNumber(value);
                if (Objects.nonNull(number)) ids.add(number.longValue());
            }
        }
        return ids;
    }

    @Nullable
    private static Object normalize(@Nullable Object value) {
        if (value instanceof CharSequence && value.toString().isEmpty()) return null;
        return value;
    }

    /** A whole number read back from JSON (a Double) shown without ".0". */
    @Nullable
    private static Object display(@Nullable Object value) {
        if (value instanceof Double && ((Double) value) == Math.rint((Double) value) && !Double.isInfinite((Double) value)) {
            return ((Double) value).longValue();
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Class<Map<String, Object>> mapClass() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    private static java.lang.reflect.Type mapType() {
        return new TypeToken<Map<String, Object>>() {
        }.getType();
    }

    /** {@link EvoForm#with}: the form's options, then {@link #build()}. */
    public static final class Builder<T> {

        private final AppCompatActivity activity;

        private final Class<T> type;

        private final List<String> attachmentFields = new ArrayList<>();

        private String draftForm;

        private String draftKey;

        private String draftOwner;

        private Runnable onDiscarded;

        @Nullable
        private final View root;

        Builder(AppCompatActivity activity, Class<T> type, @Nullable View root) {
            this.activity = activity;
            this.type = type;
            this.root = root;
        }

        /** The draft: the form's name, the instance's key (unique), its owner (e.g. the visit's customer). */
        public Builder<T> draft(@NonNull String form, @NonNull String key, @Nullable String ownerId) {
            this.draftForm = form;
            this.draftKey = key;
            this.draftOwner = ownerId;
            return this;
        }

        /** The fields holding draft attachment ids (photos): committed by {@link EvoForm#saved()}, discarded with the draft. */
        public Builder<T> attachments(@NonNull String... names) {
            Collections.addAll(attachmentFields, names);
            return this;
        }

        /** After "discard" at the restore prompt: the form shows its saved data again (e.g. reload photos). */
        public Builder<T> onDiscarded(@NonNull Runnable listener) {
            this.onDiscarded = listener;
            return this;
        }

        public EvoForm<T> build() {
            return new EvoForm<>(this);
        }
    }
}
