package com.onevour.core.components.numpad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.onevour.core.R;
import com.onevour.core.utilities.commons.ValueOf;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zuliadin on 08/10/2016.
 * Updated by zuliadin on 30/01/2021.
 */
public class NumPad implements View.OnTouchListener {

    private static final String TAG = NumPad.class.getSimpleName();

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final NumPadInputView alert = new NumPadInputView();

    private final NumPadInputView.AlertListener viewListener = new NumPadInputView.AlertListener() {

        @Override
        public void inputValue(char value) throws ParseException {
            executor.execute(() -> {
                try {
                    adapter.append(String.valueOf(value));
                    handler.post(() -> alert.setResult(adapter.getValueString(), adapter.isAfterPoint()));
                } catch (ParseException e) {
                    handler.post(() -> alert.error(e.getMessage()));
                }
            });
        }

        @Override
        public void showMaxValue() {
            alert.showMaxValue();
        }

        @Override
        public void submitToMaxValue() {
            try {
                adapter.setValueToMax();
                adapter.validateInit();
                alert.show(adapter.getValueString(), adapter.isAfterPoint());
            } catch (ParseException e) {
                alert.error(e.getMessage());
            }
        }

        @Override
        public void delete() throws ParseException {
            executor.execute(() -> {
                adapter.delete();
                handler.post(() -> alert.setResult(adapter.getValueString(), adapter.isAfterPoint()));
            });
        }

        @Override
        public void submit() {
            editText.setText(adapter.getValueString());
            if (ValueOf.isNull(listener)) return;
            listener.onValue(editText.getId(), isDecimal(), adapter.getValueInteger(), adapter.getValueDouble());
            listener.onSubmitValue();
        }
    };

    private Context context;

    private EditText editText;

    private NumberFormat numberFormat;

    private Listener listener;

    private NumPadAdapter adapter;

    public NumPad() {

    }

    public NumPad(final EditText editText) {
        setup(editText);
    }

    public NumPad(final EditText editText, final NumberFormat numberFormat, double min, double max) {
        setup(editText, numberFormat, min, max);
    }

    public void setup(final Context context) {
        setup(new EditText(context), null, null, 0, Integer.MAX_VALUE);
    }

    public void setup(final Context context, double max) {
        setup(new EditText(context), null, null, 0, max);
    }

    public void setup(final Context context, double min, double max) {
        setup(new EditText(context), null, null, min, max);
    }

    public void setup(final Context context, NumberFormat numberFormat, double min, double max) {
        setup(new EditText(context), null, numberFormat, min, max);
    }

    public void setup(final Context context, Listener listener, NumberFormat numberFormat, double min, double max) {
        setup(new EditText(context), listener, numberFormat, min, max);
    }

    public void setup(final EditText editText) {
        setup(editText, null, null, 0, Integer.MAX_VALUE);
    }

    public void setup(final EditText editText, double max) {
        setup(editText, null, null, 0, max);
    }

    public void setup(final EditText editText, double min, double max) {
        setup(editText, null, null, min, max);
    }

    public void setup(@NonNull EditText editText, NumberFormat numberFormat, double min, double max) {
        setup(editText, null, numberFormat, min, max);
    }


    @SuppressLint("ClickableViewAccessibility")
    public void setup(@NonNull EditText editText, Listener listener, NumberFormat numberFormat, double min, double max) {
        this.context = editText.getRootView().getContext();
        this.listener = listener;
        this.numberFormat = numberFormat;
        this.editText = editText;
        this.editText.setTextIsSelectable(true);
        this.editText.setCursorVisible(false);
        this.editText.setFocusable(false);
        this.editText.setOnTouchListener(this);

        ViewCompat.replaceAccessibilityAction(
                this.editText,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                context.getString(R.string.cd_open_numpad),
                null
        );

        alert.init(context, numberFormat, min, max, viewListener);
        if (isDecimal()) {
            adapter = new InputDecimal(numberFormat, min, max);
        } else {
            adapter = new InputNumeric((int) min, (int) max);
        }
    }

