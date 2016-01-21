package com.nextfaze.asyncdata.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.google.common.primitives.Ints;
import com.nextfaze.asyncdata.Data;
import com.nextfaze.asyncdata.DataBuilder;
import com.nextfaze.asyncdata.Loader;
import com.nextfaze.asyncdata.widget.DataLayout;
import com.nextfaze.poweradapters.Holder;
import com.nextfaze.poweradapters.asyncdata.DataBindingAdapter;
import com.nextfaze.poweradapters.binding.TypedBinder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;
import static com.google.common.base.Objects.equal;
import static com.nextfaze.poweradapters.binding.Mappers.singletonMapper;
import static com.nextfaze.poweradapters.recyclerview.RecyclerPowerAdapters.toRecyclerAdapter;

public class SortedFragment extends Fragment {

    @NonNull
    private final SortedListStore<Item> mCallback = new SortedListStore<>(Item.class, new SortedListStore.Callback<Item>() {
        @Override
        public int compare(@NonNull Item o1, @NonNull Item o2) {
            return Ints.compare(o1.getPosition(), o2.getPosition());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return equal(oldItem.getTitle(), newItem.getTitle());
        }

        @Override
        public boolean areItemsTheSame(@NonNull Item item1, @NonNull Item item2) {
            return item1.getId() == item2.getId();
        }
    });

    @NonNull
    private final Data<?> mData = new DataBuilder<Item>()
            .store(mCallback)
            .build(new Loader<Item>() {
                @NonNull
                @Override
                public Result<? extends Item> load() throws Throwable {
                    return new Result<>(loadItems(), 0);
                }
            });

    @NonNull
    private final TypedBinder<Item, TextView> mItemBinder = new TypedBinder<Item, TextView>(android.R.layout.simple_list_item_1) {
        @Override
        protected void bind(@NonNull Item item, @NonNull TextView v, @NonNull Holder holder) {
            v.setText(item.getTitle());
        }
    };

    @NonNull
    private final DataBindingAdapter mAdapter = new DataBindingAdapter(mData, singletonMapper(mItemBinder));

    @Bind(R.id.sorted_fragment_data_layout)
    DataLayout mDataLayout;

    @Bind(R.id.sorted_fragment_recycler)
    RecyclerView mRecyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sorted_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        mDataLayout.setData(mData);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), VERTICAL, false));
        mRecyclerView.setAdapter(toRecyclerAdapter(mAdapter));
    }

    @Override
    public void onDestroyView() {
        mRecyclerView.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add("Refresh").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mData.refresh();
                return true;
            }
        });
    }

    @Getter
    @Accessors(prefix = "m")
    @EqualsAndHashCode
    static final class Item {

        private final int mId;

        private final int mPosition;

        @Nullable
        private final String mTitle;

        Item(int id, int position, @Nullable String title) {
            mId = id;
            mPosition = position;
            mTitle = title;
        }
    }

    @NonNull
    private static List<Item> loadItems() throws IOException {
        Random random = new Random();
        ArrayList<Item> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int id = i;
            String title = "Item #" + id + ": " + randomString(random);
            items.add(new Item(id, random.nextInt(9), title));
        }
        return items;
    }

    @NonNull
    private static String randomString(@NonNull Random random) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            b.append((char) ('a' + random.nextInt('z' - 'a')));
        }
        return b.toString();
    }
}
