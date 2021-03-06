/*
 * Copyright (c) 2015. Catch Inc,
 */

package catchla.yep.fragment

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.text.TextUtils
import android.view.*
import catchla.yep.Constants.*
import catchla.yep.R
import catchla.yep.activity.SkillUpdatesActivity
import catchla.yep.activity.UserActivity
import catchla.yep.adapter.UsersAdapter
import catchla.yep.adapter.UsersGridAdapter
import catchla.yep.adapter.decorator.DividerItemDecoration
import catchla.yep.adapter.iface.ILoadMoreSupportAdapter.IndicatorPosition
import catchla.yep.constant.topicsSortOrderKey
import catchla.yep.extension.Bundle
import catchla.yep.fragment.iface.IActionButtonSupportFragment
import catchla.yep.loader.DiscoverUsersLoader
import catchla.yep.model.*
import catchla.yep.view.holder.FriendGridViewHolder

/**
 * Created by mariotaku on 15/4/29.
 */
class DiscoverFragment : AbsContentRecyclerViewFragment<UsersAdapter, RecyclerView.LayoutManager>(),
        LoaderManager.LoaderCallbacks<List<User>?>, IActionButtonSupportFragment {

    private var page = 1
    private var sortOrder: String? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        val fragmentArgs = arguments
        val loaderArgs = Bundle()
        if (fragmentArgs != null) {
            loaderArgs.putBoolean(EXTRA_READ_CACHE, !fragmentArgs.containsKey(EXTRA_LEARNING) && !fragmentArgs.containsKey(EXTRA_MASTER))
        } else {
            loaderArgs.putBoolean(EXTRA_READ_CACHE, true)
        }
        loaderManager.initLoader(0, loaderArgs, this)
        adapter.itemClickListener = { position: Int, holder: RecyclerView.ViewHolder ->
            val user = adapter.getUser(position)
            val intent = Intent(activity, UserActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT, account)
            intent.putExtra(EXTRA_USER, user)
            startActivity(intent)
        }
        (adapter as UsersGridAdapter).skillClickListener = { position: Int, skill: Skill, holder: FriendGridViewHolder ->
            val intent = Intent(activity, SkillUpdatesActivity::class.java)
            intent.putExtra(EXTRA_ACCOUNT, account)
            intent.putExtra(EXTRA_SKILL, skill)
            startActivity(intent)
        }
        showProgress()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun setupRecyclerView(context: Context, recyclerView: RecyclerView, layoutManager: RecyclerView.LayoutManager) {
        if (layoutManager is LinearLayoutManager) {
            if (layoutManager is GridLayoutManager && layoutManager.spanCount != 1) {
                return
            }
            val itemDecoration = DividerItemDecoration(context,
                    layoutManager.orientation)
            val res = context.resources
            val decorPaddingLeft = res.getDimensionPixelSize(R.dimen.element_spacing_normal) * 2 + res.getDimensionPixelSize(R.dimen.icon_size_status_profile_image)
            itemDecoration.setPadding(decorPaddingLeft, 0, 0, 0)
            recyclerView.addItemDecoration(itemDecoration)
        }

    }

    override fun onCreateLayoutManager(context: Context): RecyclerView.LayoutManager {
        return StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<List<User>?> {
        val query = DiscoverQuery()
        val fragmentArgs = arguments
        val readCache = args.getBoolean(EXTRA_READ_CACHE)
        val readOld = args.getBoolean(EXTRA_READ_OLD, readCache)
        val paging = Paging()
        paging.page(args.getInt(EXTRA_PAGE, 1))
        var writeCache = true
        if (fragmentArgs != null) {
            if (fragmentArgs.containsKey(EXTRA_LEARNING)) {
                query.learningSkills(fragmentArgs.getStringArray(EXTRA_LEARNING))
                writeCache = false
            }
            if (fragmentArgs.containsKey(EXTRA_MASTER)) {
                query.masterSkills(fragmentArgs.getStringArray(EXTRA_MASTER))
                writeCache = false
            }
        }
        val oldData: List<User>?
        if (readOld) {
            oldData = adapter.users
        } else {
            oldData = null
        }
        val sortOrder = sortOrder ?: DiscoverSortOrder.SCORE
        return DiscoverUsersLoader(activity, account, query, sortOrder, oldData, paging, readCache, writeCache)
    }

    override fun onLoadFinished(loader: Loader<List<User>?>, data: List<User>?) {
        val adapter = adapter
        adapter.users = data
        adapter.loadMoreSupportedPosition = if (data != null && !data.isEmpty()) IndicatorPosition.END else IndicatorPosition.NONE
        showContent()
        refreshing = false
        refreshEnabled = true
        loadMoreIndicatorPosition = IndicatorPosition.NONE
    }

    override fun onLoaderReset(loader: Loader<List<User>?>) {
        adapter.users = null
    }

    override fun onCreateAdapter(context: Context): UsersAdapter {
        return UsersGridAdapter(context)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.menu_fragment_chats_list, menu)
    }

    override fun onBaseViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onBaseViewCreated(view, savedInstanceState)
    }

    override fun onRefresh() {
        val loaderArgs = Bundle {
            putBoolean(EXTRA_READ_CACHE, false)
        }
        loaderManager.restartLoader(0, loaderArgs, this)
    }

    override fun onLoadMoreContents(@IndicatorPosition position: Int) {
        // Only supports load from end, skip START flag
        if (position and IndicatorPosition.START != 0) return
        super.onLoadMoreContents(position)
        val loaderArgs = Bundle {
            putBoolean(EXTRA_READ_CACHE, false)
            putBoolean(EXTRA_READ_OLD, true)
            putInt(EXTRA_PAGE, ++page)
        }
        loaderManager.restartLoader(0, loaderArgs, this)
    }

    override val reachingEnd: Boolean get() {
        val lm = layoutManager
        if (lm is LinearLayoutManager) {
            return lm.findFirstCompletelyVisibleItemPosition() >= layoutManager.itemCount - 1
        } else if (lm is StaggeredGridLayoutManager) {
            for (position in lm.findFirstCompletelyVisibleItemPositions(null)) {
                if (position >= layoutManager.itemCount - 1) return true
            }
        }
        return false
    }

    override val reachingStart: Boolean get() {
        val lm = layoutManager
        if (lm is LinearLayoutManager) {
            return lm.findFirstCompletelyVisibleItemPosition() <= 0
        } else if (lm is StaggeredGridLayoutManager) {
            for (position in lm.findFirstCompletelyVisibleItemPositions(null)) {
                if (position <= 0) return true
            }
        }
        return false
    }

    override fun onScrollToPositionWithOffset(layoutManager: RecyclerView.LayoutManager, position: Int, offset: Int) {
        layoutManager.scrollToPosition(position)
    }

    override var refreshing: Boolean
        get() = loaderManager.hasRunningLoaders()
        set(value) {
            super.refreshing = value
        }

    override fun getActionIcon(): Int {
        return 0
    }

    override fun onActionPerformed() {
    }

    override fun getActionMenuFragment(): Class<out DiscoverMenuFragment> = DiscoverMenuFragment::class.java

    private val account: Account
        get() = arguments.getParcelable<Account>(EXTRA_ACCOUNT)

    fun reloadWithSortOrder(sortOrder: String) {
        if (TextUtils.equals(sortOrder, this.sortOrder)) return
        this.sortOrder = sortOrder
        preferences[topicsSortOrderKey] = sortOrder
        val loaderArgs = Bundle {
            putBoolean(EXTRA_READ_CACHE, false)
            putBoolean(EXTRA_READ_OLD, false)
        }
        loaderManager.restartLoader(0, loaderArgs, this)
        showProgress()
    }

}
