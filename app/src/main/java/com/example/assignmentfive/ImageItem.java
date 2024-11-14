package com.example.assignmentfive;

import android.graphics.Bitmap;

public class ImageItem {
    private int imageResource;
    private Bitmap imageBitmap;
    private String tagtext;
    private boolean isChecked;
    public ImageItem(Bitmap imageBitmap, String tagtext) {
        this.imageBitmap = imageBitmap;
        //this.visionname = visionname;
        this.tagtext = tagtext;
    }

    public int getImageResource() {
        return imageResource;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public String getTagText() { return tagtext; }

    public boolean isChecked() { return isChecked; }

    public void setChecked(boolean checked) { isChecked = checked; }

    public  String getOnlyTagText() { // method for getting tags (without the date)
        int stoppingpoint = tagtext.indexOf("\n");
        String newtext = tagtext.substring(0,stoppingpoint);
        return newtext;
    }
}
