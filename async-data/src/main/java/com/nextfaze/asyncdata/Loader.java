package com.nextfaze.asyncdata;

import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.List;

import static java.lang.Math.max;
import static java.util.Collections.emptyList;

public abstract class Loader<T> {

    /**
     * Called from a worker thread to load the next increment of items.
     * @return A result containing the next set of elements to be appended, or {@code null} if there are no more items.
     * The result also indicates if these are the final elements of the data set.
     * @throws Throwable If any error occurs while trying to load.
     */
    @WorkerThread
    @NonNull
    public abstract Result<? extends T> load() throws Throwable;

    /** Called prior to elements being cleared. Always called from the UI thread. */
    @UiThread
    public void onClear() {
    }

    /** Called when loading is about to begin from the start. Always called from the UI thread. */
    @UiThread
    public void onLoadBegin() {
    }

    @Getter
    @Accessors(prefix = "m")
    public static final class Result<T> {

        @SuppressWarnings("unchecked")
        private static final Result NONE_REMAINING = new Result(emptyList(), 0);

        @NonNull
        private final List<? extends T> mElements;

        /** Indicates how many more elements available to be loaded after this. */
        private final int mRemaining;

        public Result(@NonNull List<? extends T> elements, int remaining) {
            mElements = elements;
            mRemaining = max(0, remaining);
        }

        @NonNull
        public static <T> Result<T> moreRemaining(@NonNull List<? extends T> list) {
            return new Result<>(list, Integer.MAX_VALUE);
        }

        @SuppressWarnings("unchecked")
        @NonNull
        public static <T> Result<T> noneRemaining() {
            return NONE_REMAINING;
        }
    }
}
