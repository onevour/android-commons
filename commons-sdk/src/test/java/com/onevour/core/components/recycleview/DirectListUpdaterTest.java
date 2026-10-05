package com.onevour.core.components.recycleview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import androidx.recyclerview.widget.ListUpdateCallback;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Without a diff every change is shown at once, and the adapter is told exactly what changed. */
public class DirectListUpdaterTest {

    private final List<String> events = new ArrayList<>();

    private final ListUpdateCallback rows = new ListUpdateCallback() {
        @Override
        public void onInserted(int position, int count) {
            events.add("inserted " + position + "+" + count);
        }

        @Override
        public void onRemoved(int position, int count) {
            events.add("removed " + position + "+" + count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            events.add("moved " + fromPosition + ">" + toPosition);
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            events.add("changed " + position + "+" + count);
        }
    };

    private final DirectListUpdater<String> updater =
            new DirectListUpdater<>(rows, () -> events.add("all"), Collections.singletonList("x"));

    @Test
    public void appendBackToBackIsShownAtOnce() {
        updater.append(Collections.singletonList("a"), null);
        updater.append(Arrays.asList("b", "c"), null);
        assertEquals(Arrays.asList("x", "a", "b", "c"), updater.shown());
        assertSame(updater.shown(), updater.latest());
        assertEquals(Arrays.asList("inserted 1+1", "inserted 2+2"), events);
    }

    @Test
    public void setThenAppendStartsFromTheNewValues() {
        updater.set(Collections.emptyList(), null);
        updater.append(Collections.singletonList("a"), null);
        assertEquals(Collections.singletonList("a"), updater.shown());
        assertEquals(Arrays.asList("all", "inserted 0+1"), events);
    }

    @Test
    public void setWithItsOwnListKeepsTheRows() {
        updater.set(updater.latest(), null);
        assertEquals(Collections.singletonList("x"), updater.shown());
    }

    @Test
    public void replaceChangesOneRow() {
        updater.replace(0, "y", null);
        assertEquals(Collections.singletonList("y"), updater.shown());
        assertEquals(Collections.singletonList("changed 0+1"), events);
    }

    @Test
    public void committedRunsAtOnce() {
        List<String> ran = new ArrayList<>();
        updater.set(Collections.singletonList("a"), () -> ran.add("set"));
        updater.append(Collections.singletonList("b"), () -> ran.add("append"));
        updater.replace(0, "c", () -> ran.add("replace"));
        assertEquals(Arrays.asList("set", "append", "replace"), ran);
    }
}
