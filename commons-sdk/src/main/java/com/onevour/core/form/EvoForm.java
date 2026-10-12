package com.onevour.core.form;

import android.os.Handler;
import android.os.Looper;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.onevour.core.draft.Drafts;
import com.onevour.core.draft.MemoryDraftStore;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * A form's state, held by a ViewModel (no View, no Activity), as react-hook-form keeps it in the hook: the values of
 * {@code T}'s fields, their rules (annotations of {@code T}, see com.onevour.core.form.annotation, plus
 * {@link #rule}), the errors, dirty state, validation, and the draft built in (the input kept while it is filled, so
 * a killed app loses nothing). The UI binds its views to it with {@link #bind}; it survives a rotation with
 * the ViewModel and is tested without views.
 *
 * <pre>
 * public class PromoViewModel extends ViewModel {
 *     public final EvoForm&lt;PromoValues&gt; form = EvoForm.of(PromoValues.class)
 *             .draft("PROMO", key, customerId)
 *             .in(this);                                   // closed with the ViewModel
 *
 *     void load() { form.reset(repository.promo(id)); }    // the starting values; a draft is offered to the UI
 *     void save() {
 *         if (!form.validate()) return;                     // errors go to the fields
 *         repository.save(form.values(), form::saved);      // saved(): attachments final, draft deleted
 *     }
 * }
 * </pre>
 * Call it on the main thread (values are LiveData); the draft is written on a background thread.
 */
public final class EvoForm<T> implements Closeable {

    /** A draft found at reset, for the UI to offer: continue it or discard it. */
    public static final class DraftOffer {

        @Nullable
        public final Date updatedAt;

        DraftOffer(@Nullable Date updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    /** When the model's delayed work runs (the main thread; a test runs it at once). */
    public interface Scheduler {
        void post(Runnable work);

        void postDelayed(Runnable work, long delayMs);

        void cancel(Runnable work);

        Scheduler MAIN = new Scheduler() {
            /** Made when first used: a JVM test that never posts has no Looper. */
            private Handler handler;

            private Handler handler() {
                if (handler == null) handler = new Handler(Looper.getMainLooper());
                return handler;
            }

            @Override
            public void post(Runnable work) {
                handler().post(work);
            }

            @Override
            public void postDelayed(Runnable work, long delayMs) {
                handler().postDelayed(work, delayMs);
            }

            @Override
            public void cancel(Runnable work) {
                handler().removeCallbacks(work);
            }
        };

        /** Runs everything at once, the delayed draft write too (tests). */
        Scheduler IMMEDIATE = new Scheduler() {
            @Override
            public void post(Runnable work) {
                work.run();
            }

            @Override
            public void postDelayed(Runnable work, long delayMs) {
                work.run();
            }

            @Override
            public void cancel(Runnable work) {
            }
        };
    }

    private static final long DRAFT_DELAY_MS = 700;

    private static final Gson GSON = new Gson();

    private final Class<T> type;

    private final Map<String, Object> values = new LinkedHashMap<>();

    private final Map<String, MutableLiveData<Object>> live = new LinkedHashMap<>();

    private final Map<String, Rules> rules = new LinkedHashMap<>();

    private final List<String> attachments = new ArrayList<>();

    private final List<Function<Map<String, Object>, Collection<Long>>> attachmentReaders = new ArrayList<>();

    @Nullable
    private Runnable onContinued;

    private final MutableLiveData<Map<String, FormError>> errors = new MutableLiveData<>(Collections.emptyMap());

    private final MutableLiveData<DraftOffer> draftOffer = new MutableLiveData<>();

    /** Increased by each validate(): the UI focuses the first error of a new validation. */
    private final MutableLiveData<Integer> validations = new MutableLiveData<>(0);

    private final Runnable writeDraft = this::writeDraftIfDirty;

    private final DefaultLifecycleObserver background = new DefaultLifecycleObserver() {
        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            flushDraft();
        }
    };

    @Nullable
    private String draftForm;

    @Nullable
    private String draftKey;

    @Nullable
    private String draftOwner;

    /** The timing of new models: the main thread and the drafts' thread, or {@link #testing()}'s. */
    private static Scheduler defaultScheduler = Scheduler.MAIN;

    private static Executor defaultIo = Drafts::run;

    private Scheduler scheduler = defaultScheduler;

    private Executor io = defaultIo;

    /** The values the form started with (JSON): dirty means the values differ from them. */
    @Nullable
    private String baseline;

    /** The starting values themselves ("discard" puts them back). */
    @Nullable
    private Map<String, Object> startValues;

    /** A draft was continued: unsaved input even if it equals the starting values. */
    private boolean restored;

    @Nullable
    private Map<String, Object> pendingDraft;

    private boolean watchingBackground;

    /** A binder shows the current draft offer (several binders of one model, e.g. a stepper's steps: one dialog). */
    private boolean offerShown;

    private EvoForm(Class<T> type) {
        this.type = type;
        for (FormSchema.Item item : FormSchema.of(type)) {
            values.put(item.name, null);
            live.put(item.name, new MutableLiveData<>());
            if (!item.rules.isEmptyRules()) rules.put(item.name, item.rules);
            if (item.attachment) attachments.add(item.name);
        }
    }

    /** The form of {@code T}'s fields (their rules from their annotations). */
    public static <T> EvoForm<T> of(@NonNull Class<T> type) {
        return new EvoForm<>(type);
    }

    /** The views of an Activity bound to this form (in onCreate): values both ways, errors, the draft dialog, leave. */
    public EvoFormView<T> bind(@NonNull AppCompatActivity activity, @NonNull View root) {
        return new EvoFormView<>(activity, activity, root, this);
    }

    /** The views of a Fragment (in onViewCreated), e.g. a step of a stepper sharing the Activity's ViewModel form. */
    public EvoFormView<T> bind(@NonNull Fragment fragment, @NonNull View root) {
        return new EvoFormView<>(fragment.requireContext(), fragment.getViewLifecycleOwner(), root, this);
    }

    /** The values class. */
    public Class<T> type() {
        return type;
    }

    /** The draft: the form's name, the instance's key (unique), its owner (e.g. the visit's customer). */
    public EvoForm<T> draft(@NonNull String form, @NonNull String key, @Nullable String ownerId) {
        this.draftForm = form;
        this.draftKey = key;
        this.draftOwner = ownerId;
        return this;
    }

    /** A rule of its own for a field (e.g. one that compares fields), in place of its annotations. */
    public EvoForm<T> rule(@NonNull String name, @NonNull Rules fieldRules) {
        rules.put(name, fieldRules);
        return this;
    }

    /** Fields holding draft attachment ids (photos), besides those annotated {@code @Attachment}. */
    public EvoForm<T> attachments(@NonNull String... names) {
        for (String name : names) if (!attachments.contains(name)) attachments.add(name);
        return this;
    }

    /**
     * Attachment ids that sit inside other values (e.g. the photos of a list of entries): read from the values by
     * {@code ids}, besides the attachment fields.
     */
    public EvoForm<T> attachments(@NonNull Function<Map<String, Object>, Collection<Long>> ids) {
        attachmentReaders.add(ids);
        return this;
    }

    /**
     * Called after a stored draft was continued, its values in the form: the ViewModel can merge what happened since
     * the draft was written (e.g. a photo that arrived while the app was behind the camera) with setValue.
     */
    public EvoForm<T> onDraftContinued(@NonNull Runnable continued) {
        this.onContinued = continued;
        return this;
    }

    /** Closed with the ViewModel (its draft flushed, the background watch removed): no onCleared needed. */
    public EvoForm<T> in(@NonNull ViewModel viewModel) {
        viewModel.addCloseable(this);
        return this;
    }

    /**
     * JVM unit tests (with {@code InstantTaskExecutorRule}): the models made from now on run their work at once, on the
     * test's thread, and the drafts are kept in memory. Call it in the test's setUp, before the ViewModel is made.
     */
    @VisibleForTesting
    public static void testing() {
        defaultScheduler = Scheduler.IMMEDIATE;
        defaultIo = Runnable::run;
        Drafts.useStore(new MemoryDraftStore());
    }

    /** A test's timing: {@code scheduler} for the delayed draft writes, {@code io} for the store work. */
    public EvoForm<T> withScheduler(@NonNull Scheduler scheduler, @NonNull Executor io) {
        this.scheduler = scheduler;
        this.io = io;
        return this;
    }

    /* ---------- values ---------- */

    /** The values as {@code T} (an empty text is null). */
    public T values() {
        return GSON.fromJson(GSON.toJsonTree(values), type);
    }

    /** The values by field name. */
    public Map<String, Object> valueMap() {
        return Collections.unmodifiableMap(values);
    }

    @Nullable
    public Object getValue(@NonNull String name) {
        return values.get(name);
    }

    /** A field's value as the UI shows it. */
    public LiveData<Object> value(@NonNull String name) {
        MutableLiveData<Object> data = live.get(name);
        if (Objects.isNull(data)) throw new IllegalArgumentException(name + " is not a field of " + type.getSimpleName());
        return data;
    }

    /** Sets a field (from the ViewModel or the UI): the UI shows it, its error goes, the draft is written soon. */
    public void setValue(@NonNull String name, @Nullable Object value) {
        if (!values.containsKey(name)) throw new IllegalArgumentException(name + " is not a field of " + type.getSimpleName());
        Object normalized = normalize(value);
        if (Objects.equals(canonical(values.get(name)), canonical(normalized))) return;
        values.put(name, normalized);
        live.get(name).setValue(normalized);
        clearError(name);
        if (Objects.nonNull(baseline)) changed();
    }

    /**
     * The starting values (a loaded record, or null for an empty form): nothing is dirty. With a draft, the stored one
     * (if any) is offered through {@link #draftOffer()}.
     */
    public void reset(@Nullable T defaults) {
        Map<String, Object> map = Objects.isNull(defaults) ? Collections.emptyMap()
                : GSON.fromJson(GSON.toJsonTree(defaults), new TypeToken<Map<String, Object>>() {
        }.getType());
        for (String name : values.keySet()) put(name, map.get(name));
        clearErrors();
        restored = false;
        baseline = json();
        startValues = new LinkedHashMap<>(values);
        watchBackground();
        if (Objects.isNull(draftForm)) return;
        io.execute(() -> {
            Drafts.Draft<Map> draft = Drafts.load(draftForm, draftKey, Map.class);
            if (Objects.isNull(draft)) return;
            scheduler.post(() -> {
                //noinspection unchecked
                pendingDraft = (Map<String, Object>) draft.state;
                draftOffer.setValue(new DraftOffer(draft.updatedAt));
            });
        });
    }

    /** Whether the values differ from the starting ones (or a draft was continued). */
    public boolean isDirty() {
        return restored || (Objects.nonNull(baseline) && !baseline.equals(json()));
    }

    /* ---------- validation ---------- */

    /** Checks every field: the errors go to {@link #errors()} (the UI shows them, the first focused). True when all pass. */
    public boolean validate() {
        return check(rules.keySet(), new LinkedHashMap<>());
    }

    /** Checks only these fields (a step of a stepper); the errors of the other fields stay as they are. */
    public boolean validate(@NonNull String... names) {
        Map<String, FormError> kept = new LinkedHashMap<>(current());
        for (String name : names) kept.remove(name);
        return check(Arrays.asList(names), kept);
    }

    private boolean check(Collection<String> names, Map<String, FormError> others) {
        Map<String, FormError> found = new LinkedHashMap<>();
        for (String name : names) {
            Rules fieldRules = rules.get(name);
            if (Objects.isNull(fieldRules)) continue;
            FormError error = fieldRules.validate(values.get(name), values);
            if (Objects.nonNull(error)) found.put(name, error);
        }
        Map<String, FormError> all = new LinkedHashMap<>(others);
        all.putAll(found);
        errors.setValue(Collections.unmodifiableMap(all));
        Integer count = validations.getValue();
        validations.setValue(Objects.isNull(count) ? 1 : count + 1);
        return found.isEmpty();
    }

    /** An error of a field from elsewhere (the server, a database rule). */
    public void setError(@NonNull String name, @Nullable FormError error) {
        Map<String, FormError> next = new LinkedHashMap<>(current());
        if (Objects.isNull(error)) next.remove(name);
        else next.put(name, error);
        errors.setValue(Collections.unmodifiableMap(next));
    }

    public void clearErrors() {
        errors.setValue(Collections.emptyMap());
    }

    public LiveData<Map<String, FormError>> errors() {
        return errors;
    }

    /** Increased by every {@link #validate()}. */
    public LiveData<Integer> validations() {
        return validations;
    }

    /* ---------- the draft ---------- */

    /** A stored draft found by {@link #reset}: the UI asks to continue or discard it. */
    public LiveData<DraftOffer> draftOffer() {
        return draftOffer;
    }

    /** Continue: the draft's values in the form (unsaved input). */
    public void continueDraft() {
        Map<String, Object> draft = pendingDraft;
        pendingDraft = null;
        offerShown = false;
        draftOffer.setValue(null);
        if (Objects.isNull(draft)) return;
        for (String name : values.keySet()) if (draft.containsKey(name)) put(name, draft.get(name));
        restored = true;
        if (Objects.nonNull(onContinued)) onContinued.run();
    }

    /** Discard the offered draft: it goes, with its attachments; the form keeps its starting values. */
    public void discardOfferedDraft() {
        Map<String, Object> draft = pendingDraft;
        pendingDraft = null;
        offerShown = false;
        draftOffer.setValue(null);
        if (Objects.nonNull(draft)) dropDraft(attachmentIds(draft));
    }

    /** Leaving with "continue later": the draft is written now. */
    public void flushDraft() {
        scheduler.cancel(writeDraft);
        writeDraftIfDirty();
    }

    /** Leaving with "discard": the draft and the form's draft attachments go, the values back to the starting ones. */
    public void discard() {
        scheduler.cancel(writeDraft);
        Collection<Long> ids = attachmentIds(values);
        restored = false;
        if (Objects.nonNull(startValues)) {
            for (String name : values.keySet()) put(name, startValues.get(name));
        }
        dropDraft(ids);
    }

    /** The values were saved: the draft attachments become final, the draft is done with, nothing is dirty. */
    public void saved() {
        scheduler.cancel(writeDraft);
        Collection<Long> ids = attachmentIds(values);
        restored = false;
        baseline = json();
        startValues = new LinkedHashMap<>(values);
        String form = draftForm;
        String key = draftKey;
        io.execute(() -> {
            Drafts.commitAttachments(ids);
            if (Objects.nonNull(form)) Drafts.delete(form, key);
        });
    }

    public boolean hasDraft() {
        return Objects.nonNull(draftForm);
    }

    @Override
    public void close() {
        flushDraft();
        if (watchingBackground) {
            watchingBackground = false;
            scheduler.post(() -> ProcessLifecycleOwner.get().getLifecycle().removeObserver(background));
        }
    }

    /** True for the one binder that shows the pending offer, until it {@link #releaseOffer()}s it. */
    boolean claimOffer() {
        if (offerShown || Objects.isNull(draftOffer.getValue())) return false;
        offerShown = true;
        return true;
    }

    /** The binder's screen is gone (e.g. a rotation) with the offer unanswered: the next binder shows it. */
    void releaseOffer() {
        offerShown = false;
    }

    /* ---------- internals ---------- */

    private void changed() {
        if (Objects.isNull(draftForm)) return;
        scheduler.cancel(writeDraft);
        scheduler.postDelayed(writeDraft, DRAFT_DELAY_MS);
    }

    private void writeDraftIfDirty() {
        if (Objects.isNull(draftForm) || !isDirty()) return;
        Map<String, Object> state = new LinkedHashMap<>(values);
        String form = draftForm;
        String key = draftKey;
        String owner = draftOwner;
        io.execute(() -> Drafts.save(form, key, owner, state));
    }

    private void dropDraft(Collection<Long> ids) {
        String form = draftForm;
        String key = draftKey;
        io.execute(() -> {
            Drafts.discardAttachments(ids);
            if (Objects.nonNull(form)) Drafts.delete(form, key);
        });
    }

    /** The app goes to the background: the pending draft is written (Android may kill the process later). */
    private void watchBackground() {
        if (watchingBackground || Objects.isNull(draftForm) || scheduler != Scheduler.MAIN) return;
        watchingBackground = true;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(background);
    }

    /** A value put by the model itself (reset, restore): shown, not an edit. */
    private void put(String name, @Nullable Object value) {
        Object normalized = normalize(value);
        values.put(name, normalized);
        live.get(name).setValue(normalized);
    }

    private void clearError(String name) {
        if (current().containsKey(name)) setError(name, null);
    }

    private Map<String, FormError> current() {
        Map<String, FormError> map = errors.getValue();
        return Objects.isNull(map) ? Collections.emptyMap() : map;
    }

    private Collection<Long> attachmentIds(Map<String, Object> source) {
        List<Long> ids = new ArrayList<>();
        for (Function<Map<String, Object>, Collection<Long>> reader : attachmentReaders) {
            Collection<Long> found = reader.apply(source);
            if (Objects.nonNull(found)) for (Long id : found) if (Objects.nonNull(id) && !ids.contains(id)) ids.add(id);
        }
        for (String name : attachments) {
            Object value = source.get(name);
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

    /** The values compared as text (a field shows "12" for 12): what dirty and "same value" compare. */
    private String json() {
        Map<String, Object> canonical = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) canonical.put(entry.getKey(), canonical(entry.getValue()));
        return GSON.toJson(canonical);
    }

    @Nullable
    static Object canonical(@Nullable Object value) {
        Object normalized = normalize(value);
        if (Objects.isNull(normalized) || normalized instanceof Collection || normalized instanceof Map) return normalized;
        if (normalized instanceof Number) {
            double number = ((Number) normalized).doubleValue();
            return number == Math.rint(number) && !Double.isInfinite(number) ? String.valueOf((long) number) : String.valueOf(number);
        }
        return String.valueOf(normalized);
    }

    @Nullable
    static Object normalize(@Nullable Object value) {
        if (value instanceof CharSequence && value.toString().isEmpty()) return null;
        // a whole number read back from JSON (a Double) as a Long: "12", not "12.0"
        if (value instanceof Double && ((Double) value) == Math.rint((Double) value) && !Double.isInfinite((Double) value)) {
            return ((Double) value).longValue();
        }
        return value;
    }
}
