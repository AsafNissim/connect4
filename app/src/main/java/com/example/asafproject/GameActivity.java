package com.example.asafproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity implements GeminiModule.CellStateProvider {

    public BoardGame boardGame;
    private LinearLayout ll;
    private GameMoudle gameMoudle;
    private GeminiModule geminiModule;

    private int mode;
    private TextView tvTurn;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        ll = findViewById(R.id.boardView);
        tvTurn = findViewById(R.id.tvTurn);
        Button btnRestart = findViewById(R.id.btnRestart);
        Button btnBack = findViewById(R.id.btnBack);

        gameMoudle = new GameMoudle();


        geminiModule = new GeminiModule();

        boardGame = new BoardGame(this);
        ll.addView(boardGame);


        boardGame.setGameMoudle(gameMoudle);   // TODO: 15/04/2026

        Intent intent = getIntent();
        mode = intent.getIntExtra(MainActivity.EXTRA_MODE, MainActivity.MODE_TWO_PLAYERS_RED);

        tvTurn.setText("תור: אדום");

        btnRestart.setOnClickListener(v -> {

            gameMoudle.reset();
            gameMoudle.setGameOver(false);
            boardGame.invalidate();
            tvTurn.setText("תור: אדום");
        });

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    public String boardToGeminiText() {
        return gameMoudle.boardToGeminiText();
    }

    /**
     * נקרא מ-BoardGame אחרי מהלך שחקן (בסוף האנימציה).
     */
    public void onDiskPlaced(Position placed) {
        if (placed == null) return;

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

        tvTurn.setText(gameMoudle.getCurrentPlayer() == Disk.Color.RED ? "תור: אדום" : "תור: צהוב");

        if (mode == MainActivity.MODE_TWO_PLAYERS_RED || mode == MainActivity.MODE_TWO_PLAYERS_YELLOW) return;
        if (gameMoudle.getCurrentPlayer() != Disk.Color.YELLOW) return;

        if (mode == MainActivity.MODE_EASY) {
            playEasyWithSmallDelay();
        } else if (mode == MainActivity.MODE_HARD) {
            playGeminiWithToastAndDelay();
        }
    }

    // -------- EASY --------
    private void playEasyWithSmallDelay() {
        if (gameMoudle.isGameOver()) return;

        Toast.makeText(this, "מחשב חושב...", Toast.LENGTH_SHORT).show();

        handler.postDelayed(() -> {
            Position aiPlaced = gameMoudle.aiMoveRandom();
            if (aiPlaced != null) {
                boardGame.animateDrop(aiPlaced, () -> afterAiMove(aiPlaced));
            }
        }, 800);
    }

    // -------- HARD (GEMINI) --------
    private void playGeminiWithToastAndDelay() {
        if (gameMoudle.isGameOver()) return;

        Toast.makeText(this, "Gemini חושב...", Toast.LENGTH_SHORT).show();

        geminiModule.requestHardMove(this, new GeminiModule.MoveCallback() {
            @Override
            public void onMove(int col) {
                handler.postDelayed(() -> {

                    Position aiPlaced = gameMoudle.dropDisk(col);

                    if (aiPlaced == null) {
                        Position fallback = gameMoudle.aiMoveHardLocal();
                        if (fallback != null) {
                            boardGame.animateDrop(fallback, () -> afterAiMove(fallback));
                        }
                        return;
                    }

                    boardGame.animateDrop(aiPlaced, () -> afterAiMove(aiPlaced));

                }, 3000);
            }

            @Override
            public void onError(String msg) {
                handler.postDelayed(() -> {

                    Position aiPlaced = gameMoudle.aiMoveHardLocal();
                    if (aiPlaced != null) {
                        boardGame.animateDrop(aiPlaced, () -> afterAiMove(aiPlaced));
                    }

                }, 3000);
            }
        });
    }

    // -------- אחרי מהלך מחשב --------
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

        tvTurn.setText(gameMoudle.getCurrentPlayer() == Disk.Color.RED ? "תור: אדום" : "תור: צהוב");
    }
}