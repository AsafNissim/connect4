package com.example.asafproject;

/**
 * מחלקה פשוטה המייצגת מיקום (קואורדינטה) על הלוח.
 * משמשת להעברת מידע על שורה ועמודה בין הלוגיקה לתצוגה.
 */
public class Position {

    private int row; // מספר השורה (0-5)
    private int col; // מספר העמודה (0-6)

    // פעולה בונה המקבלת שורה ועמודה ומאחסנת אותן
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    // מחזירה את מספר השורה
    public int getRow() {
        return row;
    }

    // מחזירה את מספר העמודה
    public int getCol() {
        return col;
    }
}