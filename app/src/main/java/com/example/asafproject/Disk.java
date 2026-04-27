package com.example.asafproject;


public class Disk {

    public enum Color {
        RED,
        YELLOW,
        EMPTY
    }

    private Color color;

    public Disk(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isEmpty() {
        return color == Color.EMPTY;
    }
}
