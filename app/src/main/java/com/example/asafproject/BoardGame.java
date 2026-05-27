package com.example.asafproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

// המחלקה BoardGame אחראית על התצוגה הגרפית של הלוח וניהול האינטראקציה עם המשתמש
public class BoardGame extends View {

    private GameMoudle gameMoudle; // אובייקט הלוגיקה של המשחק

    private int cellW, cellH; // רוחב וגובה של כל תא בלוח

    private final Paint boardPaint = new Paint(); // מברשת לציור רקע הלוח הכחול
    private final Paint holePaint = new Paint();  // מברשת לציור החורים בלוח

    private Bitmap bmpRed, bmpYellow; // תמונות הדיסקיות (אדום וצהוב)
    private final Rect dstRect = new Rect(); // מלבן עזר לציור התמונות במיקום הנכון

    // משתני אנימציה
    private boolean animating = false; // דגל המציין האם מתבצעת כרגע אנימציית נפילה
    private int animCol, animRow;      // המיקום (עמודה ושורה) של הדיסקית הנופלת
    private float animY;               // מיקום ה-Y הנוכחי של הדיסקית בזמן האנימציה
    private Disk.Color animColor;      // צבע הדיסקית שנופלת כרגע
    private Context context;           // הקשר האפליקציה (Context)
    private int mode;                  // מצב המשחק (שחקן נגד שחקן, אונליין וכו')
    private int turn;                  // ניהול התור הנוכחי

    // פעולה בונה (Constructor) - מאתחלת את הלוח
    public BoardGame(Context c, int mode) {
        super(c);
        this.context = c;
        this.mode = mode;
        // אם זה משחק רשת, מעדכנים את Firebase שהמשחק התחיל (ערך 10 הוא קוד מוסכם)
        if(mode==MainActivity.MODE_TWO_PLAYERS_RED || mode==MainActivity.MODE_TWO_PLAYERS_YELLOW)
        {
            FB.getInstance(context).setPlay(10);
        }

        this.turn = MainActivity.MODE_TWO_PLAYERS_RED; // הגדרת תור התחלתי לאדום

        init(); // קריאה לפעולת האתחול של הצבעים
    }

    // אתחול המברשות (הגדרת צבעים והחלקה)
    private void init() {
        boardPaint.setARGB(255,20,90,200); // צבע כחול ללוח
        holePaint.setARGB(255,40,40,40);   // צבע אפור כהה לחורים
        boardPaint.setAntiAlias(true);      // הפעלת החלקת קצוות
        holePaint.setAntiAlias(true);
    }

    // חיבור אובייקט הלוגיקה של המשחק לרכיב הגרפי
    public void setGameMoudle(GameMoudle gm) {
        gameMoudle = gm;
        invalidate(); // קריאה לציור מחדש של המסך
    }

