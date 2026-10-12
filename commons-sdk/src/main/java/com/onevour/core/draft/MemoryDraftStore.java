package com.onevour.core.draft;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Drafts kept in memory, for JVM unit tests ({@code Drafts.useStore(new MemoryDraftStore())}). */
public final class MemoryDraftStore implements DraftStore {

    private final Map<String, DraftRecord> rows = new LinkedHashMap<>();

    @Override
    public synchronized void save(DraftRecord draft) {
        rows.put(draft.form + "|" + draft.key, draft);
    }

    @Override
    public synchronized DraftRecord find(String form, String key) {
        return rows.get(form + "|" + key);
    }

    @Override
    public synchronized void delete(String form, String key) {
        rows.remove(form + "|" + key);
    }

    @Override
    public synchronized List<DraftRecord> findByOwner(String ownerId) {
        List<DraftRecord> found = new ArrayList<>();
        for (DraftRecord row : rows.values()) if (ownerId.equals(row.ownerId)) found.add(row);
        return found;
    }

    @Override
    public synchronized void deleteByOwner(String ownerId) {
        rows.values().removeIf(row -> ownerId.equals(row.ownerId));
    }

    @Override
    public synchronized List<DraftRecord> findAll() {
        return new ArrayList<>(rows.values());
    }

    @Override
    public synchronized long count() {
        return rows.size();
    }

    @Override
    public synchronized int deleteOlderThan(Date before) {
        int size = rows.size();
        rows.values().removeIf(row -> row.updatedAt != null && row.updatedAt.before(before));
        return size - rows.size();
    }

    @Override
    public synchronized void deleteAll() {
        rows.clear();
    }
}
