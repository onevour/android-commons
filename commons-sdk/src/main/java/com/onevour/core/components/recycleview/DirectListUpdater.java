package com.onevour.core.components.recycleview;

import androidx.recyclerview.widget.ListUpdateCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Without a diff: the list changes at once and the adapter is told what changed, so the list
 * shown and the list as last changed are always the same.
 */
class DirectListUpdater<E> implements ListUpdater<E> {

    private final ListUpdateCallback rows;

    private final Runnable allChanged;

    private final List<E> values;

    /**
     * @param rows       told of rows added or changed
     * @param allChanged told when the whole list is replaced (notifyDataSetChanged)
     */
    DirectListUpdater(ListUpdateCallback rows, Runnable allChanged, List<E> values) {
        this.rows = rows;
        this.allChanged = allChanged;
        this.values = new ArrayList<>(values);
    }

    @Override
    public List<E> shown() {
        return values;
    }

    @Override
    public List<E> latest() {
        return values;
    }

    @Override
    public void set(List<E> newValues, Runnable committed) {
        List<E> copy = new ArrayList<>(newValues); // newValues may be this very list
        values.clear();
        values.addAll(copy);
        allChanged.run();
        if (committed != null) committed.run();
    }

    @Override
    public void append(List<E> newValues, Runnable committed) {
        int start = values.size();
        values.addAll(newValues);
        rows.onInserted(start, newValues.size());
        if (committed != null) committed.run();
    }

    @Override
    public void replace(int index, E value, Runnable committed) {
        values.set(index, value);
        rows.onChanged(index, 1, null);
        if (committed != null) committed.run();
    }
}