    // פעולה המבצעת את אנימציית נפילת הדיסקית
    public void animateDrop(final Position p, final Runnable end) {
        if (p == null) return;
        animCol = p.getCol();
        animRow = p.getRow();
        animColor = gameMoudle.getCellColor(animRow, animCol); // קבלת הצבע של הדיסקית שהונחה
        animating = true; // חסימת אפשרות ללחיצות נוספות בזמן האנימציה

        final float start = -cellH; // נקודת התחלה: מעל הלוח
        final float finish = animRow * cellH + cellH / 2f; // נקודת סיום: מרכז התא המיועד

        // יצירת Thread נפרד כדי להריץ את האנימציה בלי לתקוע את ה-UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                int steps = 25; // מספר שלבים באנימציה
                for (int i = 0; i <= steps; i++) {
                    // חישוב מיקום ה-Y הנוכחי
                    animY = start + (finish - start) * (i / (float) steps);
                    postInvalidate(); // בקשה לרענן את הציור מה-Thread
                    try {
                        Thread.sleep(10); // השהייה קצרה בין שלב לשלב
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                animating = false; // סיום האנימציה
                postInvalidate();  // ציור סופי של הלוח
                if (end != null) {
                    post(end); // הרצת פעולת הסיום (למשל בדיקת ניצחון ב-Activity)
                }
            }
        }).start();
    }

    // נקראת כאשר גודל התצוגה משתנה (למשל כשהמסך נטען)
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){
        super.onSizeChanged(w,h,ow,oh);
        if (gameMoudle==null) return;
        // חישוב גודל התא לפי רוחב/גובה המסך חלקי מספר העמודות/שורות
        cellW = w / gameMoudle.getCols();
        cellH = h / gameMoudle.getRows();

        // טעינת תמונות הדיסקיות מהמשאבים
        bmpRed = BitmapFactory.decodeResource(getResources(), R.drawable.img_3);
        bmpYellow = BitmapFactory.decodeResource(getResources(), R.drawable.img_2);

        // התאמת גודל התמונה לגודל התא (קצת פחות כדי שיהיה רווח)
        int s = Math.min(cellW,cellH)-14;
        bmpRed = Bitmap.createScaledBitmap(bmpRed,s,s,true);
        bmpYellow = Bitmap.createScaledBitmap(bmpYellow,s,s,true);
    }

    // פעולת הציור העיקרית - רצה בכל פעם שקוראים ל-invalidate()
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        if (gameMoudle==null) return;

        int rows = gameMoudle.getRows();
        int cols = gameMoudle.getCols();

        // 1. ציור הרקע של הלוח (המלבן הכחול)
        c.drawRect(0,0,getWidth(),getHeight(),boardPaint);

        // רדיוס החורים
        float r = Math.min(cellW,cellH)*0.38f;

        // 2. ציור החורים (עיגולים ריקים)
        for(int row=0;row<rows;row++){
            for(int col=0;col<cols;col++){
                float cx = col*cellW+cellW/2f;
                float cy = row*cellH+cellH/2f;
                c.drawCircle(cx,cy,r,holePaint);
            }
        }

        // 3. ציור הדיסקיות שכבר נמצאות בלוח
        for(int row=0;row<rows;row++){
            for(int col=0;col<cols;col++){

                // אם הדיסקית הזו כרגע באמצע אנימציה, נדלג עליה (היא תצויר בנפרד למטה)
                if(animating && row==animRow && col==animCol) continue;

                Disk.Color color = gameMoudle.getCellColor(row,col);
                if(color==Disk.Color.EMPTY) continue; // אם התא ריק, דלג

                // בחירת התמונה לפי צבע הדיסקית
                Bitmap bmp = (color==Disk.Color.RED)?bmpRed:bmpYellow;

                int cx = col*cellW+cellW/2;
                int cy = row*cellH+cellH/2;

                int hw = bmp.getWidth()/2;
                int hh = bmp.getHeight()/2;

                // הגדרת המיקום שבו התמונה תצויר
                dstRect.set(cx-hw,cy-hh,cx+hw,cy+hh);
                c.drawBitmap(bmp,null,dstRect,null);
            }
        }

        // 4. ציור הדיסקית שנמצאת באנימציית נפילה
        if (animating) {
            Bitmap bmp;
            if (animColor == Disk.Color.RED) {
                bmp = bmpRed;
            } else {
                bmp = bmpYellow;
            }

            int cx = animCol * cellW + cellW / 2;
            int hw = bmp.getWidth() / 2;
            int hh = bmp.getHeight() / 2;

            // המיקום משתנה בזמן אמת לפי animY שמחושב ב-Thread
            int left = cx - hw;
            int top = (int) (animY - hh);
            int right = cx + hw;
            int bottom = (int) (animY + hh);

            dstRect.set(left, top, right, bottom);
            c.drawBitmap(bmp, null, dstRect, null);
        }
    }

    // טיפול בנגיעות המשתמש על הלוח
    @Override public boolean onTouchEvent(MotionEvent e){
        // מגיבים רק ללחיצה ראשונית (ACTION_DOWN)
        if(e.getAction()!=MotionEvent.ACTION_DOWN)
            return true;

        // אם אין לוגיקה או שיש אנימציה פעילה, לא מאפשרים ללחוץ
        if(gameMoudle==null || animating)
            return true;

        // חישוב באיזו עמודה המשתמש לחץ
        int col=(int)(e.getX()/cellW);

        // אם מדובר במשחק רשת, בודקים אם זהו תורו של השחקן הנוכחי
        if(mode==MainActivity.MODE_TWO_PLAYERS_RED || mode==MainActivity.MODE_TWO_PLAYERS_YELLOW)
        {
            if(mode == turn)
            {
                // שליחת המהלך (העמודה) ל-Firebase
                FB.getInstance(context).setPlay(col);
            }
        }
        else
        {
            // שליחת המהלך ל-Firebase (במצבים אחרים)
            FB.getInstance(context).setPlay(col);
        }

        return true;
    }

    // פעולה שמקבלת מהלך חדש (למשל מה-Firebase כשהיריב משחק)
    public void newColFromFirebase(int col) {
        // ניסיון להניח דיסקית בעמודה שנתקבלה
        final Position p = gameMoudle.dropDisk(col);
        if(p == null) return; 

        // החלפת תורות
        if (turn == MainActivity.MODE_TWO_PLAYERS_RED) {
            turn = MainActivity.MODE_TWO_PLAYERS_YELLOW;
        } else {
            turn = MainActivity.MODE_TWO_PLAYERS_RED;
        }

        // פישוט: הפיכת המסך ל-GameActivity באופן ישיר
        final GameActivity myActivity = (GameActivity) getContext();

        // יצירת המשימה שתבוצע בסיום האנימציה
        Runnable onAnimationEnd = new Runnable() {
            @Override
            public void run() {
                myActivity.onDiskPlaced(p); // בדיקת ניצחון
            }
        };

        // הפעלת האנימציה
        animateDrop(p, onAnimationEnd);
    }

    // איפוס הלוח למצב התחלתי
    public void reset() {
        this.turn = MainActivity.MODE_TWO_PLAYERS_RED; // תור התחלתי לאדום
        invalidate(); // ציור מחדש
    }
}
