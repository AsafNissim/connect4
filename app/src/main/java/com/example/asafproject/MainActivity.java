package com.example.asafproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity - מסך הבית של האפליקציה.
 * מאפשר למשתמש לבחור את מצב המשחק (נגד חבר או נגד המחשב) ולראות הוראות.
 */
public class MainActivity extends AppCompatActivity {

    // מפתח להעברת נתונים בין מסכים (Intents)
    public static final String EXTRA_MODE = "EXTRA_MODE";

    // הגדרת קבועים למצבי המשחק השונים
    public static final int MODE_TWO_PLAYERS_RED = 0;    // שחקן נגד שחקן - אדום
    public static final int MODE_TWO_PLAYERS_YELLOW = 1; // שחקן נגד שחקן - צהוב
    public static final int MODE_EASY = 2;               // נגד מחשב - רמה קלה
    public static final int MODE_HARD = 3;               // נגד מחשב - רמה קשה (AI)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // טעינת עיצוב המסך מקובץ ה-XML
        setContentView(R.layout.activity_main);

        // קישור הכפתורים מה-XML למשתנים בקוד
        Button btnTwoPlayersRed = findViewById(R.id.btnTwoPlayersRed);
        Button btnTwoPlayersYellow = findViewById(R.id.btnTwoPlayersYellow);
        Button btnEasy = findViewById(R.id.btnComputerEasy);
        Button btnHard = findViewById(R.id.btnComputerHard);
        Button btnInstructions = findViewById(R.id.btnInstructions);

        // הגדרת מאזין ללחיצה על כפתור שחקן נגד שחקן (אדום)
        btnTwoPlayersRed.setOnClickListener(v ->
                startGame(MODE_TWO_PLAYERS_RED)
        );

        // הגדרת מאזין ללחיצה על כפתור שחקן נגד שחקן (צהוב)
        btnTwoPlayersYellow.setOnClickListener(v ->
                startGame(MODE_TWO_PLAYERS_YELLOW)
        );

        // הגדרת מאזין למחשב ברמה קלה
        btnEasy.setOnClickListener(v ->
                startGame(MODE_EASY)
        );

        // הגדרת מאזין למחשב ברמה קשה (Gemini)
        btnHard.setOnClickListener(v ->
                startGame(MODE_HARD)
        );

        // הגדרת מאזין לכפתור ההוראות - מעבר למסך ההוראות
        btnInstructions.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, InstructionActivity.class);
            startActivity(i);
        });
    }

    /**
     * פונקציית עזר למעבר למסך המשחק עם המצב שנבחר.
     * @param mode המצב שנבחר על ידי המשתמש.
     */
    private void startGame(int mode) {
        Intent i = new Intent(MainActivity.this, GameActivity.class);
        // העברת המצב הנבחר כפרמטר ל-Activity הבא
        i.putExtra(EXTRA_MODE, mode);
        startActivity(i);
    }
}
