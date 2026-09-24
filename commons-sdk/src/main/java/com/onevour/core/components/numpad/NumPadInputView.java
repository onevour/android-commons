package com.onevour.core.components.numpad;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.onevour.core.R;
import com.onevour.core.utilities.commons.ValueOf;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NumPadInputView implements View.OnClickListener {

    private static final String TAG = NumPadInputView.class.getSimpleName();

    private AlertListener listener;

    private Dialog dialog;

    private boolean useBottomSheet = false;

    private char decimalSeparator = '.';

    private LinearLayout dialogRoot;

    private LinearLayout titleContent;

    private TextView titleLeft, titleRight, numPoint, result, numOption, numCancel;

    private ImageView del;

    private final List<TextView> numKeys = new ArrayList<>();

    private Context context;

    private NumberFormat numberFormat;

    private double min, max;

    private NumPadStyle currentStyle;

    protected void init(Context context, NumberFormat numberFormat, double min, double max, AlertListener listener) {
        this.context = context;
        this.listener = listener;
        this.numberFormat = numberFormat;
        this.min = min;
        this.max = max;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_input_number, null, false);
        dialogRoot = view.findViewById(R.id.dialog_root);
        titleContent = view.findViewById(R.id.title_content);
        titleContent.setVisibility(View.GONE);
        titleLeft = view.findViewById(R.id.title_left);
        titleRight = view.findViewById(R.id.title_right);
        result = view.findViewById(R.id.key_result);

        TextView num0 = view.findViewById(R.id.key_num_0);
        TextView num1 = view.findViewById(R.id.key_num_1);
        TextView num2 = view.findViewById(R.id.key_num_2);
        TextView num3 = view.findViewById(R.id.key_num_3);
        TextView num4 = view.findViewById(R.id.key_num_4);
        TextView num5 = view.findViewById(R.id.key_num_5);
        TextView num6 = view.findViewById(R.id.key_num_6);
        TextView num7 = view.findViewById(R.id.key_num_7);
        TextView num8 = view.findViewById(R.id.key_num_8);
        TextView num9 = view.findViewById(R.id.key_num_9);

        numPoint = view.findViewById(R.id.key_num_point);
        numOption = view.findViewById(R.id.key_option);
        numCancel = view.findViewById(R.id.key_cancel);
        del = view.findViewById(R.id.key_del);

        numKeys.clear();
        numKeys.add(num0);
        numKeys.add(num1);
        numKeys.add(num2);
        numKeys.add(num3);
        numKeys.add(num4);
        numKeys.add(num5);
        numKeys.add(num6);
        numKeys.add(num7);
        numKeys.add(num8);
        numKeys.add(num9);
        numKeys.add(numPoint);
        numKeys.add(numOption);

        for (TextView numKey : numKeys) {
            numKey.setOnClickListener(this);
            setupAccessibilityButton(numKey);
        }
        del.setOnClickListener(this);
        setupAccessibilityButton(del);
        numCancel.setOnClickListener(this);
        setupAccessibilityButton(numCancel);

        titleRight.setOnClickListener(v -> {
            if (Objects.isNull(listener)) return;
            listener.submitToMaxValue();
        });

        if (useBottomSheet) {
            BottomSheetDialog bsDialog = new BottomSheetDialog(view.getContext());
            bsDialog.setContentView(view);
            bsDialog.setCancelable(false);
            bsDialog.setCanceledOnTouchOutside(false);
            bsDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            dialog = bsDialog;
        } else {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view.getContext());
            alertBuilder.setView(view);
            AlertDialog alertDialog = alertBuilder.create();
            alertDialog.setCancelable(false);
            alertDialog.setCanceledOnTouchOutside(false);
            dialog = alertDialog;
        }

        if (currentStyle != null) {
            applyStyle(currentStyle);
        }

        if (Objects.isNull(numberFormat)) {
            numPoint.setVisibility(View.INVISIBLE);
            return;
        }
        if (!(numberFormat instanceof DecimalFormat)) {
            throw new IllegalArgumentException("numberFormat must be a DecimalFormat instance");
        }
        DecimalFormatSymbols d = ((DecimalFormat) numberFormat).getDecimalFormatSymbols();
        decimalSeparator = d.getDecimalSeparator();
        numPoint.setText(String.valueOf(decimalSeparator));
    }

    private void setupAccessibilityButton(View view) {
        if (view == null) return;
        ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        });
    }

    private Drawable createRippleDrawable(Drawable content) {
        if (content == null) return null;
        ColorStateList rippleColor = ColorStateList.valueOf(0x33888888);
        return new RippleDrawable(rippleColor, content, null);
    }

    public void applyStyle(NumPadStyle style) {
        this.currentStyle = style;
        if (style == null) return;

        if (style.getDialogBackgroundColor() != null && dialogRoot != null) {
            dialogRoot.setBackgroundColor(style.getDialogBackgroundColor());
        }

        if (style.getTypeface() != null) {
            if (titleLeft != null) titleLeft.setTypeface(style.getTypeface());
            if (titleRight != null) titleRight.setTypeface(style.getTypeface());
            if (result != null) result.setTypeface(style.getTypeface());
            if (numCancel != null) numCancel.setTypeface(style.getTypeface());
            for (TextView key : numKeys) {
                if (key != null) key.setTypeface(style.getTypeface());
            }
        }

        if (style.getTitleTextColor() != null) {
            if (titleLeft != null) titleLeft.setTextColor(style.getTitleTextColor());
            if (titleRight != null) titleRight.setTextColor(style.getTitleTextColor());
        }

        if (style.getTitleTextSizePx() != null) {
            if (titleLeft != null) titleLeft.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getTitleTextSizePx());
            if (titleRight != null) titleRight.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getTitleTextSizePx());
        }

        if (style.getResultTextColor() != null && result != null) {
            result.setTextColor(style.getResultTextColor());
        }

        if (style.getResultTextSizePx() != null && result != null) {
            result.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getResultTextSizePx());
        }

        if (style.getKeyTextColor() != null) {
            for (TextView key : numKeys) {
                if (key != null) key.setTextColor(style.getKeyTextColor());
            }
            if (del != null) del.setColorFilter(style.getKeyTextColor());
        }

        if (style.getKeyTextSizePx() != null) {
            for (TextView key : numKeys) {
                if (key != null) key.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.getKeyTextSizePx());
            }
        }

        if (style.getKeyBackgroundDrawable() != null) {
            Drawable keyBg = style.getKeyBackgroundDrawable();
            for (TextView key : numKeys) {
                if (key != null) {
                    Drawable.ConstantState cs = keyBg.getConstantState();
                    Drawable content = cs != null ? cs.newDrawable().mutate() : keyBg;
                    key.setBackground(createRippleDrawable(content));
                }
            }
        } else if (style.getKeyBackgroundColor() != null) {
            for (TextView key : numKeys) {
                if (key != null) {
                    ColorDrawable content = new ColorDrawable(style.getKeyBackgroundColor());
                    key.setBackground(createRippleDrawable(content));
                }
            }
        }
    }

    public void setUseBottomSheet(boolean useBottomSheet) {
        this.useBottomSheet = useBottomSheet;
    }

    public boolean isUseBottomSheet() {
        return useBottomSheet;
    }

    /**
     * show dialog
     */
    public void show(String value, boolean afterPoint) {
        if (null == dialog) return;
        dialog.show();
        if (dialog instanceof BottomSheetDialog) {
            ((BottomSheetDialog) dialog).getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        result.setText(value);
        updateBackground(afterPoint);
    }

    @Override
    public void onClick(View v) {
        if (null == listener) return;
        try {
            int i = v.getId();
            if (i == R.id.key_num_0) {
                listener.inputValue('0');
            } else if (i == R.id.key_num_1) {
                listener.inputValue('1');
            } else if (i == R.id.key_num_2) {
                listener.inputValue('2');
            } else if (i == R.id.key_num_3) {
                listener.inputValue('3');
            } else if (i == R.id.key_num_4) {
                listener.inputValue('4');
            } else if (i == R.id.key_num_5) {
                listener.inputValue('5');
            } else if (i == R.id.key_num_6) {
                listener.inputValue('6');
            } else if (i == R.id.key_num_7) {
                listener.inputValue('7');
            } else if (i == R.id.key_num_8) {
                listener.inputValue('8');
            } else if (i == R.id.key_num_9) {
                listener.inputValue('9');
            } else if (i == R.id.key_num_point) {
                listener.inputValue(decimalSeparator);
            } else if (i == R.id.key_del) {
                listener.delete();
            } else if (i == R.id.key_cancel) {
                dialog.dismiss();
            } else if (i == R.id.key_option) {
                listener.submit();
                dialog.dismiss();
            }
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void setResult(String value, boolean isAfterPoint) {
        updateBackground(isAfterPoint);
        result.setText(value);
    }

    public void updateBackground(boolean isAfterPoint) {
        if (numPoint == null) return;
        if (isAfterPoint) {
            numPoint.setTextColor(ContextCompat.getColor(context, R.color.numpad_red));
            numPoint.setBackgroundResource(R.drawable.numpad_red_ripple);
        } else {
            if (currentStyle != null && currentStyle.getKeyTextColor() != null) {
                numPoint.setTextColor(currentStyle.getKeyTextColor());
            } else {
                numPoint.setTextColor(ContextCompat.getColor(context, R.color.numpad_black));
            }
            if (currentStyle != null && currentStyle.getKeyBackgroundDrawable() != null) {
                Drawable.ConstantState cs = currentStyle.getKeyBackgroundDrawable().getConstantState();
                Drawable content = cs != null ? cs.newDrawable().mutate() : currentStyle.getKeyBackgroundDrawable();
                numPoint.setBackground(createRippleDrawable(content));
            } else if (currentStyle != null && currentStyle.getKeyBackgroundColor() != null) {
                ColorDrawable content = new ColorDrawable(currentStyle.getKeyBackgroundColor());
                numPoint.setBackground(createRippleDrawable(content));
            } else {
                numPoint.setBackgroundResource(R.drawable.numpad_ripple);
            }
        }
    }

    public void error(String message) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setTitle("Opps, something wrong!");
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        if (numberFormat instanceof DecimalFormat) {
            DecimalFormatSymbols d = ((DecimalFormat) numberFormat).getDecimalFormatSymbols();
            sb.append("\ndecimal : ").append(d.getDecimalSeparator());
            sb.append("\ngroup : ").append(d.getGroupingSeparator());
            sb.append("\ndecimal monetary : ").append(d.getMonetaryDecimalSeparator());
            sb.append("\ndecimal currency : ").append(d.getCurrencySymbol());
            sb.append("\nexponent : ").append(d.getExponentSeparator());
        }
        alertBuilder.setMessage(sb.toString());
        dialog.dismiss();
        alertBuilder.create().show();
    }

    public void setTitle(String title) {
        titleContent.setVisibility(View.VISIBLE);
        titleLeft.setVisibility(View.VISIBLE);
        titleLeft.setText(title);
    }

    public void setTitleRight(String title) {
        titleContent.setVisibility(View.VISIBLE);
        titleRight.setVisibility(View.VISIBLE);
        titleRight.setText(title);
    }

    public void showMaxValue() {
        titleContent.setVisibility(View.VISIBLE);
        titleLeft.setVisibility(View.VISIBLE);
        titleRight.setVisibility(View.VISIBLE);
        if (Objects.isNull(numberFormat)) {
            setTitleRight(String.valueOf(Double.valueOf(max).intValue()));
        } else setTitleRight(numberFormat.format(max));
    }

    public void updateMinMax(double min, double max) {
        titleContent.setVisibility(View.VISIBLE);
        titleLeft.setVisibility(View.VISIBLE);
        titleRight.setVisibility(View.VISIBLE);
        this.min = min;
        this.max = max;
        if (ValueOf.isNull(numberFormat)) {
            titleRight.setText(String.valueOf((int) max));
        } else {
            titleRight.setText(numberFormat.format(max));
        }
    }

    public interface AlertListener {

        void inputValue(char value) throws ParseException;

        void showMaxValue();

        void submitToMaxValue();

        void delete() throws ParseException;

        void submit();

    }

}
