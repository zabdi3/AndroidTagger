package com.example.assignmentfive;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class DrawableView extends View {


    private Paint paint = new Paint();
    private Path path = new Path();

    public DrawableView(Context context) {
        super(context);
    }


    public DrawableView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    public DrawableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public DrawableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Path object as a collection of zigzag lines.
        // We can say, any sketch (the whole drawing) essentially is one single Path
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5f);


       /*
       Note: Declare path somewhere outside onDraw
       Path path = new Path();
        */


        canvas.drawPath(path, p);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // “event” parameter contains the X and Y coordinates of where the user touched.
        // “event” parameter contains the type of touch, e.g., user’s finger is down, moving, or up.
        float x = event.getX(), y = event.getY();
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) { // ACTION_DOWN” event – we will move the Path to a new point.
            path.moveTo(x, y); //path is global. Same thing that onDraw uses.
        } else if (action == MotionEvent.ACTION_MOVE) { // “ACTION_MOVE” event – we will continue to add a new line segment to Path
            path.lineTo(x, y);
        }
        return true;
    }


    /*This bmp is declared outside globally in the custom view class get the drawing as a Bitmap,
     * Path object contains all the information about the drawing, we will create a
     * Bitmap object, use it to create a new canvas object, draw
     * the Path in this new canvas, then extract the bitmap out of this new canvas.*/
    Bitmap bmp;

    public Bitmap getBitmap() {  // converting to bitmap (saving file)
        bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        p.setStrokeWidth(5f);
        c.drawPath(path, p); //path is global. The very same thing that onDraw uses.
        return bmp;
    }


    // Clearing the drawing
    // path.reset() clears the points on the path
    public void resetPath() {
        path.reset();
    }
}
