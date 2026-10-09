package com.onevour.core.components.recycleview;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AdapterListUpdateCallback;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.onevour.core.utilities.beans.BeanCopy;
import com.onevour.core.utilities.commons.ValueOf;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/***
 * multiple holder<br/>
 * harus ada parameter yang mapping<br/>
 * List -> dan key holder nya<br/>
 * */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AdapterGeneric<E extends AdapterModel> extends RecyclerView.Adapter<HolderGeneric> implements HolderGeneric.Listener {

    private static final String TAG = AdapterGeneric.class.getSimpleName();

    private static final Map<Class<?>, boolean[]> bindOverridesCache = new HashMap<>();

    private boolean isLoader = false;

    private final int VIEW_LOADER = 0;

    private final Map<Integer, Class> holders = new HashMap<>();

    private final Map<Integer, Class> layoutHolderBindings = new HashMap<>();

    private final List<HolderGeneric.Listener> holderListener = new ArrayList<>();

    private Type modelType;

    private RecyclerView recyclerView;

    private boolean keepAtTop;

    // kept until a list is shown: a newer list cancels the callback of the one before it
    private boolean scrollToTopPending;

    // changes the list right away until registerAsyncListDiffer() switches to a diff
    private ListUpdater<E> updater = new DirectListUpdater<>(new AdapterListUpdateCallback(this),
            this::notifyDataSetChanged, new ArrayList<>());

    protected AdapterGeneric() {
        registerHolder();
    }

    protected abstract void registerHolder();

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (this.recyclerView == recyclerView) this.recyclerView = null;
    }

    /**
     * a list showing its first row keeps showing it when rows come in above the visible ones
     * (e.g. the first load under a footer), instead of staying on the rows it showed
     */
    public void setKeepAtTop(boolean keepAtTop) {
        this.keepAtTop = keepAtTop;
    }

    /**
     * register if use view binding
     */
    protected <VH extends HolderGeneric> void registerBindView(Class<VH> holder) {
        registerBindView(1, holder);
    }

    /**
     * diff new lists instead of replacing them, e.g. so a new order only moves rows; without it
     * the list changes right away. Rows are told apart by the differ's ItemCallback (an id),
     * not by AdapterModel, which has no equals
     */
    protected void registerAsyncListDiffer(AsyncListDiffer<E> asyncListDiffer) {
        this.updater = new DiffListUpdater<>(asyncListDiffer, updater.latest());
    }

    protected void registerAsyncListDiffer(DiffUtil.ItemCallback<E> diffCallback) {
        registerAsyncListDiffer(new AsyncListDiffer<>(this, diffCallback));
    }

    protected <VH extends HolderGeneric> void registerBindView(int type, Class<VH> holder) {
        if (type <= 0) {
            throw new IllegalArgumentException("please input type greater than 0");
        }
        if (ValueOf.nonNull(holders.get(type))) {
            throw new IllegalArgumentException("holder already register! ".concat(holder.getName()));
        }
        holders.put(type, holder);
        Type typeOfBinding = ((ParameterizedType) holder.getGenericSuperclass()).getActualTypeArguments()[0];
        Class<?> bindingClass = (Class<?>) typeOfBinding;
        layoutHolderBindings.put(type, bindingClass);
    }

    @Override
    public int getItemViewType(int position) {
        AdapterModel value = updater.shown().get(position);
        if (ValueOf.nonNull(value)) {
            return value.getType();
        }
        return VIEW_LOADER;
    }

    @NonNull
    @Override
    public HolderGeneric onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            Context context = parent.getContext();
            Class<?> holderClass = holders.get(viewType);
            if (Objects.isNull(holderClass)) {
                throw new NoSuchMethodException("Null holder class, not define before");
            }
            Class<?> bindingClass = layoutHolderBindings.get(viewType);
            if (Objects.isNull(bindingClass)) {
                throw new NoSuchMethodException("Null binding class, not define before");
            }
            Method method = bindingClass.getMethod("inflate", LayoutInflater.class, ViewGroup.class, boolean.class);
            ViewBinding binding = (ViewBinding) method.invoke(null, LayoutInflater.from(context), parent, false);
            return holderGenerator(holderClass, binding);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private HolderGeneric holderGenerator(Type type, ViewBinding convertView) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        Constructor constructor = ((Class<E>) type).getConstructor(convertView.getClass());
        HolderGeneric holderGeneric = (HolderGeneric) constructor.newInstance(convertView);
        return holderGeneric;
    }



    /**
     * HolderGeneric exposes several onBindViewHolder overloads for convenience; a concrete
     * holder normally overrides only one. Resolve (once per holder class, then cached) which
     * ones are actually overridden so bind only invokes those instead of calling all of them.
     */
    private boolean[] resolveBindOverrides(Class<?> holderClass) {
        boolean[] cached = bindOverridesCache.get(holderClass);
        if (cached != null) return cached;
        boolean[] overrides = new boolean[]{
                isOverridden(holderClass, "onBindViewHolder", Object.class),
                isOverridden(holderClass, "onBindViewHolder", Object.class, int.class),
                isOverridden(holderClass, "onBindViewHolder", Object.class, int.class, int.class),
                isOverridden(holderClass, "onBindViewHolder", List.class, int.class),
                isOverridden(holderClass, "onBindViewHolder", List.class, int.class, int.class),
                isOverridden(holderClass, "onBindViewHolder", Object.class, int.class, boolean.class, boolean.class),
        };
        bindOverridesCache.put(holderClass, overrides);
        return overrides;
    }

    private boolean isOverridden(Class<?> holderClass, String name, Class<?>... paramTypes) {
        Class<?> current = holderClass;
        while (current != null && current != HolderGeneric.class) {
            try {
                current.getDeclaredMethod(name, paramTypes);
                return true;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return false;
    }

    @Override
    public void onBindViewHolder(@NonNull HolderGeneric holder, int position) {
        E o = getItem(position);
        int size = getItemCount();
        // add listener
        for (HolderGeneric.Listener listener : holderListener) {
            holder.setListener(listener);
        }
        holder.setPosition(position);
        boolean[] overrides = resolveBindOverrides(holder.getClass());
        if (overrides[0]) holder.onBindViewHolder(o);
        if (overrides[1]) holder.onBindViewHolder(o, position);
        if (overrides[2]) holder.onBindViewHolder(o, position, size);
        if (overrides[3]) holder.onBindViewHolder(updater.shown(), position);
        if (overrides[4]) holder.onBindViewHolder(updater.shown(), position, size);
        if (overrides[5])
            holder.onBindViewHolder(o, position, 0 == position && !isLoader, position == getItemCount() - 1 && !isLoader);
    }

    @Override
    public int getItemCount() {
        return updater.shown().size();
    }

    public void setHolderListener(HolderGeneric.Listener holderListener) {
        this.holderListener.add(holderListener);
    }

    private void setLoader(boolean loader) {
        isLoader = loader;
    }

    public void addMore(E o) {
        if (ValueOf.isNull(o)) {
            Log.w(TAG, "cannot insert null value!");
            return;
        }
        updater.append(Collections.singletonList(o), this::onShown);
    }

    public void addMore(final List<E> adapterList) {
        addMore(adapterList, true);
    }

    public void addMore(final List<E> adapterList, boolean isRemoveLoader) {
        if (isRemoveLoader) removeLoader();
        updater.append(adapterList, this::onShown);
    }

    public void setValue(List<E> values) {
        setValue(values, true);
    }

    /**
     * reset adapter and add new all
     */
    public void setValue(List<E> values, boolean isRemoveLoader) {
        setValue(values, isRemoveLoader, null);
    }

    /**
     * reset adapter and add new all; committed runs once the new list is shown
     * (with a diff, that is after it is computed off the main thread), e.g. to scroll to the first row
     */
    public void setValue(List<E> values, Runnable committed) {
        setValue(values, true, committed);
    }

    public void setValue(List<E> values, boolean isRemoveLoader, Runnable committed) {
        if (isRemoveLoader) removeLoader();
        if (keepAtTop && recyclerView != null && !recyclerView.canScrollVertically(-1)) {
            scrollToTopPending = true;
        }
        updater.set(values, () -> {
            onShown();
            if (committed != null) committed.run();
        });
    }

    /**
     * reset adapter and add new all, then show the first row (e.g. after a new sort); with a diff,
     * scrolling right away would be undone by the rows moving once the diff is applied
     */
    public void setValueToTop(List<E> values) {
        scrollToTopPending = true;
        setValue(values);
    }

    public void updateItem(int index, E value) {
        updater.replace(index, BeanCopy.gson(value, modelType()), this::onShown);
    }

    public void clear() {
        updater.set(new ArrayList<>(), this::onShown);
    }

    // a list is shown: a scroll to the top asked for earlier (maybe for a list a newer one replaced) happens now
    private void onShown() {
        if (!scrollToTopPending) return;
        scrollToTopPending = false;
        if (recyclerView != null) recyclerView.scrollToPosition(0);
    }

    /**
     * the values as last set, including changes not shown yet; a copy, change it and set it back
     */
    public List<E> getAdapterList() {
        return new ArrayList<>(updater.latest());
    }

    /**
     * the item shown at this position
     */
    public E getItem(int index) {
        return updater.shown().get(index);
    }

    public void showLoader() {
        setLoader(true);
    }

    public void removeLoader() {
        setLoader(false);
    }

    public boolean isLoader() {
        return isLoader;
    }

    public Type modelType() {
        if (Objects.nonNull(modelType)) return modelType;
        Class<?> current = getClass();
        while (current != null && AdapterGeneric.class.isAssignableFrom(current)) {
            Type genericSuperclass = current.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
                if (parameterizedType.getRawType() == AdapterGeneric.class) {
                    Type typeArgument = parameterizedType.getActualTypeArguments()[0];
                    if (typeArgument instanceof Class) {
                        modelType = typeArgument;
                        return modelType;
                    }
                    break;
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Unable to resolve model type for " + getClass().getName()
                + ". A concrete subclass must directly parameterize AdapterGeneric<Model>, e.g. \"class MyAdapter extends AdapterGeneric<MyModel>\".");
    }

    @Deprecated
    public interface AdapterListener<E> {

        void onLoadRetry(int index, E o);

    }

}
