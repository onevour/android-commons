package com.onevour.core.draft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DraftsTest {

    /** A store in memory. */
    public static final class MemoryStore implements DraftStore {

        final Map<String, DraftRecord> rows = new LinkedHashMap<>();

        @Override
        public void save(DraftRecord draft) {
            rows.put(draft.form + "|" + draft.key, draft);
        }

        @Override
        public DraftRecord find(String form, String key) {
            return rows.get(form + "|" + key);
        }

        @Override
        public void delete(String form, String key) {
            rows.remove(form + "|" + key);
        }

        @Override
        public List<DraftRecord> findByOwner(String ownerId) {
            List<DraftRecord> found = new ArrayList<>();
            for (DraftRecord r : rows.values()) if (ownerId.equals(r.ownerId)) found.add(r);
            return found;
        }

        @Override
        public void deleteByOwner(String ownerId) {
            rows.values().removeIf(r -> ownerId.equals(r.ownerId));
        }

        @Override
        public List<DraftRecord> findAll() {
            return new ArrayList<>(rows.values());
        }

        @Override
        public long count() {
            return rows.size();
        }

        @Override
        public int deleteOlderThan(Date before) {
            int size = rows.size();
            rows.values().removeIf(r -> r.updatedAt != null && r.updatedAt.before(before));
            return size - rows.size();
        }

        @Override
        public void deleteAll() {
            rows.clear();
        }
    }

    static final class Reason {
        String reasonId;
        String description;
    }

    private MemoryStore store;

    private final List<Long> committed = new ArrayList<>();

    private final List<Long> discarded = new ArrayList<>();

    @Before
    public void setUp() {
        store = new MemoryStore();
        Drafts.useStore(store);
        Drafts.configure(new AttachmentHandler() {
            @Override
            public void commit(Collection<Long> ids) {
                committed.addAll(ids);
            }

            @Override
            public void discard(Collection<Long> ids) {
                discarded.addAll(ids);
            }
        }, () -> "S01");
    }

    @Test
    public void savedDraftIsReadBackWithItsOwnerAndUser() {
        Reason reason = new Reason();
        reason.reasonId = "R01";
        reason.description = "toko tutup";
        Drafts.save("REASON", "C1:2026", "C1", reason);

        Drafts.Draft<Reason> back = Drafts.load("REASON", "C1:2026", Reason.class);
        assertNotNull(back);
        assertEquals("R01", back.state.reasonId);
        assertEquals("toko tutup", back.state.description);
        assertEquals("S01", store.find("REASON", "C1:2026").userId);
        assertEquals(1, Drafts.formsOf("C1").size());
    }

    @Test
    public void unreadableDraftIsDropped() {
        store.save(new DraftRecord("REASON", "k", null, null, "{not json", new Date()));
        assertNull(Drafts.load("REASON", "k", Reason.class));
        assertEquals(0, store.count());
    }

    @Test
    public void attachmentsGoToTheAppHandler() {
        List<Long> ids = new ArrayList<>();
        ids.add(5L);
        Drafts.commitAttachments(ids);
        Drafts.discardAttachments(ids);
        Drafts.discardAttachments(new ArrayList<>());
        assertEquals(1, committed.size());
        assertEquals(1, discarded.size());
    }
}
