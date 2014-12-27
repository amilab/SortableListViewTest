package com.example.sortablelistviewtest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;

public class SortableListView extends ListView implements OnItemLongClickListener {

	private static final int SCROLL_SPEED_FAST = 40;
	private static final int SCROLL_SPEED_SLOW = 8;
	private static final int SHIFT_SIZE = 10;
	private static final float THRESHOLD_SPEED_FAST = 0.05f;
	private static final float THRESHOLD_SPEED_SLOW = 0.2f;
	private static final Bitmap.Config DRAG_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

	private boolean mSortable = false;
	private boolean mDragging = false;
	private DragListener mDragListener = new SimpleDragListener();
	private int mBitmapBackgroundColor = Color.argb(128, 0xFF, 0xFF, 0xFF);
	private Bitmap mDragBitmap = null;
	private ImageView mDragImageView = null;
	private WindowManager.LayoutParams mLayoutParams = null;
	private int mPositionFrom = INVALID_POSITION;
	
	private int mDragStartRawX = 0;
	private int mDragStartRawY = 0;
	private int mDragStartY = 0;
	private int mDraggableViewId = NO_ID;
	private int mStatusBarHeight = 0;
	private int mOffsetY = 0;
	private boolean mInScrollRange = false;
		
	/** コンストラクタ */
	public SortableListView(Context context) {
		super(context);
	}
	
