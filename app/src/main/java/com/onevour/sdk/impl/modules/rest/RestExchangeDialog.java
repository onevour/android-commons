package com.onevour.sdk.impl.modules.rest;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.onevour.core.rest.RestExchange;
import com.onevour.core.rest.RestInspector;
import com.onevour.sdk.impl.R;
import com.onevour.sdk.impl.databinding.DialogRestExchangeBinding;
import com.onevour.sdk.impl.databinding.ItemRestExchangeBinding;
import com.onevour.sdk.impl.modules.location.SampleChips;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * What one button of the REST sample sent and got, from RestInspector: a card per request (method,
 * url, query params, headers, body on top; status, time, headers, body or exception below once it
 * is back), then the @OnSuccess / @OnError / listener lines that ran.
 */
final class RestExchangeDialog extends AppCompatDialog implements RestInspector.Observer {

    /** A long body (a Pokémon is ~300 KB of JSON) is cut here: a TextView does not need it all. */
    private static final int MAX_BODY = 8_000;

    /** Lines shown before "Tampilkan semua". */
    private static final int BODY_LINES = 14;

    private static final int HEADER_LINES = 4;

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();

    private final DialogRestExchangeBinding binding;

    private final Map<Long, ItemRestExchangeBinding> cards = new HashMap<>();

    private final StringBuilder callbacks = new StringBuilder();

    RestExchangeDialog(Context context, String title, String call) {
        super(context, R.style.RestExchangeDialog);
        binding = DialogRestExchangeBinding.inflate(LayoutInflater.from(getContext()));
        setContentView(binding.getRoot());
        setTitle(title);
        binding.title.setText(title);
        binding.call.setText(call);
        binding.close.setOnClickListener(v -> dismiss());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (Objects.nonNull(window)) window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    /** A line of a callback that ran (@OnSuccess, @OnError, a listener). */
    void callback(String line) {
        if (callbacks.length() > 0) callbacks.append('\n');
        callbacks.append(line);
        binding.callbacks.setText(callbacks);
    }

    @Override
    public void onRequest(RestExchange exchange) {
        binding.waiting.setVisibility(View.GONE);
        ItemRestExchangeBinding card = ItemRestExchangeBinding.inflate(getLayoutInflater(), binding.exchanges, false);
        binding.exchanges.addView(card.getRoot());
        cards.put(exchange.getId(), card);

        method(card.method, exchange.getMethod());
        card.source.setText(Objects.isNull(exchange.getSource()) ? "" : exchange.getSource());
        card.url.setText(exchange.getUrl());
        String params = params(exchange.getUrl());
        show(card.paramsLabel, card.params, params);
        card.requestHeaders.setText(headers(exchange.getRequestHeaders()));
        show(card.requestBodyLabel, card.requestBody, body(exchange.getRequestBody()));
        SampleChips.neutral(card.status, "menunggu jawaban...");
    }

    @Override
    public void onResponse(RestExchange exchange) {
        ItemRestExchangeBinding card = cards.get(exchange.getId());
        if (Objects.isNull(card)) return;                      // sent before this dialog opened
        card.progress.setVisibility(View.GONE);
        int code = exchange.getCode();
        if (code == 0) {
            SampleChips.bad(card.status, "tidak ada jawaban");
        } else if (code < 300 && Objects.isNull(exchange.getError())) {
            SampleChips.good(card.status, "HTTP " + code + " " + reason(code));
        } else if (code < 500) {
            SampleChips.warn(card.status, "HTTP " + code + " " + reason(code));
        } else {
            SampleChips.bad(card.status, "HTTP " + code + " " + reason(code));
        }
        card.duration.setVisibility(View.VISIBLE);
        SampleChips.neutral(card.duration, "⏱ " + exchange.getDurationMs() + " ms");

        Throwable error = exchange.getError();
        show(card.errorLabel, card.error, Objects.isNull(error) ? null
                : error.getClass().getName() + (Objects.isNull(error.getMessage()) ? "" : "\n" + error.getMessage()));
        String headers = headers(exchange.getResponseHeaders());
        show(card.responseHeadersLabel, card.responseHeaders, headers.isEmpty() ? null : headers);
        collapse(card.responseHeaders, card.responseHeadersMore, HEADER_LINES);
        String body = body(exchange.getResponseBody());
        show(card.responseBodyLabel, card.responseBody, Objects.isNull(body) && Objects.isNull(error) ? "(kosong)" : body);
        collapse(card.responseBody, card.responseBodyMore, BODY_LINES);
    }

    private static void method(TextView chip, String method) {
        switch (method) {
            case "GET":
                SampleChips.good(chip, method);
                break;
            case "DELETE":
                SampleChips.bad(chip, method);
                break;
            default:
                SampleChips.warn(chip, method);
                break;
        }
    }

    /** Only the first lines, with a button to show them all (and fold them again). */
    private static void collapse(TextView value, Button toggle, int lines) {
        if (value.getVisibility() != View.VISIBLE || lineCount(value.getText()) <= lines) return;
        value.setMaxLines(lines);
        toggle.setVisibility(View.VISIBLE);
        toggle.setOnClickListener(v -> {
            boolean folded = value.getMaxLines() == lines;
            value.setMaxLines(folded ? Integer.MAX_VALUE : lines);
            toggle.setText(folded ? "Lipat" : "Tampilkan semua");
        });
    }

    private static int lineCount(CharSequence text) {
        int count = 1;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == '\n') count++;
        return count;
    }

    private static void show(View label, TextView value, String text) {
        int visibility = Objects.isNull(text) ? View.GONE : View.VISIBLE;
        label.setVisibility(visibility);
        value.setVisibility(visibility);
        value.setText(text);
    }

    /** name = value per line, decoded; null when the url has none. */
    private static String params(String url) {
        Uri uri = Uri.parse(url);
        if (uri.getQueryParameterNames().isEmpty()) return null;
        StringBuilder text = new StringBuilder();
        for (String name : uri.getQueryParameterNames()) {
            for (String value : uri.getQueryParameters(name)) {
                if (text.length() > 0) text.append('\n');
                text.append(name).append(" = ").append(value);
            }
        }
        return text.toString();
    }

    private static String headers(Map<String, List<String>> headers) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                if (text.length() > 0) text.append('\n');
                text.append(entry.getKey()).append(": ").append(value);
            }
        }
        return text.toString();
    }

    /** JSON indented, anything else as it is; cut after MAX_BODY characters; null when empty. */
    private static String body(String body) {
        if (Objects.isNull(body) || body.trim().isEmpty()) return null;
        String text = body;
        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                text = PRETTY.toJson(JsonParser.parseString(trimmed));
            } catch (JsonSyntaxException e) {
                text = body;                                    // not JSON after all
            }
        }
        if (text.length() <= MAX_BODY) return text;
        return text.substring(0, MAX_BODY) + String.format(Locale.getDefault(), "\n… (%,d karakter lagi)", text.length() - MAX_BODY);
    }

    private static String reason(int code) {
        switch (code) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 422: return "Unprocessable Entity";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            default: return "";
        }
    }
}
