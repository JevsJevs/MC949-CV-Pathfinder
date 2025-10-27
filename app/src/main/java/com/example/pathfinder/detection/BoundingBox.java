package com.example.pathfinder.detection;

public class BoundingBox {
    float x1;
    float y1;
    float x2;
    float y2;
    float cx;
    float cy;
    float h;
    float w;
    float cnf;
    float cls;
    String clsName;

    public BoundingBox(float x1, float y1, float x2, float y2, float cx, float cy, float h, float w, float cnf, float cls, String clsName) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.cx = cx;
        this.cy = cy;
        this.h = h;
        this.w = w;
        this.cnf = cnf;
        this.cls = cls;
        this.clsName = clsName;
    }

    public BoundingBox(float cx, float cy, float h, float w, float cnf, float cls, String clsName) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        var x1 = cx - (w/2F);
        var y1 = cy - (h/2F);
        var x2 = cx + (w/2F);
        var y2 = cy + (h/2F);
        this.cx = cx;
        this.cy = cy;
        this.h = h;
        this.w = w;
        this.cnf = cnf;
        this.cls = cls;
        this.clsName = clsName;
    }
}
