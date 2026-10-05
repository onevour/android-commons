package com.onevour.core.components.recycleview;

import java.util.List;

/**
 * How {@link AdapterGeneric} changes its list: {@link DirectListUpdater} right away (the default),
 * {@link DiffListUpdater} through a diff once {@code registerAsyncListDiffer} is called.
 */
interface ListUpdater<E> {

    /** The list the RecyclerView shows. */
    List<E> shown();

    /** The list as last changed, including changes not shown yet. */
    List<E> latest();

    /** Replaces the list; {@code committed} (may be null) runs once it is shown. */
    void set(List<E> values, Runnable committed);

    /** Adds rows at the end; {@code committed} as in {@link #set}. */
    void append(List<E> values, Runnable committed);

    /** Replaces the row at {@code index}; {@code committed} as in {@link #set}. */
    void replace(int index, E value, Runnable committed);
}
