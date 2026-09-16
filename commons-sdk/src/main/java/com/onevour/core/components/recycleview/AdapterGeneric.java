package com.onevour.core.components.recycleview;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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


    private AsyncListDiffer<E> asyncListDiffer = new AsyncListDiffer<E>(this, new DiffUtil.ItemCallback<E>() {
        @Override
        public boolean areItemsTheSame(@NonNull E oldItem, @NonNull E newItem) {
            return Objects.equals(oldItem, newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull E oldItem, @NonNull E newItem) {
            return Objects.equals(oldItem, newItem);
        }
    });


    protected AdapterGeneric() {
        registerHolder();
    }

    protected abstract void registerHolder();

    /**
     * register if use view binding
     */
    protected <VH extends HolderGeneric> void registerBindView(Class<VH> holder) {
        registerBindView(1, holder);
    }

    protected void registerAsyncListDiffer(AsyncListDiffer<E> asyncListDiffer) {
        this.asyncListDiffer = asyncListDiffer;
    }

    protected void registerAsyncListDiffer(DiffUtil.ItemCallback<E> diffCallback) {
        this.asyncListDiffer = new AsyncListDiffer<>(this, diffCallback);
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
        Log.d(TAG, "item view type: ".concat(String.valueOf(position)));
        AdapterModel value = asyncListDiffer.getCurrentList().get(position);
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
        if (overrides[3]) holder.onBindViewHolder(asyncListDiffer.getCurrentList(), position);
        if (overrides[4]) holder.onBindViewHolder(asyncListDiffer.getCurrentList(), position, size);
        if (overrides[5])
            holder.onBindViewHolder(o, position, 0 == position && !isLoader, position == getItemCount() - 1 && !isLoader);
        Log.d(TAG, "bind position ".concat(String.valueOf(position)));
    }

    @Override
    public int getItemCount() {
        return this.asyncListDiffer.getCurrentList().size();
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
        List<E> currentList = getAdapterList();
        currentList.add(o);
        this.asyncListDiffer.submitList(currentList);
    }

    public void addMore(final List<E> adapterList) {
        addMore(adapterList, true);
    }

    public void addMore(final List<E> adapterList, boolean isRemoveLoader) {
        if (isRemoveLoader) removeLoader();
        List<E> values = getAdapterList();
        values.addAll(adapterList);
        this.asyncListDiffer.submitList(values);
    }

    public void setValue(List<E> values) {
        setValue(values, true);
    }

    /**
     * reset adapter and add new all
     */
    public void setValue(List<E> values, boolean isRemoveLoader) {
        if (isRemoveLoader) removeLoader();
        ArrayList<E> newValues = new ArrayList<>(values);
        asyncListDiffer.submitList(newValues);
    }

    public void updateItem(int index, E value) {
        List<E> newValues = getAdapterList();
        newValues.set(index, BeanCopy.gson(value, modelType()));
        this.asyncListDiffer.submitList(newValues);
    }

    public void clear() {
        this.asyncListDiffer.submitList(new ArrayList<>());
    }

    public List<E> getAdapterList() {
        return new ArrayList<>(this.asyncListDiffer.getCurrentList());
    }

    public E getItem(int index) {
        return getAdapterList().get(index);
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
