package com.onevour.core.base;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.text.Editable;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import com.onevour.core.utilities.commons.ContextHelper;
import com.onevour.core.utilities.commons.RefSession;
import com.onevour.core.utilities.commons.ValueOf;
import com.onevour.core.rest.handler.RestRequestBuilder;
import com.onevour.core.utilities.ui.adapter.layout.AutoFitGridLayoutManager;
import com.onevour.core.components.recycleview.AdapterGeneric;
import com.onevour.core.components.recycleview.RecyclerViewScrollListener;

@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public class BaseActivity extends AppCompatActivity {

    protected RefSession session = new RefSession();

    protected int sizePage = 20;

    protected void init(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        init(recyclerView, adapter, null, null, sizePage, false);
    }

    protected void init(RecyclerView recyclerView, RecyclerView.Adapter adapter, AdapterGeneric.AdapterListener adapterListener) {
        init(recyclerView, adapter, adapterListener, null, sizePage, false);
    }

    protected void init(RecyclerView recyclerView, RecyclerView.Adapter adapter, AdapterGeneric.AdapterListener adapterListener, RecyclerViewScrollListener.PaginationListener scrollListener, boolean isLoadFirst) {
        init(recyclerView, adapter, adapterListener, scrollListener, sizePage, isLoadFirst);
    }

    protected void init(RecyclerView recyclerView, RecyclerView.Adapter adapter, AdapterGeneric.AdapterListener adapterListener, RecyclerViewScrollListener.PaginationListener scrollListener, int sizePage, boolean isLoadFirst) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        if (adapter instanceof AdapterGeneric) {
            AdapterGeneric genericAdapter = ((AdapterGeneric) adapter);
            if (isLoadFirst) genericAdapter.showLoader();
            if (null != adapterListener) {
                // genericAdapter.setAdapterListener(adapterListener);
            }
            if (null != scrollListener) {
                recyclerView.addOnScrollListener(new RecyclerViewScrollListener(genericAdapter, layoutManager, sizePage, scrollListener));
            }

        }
    }

    protected void initHorizontal(RecyclerView recyclerView, RecyclerView.Adapter adapter) {
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    protected void initGrid(RecyclerView recyclerView, RecyclerView.Adapter adapter, int width) {
        recyclerView.setLayoutManager(new AutoFitGridLayoutManager(recyclerView.getContext(), width));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    /**
     * auto fit grid
     */
    protected void initGridInLine(RecyclerView recyclerView, RecyclerView.Adapter adapter, int count) {
        recyclerView.setLayoutManager(new GridLayoutManager(recyclerView.getContext(), count));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    protected void initLayoutManager(RecyclerView... recyclerViews) {
        for (RecyclerView rv : recyclerViews) {
            rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
            rv.setItemAnimator(new DefaultItemAnimator());
        }
    }

    /**
     * auto fit grid
     */
    protected void initLayoutManagerGrid(int width, RecyclerView... recyclerViews) {
        for (RecyclerView rv : recyclerViews) {
            rv.setLayoutManager(new AutoFitGridLayoutManager(rv.getContext(), width));
            rv.setItemAnimator(new DefaultItemAnimator());
        }
    }

    protected void hideInput(View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    protected void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            view.requestLayout();
        }
    }

    protected void setMarginsInDP(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(dpToPx(left), dpToPx(top), dpToPx(right), dpToPx(bottom));
            view.requestLayout();
        }
    }

    protected int widthScreen(Context context) {
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    protected int heightScreen(Context context) {
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    protected void snapScroll(RecyclerView recyclerView) {
        LinearSnapHelper snapHelper = new LinearSnapHelper() {

            @Override
            public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
                View centerView = findSnapView(layoutManager);
                if (centerView == null)
                    return RecyclerView.NO_POSITION;

                int position = layoutManager.getPosition(centerView);
                int targetPosition = -1;
                if (layoutManager.canScrollHorizontally()) {
                    if (velocityX < 0) {
                        targetPosition = position - 1;
                    } else {
                        targetPosition = position + 1;
                    }
                }

                if (layoutManager.canScrollVertically()) {
                    if (velocityY < 0) {
                        targetPosition = position - 1;
                    } else {
                        targetPosition = position + 1;
                    }
                }

                final int firstItem = 0;
                final int lastItem = layoutManager.getItemCount() - 1;
                targetPosition = Math.min(lastItem, Math.max(targetPosition, firstItem));
                return targetPosition;
            }
        };
        snapHelper.attachToRecyclerView(recyclerView);
    }

    protected String string(EditText editText) {
        Editable editable = editText.getText();
        if (null == editable) return null;
        return editable.toString().trim();
    }

    protected String string(TextInputEditText editText) {
        Editable editable = editText.getText();
        if (null == editable) return null;
        return editable.toString().trim();
    }

    /**
     * return default empty string if null
     *
     * @param value
     */
    protected String stringDefEmpty(String value) {
        if (value == null) return "";
        return value;
    }

    protected boolean isEmpty(EditText... editTexts) {
        for (EditText o : editTexts) {
            if (ValueOf.isEmpty(string(o))) {
                return true;
            }
        }
        return false;
    }

    protected boolean isEmpty(TextInputEditText... editTexts) {
        for (TextInputEditText o : editTexts) {
            if (ValueOf.isEmpty(string(o))) {
                return true;
            }
        }
        return false;
    }

    protected void error(String message, EditText... editTexts) {
        for (EditText o : editTexts) {
            o.setError(message);
        }
    }

    protected void error(String message, TextInputEditText... editTexts) {
        for (TextInputEditText o : editTexts) {
            o.setError(message);
        }
    }

    protected void clearError(EditText... editTexts) {
        for (EditText o : editTexts) {
            o.setError(null);
        }
    }

    protected void clearError(TextInputEditText... editTexts) {
        for (TextInputEditText o : editTexts) {
            o.setError(null);
        }
    }

    protected boolean isNull(Object... values) {
        return ValueOf.isNull(values);
    }

    protected boolean isEmpty(String... values) {
        return ValueOf.isEmpty(values);
    }

    protected int dpToPx(int value) {
        Resources resources = ContextHelper.getApplication().getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.getDisplayMetrics()));
    }

    protected String append(String... strings) {
        StringBuilder sb = new StringBuilder();
        for (String s : strings) sb.append(s).append(" ");
        return sb.toString();
    }

    protected RestRequestBuilder api() {
        return new RestRequestBuilder();
    }

    protected void shortToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void longToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void shortSnack(View view, String message) {
        Snackbar.make(view, message, 1200).show();
    }

    protected void longSnack(View view, String message) {
        Snackbar.make(view, message, 2000).show();
    }

}