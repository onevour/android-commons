package com.onevour.core.components.recycleview;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Changes made before the previous diff is shown must build on each other, as the adapter
 * did before it diffed asynchronously.
 */
public class AdapterGenericTest {

    static class Model extends AdapterModel<String> {

        Model(String value) {
            super(value);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Model && Objects.equals(getModel(), ((Model) o).getModel());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getModel());
        }
    }

    // no RecyclerView in a JVM test (its observable is an Android stub): nothing to notify
    private static final ListUpdateCallback NO_VIEW = new ListUpdateCallback() {
        @Override
        public void onInserted(int position, int count) {
        }

        @Override
        public void onRemoved(int position, int count) {
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
        }
    };

    /** Diffs right away, but shows a list only on {@link #show()}, like a busy main thread. */
    static class Adapter extends AdapterGeneric<Model> {

        private final Queue<Runnable> mainThread = new ArrayDeque<>();

        Adapter() {
            DiffUtil.ItemCallback<Model> callback = new DiffUtil.ItemCallback<Model>() {
                @Override
                public boolean areItemsTheSame(@NonNull Model oldItem, @NonNull Model newItem) {
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Model oldItem, @NonNull Model newItem) {
                    return oldItem.equals(newItem);
                }
            };
            registerAsyncListDiffer(new AsyncListDiffer<>(NO_VIEW,
                    new AsyncDifferConfig.Builder<>(callback)
                            .setBackgroundThreadExecutor(Runnable::run)
                            .setMainThreadExecutor(mainThread::add)
                            .build()));
        }

        @Override
        protected void registerHolder() {
        }

        void show() {
            while (!mainThread.isEmpty()) mainThread.poll().run();
        }

        List<String> shown() {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < getItemCount(); i++) values.add(getItem(i).getModel());
            return values;
        }
    }

    private static List<Model> models(String... values) {
        List<Model> models = new ArrayList<>();
        for (String value : values) models.add(new Model(value));
        return models;
    }

    private static Adapter adapterShowing(String... values) {
        Adapter adapter = new Adapter();
        adapter.setValue(models(values));
        adapter.show();
        return adapter;
    }

    @Test
    public void addMoreBackToBackKeepsEveryRow() {
        Adapter adapter = adapterShowing("x");
        adapter.addMore(new Model("a"));
        adapter.addMore(new Model("b"));
        adapter.show();
        assertEquals(Arrays.asList("x", "a", "b"), adapter.shown());
    }

    @Test
    public void addMoreListsBackToBackKeepsEveryRow() {
        Adapter adapter = adapterShowing("x");
        adapter.addMore(models("a", "b"));
        adapter.addMore(models("c"));
        adapter.show();
        assertEquals(Arrays.asList("x", "a", "b", "c"), adapter.shown());
    }

    @Test
    public void addMoreAfterClearStartsEmpty() {
        Adapter adapter = adapterShowing("x", "y");
        adapter.clear();
        adapter.addMore(new Model("a"));
        adapter.show();
        assertEquals(Arrays.asList("a"), adapter.shown());
    }

    @Test
    public void addMoreAfterSetValueAddsToTheNewValues() {
        Adapter adapter = adapterShowing("x");
        adapter.setValue(models("p", "q"));
        adapter.addMore(new Model("a"));
        adapter.show();
        assertEquals(Arrays.asList("p", "q", "a"), adapter.shown());
    }

    @Test
    public void adapterListHasChangesNotShownYet() {
        Adapter adapter = adapterShowing("x");
        adapter.addMore(new Model("a"));
        assertEquals(Arrays.asList("x"), adapter.shown());
        assertEquals(2, adapter.getAdapterList().size());
    }
}
