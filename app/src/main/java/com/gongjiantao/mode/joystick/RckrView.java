package com.gongjiantao.mode.joystick;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.gongjiantao.mode.R;

public class RckrView extends View {
    private Paint op;
    private Paint ip;
    private Paint iip;
    /** 内圆中心x坐标 */
    private float icx;
    /** 内圆中心y坐标 */
    private float icy;
    /** view中心点x坐标 */
    private float vcx;
    /** view中心点y左边 */
    private float vcy;
    /** 外圆半径 */
    private int ocr;
    /** 内圆半径 */
    private int icr;

    private Bitmap rbmp = null;
    private boolean auto = false;
    private boolean clk = false;

    private RckrClickLsn lsn;
    private final Context ctx;

    private Rect sr = null;
    private Rect dr = null;

    public RckrView(Context context) {
        super(context);
        ctx = context;
        init();
    }

    public RckrView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ctx = context;
        init();
    }

    private void init() {
        op = new Paint();
        op.setColor(ContextCompat.getColor(ctx, R.color.grey));
        op.setAlpha(180);
        op.setAntiAlias(true);

        ip = new Paint();
        ip.setColor(ContextCompat.getColor(ctx, R.color.lightgrey));
        ip.setAlpha(180);
        ip.setAntiAlias(true);

        iip = new Paint();
        iip.setAlpha(200);
        iip.setAntiAlias(true);
        iip.setFilterBitmap(true);

        auto = true;
        rbmp = sclBmp(getBitmap(getContext(), R.drawable.ic_lock_close));
        sr = new Rect(0, 0, rbmp.getWidth(), rbmp.getHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (dr == null) {      // 只需要测量一次即可
            int size = getMeasuredWidth();
            setMeasuredDimension(size, size);

            icx = (float)size / 2;
            icy = (float)size / 2;
            vcx = (float)size / 2;
            vcy = (float)size / 2;
            ocr = size / 2;
            icr = size / 5;
            dr = new Rect(
                    (int) (icx - rbmp.getWidth()),
                    (int) (icy - rbmp.getHeight()),
                    (int) (icx + rbmp.getWidth()),
                    (int) (icy + rbmp.getHeight()));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(vcx, vcy, ocr, op);
        /* 摇杆的控制部分由两部分组成 */
        canvas.drawCircle(icx, icy, icr, ip);
        canvas.drawBitmap(rbmp, sr, dr, iip);
    }

    @Override
    public boolean performClick() {
        // Calls the super implementation, which generates an AccessibilityEvent
        // and calls the onClick() listener on the view, if any
        super.performClick();

        // Handle the action for the custom click here

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                /* 如果初始点击位置 不再内圆中,返回 false 将不再继续处理后续事件 */
                if (event.getX() < icx - icr || event.getX() > icx + icr
                || event.getY() < icy - icr || event.getY() > icy + icr)
                {
                    return true;
                }
                clk = true;
                break;
            case MotionEvent.ACTION_MOVE:
                mv(event.getX(), event.getY());
                clk = false;
                break;
            case MotionEvent.ACTION_UP:
                if (clk) {
                    clk = false;
                    togLock();
                    invalidate();
                }
                if (!auto) {
                    mv(vcx, vcy);
                }
                performClick();
                break;
        }

        return true;
    }

    private void mv(float x, float y) {
        float distance = (float) Math.sqrt(Math.pow(x-vcx, 2) + Math.pow(y-vcy, 2)); //触摸点与view中心距离

        if (distance < ocr-icr) {
            //在自由域之内，触摸点实时作为内圆圆心
            icx = x;
            icy = y;
        } else {
            //在自由域之外，内圆圆心在触摸点与外圆圆心的线段上
            int innerDistance = ocr-icr;  //内圆圆心到中心点距离
            //相似三角形的性质，两个相似三角形各边比例相等得到等式
            icx = (x-vcx)*innerDistance/distance + vcx;
            icy = (y-vcy)*innerDistance/distance + vcy;
        }

        dr = new Rect(
                (int) (icx - rbmp.getWidth()),
                (int) (icy - rbmp.getHeight()),
                (int) (icx + rbmp.getWidth()),
                (int) (icy + rbmp.getHeight()));

        invalidate();
        double angle = Math.toDegrees(Math.atan2((icx - vcx), (icy-vcy))) - 90;
        double r = Math.sqrt(Math.pow(icx - vcx, 2) + Math.pow(icy-vcy, 2)) / (ocr-icr);
        lsn.onAngle(true, angle, r);
    }

    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    private static Bitmap getBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return BitmapFactory.decodeResource(context.getResources(), drawableId);
        } else if (drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    private Bitmap sclBmp(Bitmap bitmap) {
        Matrix mMatrix = new Matrix();
        mMatrix.postScale(0.45f,0.45f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mMatrix,true);
    }

    private void togLock() {
        Bitmap bitmap;

        auto = !auto;

        if (auto) {
            bitmap = getBitmap(getContext(), R.drawable.ic_lock_close);
        } else {
            bitmap = getBitmap(getContext(), R.drawable.ic_lock_open);
        }
        
        if (rbmp != null) {
            rbmp.recycle();
        }
        rbmp = sclBmp(bitmap);

        dr = new Rect(
                (int) (icx - rbmp.getWidth()),
                (int) (icy - rbmp.getHeight()),
                (int) (icx + rbmp.getWidth()),
                (int) (icy + rbmp.getHeight()));
    }

    public void setListener(RckrClickLsn lsn) {
        this.lsn = lsn;
    }

    public interface RckrClickLsn {
        /**
         * 点击的角度信息
         */
        void onAngle(boolean auto, double angle, double r);
    }
}
