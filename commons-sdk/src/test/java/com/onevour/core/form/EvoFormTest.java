package com.onevour.core.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.onevour.core.draft.Drafts;
import com.onevour.core.draft.DraftsTest;
import com.onevour.core.form.annotation.Required;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.Executor;

public class EvoFormTest {

    public static class Values {
        @Required
        String name;

        Long quantity;

        String note;
    }

    /** Runs the posted work at once; the delayed draft write waits for {@link #runDelayed()}. */
    static final class Immediate implements EvoForm.Scheduler {

        Runnable delayed;

        @Override
        public void post(Runnable work) {
            work.run();
        }

        @Override
        public void postDelayed(Runnable work, long delayMs) {
            delayed = work;
        }

        @Override
        public void cancel(Runnable work) {
            if (delayed == work) delayed = null;
        }

        void runDelayed() {
            Runnable work = delayed;
            delayed = null;
            if (work != null) work.run();
        }
    }

    @Rule
    public InstantTaskExecutorRule liveData = new InstantTaskExecutorRule();

    private final Executor now = Runnable::run;

    private DraftsTest.MemoryStore store;

    private Immediate scheduler;

    @Before
    public void setUp() {
        store = new DraftsTest.MemoryStore();
        Drafts.useStore(store);
        Drafts.configure(null, null);
        scheduler = new Immediate();
    }

    private EvoForm<Values> model() {
        return EvoForm.of(Values.class).draft("TEST", "k1", "owner").withScheduler(scheduler, now);
    }

    @Test
    public void resetIsNotDirtyAndTextOfANumberIsTheSameValue() {
        EvoForm<Values> form = model();
        Values start = new Values();
        start.quantity = 12L;
        form.reset(start);
        assertFalse(form.isDirty());
        form.setValue("quantity", "12");
        assertFalse(form.isDirty());
        form.setValue("note", "");
        assertFalse(form.isDirty());
        form.setValue("note", "hello");
        assertTrue(form.isDirty());
        assertEquals("hello", form.value("note").getValue());
    }

    @Test
    public void validateFillsErrorsAndAnEditClearsItsError() {
        EvoForm<Values> form = model();
        form.reset(null);
        assertFalse(form.validate());
        Map<String, FormError> errors = form.errors().getValue();
        assertNotNull(errors);
        assertTrue(errors.containsKey("name"));
        assertEquals(Integer.valueOf(1), form.validations().getValue());
        form.setValue("name", "Ani");
        assertTrue(form.errors().getValue().isEmpty());
        assertTrue(form.validate());
        assertEquals("Ani", form.values().name);
    }

    @Test
    public void editsAreKeptAsADraftAndOfferedAgain() {
        EvoForm<Values> form = model();
        form.reset(null);
        form.setValue("name", "Ani");
        scheduler.runDelayed();
        assertEquals(1, store.count());

        EvoForm<Values> again = model();
        again.reset(null);
        assertNotNull(again.draftOffer().getValue());
        assertNull(again.getValue("name"));
        again.continueDraft();
        assertNull(again.draftOffer().getValue());
        assertEquals("Ani", again.getValue("name"));
        assertTrue(again.isDirty());
    }

    @Test
    public void discardingTheOfferKeepsTheStartAndDropsTheDraft() {
        EvoForm<Values> form = model();
        form.reset(null);
        form.setValue("name", "Ani");
        form.flushDraft();

        EvoForm<Values> again = model();
        again.reset(null);
        again.discardOfferedDraft();
        assertNull(again.getValue("name"));
        assertEquals(0, store.count());
    }

    @Test
    public void savedDeletesTheDraftAndIsCleanAgain() {
        EvoForm<Values> form = model();
        form.reset(null);
        form.setValue("name", "Ani");
        form.flushDraft();
        form.saved();
        assertFalse(form.isDirty());
        assertEquals(0, store.count());
        assertNull(scheduler.delayed);
    }

    @Test
    public void discardPutsTheStartingValuesBack() {
        EvoForm<Values> form = model();
        Values start = new Values();
        start.name = "Budi";
        form.reset(start);
        form.setValue("name", "Ani");
        form.flushDraft();
        form.discard();
        assertEquals("Budi", form.getValue("name"));
        assertFalse(form.isDirty());
        assertEquals(0, store.count());
    }

    @Test
    public void aFormWithoutDraftWritesNothing() {
        EvoForm<Values> form = EvoForm.of(Values.class).withScheduler(scheduler, now);
        form.reset(null);
        form.setValue("name", "Ani");
        form.flushDraft();
        assertEquals(0, store.count());
        assertFalse(form.hasDraft());
    }
}
