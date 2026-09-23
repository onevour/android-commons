package com.onevour.core;

import android.content.Context;

import com.onevour.core.rest.builder.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RestBaseTest {

    protected final RestClient client = new RestClient();

    protected String readAsset(Context context, String fileName) throws IOException {

        InputStream inputStream = context.getAssets().open(fileName);

        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }

        return builder.toString();
    }

}
