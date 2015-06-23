/*
 * Copyright (c) 2015. Catch Inc,
 */

package catchla.yep.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.bluelinelabs.logansquare.LoganSquare;

import java.io.IOException;
import java.util.List;

import catchla.yep.R;
import catchla.yep.activity.UserActivity;
import catchla.yep.adapter.FriendsListAdapter;
import catchla.yep.adapter.decorator.DividerItemDecoration;
import catchla.yep.adapter.iface.ItemClickListener;
import catchla.yep.loader.FriendshipsLoader;
import catchla.yep.model.Friendship;
import catchla.yep.model.TaskResponse;
import catchla.yep.model.User;
import catchla.yep.util.Utils;

/**
 * Created by mariotaku on 15/4/29.
 */
public class FriendsListFragment extends AbsContentRecyclerViewFragment<FriendsListAdapter>
        implements LoaderManager.LoaderCallbacks<TaskResponse<List<Friendship>>> {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        final Context viewContext = getActivity();

        final RecyclerView recyclerView = getRecyclerView();
        final LinearLayoutManager layoutManager = getLayoutManager();
        final DividerItemDecoration itemDecoration = new DividerItemDecoration(viewContext, layoutManager.getOrientation());
        final Resources res = viewContext.getResources();
        final int decorPaddingLeft = res.getDimensionPixelSize(R.dimen.element_spacing_normal) * 2
                + res.getDimensionPixelSize(R.dimen.icon_size_status_profile_image);
        itemDecoration.setPadding(decorPaddingLeft, 0, 0, 0);
        recyclerView.addItemDecoration(itemDecoration);
        getLoaderManager().initLoader(0, null, this);
        showContent();
        getAdapter().setItemClickListener(new ItemClickListener() {
            @Override
            public void onItemClick(final int position, final RecyclerView.ViewHolder holder) {
                final Friendship friendship = getAdapter().getFriendship(position);
                final Intent intent = new Intent(getActivity(), UserActivity.class);
                try {
                    intent.putExtra(EXTRA_USER, LoganSquare.mapperFor(User.class).serialize(friendship.getFriend()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                startActivity(intent);
            }
        });
    }

    @NonNull
    @Override
    protected FriendsListAdapter onCreateAdapter(Context context) {
        return new FriendsListAdapter(context);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_chats_list, menu);
    }

    @Override
    public void onBaseViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onBaseViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean isRefreshing() {
        return false;
    }


    @Override
    public Loader<TaskResponse<List<Friendship>>> onCreateLoader(final int id, final Bundle args) {
        return new FriendshipsLoader(getActivity(), Utils.getCurrentAccount(getActivity()), null);
    }

    @Override
    public void onLoadFinished(final Loader<TaskResponse<List<Friendship>>> loader, final TaskResponse<List<Friendship>> data) {
        getAdapter().setData(data.getData());
    }

    @Override
    public void onLoaderReset(final Loader<TaskResponse<List<Friendship>>> loader) {

    }
}