    public void setStyle(NumPadStyle style) {
        alert.applyStyle(style);
    }

    public void setUseBottomSheet(boolean useBottomSheet) {
        alert.setUseBottomSheet(useBottomSheet);
    }

    public boolean isUseBottomSheet() {
        return alert.isUseBottomSheet();
    }

    public void updateMinMax(int min, int max) {
        updateMinMax(min, max, false);
    }

    public void updateMinMax(int min, int max, boolean showMax) {
        alert.updateMinMax(min, max);
        adapter.updateMinMax(min, max);
        if (showMax) showMaxValue();
    }

    public void updateMinMax(double min, double max) {
        updateMinMax(min, max, false);
    }

    public void updateMinMax(double min, double max, boolean showMax) {
        alert.updateMinMax(min, max);
        adapter.updateMinMax(min, max);
        if (showMax) showMaxValue();
    }

    private boolean isDecimal() {
        return Objects.nonNull(numberFormat);
    }


    public void disableTouch() {
        editText.setOnTouchListener(null);
    }

    public void enableTouch() {
        editText.setOnTouchListener(this);
    }

    public void show() {
        editText.setOnTouchListener(this);
        editText.dispatchTouchEvent(triggerTouch());
    }

    public void setTitle(String left) {
        if (ValueOf.isEmpty(left)) return;
        alert.setTitle(left);
    }

    /*
     * show UI on touch
     *
     * */
    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (ValueOf.nonNull(imm)) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        if (v.getId() == editText.getId() && motionEvent.getAction() == MotionEvent.ACTION_UP) {
            Log.d(TAG, "Action touch event : ".concat(String.valueOf(motionEvent.getAction())));
            executor.execute(() -> {
                try {
                    String valueStr = editText.getText().toString();
                    adapter.setValue(valueStr);
                    adapter.validateInit();
                    handler.post(() -> alert.show(adapter.getValueString(), adapter.isAfterPoint()));
                } catch (ParseException e) {
                    handler.post(() -> alert.error(e.getMessage()));
                }
            });
        }
        return false;
    }


    public void inputValue(int intValue) {
        inputValue(Double.valueOf(intValue));
    }

    public void inputValue(Double doubleValue) {
        if (Objects.isNull(doubleValue)) doubleValue = 0.0;
        if (!Double.isFinite(doubleValue)) {
            Log.w(TAG, "inputValue received a non-finite value (NaN/Infinity): " + doubleValue);
            if (ValueOf.nonNull(listener)) listener.onInvalidValue(editText.getId(), doubleValue);
            return;
        }
        if (Objects.isNull(numberFormat)) {
            editText.setText(String.valueOf(doubleValue.intValue()));
        } else {
            editText.setText(numberFormat.format(doubleValue));
        }
    }


    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Call from the owning Activity/Fragment's onDestroy() to stop the background executor.
     * Without this, every NumPad leaks a dedicated background thread.
     */
    public void destroy() {
        executor.shutdown();
    }


    public void showMaxValue() {
        alert.showMaxValue();
    }

    private MotionEvent triggerTouch() {
        // Obtain MotionEvent object
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        float x = 0.0f;
        float y = 0.0f;
        int metaState = 0;
        return MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                metaState
        );
    }

    public String getValueString() {
        return adapter.getValueString();
    }


    public interface Listener {

        void onSubmitValue();

        void onValue(@IdRes int id, boolean isDecimal, int intValue, double doubleValue);

        /**
         * Called when inputValue(Double) receives a non-finite value (NaN or Infinity),
         * e.g. from a calculation elsewhere that divided by zero. The field is left
         * unchanged; the app should decide how to recover (reset, show a message, etc).
         */
        default void onInvalidValue(@IdRes int id, double invalidValue) {
            // no-op by default
        }

    }

}
