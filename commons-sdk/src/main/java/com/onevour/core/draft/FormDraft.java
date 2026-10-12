package com.onevour.core.draft;

import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.onevour.core.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A form's draft, the same way in every form: the input is written as the form's draft shortly after each change and
 * when the app goes to the background; the draft is offered back when the form opens again; saving deletes it;
 * leaving with unsaved input asks "continue later" (the draft stays), "discard" (draft and draft attachments go) or
 * stay. "Unsaved" means: the form's state differs from what it showed once loaded ({@link #offerRestore()}), so the
 * values a form puts in by itself are never a change.
 *
 * <pre>
 * // a complex form: the ViewModel holds the input
 * draft = FormDraft.forModel(this, "SOS", key, ownerId, SosState.class, vm::formState, vm::restore)
 *         .photos(SosState::photoIds);
 * // a simple one-screen form: the inputs with an id are the input
 * draft = FormDraft.forLayout(this, "REASON", key, ownerId, binding.getRoot());
 *
 * draft.offerRestore();            // once the form shows its data
 * draft.changed();                 // after an edit (forLayout watches its text fields itself)
 * draft.saved();                   // after the form was saved
 * onBackPressed: draft.leave(this::finish);
 * </pre>
 */
public final class FormDraft<T> {

    /** The form behind a draft: its state as a plain object (Gson), and back. */
    public interface Host<T> {

        T capture();

        void restore(T state);

        /** The ids of the draft attachments a state holds (thrown away with the draft); empty when none. */
        default Collection<Long> photoIds(T state) {
            return Collections.emptyList();
        }

        /** The draft was thrown away at the restore prompt: the form shows its saved data again. */
        default void onDiscarded() {
        }
    }

    private static final long DELAY_MS = 700;

    private static final Gson GSON = new Gson();

    private final AppCompatActivity activity;

    private final String form;

    private final String key;

    private final String ownerId;

    private final Class<T> type;

    private final Host<T> host;

    private final Handler main = new Handler(Looper.getMainLooper());

    private final Runnable write = this::writeIfChanged;

    /** What the form showed once loaded (JSON): the state is a change only when it differs from it. */
    @Nullable
    private String baseline;

    /** A draft was restored: unsaved input even if it equals the loaded form. */
    private boolean restored;

    /** Saved or being restored: nothing is a draft then. */
    private boolean paused;

    @Nullable
    private Function<T, Collection<Long>> photos;

