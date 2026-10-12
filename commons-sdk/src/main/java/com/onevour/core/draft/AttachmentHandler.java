package com.onevour.core.draft;

import androidx.annotation.WorkerThread;

import java.util.Collection;

/**
 * The files a form attaches while its input is a draft (photos), as the app keeps them: kept aside until the form is
 * saved ({@link #commit}), thrown away with the draft ({@link #discard}). The library only passes their ids along.
 */
@WorkerThread
public interface AttachmentHandler {

    /** The form was saved: its draft attachments become final. */
    void commit(Collection<Long> ids);

    /** The draft was thrown away: its draft attachments go (final ones are never passed here). */
    void discard(Collection<Long> ids);

    /** An app whose forms attach nothing. */
    AttachmentHandler NONE = new AttachmentHandler() {
        @Override
        public void commit(Collection<Long> ids) {
        }

        @Override
        public void discard(Collection<Long> ids) {
        }
    };
}