	/** コンストラクタ */
	public SortableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnItemLongClickListener(this);
	}

	/** コンストラクタ */
	public SortableListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setOnItemLongClickListener(this);
	}
	
	/** ドラッグイベントリスナの設定 */
	public void setDragListener(DragListener listener) {
		mDragListener = listener;
	}

	/** ソートモードの切替 */
	public void setSortable(boolean sortable) {
		this.mSortable = sortable;
	}

	/** ソート中アイテムの背景色を設定 */
	@Override
	public void setBackgroundColor(int color) {
		mBitmapBackgroundColor = color;
	}
	
	/** ドラッグ対象の View を指定する */
	public void setDraggableViewId(int id) {
		mDraggableViewId = id;
	}
	
	/** ソートモードの設定 */
	public boolean getSortable() {
		return mSortable;
	}

	/** MotionEvent から position を取得する */
	private int eventToPosition(MotionEvent event) {
		return pointToPosition((int) event.getX(), (int) event.getY());
	}

	/** タッチイベント処理 */
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    if (!mSortable) {
	        return super.onTouchEvent(event);
	    }
	    switch (event.getAction()) {
	        case MotionEvent.ACTION_DOWN: {
	            storeMotionEvent(event);
	            if (isInUpwardScrollRange((int) event.getY())
	            		|| isInDownwardScrollRange((int) event.getY())) {
	            	mInScrollRange = true;
	            } else {
	            	mInScrollRange = false;
	            }
	            break;
	        }
	        case MotionEvent.ACTION_MOVE: {
	            if (duringDrag(event)) {
	                return true;
	            }
	            break;
	        }
	        case MotionEvent.ACTION_UP: {
	            if (stopDrag(event, true)) {
	                return true;
	            }
	            break;
	        }
	        case MotionEvent.ACTION_CANCEL:
	        case MotionEvent.ACTION_OUTSIDE: {
	            if (stopDrag(event, false)) {
	                return true;
	            }
	            break;
	        }
	    }
	    return super.onTouchEvent(event);
	}
	
    /** リスト要素長押しイベント処理 */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        return startDrag(view);
    }

    /** ACTION_DOWN 時の MotionEvent をプロパティに格納 */
    private void storeMotionEvent(MotionEvent event) {
    	mDragStartRawX = (int) event.getRawX();
       	mDragStartRawY = (int) event.getRawY();
       	mDragStartY = (int) event.getY();
    }

    /** ドラッグ開始 */
	public boolean startDrag(View view) {
	    if (!mSortable) {
	    	return false;
	    }
	    
	    // タッチされた　View　に対応するアイテムの番号を取得する
		mPositionFrom = getPositionForView(view); 
				
		// 対応するアイテムが存在しない場合はドラッグを開始しない
		if (mPositionFrom == INVALID_POSITION) {
			return false;
		}
		
		// ドラッグ対象の View　の内部がタッチされたかを確認する
		if (mDraggableViewId != NO_ID) {
			final View v = view.findViewById(mDraggableViewId);
			if (v != null) {
				// ドラッグ対象の View　の位置を取得
				final int[] location = new int[2];
				v.getLocationOnScreen(location);
				
				// View の外部がタッチされた場合はドラッグを開始しない
				if (mDragStartRawX < location[0]
						|| location[0] + v.getWidth() < mDragStartRawX) {
					return false;
				}
				if (mDragStartRawY < location[1]
						|| location[1] + v.getHeight() < mDragStartRawY) {
					return false;
				}
			}
		}
		
		// リスナーのドラッグ開始時の処理を呼び出す
		if (mDragListener != null) {
			mPositionFrom = mDragListener.onStartDrag(mPositionFrom);
			
			// リスナーから無効な番号が返された場合はドラッグを開始しない
			if (mPositionFrom == INVALID_POSITION) {
				return false;
			}
		}
		
		// ドラッグ中を示すフラグを立てる
		mDragging = true;
		
		// ステータスバーの高さを取得する
		final Rect rect = new Rect();
		final Window window = ((Activity)getContext()).getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rect);
		mStatusBarHeight = rect.top;
		
		// タッチされた View の上端からタッチされた位置までの距離を取得
		mOffsetY = mDragStartY - view.getTop();

		// タッチされた View を Bitmap に描画する
		Canvas canvas = new Canvas();
		mDragBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), DRAG_BITMAP_CONFIG);
		canvas.setBitmap(mDragBitmap);
		view.draw(canvas);

		// WindowManager を取得する
		WindowManager wm = getWindowManager();

		// 前回使用した ImageView が残っている場合は除去する（念のため？）
		if (mDragImageView != null) {
			wm.removeView(mDragImageView);
		}

		// ImageView 用の LayoutParams が未設定の場合は設定する
		if (mLayoutParams == null) {
			mLayoutParams = new WindowManager.LayoutParams();
		}
		initLayoutParams(mLayoutParams);

		// ImageView を生成し WindowManager に addChild する
		mDragImageView = new ImageView(getContext());
		mDragImageView.setBackgroundColor(mBitmapBackgroundColor);
		mDragImageView.setImageBitmap(mDragBitmap);
		wm.addView(mDragImageView, mLayoutParams);

		return true;
	}

	/** ドラッグ処理 */
	private boolean duringDrag(MotionEvent event) {
		if (!mDragging || mDragImageView == null) {
			return false;
		}
		
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        final int height = getHeight();

        // スクロール速度の決定
        int speed;
        float threshold;
        if (event.getEventTime() - event.getDownTime() < 500) {
            // ドラッグの開始から500ミリ秒の間はスクロールしない
            speed = 0;
        } else if (isInUpwardScrollRange((int) event.getY())) {
        	// タッチされている位置が上端に近い場合は上方向にスクロール
        	threshold = height * THRESHOLD_SPEED_FAST;
            speed = y < threshold ? -SCROLL_SPEED_FAST : -SCROLL_SPEED_SLOW;
        } else if (isInDownwardScrollRange((int) event.getY())) {
        	// タッチされている位置が下端に近い場合は下方向にスクロール
        	threshold = height * (1.0f - THRESHOLD_SPEED_FAST);
            speed = y > threshold ? SCROLL_SPEED_FAST : SCROLL_SPEED_SLOW;
        } else {
        	// タッチされている位置が中央に近い場合はスクロールしない
            speed = 0;
            
            // スクロール実行範囲から出たのでフラグを落とす
            mInScrollRange = false;
        }
        
        // ドラッグ開始時からスクロール実行範囲にいる場合はスクロールしない
        if (mInScrollRange) {
        	speed = 0;
        }

		// スクロール処理
		if (speed != 0) {
			// 表示されているアイテムのうち真ん中に位置しているものの View を取得する
			final int top = getFirstVisiblePosition();
			final int bottom = getLastVisiblePosition();
			final int mid = (top + bottom) / 2;
			final View middleView = getChildAt(mid - top);
			
			// 取得した View の位置を変更する
			if (middleView != null) {
				setSelectionFromTop(mid, middleView.getTop() - speed);
			}
		}

		// ImageView の表示や位置を更新
		if (mDragImageView.getHeight() < 0) {
			mDragImageView.setVisibility(View.INVISIBLE);
		} else {
			mDragImageView.setVisibility(View.VISIBLE);
		}
		updateLayoutParams((int) event.getRawY());
		getWindowManager().updateViewLayout(mDragImageView, mLayoutParams);
		
		// リスナーのドラッグ中の処理を呼び出す
		if (mDragListener != null) {
			mPositionFrom = mDragListener.onDuringDrag(mPositionFrom,
					pointToPosition(x, y));
		}
		return true;
	}

	/** ドラッグ終了 */
	private boolean stopDrag(MotionEvent event, boolean isDrop) {
		if (!mDragging) {
			return false;
		}
		
		// リスナーのドラッグ終了時の処理を呼び出す
		if (isDrop && mDragListener != null) {
			mDragListener.onStopDrag(mPositionFrom, eventToPosition(event));
		}
		
		// ドラッグ中を示すフラグを落とす
		mDragging = false;
		
		// 後処理
		if (mDragImageView != null) {
			getWindowManager().removeView(mDragImageView);
			mDragImageView = null;
			// リサイクルするとたまに死ぬけどタイミング分からない by vvakame
			// mDragBitmap.recycle();
			mDragBitmap = null;
			return true;
		}
		
		return false;
	}
	
	/** WindowManager の取得 */ 
	protected WindowManager getWindowManager() {
		return (WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE);
	}

	/** ImageView 用 LayoutParams の初期化 */
	protected void initLayoutParams(WindowManager.LayoutParams params) {
		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.height = WindowManager.LayoutParams.WRAP_CONTENT;
		params.width = WindowManager.LayoutParams.WRAP_CONTENT;
		params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		params.format = PixelFormat.TRANSLUCENT;
		params.windowAnimations = 0;
		params.x = getLeft();
		params.y = mDragStartRawY - mOffsetY - mStatusBarHeight + SHIFT_SIZE;
	}

	 /** ImageView 用 LayoutParams の座標情報を更新 */
	protected void updateLayoutParams(int y) {
		// ドラッグ開始が分かり易いように少しだけずらす
		mLayoutParams.y = y - mOffsetY - mStatusBarHeight + SHIFT_SIZE;
	}
	
	/** 上方向へのスクロール実行範囲にいるかを判定 */
	private boolean isInUpwardScrollRange(int y) {
		// 上側の閾値よりも上端に近ければスクロール範囲にいると判定
		final float upperThreshold = getHeight() * THRESHOLD_SPEED_SLOW;
		if (y < upperThreshold) {
			return true;
		} else {
			return false;
		}
	}
	
	/** 下方向へのスクロール実行範囲にいるかを判定 */
	private boolean isInDownwardScrollRange(int y) {
		// 下側の閾値よりも下端に近ければスクロール範囲にいると判定
		final float lowerThreshold = getHeight() * (1.0f - THRESHOLD_SPEED_SLOW);
		if (y > lowerThreshold) {
			return true;
		} else {
			return false;
		}
	}

	/** ドラッグイベントリスナーインターフェース */
	public interface DragListener {
		// ドラッグ開始時の処理
		public int onStartDrag(int position);

		// ドラッグ中の処理
		public int onDuringDrag(int positionFrom, int positionTo);

		// ドラッグ終了時の処理
		public boolean onStopDrag(int positionFrom, int positionTo);
	}

	/** ドラッグイベントリスナー実装 */
	public static class SimpleDragListener implements DragListener {
		// ドラッグ開始時の処理 
		@Override
		public int onStartDrag(int position) {
			return position;
		}

		// ドラッグ中の処理
		@Override
		public int onDuringDrag(int positionFrom, int positionTo) {
			return positionFrom;
		}

		// ドラッグ終了時の処理
		@Override
		public boolean onStopDrag(int positionFrom, int positionTo) {
			return positionFrom != positionTo && positionFrom >= 0 || positionTo >= 0;
		}
	}
}