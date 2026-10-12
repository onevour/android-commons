package com.onevour.core.draft;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Date;
import java.util.List;

/**
 * Where the drafts are kept: one per form and key, written again on every change. {@link SqliteDraftStore} (a small
 * database of the library) is the default; an app keeping its drafts in its own database implements this. All calls
 * are made off the main thread.
 */
@WorkerThread
public interface DraftStore {

    /** Writes the draft, replacing the one of the same form and key. */
    void save(DraftRecord draft);

    @Nullable
    DraftRecord find(String form, String key);

    void delete(String form, String key);

    List<DraftRecord> findByOwner(String ownerId);

    void deleteByOwner(String ownerId);

    List<DraftRecord> findAll();

    long count();

    /** The drafts not written since {@code before}; how many went. */
    int deleteOlderThan(Date before);

    void deleteAll();
}
