package com.onevour.sdk.impl.modules.location;

import android.widget.TextView;

import com.onevour.core.location.LocationCapture;
import com.onevour.sdk.impl.R;

import java.util.Locale;
import java.util.Objects;

/** The chips of the location sample screens: green good, amber fair, red bad, grey neutral (text contrast AA). */
final class SampleChips {

    private SampleChips() {
    }

    static void good(TextView view, String text) {
        set(view, R.drawable.history_chip_good, 0xFF1E6B34, text);
    }

    static void warn(TextView view, String text) {
        set(view, R.drawable.history_chip_warn, 0xFF8A4B00, text);
    }

    static void bad(TextView view, String text) {
        set(view, R.drawable.history_chip_bad, 0xFFA11B1B, text);
    }

    static void neutral(TextView view, String text) {
        set(view, R.drawable.history_chip_neutral, 0xFF37474F, text);
    }

    /** Green up to 20 m, amber up to the library's limit, red beyond (or unknown). */
    static void accuracy(TextView view, Float accuracy) {
        if (Objects.isNull(accuracy)) {
            bad(view, "akurasi ?");
        } else if (accuracy <= 20f) {
            good(view, "◎ akurasi " + metres(accuracy));
        } else if (accuracy <= LocationCapture.MAX_ACCURACY_METRES) {
            warn(view, "◎ akurasi " + metres(accuracy));
        } else {
            bad(view, "◎ akurasi " + metres(accuracy));
        }
    }

    static String metres(float value) {
        if (value >= 1000f) return String.format(Locale.getDefault(), "%.1f km", value / 1000f);
        return String.format(Locale.getDefault(), "%.0f m", value);
    }

    private static void set(TextView view, int background, int textColor, String text) {
        view.setBackgroundResource(background);
        view.setTextColor(textColor);
        view.setText(text);
    }
}
