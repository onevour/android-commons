package com.onevour.core.utilities.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;

import org.junit.Test;

import java.util.Date;

/** RefSession writes dates as epoch milliseconds and still reads every format the apps stored before. */
public class RefSessionDateAdapterTest {

    /** 2026-10-10 08:30:00.000 at UTC+7. */
    private static final long AT = 1_791_595_800_000L;

    public static class Visit {
        @Expose Date at;
        @Expose String note;
        String notExposed = "skip";
    }

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(Date.class, new RefSession.DateAdapter())
            .create();

    private Date read(String json) {
        return gson.fromJson("{\"at\":" + json + "}", Visit.class).at;
    }

    @Test
    public void writes_epochMilliseconds_andKeepsStringsAsTheyAre() {
        Visit visit = new Visit();
        visit.at = new Date(AT);
        visit.note = "  Toko A  ";
        assertEquals("{\"at\":1791595800000,\"note\":\"  Toko A  \"}", gson.toJson(visit));
    }

    @Test
    public void reads_sdsEpochMilliseconds() {
        assertEquals(new Date(AT), read("1791595800000"));
        assertEquals(new Date(AT), read("\"1791595800000\""));
        assertEquals(new Date(86_400_000L), read("86400000"));                 // 1970-01-02, not taken for seconds
    }

    @Test
    public void reads_csaAndFuguhIso() {
        assertEquals(new Date(AT), read("\"2026-10-10T08:30:00.000+0700\""));  // CSA
        assertEquals(new Date(AT), read("\"2026-10-10T08:30:00.000+07\""));    // fuguh
        assertEquals(new Date(AT), read("\"2026-10-10T01:30:00.000Z\""));
        assertEquals(new Date(AT), read("\"2026-10-10T08:30:00+07:00\""));
    }

    @Test
    public void reads_gsonDefaultText() {
        String written = new Gson().toJson(new Date(AT));                         // what apps without initialize stored
        Date read = read(written);
        assertEquals(AT / 1000, read.getTime() / 1000);                          // Gson's default text has no milliseconds
    }

    @Test
    public void nullAndEmpty_giveNull() {
        assertNull(read("null"));
        assertNull(read("\"\""));
    }

    @Test(expected = JsonParseException.class)
    public void unknownText_isAnError() {
        read("\"kemarin\"");
    }
}
