package com.onevour.core.draft;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** The library's own store: a small database ({@value #NAME}) apart from the app's, so an app needs no migration. */
public final class SqliteDraftStore extends SQLiteOpenHelper implements DraftStore {

    public static final String NAME = "commons_drafts.db";

    private static final int VERSION = 1;

    private static final String TABLE = "draft";

    public SqliteDraftStore(Context context) {
        super(context.getApplicationContext(), NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (form TEXT NOT NULL, draft_key TEXT NOT NULL, owner_id TEXT, user_id TEXT, "
                + "payload TEXT, updated_at INTEGER, PRIMARY KEY (form, draft_key))");
        db.execSQL("CREATE INDEX idx_draft_owner ON " + TABLE + " (owner_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // version 1 only
    }

    @Override
    public void save(DraftRecord draft) {
        ContentValues values = new ContentValues();
        values.put("form", draft.form);
        values.put("draft_key", draft.key);
        values.put("owner_id", draft.ownerId);
        values.put("user_id", draft.userId);
        values.put("payload", draft.payload);
        values.put("updated_at", draft.updatedAt == null ? null : draft.updatedAt.getTime());
        getWritableDatabase().insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Nullable
    @Override
    public DraftRecord find(String form, String key) {
        List<DraftRecord> found = query("form = ? AND draft_key = ?", new String[]{form, key});
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public void delete(String form, String key) {
        getWritableDatabase().delete(TABLE, "form = ? AND draft_key = ?", new String[]{form, key});
    }

    @Override
    public List<DraftRecord> findByOwner(String ownerId) {
        return query("owner_id = ?", new String[]{ownerId});
    }

    @Override
    public void deleteByOwner(String ownerId) {
        getWritableDatabase().delete(TABLE, "owner_id = ?", new String[]{ownerId});
    }

    @Override
    public List<DraftRecord> findAll() {
        return query(null, null);
    }

    @Override
    public long count() {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE, null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
    }

    @Override
    public int deleteOlderThan(Date before) {
        return getWritableDatabase().delete(TABLE, "updated_at < ?", new String[]{String.valueOf(before.getTime())});
    }

    @Override
    public void deleteAll() {
        getWritableDatabase().delete(TABLE, null, null);
    }

    private List<DraftRecord> query(@Nullable String where, @Nullable String[] args) {
        List<DraftRecord> drafts = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE, null, where, args, null, null, "updated_at DESC")) {
            while (c.moveToNext()) {
                int updated = c.getColumnIndexOrThrow("updated_at");
                drafts.add(new DraftRecord(
                        c.getString(c.getColumnIndexOrThrow("form")),
                        c.getString(c.getColumnIndexOrThrow("draft_key")),
                        c.getString(c.getColumnIndexOrThrow("owner_id")),
                        c.getString(c.getColumnIndexOrThrow("user_id")),
                        c.getString(c.getColumnIndexOrThrow("payload")),
                        c.isNull(updated) ? null : new Date(c.getLong(updated))));
            }
        }
        return drafts;
    }
}
