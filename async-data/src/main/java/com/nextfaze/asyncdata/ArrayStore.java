package com.nextfaze.asyncdata;

import lombok.NonNull;

import java.util.ArrayList;

final class ArrayStore<T> extends AbstractStore<T> {

    @NonNull
    private final ArrayList<T> mData = new ArrayList<>();

    @Override
    public void add(@NonNull Iterable<? extends T> elements) {
        for (T t : elements) {
            if (t != null) {
                mData.add(t);
            }
        }
    }

    @Override
    public void clear() {
        mData.clear();
    }

    @NonNull
    @Override
    public T get(int position) {
        return mData.get(position);
    }

    @Override
    public int size() {
        return mData.size();
    }
}
