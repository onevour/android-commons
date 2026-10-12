package com.onevour.core.draft;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.onevour.core.utilities.commons.ContextHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Form drafts: a form's input kept while it is filled ({@link FormDraft} writes it as it changes), so a dead battery
 * or a killed app never makes the user type it again. Like RefSession, it keeps its own database
 * ({@link SqliteDraftStore#NAME}) and needs nothing but ContextHelper.init(application): a form just uses
 * {@link FormDraft#forModel} or {@link FormDraft#forLayout}. An app whose forms attach files (photos) or that names
 * the user writing the drafts says so once with {@link #configure}. The rule a form follows: its input is a draft until
 * it is saved (the draft is then deleted); leaving asks "continue later" (the draft stays) or "discard".
 *
 * <p>The calls here touch the store: call them off the main thread ({@link #run}), as FormDraft does.
 */
public final class Drafts {

    /** A draft read back: its state and when it was written. */
    public static final class Draft<T> {

        public final T state;

        @Nullable
        public final Date updatedAt;

        Draft(T state, @Nullable Date updatedAt) {
            this.state = state;
            this.updatedAt = updatedAt;
        }
    }

    private static final Gson GSON = new Gson();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "commons-draft");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile DraftStore store;

    private static AttachmentHandler attachments = AttachmentHandler.NONE;

    private static Supplier<String> user = () -> null;

    private Drafts() {
    }

    /**
     * Optional, once (e.g. in the Application): {@code attachments}: the app's draft files (photos), committed with a
     * saved form and thrown away with a discarded draft; {@code user}: who writes the drafts now (e.g. the logged-in
     * salesman). The drafts stay in the library's own database.
     */
    public static void configure(@Nullable AttachmentHandler attachments, @Nullable Supplier<String> user) {
        Drafts.attachments = Objects.isNull(attachments) ? AttachmentHandler.NONE : attachments;
        Drafts.user = Objects.isNull(user) ? () -> null : user;
    }

    /** Another store in place of the library's database (a test, or an app keeping drafts elsewhere). */
    public static void useStore(@NonNull DraftStore store) {
        synchronized (Drafts.class) {
            Drafts.store = store;
        }
    }

    /** Runs store work on the library's background thread. */
    public static void run(Runnable work) {
        EXECUTOR.execute(work);
    }

    @WorkerThread
    public static void save(String form, String key, @Nullable String ownerId, Object state) {
        store().save(new DraftRecord(form, key, ownerId, user.get(), GSON.toJson(state), new Date()));
    }

    /** The draft of the form and key, null when none or no longer readable (an older shape: it is dropped). */
    @Nullable
    @WorkerThread
    public static <T> Draft<T> load(String form, String key, Class<T> type) {
        DraftRecord record = store().find(form, key);
        if (Objects.isNull(record) || Objects.isNull(record.payload)) return null;
        try {
            T state = GSON.fromJson(record.payload, type);
            return Objects.isNull(state) ? null : new Draft<>(state, record.updatedAt);
        } catch (JsonParseException e) {
            store().delete(form, key);
            return null;
        }
    }

    @WorkerThread
    public static void delete(String form, String key) {
        store().delete(form, key);
    }

    /** The forms holding a draft for the owner (e.g. a visit's menus to mark). */
    @WorkerThread
    public static List<String> formsOf(String ownerId) {
        List<String> forms = new ArrayList<>();
        for (DraftRecord record : store().findByOwner(ownerId)) {
            if (!forms.contains(record.form)) forms.add(record.form);
        }
        return forms;
    }

    @WorkerThread
    public static List<DraftRecord> all() {
        return store().findAll();
    }

    @WorkerThread
    public static long count() {
        return store().count();
    }

    /** The drafts of the given forms only (an app counts the forms it still keeps drafts for). */
    @WorkerThread
    public static long count(Collection<String> forms) {
        long count = 0;
        for (DraftRecord record : store().findAll()) {
            if (forms.contains(record.form)) count++;
        }
        return count;
    }

    /** Drafts of any other form are removed (forms that no longer keep drafts can never be finished). */
    @WorkerThread
    public static void retainForms(Collection<String> forms) {
        for (DraftRecord record : store().findAll()) {
            if (!forms.contains(record.form)) store().delete(record.form, record.key);
        }
    }

    @WorkerThread
    public static void deleteByOwner(String ownerId) {
        store().deleteByOwner(ownerId);
    }

    @WorkerThread
    public static int deleteOlderThan(Date before) {
        return store().deleteOlderThan(before);
    }

    @WorkerThread
    public static void deleteAll() {
        store().deleteAll();
    }

    /** The form was saved: its draft attachments become final (the app's {@link AttachmentHandler}). */
    @WorkerThread
    public static void commitAttachments(Collection<Long> ids) {
        if (Objects.nonNull(ids) && !ids.isEmpty()) attachments.commit(ids);
    }

    /** The draft was thrown away: its draft attachments go. */
    @WorkerThread
    public static void discardAttachments(Collection<Long> ids) {
        if (Objects.nonNull(ids) && !ids.isEmpty()) attachments.discard(ids);
    }

    /** The library's own database, opened the first time it is needed (as RefSession's). */
    private static DraftStore store() {
        DraftStore current = store;
        if (Objects.nonNull(current)) return current;
        synchronized (Drafts.class) {
            if (Objects.isNull(store)) {
                Context context = ContextHelper.getApplication();
                if (Objects.isNull(context)) throw new IllegalStateException("call ContextHelper.init(application) first");
                store = new SqliteDraftStore(context);
            }
            return store;
        }
    }
}
