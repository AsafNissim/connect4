package com.example.asafproject;

/**
 * GameMoudle - ה"מוח" או הלוגיקה של המשחק.
 * המחלקה הזו אחראית על ניהול מצב הלוח, בדיקת ניצחון, וביצוע מהלכי מחשב.
 * היא לא יודעת כלום על גרפיקה, רק על נתונים.
 */
public class GameMoudle {

    // קבועים לייצוג מצב הניצחון
    public static final int redWin = 0;
    public static final int yellowWin = 1;
    public static final int noWin = 2;

    // גודל הלוח הסטנדרטי של קונקט 4
    private static final int ROWS = 6;
    private static final int COLS = 7;

    // המערך הדו-ממדי שמייצג את הלוח הפיזי
    private final Disk.Color[][] board = new Disk.Color[6][7];
    
    // משתנה שעוקב אחרי מי השחקן שצריך לשחק עכשיו
    private Disk.Color currentPlayer = Disk.Color.RED;

    // פעולה בונה - מאתחלת את המשחק
    public GameMoudle() {
        reset();
    }

    /**
     * מאתחלת את הלוח - צובעת את כל התאים ב"ריק" וקובעת שהאדום מתחיל.
     */
    public void reset() {
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                board[r][c] = Disk.Color.EMPTY;
            }
        }
        currentPlayer = Disk.Color.RED;
    }

    // פונקציות עזר לקבלת נתוני הלוח
    public int getRows() { return ROWS; }
    public int getCols() { return COLS; }

    public Disk.Color getCurrentPlayer() {
        return currentPlayer;
    }

    // קבלת הצבע של תא ספציפי בלוח
    public Disk.Color getCellColor(int row, int col) {
        return board[row][col];
    }

    /**
     * הפעולה שמבצעת "נפילה" של דיסקית לתוך עמודה.
     * @param col העמודה אליה השחקן רוצה להכניס דיסקית.
     * @return אובייקט Position עם השורה והעמודה שבה הדיסקית נחתה, או null אם העמודה מלאה.
     */
    public Position dropDisk(int col) {
        // הגנה: בדיקה שהעמודה בטווח התקין
        if (col < 0 || col >= COLS) return null;

        // עוברים מהשורה התחתונה כלפי מעלה כדי למצוא את התא הריק הראשון
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col] == Disk.Color.EMPTY) {
                // מניחים את הדיסקית של השחקן הנוכחי
                board[row][col] = currentPlayer;

                Position placed = new Position(row, col);

                // החלפת תורות בין אדום לצהוב
                if (currentPlayer == Disk.Color.RED) {
                    currentPlayer = Disk.Color.YELLOW;
                } else {
                    currentPlayer = Disk.Color.RED;
                }

                return placed; // מחזירים את המיקום בו הדיסקית נעצרה
            }
        }
        return null; // אם הגענו לכאן, סימן שהעמודה מלאה לגמרי
    }

    /**
     * בדיקה האם המהלך האחרון הוביל לניצחון (4 בשירוה).
     * @param lastMove המיקום האחרון שבו הונחה דיסקית.
     * @return redWin / yellowWin / noWin.
     */
    public int isWin(Position lastMove) {
        if (lastMove == null) return noWin;

        int r = lastMove.getRow();
        int c = lastMove.getCol();
        Disk.Color color = board[r][c];
        if (color == Disk.Color.EMPTY) return noWin;

        // בדיקה ב-4 כיוונים: אופקי, אנכי, ו-2 אלכסונים
        // משתמשים בפונקציה count כדי לספור כמה דיסקיות מאותו צבע יש ברצף
        boolean win =
                count(r, c, 0, 1, color) + count(r, c, 0, -1, color) - 1 >= 4 || // אופקי
                count(r, c, 1, 0, color) + count(r, c, -1, 0, color) - 1 >= 4 || // אנכי
                count(r, c, 1, 1, color) + count(r, c, -1, -1, color) - 1 >= 4 || // אלכסון 1
                count(r, c, 1, -1, color) + count(r, c, -1, 1, color) - 1 >= 4;   // אלכסון 2

        if (!win) return noWin;
        // אם יש ניצחון, מחזירים את הצבע המנצח
        return (color == Disk.Color.RED) ? redWin : yellowWin;
    }

    /**
     * פונקציית עזר לספירת רצף של דיסקיות באותו צבע בכיוון מסוים.
     * @param dr שינוי בשורה (דלתא row)
     * @param dc שינוי בעמודה (דלתא col)
     */
    private int count(int r, int c, int dr, int dc, Disk.Color color) {
        int cnt = 0;
        // לולאה שממשיכה כל עוד אנחנו בתוך הלוח ורואים את אותו הצבע
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

    /**
     * מהלך מחשב ברמה קלה: בוחר עמודה פנויה בצורה אקראית לחלוטין.
     */
    public Position aiMoveRandom() {
        if (gameOver) return null;
        if (getCurrentPlayer() != Disk.Color.YELLOW) return null;

        // יצירת רשימה של כל העמודות שיש בהן מקום פנוי
        java.util.ArrayList<Integer> validCols = new java.util.ArrayList<>();
        for (int c = 0; c < getCols(); c++) {
            if (getCellColor(0, c) == Disk.Color.EMPTY) {
                validCols.add(c);
            }
        }

        if (validCols.isEmpty()) return null;

        // בחירת מספר אקראי מתוך רשימת העמודות הפנויות
        int col = validCols.get(new java.util.Random().nextInt(validCols.size()));
        return dropDisk(col);
    }

    /**
     * ממיר את הלוח הנוכחי למחרוזת טקסט כדי שה-Gemini AI יוכל לנתח את המצב.
     */
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

    /**
     * מהלך מחשב ברמה קשה (אלגוריתם מקומי):
     * 1. מחפש אם המחשב יכול לנצח במהלך הזה.
     * 2. מחפש אם השחקן עומד לנצח וחוסם אותו.
     * 3. מנסה לתפוס את המרכז (עמודה 3).
     * 4. מנסה עמודות קרובות למרכז.
     */
    public Position aiMoveHardLocal() {
        if (gameOver) return null;
        if (getCurrentPlayer() != Disk.Color.YELLOW) return null;

        // 1) בדיקה: האם צהוב (מחשב) יכול לנצח עכשיו?
        Integer winCol = findWinningCol(Disk.Color.YELLOW);
        if (winCol != null) return dropDisk(winCol);

        // 2) בדיקה: האם אדום (שחקן) עומד לנצח? אם כן, תחסום אותו!
        Integer blockCol = findWinningCol(Disk.Color.RED);
        if (blockCol != null) return dropDisk(blockCol);

        // 3) אסטרטגיה: עדיפות לעמודה המרכזית (עמודה 3)
        if (getCellColor(0, 3) == Disk.Color.EMPTY) {
            return dropDisk(3);
        }

        // 4) סדר עדיפויות של עמודות מהמרכז החוצה
        int[] order = {2, 4, 1, 5, 0, 6};
        for (int col : order) {
            if (getCellColor(0, col) == Disk.Color.EMPTY) {
                return dropDisk(col);
            }
        }

        return null;
    }

    /**
     * פונקציית עזר ל-AI: בודקת באופן וירטואלי (בלי להזיז באמת) אם הנחת דיסקית בעמודה מסוימת תנצח.
     */
    private Integer findWinningCol(Disk.Color color) {
        for (int col = 0; col < COLS; col++) {
            int row = getDropRow(col);
            if (row == -1) continue; // עמודה מלאה

            // "כאילו" מניחים את הדיסקית
            board[row][col] = color;
            boolean win = isWin(new Position(row, col)) != noWin;
            // מחזירים את המצב לקדמותו (Undo)
            board[row][col] = Disk.Color.EMPTY;

            if (win) return col; // מצאנו עמודה מנצחת
        }
        return null;
    }

    /**
     * מחזירה באיזו שורה תנחת דיסקית אם נזרוק אותה לעמודה מסוימת.
     * מחזירה -1 אם העמודה מלאה.
     */
    private int getDropRow(int col) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col] == Disk.Color.EMPTY) return row;
        }
        return -1;
    }
}
