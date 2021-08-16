package com.example.wavemockapplication;

public class Point {

    public float x;
    public float y;

    public Point(float cx, float cy) {
        x = cx;
        y = cy;
    }

    @Override
    public String toString() {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
