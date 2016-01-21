package com.nextfaze.asyncdata;

import android.support.annotation.Nullable;
import lombok.NonNull;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public final class DataBuilder<T> {

    private static final NamedThreadFactory DEFAULT_THREAD_FACTORY = new NamedThreadFactory("Data Thread %d");

    @Nullable
    private Store<T> mStore;

    @Nullable
    private ExecutorService mExecutorService;

    /** The number of rows to look ahead before loading. */
    private int mLookAhead = 5;

    @NonNull
    public DataBuilder<T> store(@Nullable Store<T> store) {
        mStore = store;
        return this;
    }

    @NonNull
    public DataBuilder<T> executorService(@Nullable ExecutorService executorService) {
        mExecutorService = executorService;
        return this;
    }

    /** Set the number of rows to "look ahead" before loading automatically. */
    @NonNull
    public DataBuilder<T> lookAhead(int lookAhead) {
        mLookAhead = lookAhead;
        return this;
    }

    @NonNull
    public Data<T> build(@NonNull Loader<T> loader) {
        ExecutorService executorService = mExecutorService;
        if (executorService == null) {
            executorService = newSingleThreadExecutor(DEFAULT_THREAD_FACTORY);
        }
        Store<T> store = mStore;
        if (store == null) {
            throw new IllegalArgumentException("Store is missing, but required");
        }
        return new DataImpl<>(executorService, store, loader, mLookAhead);
    }
}
