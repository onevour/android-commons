package com.onevour.core.components.recycleview;

import androidx.recyclerview.widget.AsyncListDiffer;

import java.util.ArrayList;
import java.util.List;

/**
 * With a diff: a new list is shown only once its diff, computed off the main thread, is applied.
 * Each change builds on the list submitted last, not on the one shown, so back-to-back changes
 * (addMore, clear then addMore) all land; building on the shown one dropped rows.
 */
class DiffListUpdater<E> implements ListUpdater<E> {

    private final AsyncListDiffer<E> differ;

    private List<E> submitted;

    DiffListUpdater(AsyncListDiffer<E> differ, List<E> values) {
        this.differ = differ;
        this.submitted = new ArrayList<>(differ.getCurrentList());
        // rows set before the diff was registered are kept
        if (submitted.isEmpty() && !values.isEmpty()) submit(new ArrayList<>(values), null);
    }

    @Override
    public List<E> shown() {
        return differ.getCurrentList();
    }

    @Override
    public List<E> latest() {
        return submitted;
    }

    @Override
    public void set(List<E> values, Runnable committed) {
        submit(new ArrayList<>(values), committed);
    }

    @Override
    public void append(List<E> values, Runnable committed) {
        List<E> next = new ArrayList<>(submitted);
        next.addAll(values);
        submit(next, committed);
    }

    @Override
    public void replace(int index, E value, Runnable committed) {
        List<E> next = new ArrayList<>(submitted);
        next.set(index, value);
        submit(next, committed);
    }

    private void submit(List<E> values, Runnable committed) {
        submitted = values;
        differ.submitList(values, committed);
    }
}
