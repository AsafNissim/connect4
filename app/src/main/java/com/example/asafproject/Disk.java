package com.example.asafproject;

/**
 * מחלקה המייצגת דיסקית בודדת בלוח המשחק.
 * הדיסקית מחזיקה את הצבע שלה (אדום, צהוב או ריק).
 */
public class Disk {

    // Enum המגדיר את הצבעים האפשריים של דיסקית
    public enum Color {
        RED,    // שחקן אדום
        YELLOW, // שחקן צהוב
        EMPTY   // תא ריק בלוח
    }

    private Color color; // משתנה לשמירת הצבע הנוכחי של הדיסקית

    // פעולה בונה המקבלת צבע ומאתחלת את הדיסקית
    public Disk(Color color) {
        this.color = color;
    }

    // מחזירה את הצבע של הדיסקית
    public Color getColor() {
        return color;
    }

    // מאפשרת לשנות את הצבע של הדיסקית
    public void setColor(Color color) {
        this.color = color;
    }

    // פעולת עזר הבודקת אם הדיסקית מייצגת תא ריק
    public boolean isEmpty() {
        return color == Color.EMPTY;
    }
}
