package com.onevour.core.components.recycleview;
//
// Created by Zuliadin on 2019-12-24.
//


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.security.InvalidParameterException;

public class RecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    private PaginationListener listener;

    public static final int PAGE_START = 1;

    @NonNull
    private AdapterGeneric adapter;

    @NonNull
    private LinearLayoutManager layoutManager;
    /**
     * Set scrolling threshold here (for now i'm assuming 10 item in one page)
     */
    private int size = 15;

    /**
     * Supporting only LinearLayoutManager for now.
     */
    public RecyclerViewScrollListener(@NonNull AdapterGeneric adapter, @NonNull LinearLayoutManager layoutManager, int size, PaginationListener listener) {
        this.adapter = adapter;
        this.layoutManager = layoutManager;
        if (size > this.size) {
            this.size = size;
        }
        this.listener = listener;
        if (size == 0) {
            throw new InvalidParameterException("size cannot must greater than 0");
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        if (null != listener && !adapter.isLoader()) {
            if (visibleItemCount + firstVisibleItemPosition == totalItemCount) {
                recyclerView.post(() -> {
                    if (adapter.getAdapterList().size() >= size) {
                        adapter.showLoader();
                    }
                });
            }
            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0 && totalItemCount >= size) {
                listener.loadMoreItems(adapter.getItem(totalItemCount - 1));
            }
        }
    }

    public interface PaginationListener<E> {

        void loadMoreItems(E e);

    }


}