    public FormDraft(@NonNull AppCompatActivity activity, @NonNull String form, @NonNull String key,
                     @Nullable String ownerId, @NonNull Class<T> type, @NonNull Host<T> host) {
        this.activity = activity;
        this.form = form;
        this.key = key;
        this.ownerId = ownerId;
        this.type = type;
        this.host = host;
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                flush();
            }
        });
    }

    /** A form whose input is held by its ViewModel: {@code capture} reads it, {@code restore} puts a draft back. */
    public static <T> FormDraft<T> forModel(@NonNull AppCompatActivity activity, @NonNull String form, @NonNull String key,
                                            @Nullable String ownerId, @NonNull Class<T> type,
                                            @NonNull Supplier<T> capture, @NonNull Consumer<T> restore) {
        return new FormDraft<>(activity, form, key, ownerId, type, new Host<T>() {
            @Override
            public T capture() {
                return capture.get();
            }

            @Override
            public void restore(T state) {
                restore.accept(state);
            }
        });
    }

    /**
     * A one-screen form: every input under {@code root} with an id (text fields, check boxes and switches, radio groups,
     * spinners) is its input, by id name. Its text fields are watched; other inputs are written with the next write
     * (a text change, the app going to the background, leaving) or at once if the form calls {@link #changed()}.
     * A view tagged {@link LayoutState#IGNORE} (a computed value) is left out.
     */
    @SuppressWarnings("unchecked")
    public static FormDraft<Map<String, Object>> forLayout(@NonNull AppCompatActivity activity, @NonNull String form,
                                                           @NonNull String key, @Nullable String ownerId, @NonNull View root) {
        LayoutState layout = new LayoutState(root);
        FormDraft<Map<String, Object>> draft = new FormDraft<>(activity, form, key, ownerId, (Class<Map<String, Object>>) (Class<?>) Map.class,
                new Host<Map<String, Object>>() {
                    @Override
                    public Map<String, Object> capture() {
                        return layout.capture();
                    }

                    @Override
                    public void restore(Map<String, Object> state) {
                        layout.restore(state);
                    }
                });
        layout.watch(draft::changed);
        return draft;
    }

    /** The draft attachments (photo ids) a state holds: thrown away with a discarded draft. */
    public FormDraft<T> photos(@NonNull Function<T, Collection<Long>> photoIds) {
        this.photos = photoIds;
        return this;
    }

    /** Once the form shows its data: what it shows is not a change; a draft of this form and key is offered back. */
    public void offerRestore() {
        baseline = json(host.capture());
        Drafts.run(() -> {
            Drafts.Draft<T> draft = Drafts.load(form, key, type);
            main.post(() -> {
                if (Objects.isNull(draft) || activity.isFinishing()) return;
                String when = Objects.isNull(draft.updatedAt) ? "" : DateFormat.getTimeFormat(activity).format(draft.updatedAt);
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.commons_draft_restore_title)
                        .setMessage(activity.getString(R.string.commons_draft_restore_message, when))
                        .setCancelable(false)
                        .setPositiveButton(R.string.commons_draft_continue, (d, w) -> {
                            paused = true;
                            host.restore(draft.state);
                            paused = false;
                            restored = true;
                        })
                        .setNegativeButton(R.string.commons_draft_discard, (d, w) -> discard(draft.state, null))
                        .show();
            });
        });
    }

    /** The form changed: written as its draft shortly (a burst of typing is one write). */
    public void changed() {
        if (paused) return;
        main.removeCallbacks(write);
        main.postDelayed(write, DELAY_MS);
    }

    /** Writes the draft now when the form holds unsaved input (the app goes to the background, "continue later"). */
    public void flush() {
        main.removeCallbacks(write);
        writeIfChanged();
    }

    /** The form was saved: its input is final, the draft is done with. */
    public void saved() {
        main.removeCallbacks(write);
        paused = true;
        restored = false;
        Drafts.run(() -> Drafts.delete(form, key));
    }

    /** Whether the form holds input not saved yet. */
    public boolean isDirty() {
        if (paused || Objects.isNull(baseline)) return false;
        return restored || !baseline.equals(json(host.capture()));
    }

    /** Back / close: nothing unsaved leaves at once; else "continue later", "discard" or stay. */
    public void leave(@NonNull Runnable onLeave) {
        if (!isDirty()) {
            onLeave.run();
            return;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.commons_draft_leave_title)
                .setMessage(R.string.commons_draft_leave_message)
                .setPositiveButton(R.string.commons_draft_later, (d, w) -> {
                    flush();
                    onLeave.run();
                })
                .setNegativeButton(R.string.commons_draft_discard, (d, w) -> discard(null, onLeave))
                .setNeutralButton(R.string.commons_draft_stay, null)
                .show();
    }

    private void writeIfChanged() {
        if (!isDirty()) return;
        T state = host.capture();
        Drafts.run(() -> Drafts.save(form, key, ownerId, state));
    }

    /** The draft and its attachments go: those of {@code state} (a draft discarded unseen), else the form's now. */
    private void discard(@Nullable T state, @Nullable Runnable then) {
        main.removeCallbacks(write);
        restored = false;
        T of = Objects.nonNull(state) ? state : host.capture();
        Collection<Long> ids = new ArrayList<>(host.photoIds(of));
        if (Objects.nonNull(photos)) ids.addAll(photos.apply(of));
        Drafts.run(() -> {
            Drafts.discardAttachments(ids);
            Drafts.delete(form, key);
            main.post(() -> {
                if (Objects.nonNull(then)) then.run();
                else if (!activity.isFinishing()) host.onDiscarded();
            });
        });
    }

    private static String json(Object state) {
        return GSON.toJson(state);
    }
}
