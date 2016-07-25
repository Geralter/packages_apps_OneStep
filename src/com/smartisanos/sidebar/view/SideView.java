package com.smartisanos.sidebar.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.DragEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.android.internal.sidebar.ISidebarService;
import com.smartisanos.sidebar.R;
import com.smartisanos.sidebar.SidebarController;
import com.smartisanos.sidebar.SidebarMode;
import com.smartisanos.sidebar.util.ContactItem;
import com.smartisanos.sidebar.util.LOG;
import com.smartisanos.sidebar.util.ResolveInfoGroup;
import com.smartisanos.sidebar.util.anim.Anim;
import com.smartisanos.sidebar.util.anim.AnimListener;
import com.smartisanos.sidebar.util.anim.AnimStatusManager;
import com.smartisanos.sidebar.util.anim.AnimTimeLine;
import com.smartisanos.sidebar.util.anim.Vector3f;
import com.smartisanos.sidebar.view.ContentView.ContentType;

public class SideView extends RelativeLayout {
    private static final LOG log = LOG.getInstance(SideView.class);

    private View mExitAndAdd;
    private View mLeftShadow, mRightShadow;
    private ImageView mExit, mAdd;

    private SidebarListView mShareList, mContactList;
    private SidebarListView mShareListFake, mContactListFake;

    private ResolveInfoListAdapter mResolveAdapter;
    private ScrollView mScrollList;

    private ContactListAdapter mContactAdapter;

    private Context mContext;

    public SideView(Context context) {
        this(context, null);
    }

