package com.onevour.core.utilities.jwt;

import android.util.Base64;
import android.util.Log;

import com.onevour.core.utilities.commons.ValueOf;
import com.onevour.core.utilities.json.gson.GsonHelper;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.io.UnsupportedEncodingException;
import java.util.Date;

public class JWTCommons {

    private static String TAG = JWTCommons.class.getSimpleName();

    public static void decoded(String JWTEncoded) throws Exception {
        try {
            String[] split = JWTEncoded.split("\\.");
            String body = getJson(split[1]);
            Log.d(TAG, "Header: " + getJson(split[0]) + " Body: " + body);
            JWTBody jwtBody = GsonHelper.newInstance().getGson().fromJson(body, JWTBody.class);
            Date date = new Date(jwtBody.getExp() * 1000L);
            Seconds seconds = Seconds.secondsBetween(new DateTime(), new DateTime(date));
            Log.d(TAG, "exp: " + jwtBody.getExp() + " | " + seconds.getSeconds());
        } catch (UnsupportedEncodingException e) {
            //HttpErrorResponse
        }
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }

    public static boolean isExpired(String token) {
        if (ValueOf.isEmpty(token)) return true;
        try {
            String[] split = token.split("\\.");
            if (split.length <= 2) return true;
            String body = getJson(split[1]);
            Log.d(TAG, "Header: " + getJson(split[0]));
            Log.d(TAG, "Body: " + body);
            JWTBody jwtBody = GsonHelper.newInstance().getGson().fromJson(body, JWTBody.class);
            Date date = new Date(jwtBody.getExp() * 1000L);
            Seconds seconds = Seconds.secondsBetween(new DateTime(), new DateTime(date));
            Log.d(TAG, "exp: " + jwtBody.getExp() + " | " + seconds.getSeconds());
            return seconds.getSeconds() < 0;
        } catch (UnsupportedEncodingException e) {
            //HttpErrorResponse
            return true;
        }
    }
}
