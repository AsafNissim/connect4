package com.example.asafproject;

public class GameMoudle {

    public static final int redWin = 0;
    public static final int yellowWin = 1;
    public static final int noWin = 2;

    private static final int ROWS = 6;
    private static final int COLS = 7;

    private final Disk.Color[][] board = new Disk.Color[6][7];
    private Disk.Color currentPlayer = Disk.Color.RED;

    public GameMoudle() {
        reset();

    }

    public void reset() {
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                board[r][c] = Disk.Color.EMPTY;
            }
        }
        currentPlayer = Disk.Color.RED;

    }

    public int getRows() { return ROWS; }
    public int getCols() { return COLS; }

    public Disk.Color getCurrentPlayer() {
        return currentPlayer;
    }

/*    public int getCurrentPlayerInInt() {
        if (currentPlayer == Disk.Color.RED) {
            return 0;
        }
        else
            return 1;
    }*/

    public Disk.Color getCellColor(int row, int col) {
        return board[row][col];
    }

    // מפיל דיסק בעמודה ומחזיר Position של הנחיתה. אם העמודה מלאה מחזיר null.
    public Position dropDisk(int col) {
        if (col < 0 || col >= COLS) return null;

        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col] == Disk.Color.EMPTY) {
                board[row][col] = currentPlayer;

                Position placed = new Position(row, col);

                // החלפת תור
                if (currentPlayer == Disk.Color.RED) {
                    currentPlayer = Disk.Color.YELLOW;
                } else {
                    currentPlayer = Disk.Color.RED;
                }

                return placed;
            }
        }
        return null; // עמודה מלאה
    }

    // מחזיר redWin / yellowWin / noWin
    public int isWin(Position lastMove) {
        if (lastMove == null) return noWin;

        int r = lastMove.getRow();
        int c = lastMove.getCol();
        Disk.Color color = board[r][c];
        if (color == Disk.Color.EMPTY) return noWin;

        boolean win =
                count(r, c, 0, 1, color) + count(r, c, 0, -1, color) - 1 >= 4 ||
                        count(r, c, 1, 0, color) + count(r, c, -1, 0, color) - 1 >= 4 ||
                        count(r, c, 1, 1, color) + count(r, c, -1, -1, color) - 1 >= 4 ||
                        count(r, c, 1, -1, color) + count(r, c, -1, 1, color) - 1 >= 4;

        if (!win) {
            return noWin;
        }

        if (color == Disk.Color.RED) {
            return redWin;
        } else {
            return yellowWin;
        }
    }

    private int count(int r, int c, int dr, int dc, Disk.Color color) {
        int cnt = 0;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == color) {
            cnt++;
            r += dr;
            c += dc;
        }
        return cnt;
    }

    private boolean gameOver = false;

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    // מחשב קל: רנדום
    public Position aiMoveRandom() {
        if (gameOver) return null;
        if (getCurrentPlayer() != Disk.Color.YELLOW) return null;

        java.util.ArrayList<Integer> validCols = new java.util.ArrayList<>();
        for (int c = 0; c < getCols(); c++) {
            if (getCellColor(0, c) == Disk.Color.EMPTY) {
                validCols.add(c);
            }
        }

        if (validCols.isEmpty()) return null;

        int col = validCols.get(new java.util.Random().nextInt(validCols.size()));
        return dropDisk(col);
    }

    // טקסט ל-Gemini
    public String boardToGeminiText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Board 6x7 (row 0 is top):\n");
        for (int r = 0; r < getRows(); r++) {
            for (int c = 0; c < getCols(); c++) {
                Disk.Color col = getCellColor(r, c);
                if (col == Disk.Color.RED) sb.append("R ");
                else if (col == Disk.Color.YELLOW) sb.append("Y ");
                else sb.append(". ");
            }
            sb.append("\n");
        }
        sb.append("Columns are 0..6.\n");
        return sb.toString();
    }

    // ================= HARD LOCAL SIMPLE =================
    public Position aiMoveHardLocal() {
        if (gameOver) return null;
        if (getCurrentPlayer() != Disk.Color.YELLOW) return null;

        // 1) ניצחון מיידי לצהוב
        Integer winCol = findWinningCol(Disk.Color.YELLOW);
        if (winCol != null) return dropDisk(winCol);

        // 2) חסימה מיידית לאדום
        Integer blockCol = findWinningCol(Disk.Color.RED);
        if (blockCol != null) return dropDisk(blockCol);

        // 3) מרכז אם פנוי
        if (getCellColor(0, 3) == Disk.Color.EMPTY) {
            return dropDisk(3);
        }

        // 4) אחרת קרוב למרכז
        int[] order = {2, 4, 1, 5, 0, 6};
        for (int col : order) {
            if (getCellColor(0, col) == Disk.Color.EMPTY) {
                return dropDisk(col);
            }
        }

        return null;
    }

    // מחפש עמודה שמנצחת לצבע
    private Integer findWinningCol(Disk.Color color) {
        for (int col = 0; col < COLS; col++) {
            int row = getDropRow(col);
            if (row == -1) continue;

            board[row][col] = color;
            boolean win = isWin(new Position(row, col)) != noWin;
            board[row][col] = Disk.Color.EMPTY;

            if (win) return col;
        }
        return null;
    }

    // איפה הדיסק ינחת
    private int getDropRow(int col) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col] == Disk.Color.EMPTY) return row;
        }
        return -1;
    }
}
