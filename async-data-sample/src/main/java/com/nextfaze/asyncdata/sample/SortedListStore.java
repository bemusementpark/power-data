package com.nextfaze.asyncdata.sample;

import android.support.v7.util.SortedList;
import com.nextfaze.asyncdata.AbstractStore;
import lombok.NonNull;

public final class SortedListStore<T> extends AbstractStore<T> {

    @NonNull
    private final SortedList<T> mSortedList;

    public SortedListStore(@NonNull Class<T> klass, @NonNull final Callback<T> callback) {
        mSortedList = new SortedList<>(klass, new SortedList.Callback<T>() {
            @Override
            public int compare(T o1, T o2) {
                return callback.compare(o1, o2);
            }

            @Override
            public boolean areContentsTheSame(T oldItem, T newItem) {
                return callback.areContentsTheSame(oldItem, newItem);
            }

            @Override
            public boolean areItemsTheSame(T item1, T item2) {
                return callback.areItemsTheSame(item1, item2);
            }

            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }
        });
    }

    @Override
    public void add(@NonNull Iterable<? extends T> elements) {
        for (T t : elements) {
            if (t != null) {
                mSortedList.add(t);
            }
        }
    }

    @NonNull
    @Override
    public T get(int position) {
        return mSortedList.get(position);
    }

    @Override
    public int size() {
        return mSortedList.size();
    }

    @Override
    public void batch(@NonNull Operation<T> operation) {
        mSortedList.beginBatchedUpdates();
        try {
            operation.run(this);
        } finally {
            mSortedList.endBatchedUpdates();
        }
    }

    @Override
    public void clear() {
        mSortedList.clear();
    }

    public interface Callback<T> {
        /** @see SortedList.Callback#compare(Object, Object) */
        int compare(@NonNull T o1, @NonNull T o2);

        /** @see SortedList.Callback#areContentsTheSame(Object, Object) */
        boolean areContentsTheSame(@NonNull T oldItem, @NonNull T newItem);

        /** @see SortedList.Callback#areItemsTheSame(Object, Object) */
        boolean areItemsTheSame(@NonNull T item1, @NonNull T item2);
    }
}
