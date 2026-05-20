package com.example.asafproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

public class BoardGame extends View {

    private GameMoudle gameMoudle;

    private int cellW, cellH;

    private final Paint boardPaint = new Paint();
    private final Paint holePaint = new Paint();

    private Bitmap bmpRed, bmpYellow;
    private final Rect dstRect = new Rect();

    // animation
    private boolean animating = false;
    private int animCol, animRow;
    private float animY;
    private Disk.Color animColor;
    private Context context;
    private int mode;
    private int turn;

    public BoardGame(Context c, int mode) {
        super(c);
        this.context = c;
        this.mode = mode;
        if(mode==MainActivity.MODE_TWO_PLAYERS_RED || mode==MainActivity.MODE_TWO_PLAYERS_YELLOW)
        {
            FB.getInstance(context).setPlay(10);
        }
        this.turn = mode;
        this.turn = 0;
        this.turn = MainActivity.MODE_TWO_PLAYERS_RED;

        init();
    }

    private void init() {
        boardPaint.setARGB(255,20,90,200);
        holePaint.setARGB(255,40,40,40);
        boardPaint.setAntiAlias(true);
        holePaint.setAntiAlias(true);
    }

    public void setGameMoudle(GameMoudle gm) {
        gameMoudle = gm;
        invalidate();
    }

    public void animateDrop(final Position p, final Runnable end) {
        if (p == null) return;
        animCol = p.getCol();
        animRow = p.getRow();
        animColor = gameMoudle.getCellColor(animRow, animCol);
        animating = true;

        final float start = -cellH;
        final float finish = animRow * cellH + cellH / 2f;

        new Thread(new Runnable() {
            @Override
            public void run() {
                int steps = 25;
                for (int i = 0; i <= steps; i++) {
                    animY = start + (finish - start) * (i / (float) steps);
                    postInvalidate();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                animating = false;
                postInvalidate();
                if (end != null) {
                    post(end);
                }
            }
        }).start();
    }

    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        super.onSizeChanged(w,h,ow,oh);
        if (gameMoudle==null) return;
        cellW = w / gameMoudle.getCols();
        cellH = h / gameMoudle.getRows();

        bmpRed = BitmapFactory.decodeResource(getResources(), R.drawable.img_3);
        bmpYellow = BitmapFactory.decodeResource(getResources(), R.drawable.img_2);

        int s = Math.min(cellW,cellH)-14;
        bmpRed = Bitmap.createScaledBitmap(bmpRed,s,s,true);
        bmpYellow = Bitmap.createScaledBitmap(bmpYellow,s,s,true);
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if (gameMoudle==null) return;

        int rows = gameMoudle.getRows();
        int cols = gameMoudle.getCols();

        c.drawRect(0,0,getWidth(),getHeight(),boardPaint);

        float r = Math.min(cellW,cellH)*0.38f;

        for(int row=0;row<rows;row++){
            for(int col=0;col<cols;col++){
                float cx = col*cellW+cellW/2f;
                float cy = row*cellH+cellH/2f;
                c.drawCircle(cx,cy,r,holePaint);
            }
        }

        // draw disks
        for(int row=0;row<rows;row++){
            for(int col=0;col<cols;col++){

                if(animating && row==animRow && col==animCol) continue;

                Disk.Color color = gameMoudle.getCellColor(row,col);
                if(color==Disk.Color.EMPTY) continue;

                Bitmap bmp = (color==Disk.Color.RED)?bmpRed:bmpYellow;

                int cx = col*cellW+cellW/2;
                int cy = row*cellH+cellH/2;

                int hw = bmp.getWidth()/2;
                int hh = bmp.getHeight()/2;

                dstRect.set(cx-hw,cy-hh,cx+hw,cy+hh);
                c.drawBitmap(bmp,null,dstRect,null);
            }
        }

        // anim disk
        if(animating){
            Bitmap bmp = (animColor==Disk.Color.RED)?bmpRed:bmpYellow;
            int cx = animCol*cellW+cellW/2;

            int hw=bmp.getWidth()/2;
            int hh=bmp.getHeight()/2;

            dstRect.set(cx-hw,(int)(animY-hh),cx+hw,(int)(animY+hh));
            c.drawBitmap(bmp,null,dstRect,null);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()!=MotionEvent.ACTION_DOWN)
            return true;

        if(gameMoudle==null || animating)
            return true;

        int col=(int)(e.getX()/cellW);

        if(mode==MainActivity.MODE_TWO_PLAYERS_RED || mode==MainActivity.MODE_TWO_PLAYERS_YELLOW)
        {
            if(mode == turn)
            {
                FB.getInstance(context).setPlay(col);
            }
        }
        else
        {
            FB.getInstance(context).setPlay(col);
        }

        return true;
    }

    public void newColFromFirebase(int col) {
        final Position p = gameMoudle.dropDisk(col);
        if(p==null) return;

        if(turn == MainActivity.MODE_TWO_PLAYERS_RED)
        {
            turn = MainActivity.MODE_TWO_PLAYERS_YELLOW;
        }
        else
            turn = MainActivity.MODE_TWO_PLAYERS_RED;

        if(getContext() instanceof GameActivity){
            animateDrop(p, new Runnable() {
                @Override
                public void run() {
                    ((GameActivity)getContext()).onDiskPlaced(p);
                }
            });
        } else invalidate();
    }

    public void reset() {
        this.turn = MainActivity.MODE_TWO_PLAYERS_RED;
        invalidate();
    }
}