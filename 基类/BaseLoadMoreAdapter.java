package com.kf.app.user.base;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * 创建时间:2017年11月29日
 * 用    途:一个简单的上下拉刷新加载,显示空数据的轮子.需要注意 构造方法 super不能去掉,需要RecyclerView.addOnScrollListener
 * 以及setCount();
 *
 * @author ErQi
 */
public abstract class BaseLoadMoreAdapter<T> extends RecyclerView.Adapter implements SwipeRefreshLayout.OnRefreshListener {
    private static final int EMPTY = -1;
    private static final int LOAD = -2;
    private Integer mCount;
    private LoadMoreListener mMoreListener;
    private boolean mLoadMore;

    public BaseLoadMoreAdapter() {
        mMoreListener = new LoadMoreListener();
    }

    public void setCount(Integer count) {
        mCount = count;
    }

    public LoadMoreListener getMoreListener() {
        return mMoreListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == LOAD) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getLoadMoreLayout(), parent, false)) {
            };
        } else if (viewType == EMPTY) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(getEmptyLayout(), parent, false)) {
            };
        } else {
            return onCreateHolder(parent, viewType);
        }
    }

    /**
     * 返回空界面的布局
     *
     * @return 对应的layout资源
     */
    protected abstract int getEmptyLayout();

    /**
     * 用于返回加载更多的layout界面
     *
     * @return 返回layou资源id
     */
    protected abstract int getLoadMoreLayout();

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == LOAD) {
            onLoadInfo(holder);
            holder.itemView.setVisibility(mLoadMore ? View.VISIBLE : View.GONE);
        } else if (type == EMPTY) {
            onHideInfo(holder);
        } else {
            onBindHolder(holder, position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && getCount() == 0) {
            return EMPTY;
        } else if (getCount() != position) {
            return getViewType();
        } else {
            return LOAD;
        }
    }

    @Override
    public int getItemCount() {
        return getCount() + 1;
    }

    /**
     * 创建自己的界面
     *
     * @param parent   RecyclerView
     * @param viewType 界面类型
     * @return 返回对应的holder
     */
    protected abstract RecyclerView.ViewHolder onCreateHolder(ViewGroup parent, int viewType);

    /**
     * 创建自己的数据绑定界面
     *
     * @param holder   自己创建的holder
     * @param position 当前的指针
     */
    protected abstract void onBindHolder(RecyclerView.ViewHolder holder, int position);

    /**
     * 没有数据时用于提示
     *
     * @param holder 没有数据的时候的holder,可以设置点击刷新等等
     */
    protected void onHideInfo(RecyclerView.ViewHolder holder) {
    }

    /**
     * 加载界面的holder,用于做自定义的处理
     *
     * @param holder 加载更多
     */
    private void onLoadInfo(RecyclerView.ViewHolder holder) {
    }

    /**
     * 加载更多数据,根据情况决定是否需要
     */
    protected void onLoadMore() {
    }

    @Override
    public void onRefresh() {
    }

    /**
     * 完成数据加载之后暴露的接口,用于设置数据,继承类去实现loadData方法
     *
     * @param list 添加数据
     */
    public void addData(List<T> list) {
        setLoadMore(list);
        loadData(list);
    }

    /**
     * 根据数据条数来设置是否加载更多
     *
     * @param list 数据;
     */
    private void setLoadMore(List<T> list) {
        if (list != null && list.size() == mCount) {
            mLoadMore = true;
        } else {
            mLoadMore = false;
        }
    }

    /**
     * 加载数据,用于加载更多数据
     *
     * @param list 获得的数据
     */
    protected abstract void loadData(List<T> list);

    /**
     * 暴露给外界使用的数据刷新方法
     *
     * @param list 刷新的数据
     */
    public void onRefreshData(List<T> list) {
        setLoadMore(list);
        onRefresh(list);
    }

    /**
     * 内部实际处理的数据刷新方法
     *
     * @param list 刷新的数据
     */
    protected void onRefresh(List<T> list) {
    }

    /**
     * 返回数据条数
     *
     * @return 条数
     */
    public abstract int getCount();

    /**
     * 返回数据类型
     *
     * @return 默认返回0 自定义返回从0开始返回正整数
     */
    public int getViewType() {
        return 0;
    }

    class LoadMoreListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
            // 当不滑动时
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                // 获取最后一个完全显示的itemPosition
                int lastItemPosition = manager.findLastCompletelyVisibleItemPosition();
                int itemCount = manager.getItemCount();
                // 判断是否滑动到了最后一个Item，并且是向左滑动
                if (mLoadMore && lastItemPosition == (itemCount - 1)) {
                    onLoadMore();
                }
            }
        }
    }
}
