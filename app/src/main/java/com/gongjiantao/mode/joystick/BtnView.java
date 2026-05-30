package com.gongjiantao.mode.joystick;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.gongjiantao.mode.R;

public class BtnView extends LinearLayout {
    private BtnViewClickLsn lsn;
    private boolean ctr = true;
    private ImageButton bCtr;
    private boolean n;
    private ImageButton bN;
    private boolean s;
    private ImageButton bS;
    private boolean w;
    private ImageButton bW;
    private boolean e;
    private ImageButton bE;
    private boolean en;
    private ImageButton bEN;
    private boolean es;
    private ImageButton bES;
    private boolean wn;
    private ImageButton bWN;
    private boolean ws;
    private ImageButton bWS;
    private final Context ctx;

    public BtnView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ctx = context;
        LayoutInflater.from(context).inflate(R.layout.joystick_button, this);

        initBtns();
    }

    public BtnView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ctx = context;

        LayoutInflater.from(context).inflate(R.layout.joystick_button, this);
        initBtns();
    }

    public BtnView(Context context) {
        super(context);
        ctx = context;

        LayoutInflater.from(context).inflate(R.layout.joystick_button, this);

        initBtns();
    }

    private void initBtns() {
        bCtr = findViewById(R.id.btn_center);
        bCtr.setOnClickListener(view -> {
            if (!ctr) {
                ctr = true;
                bCtr.setImageResource(R.drawable.ic_lock_close);
                bCtr.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));
            } else {
                ctr = false;
                bCtr.setImageResource(R.drawable.ic_lock_open);
                bCtr.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));

                if (n) {
                    n = false;
                    bN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                }
                if (s) {
                    s = false;
                    bS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                }
                if (w) {
                    w = false;
                    bW.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                }
                if (e) {
                    e = false;
                    bE.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                }
                if (en) {
                    en = false;
                    bEN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                }
                if (es) {
                    es = false;
                    bES.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                }
                if (wn) {
                    wn = false;
                    bWN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                }
                if (ws) {
                    ws = false;
                    bWS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                }
                if (lsn != null) {
                    lsn.clickAngleInfo(false,0, 0);
                }
            }
        });
        /* 默认 */
        ctr = true;
        bCtr.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

        n = false;
        bN = findViewById(R.id.btn_north);
        bN.setOnClickListener(view -> {
            if (ctr) {
                if (!n) {
                    n = true;
                    bN.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

                    s = false;
                    bS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    w = false;
                    bW.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    e = false;
                    bE.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    en = false;
                    bEN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    es = false;
                    bES.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    wn = false;
                    bWN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    ws = false;
                    bWS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    if (lsn != null) {
                        lsn.clickAngleInfo(true,90, 1);
                    }
                } else {
                    n = false;
                    bN.setImageResource(R.drawable.ic_up);
                    if (lsn != null) {
                        lsn.clickAngleInfo(false,90, 0);
                    }
                }
            } else {
                if (lsn != null) {
                    lsn.clickAngleInfo(false,90, 1);
                }
            }
        });

        s = false;
        bS = findViewById(R.id.btn_south);
        bS.setOnClickListener(view -> {
            if (ctr) {
                if (!s) {
                    s = true;
                    bS.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

                    n = false;
                    bN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    w = false;
                    bW.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    e = false;
                    bE.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    en = false;
                    bEN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    es = false;
                    bES.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    wn = false;
                    bWN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    ws = false;
                    bWS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));

                    if (lsn != null) {
                        lsn.clickAngleInfo(true,270, 1);
                    }
                } else {
                    s = false;
                    bS.setImageResource(R.drawable.ic_down);
                    if (lsn != null) {
                        lsn.clickAngleInfo(false,270, 0);
                    }
                }
            } else {
                if (lsn != null) {
                    lsn.clickAngleInfo(false,270, 1);
                }
            }
        });

        w = false;
        bW = findViewById(R.id.btn_west);
        bW.setOnClickListener(view -> {
            if (ctr) {
                if (!w) {
                    w = true;
                    bW.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

                    s = false;
                    bS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    n = false;
                    bN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    e = false;
                    bE.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    en = false;
                    bEN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    es = false;
                    bES.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    wn = false;
                    bWN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    ws = false;
                    bWS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));

                    if (lsn != null) {
                        lsn.clickAngleInfo(true,180, 1);
                    }
                } else {
                    w = false;
                    bW.setImageResource(R.drawable.ic_left);
                    if (lsn != null) {
                        lsn.clickAngleInfo(false,180, 0);
                    }
                }
            } else {
                if (lsn != null) {
                    lsn.clickAngleInfo(false,180, 1);
                }
            }
        });

        e = false;
        bE = findViewById(R.id.btn_east);
        bE.setOnClickListener(view -> {
            if (ctr) {
                if (!e) {
                    e = true;
                    bE.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

                    s = false;
                    bS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    n = false;
                    bN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    w = false;
                    bW.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    en = false;
                    bEN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    es = false;
                    bES.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    wn = false;
                    bWN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    ws = false;
                    bWS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));

                    if (lsn != null) {
                        lsn.clickAngleInfo(true,0, 1);
                    }
                } else {
                    e = false;
                    bE.setImageResource(R.drawable.ic_right);
                    if (lsn != null) {
                        lsn.clickAngleInfo(false,0, 0);
                    }
                }
            } else {
                if (lsn != null) {
                    lsn.clickAngleInfo(false,0, 1);
                }
            }
        });

        en = false;
        bEN = findViewById(R.id.btn_north_east);
        bEN.setOnClickListener(view -> {
            if (ctr) {
                if (!en) {
                    en = true;
                    bEN.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

                    s = false;
                    bS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    n = false;
                    bN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    w = false;
                    bW.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    e = false;
                    bE.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    es = false;
                    bES.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    wn = false;
                    bWN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    ws = false;
                    bWS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));

                    if (lsn != null) {
                        lsn.clickAngleInfo(true,45, 1);
                    }
                } else {
                    en = false;
                    bEN.setImageResource(R.drawable.ic_right_up);
                    if (lsn != null) {
                        lsn.clickAngleInfo(false,45, 0);
                    }
                }
            } else {
                if (lsn != null) {
                    lsn.clickAngleInfo(false,45, 1);
                }
            }
        });

        es = false;
        bES = findViewById(R.id.btn_south_east);
        bES.setOnClickListener(view -> {
            if (ctr) {
                if (!es) {
                    es = true;
                    bES.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

                    s = false;
                    bS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    n = false;
                    bN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    w = false;
                    bW.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    e = false;
                    bE.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    en = false;
                    bEN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    wn = false;
                    bWN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    ws = false;
                    bWS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));

                    if (lsn != null) {
                        lsn.clickAngleInfo(true,315, 1);
                    }
                } else {
                    es = false;
                    bES.setImageResource(R.drawable.ic_right_down);
                    if (lsn != null) {
                        lsn.clickAngleInfo(false,315, 0);
                    }
                }
            } else {
                if (lsn != null) {
                    lsn.clickAngleInfo(false,315, 1);
                }
            }
        });

        wn = false;
        bWN = findViewById(R.id.btn_north_west);
        bWN.setOnClickListener(view -> {
            if (ctr) {
                if (!wn) {
                    wn = true;
                    bWN.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

                    s = false;
                    bS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    n = false;
                    bN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    w = false;
                    bW.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    e = false;
                    bE.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    en = false;
                    bEN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    es = false;
                    bES.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    ws = false;
                    bWS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));

                    if (lsn != null) {
                        lsn.clickAngleInfo(true,135, 1);
                    }
                } else {
                    wn = false;
                    bWN.setImageResource(R.drawable.ic_left_up);
                    if (lsn != null) {
                        lsn.clickAngleInfo(false,135, 0);
                    }
                }
            } else {
                if (lsn != null) {
                    lsn.clickAngleInfo(false,135, 1);
                }
            }
        });

        ws = false;
        bWS = findViewById(R.id.btn_south_west);
        bWS.setOnClickListener(view -> {
            if (ctr) {
                if (!ws) {
                    ws = true;
                    bWS.setColorFilter(getResources().getColor(R.color.colorAccent, ctx.getTheme()));

                    s = false;
                    bS.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    n = false;
                    bN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    w = false;
                    bW.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    e = false;
                    bE.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    en = false;
                    bEN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    es = false;
                    bES.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));
                    wn = false;
                    bWN.setColorFilter(getResources().getColor(R.color.gray, ctx.getTheme()));

                    if (lsn != null) {
                        lsn.clickAngleInfo(true,225, 1);
                    }
                } else {
                    ws = false;
                    bWS.setImageResource(R.drawable.ic_left_down);
                    if (lsn != null) {
                        lsn.clickAngleInfo(false,225, 0);
                    }
                }
            } else {
                if (lsn != null) {
                    lsn.clickAngleInfo(false,225, 1);
                }
            }
        });
    }

    public void setListener(BtnViewClickLsn lsn) {
        this.lsn = lsn;
    }

    public interface BtnViewClickLsn {
        /**
         * 点击的角度信息
         */
        void clickAngleInfo(boolean auto, double angle, double r);
    }
}
