package com.example.asafproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * GameActivity היא המחלקה שמנהלת את מסך המשחק.
 * היא מחברת בין הלוח הגרפי (BoardGame), לבין הלוגיקה (GameMoudle) והבינה המלאכותית (GeminiModule).
 */
public class GameActivity extends AppCompatActivity implements GeminiModule.CellStateProvider {

    public BoardGame boardGame;         // רכיב הלוח הגרפי
    private LinearLayout ll;            // הקונטיינר (Layout) שיכיל את הלוח
    private GameMoudle gameMoudle;      // המוח של המשחק (לוגיקה)
    private GeminiModule geminiModule;  // רכיב הבינה המלאכותית (Gemini)

    private int mode;                   // מצב המשחק (קל, קשה, שני שחקנים וכו')
    private TextView tvTurn;            // טקסט המציג למי התור

    // Handler משמש להרצת קוד בעיכוב (Delay) על ה-UI Thread
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game); // טעינת עיצוב המסך

        // קישור משתנים לרכיבי ה-XML
        ll = findViewById(R.id.boardView);
        tvTurn = findViewById(R.id.tvTurn);
        Button btnRestart = findViewById(R.id.btnRestart);
        Button btnBack = findViewById(R.id.btnBack);

        // קבלת מצב המשחק מה-Intent שנשלח מהמסך הקודם
        Intent intent = getIntent();
        mode = intent.getIntExtra(MainActivity.EXTRA_MODE, MainActivity.MODE_TWO_PLAYERS_RED);

        // יצירת האובייקטים של הלוגיקה והבינה המלאכותית
        gameMoudle = new GameMoudle();
        geminiModule = new GeminiModule();

        // יצירת הלוח הגרפי והוספתו למסך
        boardGame = new BoardGame(this, mode);
        ll.addView(boardGame);

        // חיבור המוח (Lógic) ללוח (View)
        boardGame.setGameMoudle(gameMoudle);

        updateTurnUI(); // עדכון ראשוני של הטקסט המציג את התור

        // כפתור איפוס משחק
        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameMoudle.reset();        // איפוס נתוני המשחק במוח
                gameMoudle.setGameOver(false);
                boardGame.reset();         // איפוס התצוגה הגרפית
                updateTurnUI();            // עדכון הטקסט
            }
        });

        // כפתור חזרה למסך הבית
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // סגירת ה-Activity
            }
        });
    }

    /**
     * ממיר את מצב הלוח לטקסט שה-Gemini יכול להבין.
     * מיושם בגלל ה-Interface של GeminiModule.
     */
    @Override
    public String boardToGeminiText() {
        return gameMoudle.boardToGeminiText();
    }

    /**
     * מעדכן את ה-TextView שמציג למי התור ואיזה שחקן המשתמש.
     */
    private void updateTurnUI() {
        String turn;
        // קביעת טקסט לפי הצבע שהמשתמש בחר
        if(mode == MainActivity.MODE_TWO_PLAYERS_RED)
            turn = "אני אדום";
        else
            turn = "אני צהוב";

        // בדיקה מי התור הנוכחי במוח של המשחק
        if (gameMoudle.getCurrentPlayer() == Disk.Color.RED)
        {
            tvTurn.setText(turn +"  תור: אדום  ");
        } else
        {
            tvTurn.setText(turn +"  תור: צהוב  ");
        }
    }

    /**
     * פעולה זו נקראת מה-BoardGame בכל פעם שדיסקית מסיימת ליפול (בסוף האנימציה).
     */
    public void onDiskPlaced(Position placed) {
        if (placed == null) return;

        // בדיקה האם המהלך האחרון הוביל לניצחון
        int win = gameMoudle.isWin(placed);
        if (win == GameMoudle.redWin) {
            Toast.makeText(this, "🔴 אדום ניצח!", Toast.LENGTH_LONG).show();
            gameMoudle.setGameOver(true);
            return;
        }
        if (win == GameMoudle.yellowWin) {
            Toast.makeText(this, "🟡 צהוב ניצח!", Toast.LENGTH_LONG).show();
            gameMoudle.setGameOver(true);
            return;
        }

        updateTurnUI(); // עדכון התצוגה לאחר המהלך

        // אם זה משחק נגד שחקן אחר (אופליין או אונליין), עוצרים כאן
        if (mode == MainActivity.MODE_TWO_PLAYERS_RED || mode == MainActivity.MODE_TWO_PLAYERS_YELLOW) return;
        
        // אם התור עכשיו הוא של המחשב (צהוב)
        if (gameMoudle.getCurrentPlayer() != Disk.Color.YELLOW) return;

        // הפעלת הבינה המלאכותית לפי רמת הקושי
        if (mode == MainActivity.MODE_EASY) {
            playEasyWithSmallDelay(); // רמה קלה - מהלך אקראי
        } else if (mode == MainActivity.MODE_HARD) {
            playGeminiWithToastAndDelay(); // רמה קשה - שימוש ב-AI (Gemini)
        }
    }

    /**
     * ניהול מהלך המחשב ברמה קלה (אקראי).
     */
    private void playEasyWithSmallDelay() {
        if (gameMoudle.isGameOver()) return;

        Toast.makeText(this, "מחשב חושב...", Toast.LENGTH_SHORT).show();

        // השהייה של 800 מילי-שניות כדי שזה ייראה "אנושי"
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final Position aiPlaced = gameMoudle.aiMoveRandom(); // בחירת מהלך אקראי
                if (aiPlaced != null) {
                    // הפעלת האנימציה של הנפילה עבור מהלך המחשב
                    boardGame.animateDrop(aiPlaced, new Runnable() {
                        @Override
                        public void run() {
                            afterAiMove(aiPlaced); // בדיקת ניצחון אחרי המהלך
                        }
                    });
                }
            }
        }, 800);
    }

    /**
     * ניהול מהלך המחשב ברמה קשה (Gemini AI).
     */
    private void playGeminiWithToastAndDelay() {
        if (gameMoudle.isGameOver()) return;

        Toast.makeText(this, "Gemini חושב...", Toast.LENGTH_SHORT).show();

        // בקשת מהלך מה-Gemini
        geminiModule.requestHardMove(this, new GeminiModule.MoveCallback() {
            @Override
            public void onMove(final int col) {
                // לאחר קבלת התשובה מה-AI, מחכים קצת ואז מבצעים
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Position aiPlaced = gameMoudle.dropDisk(col); // ביצוע המהלך בלוגיקה

                        // אם העמודה ש-Gemini בחר מלאה בטעות, נשתמש באלגוריתם מקומי כגיבוי
                        if (aiPlaced == null) {
                            final Position fallback = gameMoudle.aiMoveHardLocal();
                            if (fallback != null) {
                                boardGame.animateDrop(fallback, new Runnable() {
                                    @Override
                                    public void run() {
                                        afterAiMove(fallback);
                                    }
                                });
                            }
                            return;
                        }

                        // אנימציה של המהלך ש-Gemini בחר
                        boardGame.animateDrop(aiPlaced, new Runnable() {
                            @Override
                            public void run() {
                                afterAiMove(aiPlaced);
                            }
                        });
                    }
                }, 3000); // השהייה של 3 שניות
            }

            @Override
            public void onError(String msg) {
                // במקרה של שגיאת אינטרנט/שרת, עוברים למנגינון גיבוי מקומי
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Position aiPlaced = gameMoudle.aiMoveHardLocal();
                        if (aiPlaced != null) {
                            boardGame.animateDrop(aiPlaced, new Runnable() {
                                @Override
                                public void run() {
                                    afterAiMove(aiPlaced);
                                }
                            });
                        }
                    }
                }, 3000);
            }
        });
    }

    /**
     * פעולת עזר שבודקת ניצחון ומעדכנת UI אחרי שהמחשב סיים את המהלך שלו.
     */
    private void afterAiMove(Position aiPlaced) {
        int win = gameMoudle.isWin(aiPlaced);

        if (win == GameMoudle.redWin) {
            Toast.makeText(this, "🔴 אדום ניצח!", Toast.LENGTH_LONG).show();
            gameMoudle.setGameOver(true);
            return;
        }
        if (win == GameMoudle.yellowWin) {
            Toast.makeText(this, "🟡 צהוב ניצח!", Toast.LENGTH_LONG).show();
            gameMoudle.setGameOver(true);
            return;
        }

        updateTurnUI(); // עדכון התור חזרה לשחקן האנושי
    }
}
