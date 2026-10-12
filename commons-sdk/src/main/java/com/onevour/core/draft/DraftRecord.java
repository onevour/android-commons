package com.onevour.core.draft;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

/** One stored draft: which form, which instance of it (key), whose (owner, user), the input as JSON, when. */
public final class DraftRecord {

    @NonNull
    public final String form;

    @NonNull
    public final String key;

    /** What the draft belongs to (e.g. the visit's customer), to list an owner's drafts; null when nothing. */
    @Nullable
    public final String ownerId;

    /** Who wrote it (e.g. the salesman), null when the app does not say. */
    @Nullable
    public final String userId;

    @Nullable
    public final String payload;

    @Nullable
    public final Date updatedAt;

    public DraftRecord(@NonNull String form, @NonNull String key, @Nullable String ownerId, @Nullable String userId,
                       @Nullable String payload, @Nullable Date updatedAt) {
        this.form = form;
        this.key = key;
        this.ownerId = ownerId;
        this.userId = userId;
        this.payload = payload;
        this.updatedAt = updatedAt;
    }
}