    public SideView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SideView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SideView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }

    private SidebarRootView mRootView;

    public void setRootView(SidebarRootView view) {
        mRootView = view;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mExitAndAdd = findViewById(R.id.exit_and_add);
        mExit = (ImageView) findViewById(R.id.exit);
        mLeftShadow = findViewById(R.id.left_shadow);
        mRightShadow = findViewById(R.id.right_shadow);
        updateUIBySIdebarMode();
        mExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ISidebarService sidebarService = ISidebarService.Stub.asInterface(ServiceManager.getService(Context.SIDEBAR_SERVICE));
                if (sidebarService != null) {
                    try {
                        sidebarService.resetWindow();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mAdd = (ImageView) findViewById(R.id.add);
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AnimStatusManager.getInstance().canShowContentView()) {
                    AnimStatusManager.getInstance().dumpStatus();
                    return;
                }
                if (mAddButtonRotateAnim != null) {
                    return;
                }
                SidebarController sc = SidebarController.getInstance(mContext);
                if(sc.getCurrentContentType() == ContentType.NONE){
                    sc.dimTopView();
                    sc.showContent(ContentType.ADDTOSIDEBAR);
                }else if(sc.getCurrentContentType() == ContentType.ADDTOSIDEBAR){
                    sc.resumeTopView();
                    sc.dismissContent(true);
                }
            }
        });

        //contact
        mContactList = (SidebarListView) findViewById(R.id.contactlist);
        mContactList.setSideView(this);
        mContactList.setNeedFootView(true);
        mContactAdapter = new ContactListAdapter(mContext);
        mContactList.setAdapter(mContactAdapter);
        mContactList.setOnItemClickListener(mContactItemOnClickListener);
        mContactList.setOnItemLongClickListener(mContactItemOnLongClickListener);

        mContactListFake = (SidebarListView) findViewById(R.id.contactlist_fake);
        mContactListFake.setSideView(this);
        mContactListFake.setNeedFootView(true);
        mContactListFake.setIsFake(true);
        mContactListFake.setAdapter(new ContactListAdapter(mContext));

        mContactList.setFake(mContactListFake);

        //resolve
        mShareList = (SidebarListView) findViewById(R.id.sharelist);
        mShareList.setSideView(this);
        mResolveAdapter = new ResolveInfoListAdapter(mContext);
        mShareList.setAdapter(mResolveAdapter);
        mShareList.setOnItemClickListener(mShareItemOnClickListener);
        mShareList.setOnItemLongClickListener(mShareItemOnLongClickListener);

        mShareListFake = (SidebarListView) findViewById(R.id.sharelist_fake);
        mShareListFake.setSideView(this);
        mShareListFake.setIsFake(true);
        mShareListFake.setCanAcceptDrag(false);
        mShareListFake.setAdapter(new ResolveInfoListAdapter(mContext));

        mShareList.setFake(mShareListFake);
        mScrollList = (ScrollView) findViewById(R.id.sideview_scroll_list);
    }

    public void refreshDivider() {
        if (mContactList != null) {
            mContactList.requestLayout();
        }
        if (mContactListFake != null) {
            mContactListFake.requestLayout();
        }
    }

    public boolean someListIsEmpty() {
        if (mContactList.getAdapter() != null && mContactList.getAdapter().getCount() > 0
                && mShareList.getAdapter() != null && mShareList.getAdapter().getCount() > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean dispatchDragEvent(DragEvent event) {
        int action = event.getAction();
        switch (action) {
        case DragEvent.ACTION_DRAG_STARTED:
            mContactList.onDragStart(event);
            mShareList.onDragStart(event);
            return super.dispatchDragEvent(event);
        case DragEvent.ACTION_DRAG_ENDED:
            boolean ret = super.dispatchDragEvent(event);
            mContactList.onDragEnd();
            mShareList.onDragEnd();
            return ret;
        }
        return super.dispatchDragEvent(event);
    }

    public ResolveInfoListAdapter getAppListAdapter() {
        return mResolveAdapter;
    }

    public ContactListAdapter getContactListAdapter() {
        return mContactAdapter;
    }

    private void updateUIBySIdebarMode() {
        if (SidebarController.getInstance(mContext).getSidebarMode() == SidebarMode.MODE_LEFT) {
            //mExit.setBackgroundResource(R.drawable.exit_icon_left);
            mExit.setImageResource(R.drawable.exit_icon_left);
            mExitAndAdd.setBackgroundResource(R.drawable.exitandadd_bg_left);
            mLeftShadow.setVisibility(View.VISIBLE);
            mRightShadow.setVisibility(View.GONE);
        } else {
            //mExit.setBackgroundResource(R.drawable.exit_icon_right);
            mExit.setImageResource(R.drawable.exit_icon_right);
            mExitAndAdd.setBackgroundResource(R.drawable.exitandadd_bg_right);
            mLeftShadow.setVisibility(View.GONE);
            mRightShadow.setVisibility(View.VISIBLE);
        }
    }

    public void onSidebarModeChanged(){
        updateUIBySIdebarMode();
        mResolveAdapter.notifyDataSetChanged();
    }

    private AdapterView.OnItemClickListener mShareItemOnClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            if (view == null || view.getTag() == null) {
                return;
            }
            final View v = view;
            AnimTimeLine timeLine = shakeIconAnim(v);
            timeLine.setAnimListener(new AnimListener() {
                @Override
                public void onStart() {
                }

                @Override
                public void onComplete(int type) {
                    v.setTranslationX(0);
                }
            });
            timeLine.start();
        }
    };

    private AdapterView.OnItemLongClickListener mShareItemOnLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
            if (view.getTag() == null) {
                log.error("onItemLongClick return by tag is null");
                return false;
            }
            ResolveInfoListAdapter.ViewHolder holder = (ResolveInfoListAdapter.ViewHolder) view.getTag();
            PackageManager pm = mContext.getPackageManager();
            ResolveInfoGroup data = holder.resolveInfoGroup;
            Drawable icon = data.loadIcon(pm);

            int index = mResolveAdapter.objectIndex(data);
            mShareList.setPrePosition(index);
            int[] viewLoc = new int[2];
            view.getLocationOnScreen(viewLoc);
            viewLoc[0] = viewLoc[0] + view.getWidth() / 2;
            viewLoc[1] = viewLoc[1] + view.getHeight() / 2;

            SidebarRootView.DragItem dragItem = new SidebarRootView.DragItem(SidebarRootView.DragItem.TYPE_APPLICATION, icon, data, index);
            dragItem.mListItemView = view;
            mRootView.startDrag(dragItem, viewLoc);
            dragItemType = SidebarRootView.DragItem.TYPE_APPLICATION;
            getScrollViewLayoutParams();
            holder.view.setVisibility(View.INVISIBLE);
            return false;
        }
    };

    private AdapterView.OnItemClickListener mContactItemOnClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            if (view == null || view.getTag() == null) {
                return;
            }
            final View v = view;
            AnimTimeLine timeLine = shakeIconAnim(view);
            timeLine.setAnimListener(new AnimListener() {
                @Override
                public void onStart() {
                }

                @Override
                public void onComplete(int type) {
                    v.setTranslationX(0);
                }
            });
            timeLine.start();
        }
    };

    private AdapterView.OnItemLongClickListener mContactItemOnLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            if (view.getTag() == null) {
                log.error("mContactItemOnLongClickListener return by tag is null");
                return false;
            }

            int[] viewLoc = new int[2];
            view.getLocationOnScreen(viewLoc);
            viewLoc[0] = viewLoc[0] + view.getWidth() / 2;
            viewLoc[1] = viewLoc[1] + view.getHeight() / 2;

            ContactListAdapter.ViewHolder holder = (ContactListAdapter.ViewHolder) view.getTag();
            ContactItem item = holder.mItem;
            Drawable icon = new BitmapDrawable(getResources(), item.getAvatar());
            int index = mContactAdapter.objectIndex(item);
            mContactList.setPrePosition(index);
            SidebarRootView.DragItem dragItem = new SidebarRootView.DragItem(SidebarRootView.DragItem.TYPE_SHORTCUT, icon, item, index);
            dragItem.mListItemView = view;
            mRootView.startDrag(dragItem, viewLoc);
            dragItemType = SidebarRootView.DragItem.TYPE_SHORTCUT;
            getScrollViewLayoutParams();
            log.error("onItemLongClick " + holder.view);
            holder.view.setVisibility(View.INVISIBLE);
            return false;
        }
    };

    public int[] appListLoc = new int[2];
    public int[] contactListLoc = new int[2];
    private Rect drawingRect = new Rect();
    private Rect scrollViewRect = new Rect();
    private int[] scrollViewLoc = new int[2];
    private int[] scrollViewSize = new int[2];
    private int mAppListHeight;
    private int mContactListHeight;
    private int dragItemType = 0;

    private int[] preLoc = new int[2];

    private void getScrollViewLayoutParams() {
        initWindowSize(mContext);
        mAppListHeight = mShareList.getHeight();
        mContactListHeight = mContactList.getHeight();
        mScrollList.getLocationOnScreen(scrollViewLoc);
        scrollViewSize[0] = mScrollList.getWidth();
        scrollViewSize[1] = mScrollList.getHeight();
        log.error("scroll view size ("+mScrollList.getWidth()+", "+mScrollList.getHeight()+")");
        log.error("scroll view loc ["+ scrollViewLoc[0]+", "+ scrollViewLoc[1]+"] " +
                "rect ["+scrollViewRect.left+", "+scrollViewRect.top+", "+scrollViewRect.right+", "+scrollViewRect.bottom+"]");
    }

    private int mWindowWidth;
    private int mWindowHeight;

    private void initWindowSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        int widthPixels;
        int heightPixels;
        if (metrics.heightPixels > metrics.widthPixels) {
            widthPixels = metrics.widthPixels;
            heightPixels = metrics.heightPixels;
        } else {
            widthPixels = metrics.heightPixels;
            heightPixels = metrics.widthPixels;
        }
        mWindowWidth = widthPixels;
        mWindowHeight = heightPixels;
    }

    private int AREA_TYPE_NORMAL = 0;
    private int AREA_TYPE_TOP = 1;
    private int AREA_TYPE_BOTTOM = 2;

    private int areaType(int x, int y, int itemViewHeight) {
        if (x < scrollViewLoc[0]) {
            return AREA_TYPE_NORMAL;
        }
        if ((scrollViewLoc[1] - 20) < y && y < (scrollViewLoc[1] + itemViewHeight)) {
            if (scrollViewRect.top != 0) {
                return AREA_TYPE_TOP;
            }
        }
        if (y > mWindowHeight - itemViewHeight) {
            if (scrollViewRect.bottom < (mAppListHeight + mContactListHeight)) {
                return AREA_TYPE_BOTTOM;
            }
        }
        return AREA_TYPE_NORMAL;
    }

    private volatile boolean scrolling = false;

    private void setScrollTo(int area, int itemViewHeight) {
        if (itemViewHeight == 0) {
            return;
        }
        int scrollY = 0;
        if (area == AREA_TYPE_TOP) {
            scrollY = -itemViewHeight;
        } else if (area == AREA_TYPE_BOTTOM) {
            scrollY = itemViewHeight;
        }
        final int y = scrollY;
//        log.error("setScrollTo type "+area+", Y => " + y);
        scrolling = true;
        mScrollList.setSmoothScrollingEnabled(true);
        post(new Runnable() {
            @Override
            public void run() {
                mScrollList.smoothScrollBy(0, y);
                post(new Runnable() {
                    @Override
                    public void run() {
                        scrolling = false;
                    }
                });
            }
        });
    }

    private long preScrollTime;

    public void dragObjectMove(int x, int y, long eventTime) {
        if (x < scrollViewLoc[0]) {
            return;
        }
        preLoc[0] = x;
        preLoc[1] = y;
        mShareList.getLocationOnScreen(appListLoc);
        mContactList.getLocationOnScreen(contactListLoc);
        int itemViewHeight = 0;
        mScrollList.getDrawingRect(scrollViewRect);
        int area = AREA_TYPE_NORMAL;
        int position = -1;
        if (dragItemType == SidebarRootView.DragItem.TYPE_APPLICATION
                && inArea(x, y, mShareList, appListLoc)) {
            int count = mResolveAdapter.getCount();
            if (count > 0) {
                //convert global coordinate to view local coordinate
                mShareList.getDrawingRect(drawingRect);
                int[] localLoc = convertToLocalCoordinate(x, y, appListLoc, drawingRect);
                int subViewHeight = drawingRect.bottom / count;
                itemViewHeight = subViewHeight;
                position = localLoc[1] / subViewHeight;
                if (position >= count) {
                    return;
                }
                area = areaType(x, y, itemViewHeight);
//                log.error("A position ["+position+"], Y ["+localLoc[1]+"] AREA ["+area+"]");
            }
        } else if (dragItemType == SidebarRootView.DragItem.TYPE_SHORTCUT
                && inArea(x, y, mContactList, contactListLoc)) {
            int count = mContactAdapter.getCount();
            if (count > 0) {
                mContactList.getDrawingRect(drawingRect);
                int[] localLoc = convertToLocalCoordinate(x, y, contactListLoc, drawingRect);
                int subViewHeight = drawingRect.bottom / count;
                itemViewHeight = subViewHeight;
                position = localLoc[1] / subViewHeight;
                if (position >= count) {
                    return;
                }
                area = areaType(x, y, itemViewHeight);
//                log.error("B position ["+position+"], Y ["+localLoc[1]+"] AREA ["+area+"]");
            }
        }
        if (area != AREA_TYPE_NORMAL) {
            if (scrolling) {
                return;
            }
            long delta = eventTime - preScrollTime;
            if (delta < 0) {
                delta = delta * -1;
            }
            if (delta < 250) {
//                log.error("preScrollTime ["+preScrollTime+"] ["+eventTime+"]");
                return;
            }
            preScrollTime = eventTime;
            setScrollTo(area, itemViewHeight);
        }
        if (position >= 0) {
            if (dragItemType == SidebarRootView.DragItem.TYPE_APPLICATION) {
                mShareList.pointToNewPositionWithAnim(position);
            } else if (dragItemType == SidebarRootView.DragItem.TYPE_SHORTCUT) {
                mContactList.pointToNewPositionWithAnim(position);
            }
            if (mRootView.getDraggedView() != null) {
                mRootView.getDraggedView().getDragItem().viewIndex = position;
            }
        }
    }

    private int[] convertToLocalCoordinate(int x, int y, int[] viewLoc, Rect drawingRect) {
        int[] loc = new int[2];
        loc[0] = x - viewLoc[0];
        loc[1] = y - viewLoc[1];
        loc[0] = loc[0] + drawingRect.left;
        loc[1] = loc[1] + drawingRect.top;
        return loc;
    }

    private boolean inArea(float x, float y, View view, int[] loc) {
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        int left   = loc[0];
        int top    = loc[1];
        int right  = left + viewWidth;
        int bottom = top + viewHeight;
        if (left < x && x < right) {
            if (top < y && y < bottom) {
                return true;
            }
        }
        return false;
    }

    public void initViewAnim() {
        mScrollList.setVisibility(INVISIBLE);
    }

    public void showAnimWhenSplitWindow() {
        mScrollList.setVisibility(VISIBLE);
        boolean isLeft = SidebarController.getInstance(mContext).getSidebarMode() == SidebarMode.MODE_LEFT;
        int fromX = isLeft ? -mContactList.getWidth() : mContactList.getWidth();
        int toX = 0;
        log.error("sidebarListShowAnim from ["+fromX+"] to ["+toX+"]");
        Anim anim = new Anim(mScrollList, Anim.TRANSLATE, 200, Anim.CUBIC_OUT, new Vector3f(fromX, 0), new Vector3f(toX, 0));
        anim.start();
    }

    private void restoreListItemView(SidebarListView listView) {
        if (listView != null) {
            try {
                int count = listView.getCount();
                if (count == 0) {
                    return;
                }
                for (int i = 0; i < count; i++) {
                    View view = listView.getChildAt(i);
                    if (view == null) {
                        continue;
                    }
                    view.setScaleX(1);
                    view.setScaleY(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void restoreView() {
        mShareList.onDragEnd();
        mContactList.onDragEnd();
        restoreListItemView(mContactList);
        restoreListItemView(mShareList);
        mAdd.setRotation(0);
    }

    private Anim mAddButtonRotateAnim = null;
    public void clickAddButtonAnim(boolean isLeft, boolean isEnter, final Runnable taskForComplete) {
        if (mAdd == null) {
            return;
        }
        if (mAddButtonRotateAnim != null) {
            mAddButtonRotateAnim.cancel();
        }
        int from = 0;
        int to = 0;
        if (isLeft) {
            if (isEnter) {
                to = 45;
            } else {
                from = 45;
            }
        } else {
            if (isEnter) {
                to = -45;
            } else {
                from = -45;
            }
        }
        int width = mAdd.getWidth();
        int height = mAdd.getHeight();
        mAdd.setPivotX(width / 2);
        mAdd.setPivotY(height / 2);
        mAdd.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mAdd.setDrawingCacheEnabled(false);

        mAddButtonRotateAnim = new Anim(mAdd, Anim.ROTATE, 300, Anim.CUBIC_OUT, new Vector3f(0, 0, from), new Vector3f(0, 0, to));
        mAddButtonRotateAnim.setListener(new AnimListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onComplete(int type) {
                if (mAddButtonRotateAnim == null) {
                    return;
                }
                mAdd.setRotation(mAddButtonRotateAnim.getTo().z);
                if (taskForComplete != null) {
                    taskForComplete.run();
                }
                mAddButtonRotateAnim = null;
            }
        });
        mAddButtonRotateAnim.start();
    }

    private AnimTimeLine shakeIconAnim(View view) {
        AnimTimeLine timeLine = new AnimTimeLine();
        int time = 100;
        Anim anim1 = new Anim(view, Anim.MOVE, time, Anim.CUBIC_OUT, new Vector3f(), new Vector3f(-5, 0));
        Anim anim2 = new Anim(view, Anim.MOVE, time, Anim.CUBIC_OUT, new Vector3f(-5, 0), new Vector3f(5, 0));
        anim2.setDelay(time);
        Anim anim3 = new Anim(view, Anim.MOVE, time, Anim.CUBIC_OUT, new Vector3f(5, 0), new Vector3f(-5, 0));
        anim3.setDelay(time * 2);
        Anim anim4 = new Anim(view, Anim.MOVE, time, Anim.CUBIC_OUT, new Vector3f(-5, 0), new Vector3f());
        anim4.setDelay(time * 3);
        timeLine.addAnim(anim1);
        timeLine.addAnim(anim2);
        timeLine.addAnim(anim3);
        timeLine.addAnim(anim4);
        return timeLine;
    }
}
