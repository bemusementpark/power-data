package com.nextfaze.asyncdata;

import android.support.annotation.UiThread;
import lombok.NonNull;

public interface Store<T> {

    /** Add the specified non-null elements. */
    void add(@NonNull Iterable<? extends T> elements);

    @NonNull
    T get(int position);

    int size();

    void clear();

    void batch(@NonNull Operation<T> operation);

    @UiThread
    void registerDataObserver(@NonNull DataObserver dataObserver);

    @UiThread
    void unregisterDataObserver(@NonNull DataObserver dataObserver);

    interface Operation<T> {
        void run(@NonNull Store<T> store);
    }
}
