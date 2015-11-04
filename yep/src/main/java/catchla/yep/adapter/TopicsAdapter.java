/*
 * Copyright (c) 2015. Catch Inc,
 */

package catchla.yep.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import catchla.yep.R;
import catchla.yep.adapter.iface.ItemClickListener;
import catchla.yep.model.Topic;
import catchla.yep.view.holder.LoadIndicatorViewHolder;
import catchla.yep.view.holder.TopicViewHolder;

/**
 * Created by mariotaku on 15/4/29.
 */
public class TopicsAdapter extends LoadMoreSupportAdapter {
    private static final int ITEM_VIEW_TYPE_USER_ITEM = 1;

    private final LayoutInflater mInflater;

    public void setClickListener(final TopicClickAdapter listener) {
        this.mClickListener = listener;
    }

    private TopicClickAdapter mClickListener;

    private List<Topic> mData;

    public TopicsAdapter(Context context) {
        super(context);
        mInflater = LayoutInflater.from(context);

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE_USER_ITEM: {
                final View view = mInflater.inflate(R.layout.list_item_topic, parent, false);
                return new TopicViewHolder(view, getContext(), getImageLoader(), mClickListener);
            }
            case ITEM_VIEW_TYPE_LOAD_INDICATOR: {
                final View view = mInflater.inflate(R.layout.card_item_load_indicator, parent, false);
                return new LoadIndicatorViewHolder(view);
            }
        }
        throw new UnsupportedOperationException("Unknown viewType " + viewType);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getTopicsCount()) return ITEM_VIEW_TYPE_LOAD_INDICATOR;
        return ITEM_VIEW_TYPE_USER_ITEM;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ITEM_VIEW_TYPE_LOAD_INDICATOR: {
                break;
            }
            case ITEM_VIEW_TYPE_USER_ITEM: {
                final TopicViewHolder topicViewHolder = (TopicViewHolder) holder;
                topicViewHolder.displayTopic(mData.get(position));
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return (isLoadMoreIndicatorVisible() ? 1 : 0) + getTopicsCount();
    }

    public int getTopicsCount() {
        if (mData == null) return 0;
        return mData.size();
    }


    public void setData(final List<Topic> data) {
        mData = data;
        notifyDataSetChanged();
    }

    public Topic getTopic(final int position) {
        return mData.get(position);
    }

    public List<Topic> getTopics() {
        return mData;
    }

    public interface TopicClickAdapter extends ItemClickListener {
        void onSkillClick(int position, TopicViewHolder holder);
    }
}