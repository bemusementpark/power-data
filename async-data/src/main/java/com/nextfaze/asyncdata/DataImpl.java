package com.nextfaze.asyncdata;

import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.currentThread;

/**
 * Mutable {@link Data} implementation backed by an {@link ArrayList}, which is loaded incrementally until the source
 * has no more data. Cannot contain {@code null} elements. Not thread-safe.
 * @param <T> The type of element this data contains.
 */
@Accessors(prefix = "m")
final class DataImpl<T> extends AbstractData<T> {

    @NonNull
    private final DataObserver mStoreDataObserver = new DataObserver() {
        @Override
        public void onChange() {
            notifyDataChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            notifyItemRangeMoved(fromPosition, toPosition, itemCount);
        }
    };

    @NonNull
    private final ExecutorService mExecutorService;

    @NonNull
    private final Store<T> mStore;

    @NonNull
    private final Loader<T> mLoader;

    @NonNull
    private final Lock mLock = new ReentrantLock();

    @NonNull
    private final Condition mLoad = mLock.newCondition();

    private final int mLookAhead;

    @Nullable
    private Future<?> mFuture;

    /** Indicates the last attempt to load a page failed. */
    private volatile boolean mError;

    private boolean mLoading;
    private int mAvailable = Integer.MAX_VALUE;
    private boolean mDirty = true;
    private boolean mClear;

    DataImpl(@NonNull ExecutorService executorService,
             @NonNull Store<T> store,
             @NonNull Loader<T> loader,
             int lookAhead) {
        mExecutorService = executorService;
        mLoader = loader;
        mLookAhead = lookAhead;
        mStore = store;
        mStore.registerDataObserver(mStoreDataObserver);
    }

    @Override
    public final void close() {
        mStore.unregisterDataObserver(mStoreDataObserver);
        stopTask();
        super.close();
    }

    @Override
    public final int size() {
        return mStore.size();
    }

    @Override
    public final boolean isEmpty() {
        return mStore.size() <= 0;
    }

    @UiThread
    @NonNull
    @Override
    public final T get(int position, int flags) {
        // Requested end of data? Time to load more.
        // The presence of the presentation flag indicates this is a good time to continue loading elements.
        if ((flags & FLAG_PRESENTATION) != 0 && position >= size() - 1 - mLookAhead) {
            proceed();
        }
        return mStore.get(position);
    }

    @Override
    public final void invalidate() {
        stopTask();
        mDirty = true;
        mClear = true;
    }

    @Override
    public final void refresh() {
        stopTask();
        mDirty = true;
        setAvailable(Integer.MAX_VALUE);
        startTaskIfNeeded();
    }

    @Override
    public final void reload() {
        clear();
        refresh();
    }

    @UiThread
    @Override
    public final void next() {
        proceed();
    }

    @Override
    public final boolean isLoading() {
        return mLoading;
    }

    @Override
    public final int available() {
        return mAvailable;
    }

    @Override
    protected final void onFirstDataObserverRegistered() {
        super.onFirstDataObserverRegistered();
        if (mError) {
            // Last attempt to load an increment failed, so try again now we've become visible again.
            proceed();
        }
        if (mClear) {
            clear();
        }
        startTaskIfNeeded();
    }

    private void clear() {
        mClear = false;
        int size = mStore.size();
        if (size > 0) {
            mLoader.onClear();
            mStore.clear();
        }
    }

    private void startTaskIfNeeded() {
        if (mDirty && mFuture == null && getDataObserverCount() > 0) {
            mDirty = false;
            mLoader.onLoadBegin();
            setLoading(true);
            mFuture = mExecutorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    runLoadLoop();
                    return null;
                }
            });
        }
    }

    private void stopTask() {
        if (mFuture != null) {
            mFuture.cancel(true);
            mFuture = null;
        }
    }

    private void runLoadLoop() {
        try {
            loadLoop();
        } catch (InterruptedException e) {
            // Normal thread termination.
        }
    }

    /**
     * Loads each increment until full range has been loading, halting in between increment until instructed to
     * proceed.
     */
    private void loadLoop() throws InterruptedException {
        boolean firstItem = true;
        boolean moreAvailable = true;

        // Loop until all loaded.
        while (moreAvailable) {
            // Thread interruptions terminate the loop.
            if (currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            try {
                setLoading(true);

                // Load next increment of items.
                final Loader.Result<? extends T> result = mLoader.load();
                moreAvailable = result.getRemaining() > 0;
                setAvailable(result.getRemaining());

                if (!result.getElements().isEmpty()) {
                    // If invalidated while shown, we lazily clear the data so the user doesn't see blank data while loading.
                    final boolean needToClear = firstItem;
                    firstItem = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (needToClear) {
                                overwrite(result);
                            } else {
                                append(result);
                            }
                        }
                    });
                }
            } catch (InterruptedException | InterruptedIOException e) {
                throw new InterruptedException();
            } catch (Throwable e) {
                notifyError(e);
                mError = true;
            } finally {
                setLoading(false);
            }

            // Block until instructed to continue, even if an error occurred.
            // In this case, loading must be explicitly resumed.
            block();
        }
    }

    private void overwrite(@NonNull final Loader.Result<? extends T> result) {
        mStore.batch(new Store.Operation<T>() {
            @Override
            public void run(@NonNull Store<T> store) {
//                clear();
                store.add(result.getElements());
            }
        });
    }

    private void append(@NonNull Loader.Result<? extends T> result) {
        mStore.add(result.getElements());
    }

    private void setLoading(final boolean loading) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLoading != loading) {
                    mLoading = loading;
                    notifyLoadingChanged();
                }
            }
        });
    }

    private void setAvailable(final int available) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mAvailable != available) {
                    mAvailable = available;
                    notifyAvailableChanged();
                }
            }
        });
    }

    private void block() throws InterruptedException {
        mLock.lock();
        try {
            mLoad.await();
        } finally {
            mLock.unlock();
        }
    }

    private void proceed() {
        mError = false;
        mLock.lock();
        try {
            mLoad.signal();
        } finally {
            mLock.unlock();
        }
    }
}
