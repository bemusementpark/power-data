package com.nextfaze.asyncdata;

import lombok.NonNull;

public abstract class AbstractStore<T> implements Store<T> {

    @NonNull
    private final DataObservers mDataObservers = new DataObservers();

    @Override
    public void batch(@NonNull Operation<T> operation) {
        operation.run(this);
    }

    @Override
    public void registerDataObserver(@NonNull DataObserver dataObserver) {
        mDataObservers.register(dataObserver);
    }

    @Override
    public void unregisterDataObserver(@NonNull DataObserver dataObserver) {
        mDataObservers.unregister(dataObserver);
    }

    protected void notifyDataChanged() {
        mDataObservers.notifyDataChanged();
    }

    protected void notifyItemChanged(final int position) {
        notifyItemRangeChanged(position, 1);
    }

    protected void notifyItemRangeChanged(final int positionStart, final int itemCount) {
        mDataObservers.notifyItemRangeChanged(positionStart, itemCount);
    }

    protected void notifyItemInserted(int position) {
        notifyItemRangeInserted(position, 1);
    }

    protected void notifyItemRangeInserted(final int positionStart, final int itemCount) {
        mDataObservers.notifyItemRangeInserted(positionStart, itemCount);
    }

    protected void notifyItemMoved(int fromPosition, int toPosition) {
        notifyItemRangeMoved(fromPosition, toPosition, 1);
    }

    protected void notifyItemRangeMoved(final int fromPosition, final int toPosition, final int itemCount) {
        mDataObservers.notifyItemRangeMoved(fromPosition, toPosition, itemCount);
    }

    protected void notifyItemRemoved(int position) {
        notifyItemRangeRemoved(position, 1);
    }

    protected void notifyItemRangeRemoved(final int positionStart, final int itemCount) {
        mDataObservers.notifyItemRangeRemoved(positionStart, itemCount);
    }
}
