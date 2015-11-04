package catchla.yep.fragment;

import android.accounts.Account;
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

import java.util.List;

import catchla.yep.R;
import catchla.yep.activity.NewTopicActivity;
import catchla.yep.activity.SkillUpdatesActivity;
import catchla.yep.activity.TopicChatActivity;
import catchla.yep.adapter.TopicsAdapter;
import catchla.yep.adapter.decorator.DividerItemDecoration;
import catchla.yep.fragment.iface.IActionButtonSupportFragment;
import catchla.yep.loader.DiscoverTopicsLoader;
import catchla.yep.model.Paging;
import catchla.yep.model.TaskResponse;
import catchla.yep.model.Topic;
import catchla.yep.view.holder.TopicViewHolder;

/**
 * Created by mariotaku on 15/10/12.
 */
public class TopicsListFragment extends AbsContentListRecyclerViewFragment<TopicsAdapter>
        implements LoaderManager.LoaderCallbacks<TaskResponse<List<Topic>>>, TopicsAdapter.TopicClickAdapter,
        IActionButtonSupportFragment {

    private int mPage = 1;

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
                + res.getDimensionPixelSize(R.dimen.icon_size_topic_item_profile_image);
        itemDecoration.setPadding(decorPaddingLeft, 0, 0, 0);
        recyclerView.addItemDecoration(itemDecoration);
        final Bundle fragmentArgs = getArguments();
        final Bundle loaderArgs = new Bundle();
        if (fragmentArgs != null) {
            loaderArgs.putBoolean(EXTRA_READ_CACHE, !fragmentArgs.containsKey(EXTRA_LEARNING)
                    && !fragmentArgs.containsKey(EXTRA_MASTER));
        } else {
            loaderArgs.putBoolean(EXTRA_READ_CACHE, true);
        }
        getLoaderManager().initLoader(0, loaderArgs, this);
        getAdapter().setClickListener(this);
        showProgress();
    }


    @Override
    public Loader<TaskResponse<List<Topic>>> onCreateLoader(final int id, final Bundle args) {
        final Bundle fragmentArgs = getArguments();
        final boolean readCache = args.getBoolean(EXTRA_READ_CACHE);
        final boolean readOld = args.getBoolean(EXTRA_READ_OLD, readCache);
        boolean writeCache = true;
        if (fragmentArgs != null) {

        }
        final Paging paging = new Paging();
        paging.page(args.getInt(EXTRA_PAGE, 1));
        final List<Topic> oldData;
        if (readOld) {
            oldData = getAdapter().getTopics();
        } else {
            oldData = null;
        }
        return new DiscoverTopicsLoader(getActivity(), getAccount(), Topic.SortOrder.TIME,
                oldData, paging, readCache, writeCache);
    }

    @Override
    public void onLoadFinished(final Loader<TaskResponse<List<Topic>>> loader, final TaskResponse<List<Topic>> data) {
        final TopicsAdapter adapter = getAdapter();
        final List<Topic> list = data.getData();
        adapter.setData(list);
        adapter.setLoadMoreSupported(list != null && !list.isEmpty());
        showContent();
        setRefreshing(false);
        setRefreshEnabled(true);
        setLoadMoreIndicatorVisible(false);
    }

    @Override
    public void onLoaderReset(final Loader<TaskResponse<List<Topic>>> loader) {

    }

    @NonNull
    @Override
    protected TopicsAdapter onCreateAdapter(Context context) {
        return new TopicsAdapter(context);
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
    public void onRefresh() {
        final Bundle loaderArgs = new Bundle();
        loaderArgs.putBoolean(EXTRA_READ_CACHE, false);
        getLoaderManager().restartLoader(0, loaderArgs, this);
    }

    @Override
    public boolean isRefreshing() {
        return getLoaderManager().hasRunningLoaders();
    }


    @Override
    public void onItemClick(final int position, final RecyclerView.ViewHolder holder) {
        final Topic topic = getAdapter().getTopic(position);
        final Intent intent = new Intent(getActivity(), TopicChatActivity.class);
        intent.putExtra(EXTRA_ACCOUNT, getAccount());
        intent.putExtra(EXTRA_TOPIC, topic);
        startActivity(intent);
    }

    private Account getAccount() {
        return getArguments().getParcelable(EXTRA_ACCOUNT);
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_action_edit;
    }

    @Override
    public void onActionPerformed() {
        final Intent intent = new Intent(getContext(), NewTopicActivity.class);
        intent.putExtra(EXTRA_ACCOUNT, getAccount());
        startActivity(intent);
    }

    @Override
    public void onLoadMoreContents() {
        super.onLoadMoreContents();
        final Bundle loaderArgs = new Bundle();
        loaderArgs.putBoolean(EXTRA_READ_CACHE, false);
        loaderArgs.putBoolean(EXTRA_READ_OLD, true);
        loaderArgs.putInt(EXTRA_PAGE, ++mPage);
        getLoaderManager().restartLoader(0, loaderArgs, this);
    }

    @Override
    public void onSkillClick(final int position, final TopicViewHolder holder) {
        final Intent intent = new Intent(getContext(), SkillUpdatesActivity.class);
        intent.putExtra(EXTRA_ACCOUNT, getAccount());
        intent.putExtra(EXTRA_SKILL, getAdapter().getTopic(position).getSkill());
        startActivity(intent);
    }
}