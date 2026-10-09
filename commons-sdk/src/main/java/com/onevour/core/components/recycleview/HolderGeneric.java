package com.onevour.core.components.recycleview;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.onevour.core.utilities.commons.ValueOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class HolderGeneric<S extends ViewBinding, E> extends RecyclerView.ViewHolder {

    protected Context context;

    private int position;

    private final List<Listener> listener = new ArrayList<>();

    private final Map<Class, Listener> cachedListener = new HashMap<>();

    protected E value;

    protected S binding;

    public HolderGeneric(S binding) {
        super(binding.getRoot());
        this.binding = binding;
        this.context = binding.getRoot().getContext();
    }

    public void setListener(HolderGeneric.Listener listener) {
        this.listener.add(listener);
    }

    protected Context getContext() {
        return context;
    }

    protected void onBindViewHolder(List<E> o, int position) {
        value = o.get(position);
    }

    protected void onBindViewHolder(List<E> o, int position, int size) {
        value = o.get(position);
    }

    protected void onBindViewHolder(E o) {
        value = o;
    }

    protected void onBindViewHolder(E o, int position) {
        value = o;
    }

    protected void onBindViewHolder(E o, int position, int size) {
        value = o;
    }

    protected void onBindViewHolder(E o, int position, boolean isFirst, boolean isLast) {
        value = o;
    }

    protected List<Listener> getListener() {
        return listener;
    }

    public <E extends HolderGeneric.Listener> E getListener(Class<E> clazz) {
        HolderGeneric.Listener listenerTMP = cachedListener.get(clazz);
        if (ValueOf.nonNull(listenerTMP)) {
            return (E) listenerTMP;
        }
        for (HolderGeneric.Listener o : listener) {
            for (Class<?> impl : o.getClass().getInterfaces()) {
                if (impl.equals(clazz)) {
                    cachedListener.put(clazz, o);
                    return (E) o;
                }
            }
        }
        return null;
    }

    protected void setPosition(int position) {
        this.position = position;
    }

    public int getCurrentPosition() {
        return position;
    }

    public interface AdapterListener<E> {

        void onLoadRetry(int index, E o);

    }

    public interface Listener {

    }
}
