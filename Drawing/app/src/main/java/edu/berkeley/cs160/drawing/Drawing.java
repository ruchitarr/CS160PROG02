package edu.berkeley.cs160.drawing;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.graphics.Color;


/**
 * Created by ruchitarathi on 10/5/14.
 */
public class Drawing extends View {
    //drawing path
    private Path drawPath;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint;
    //initial color
    private int paintColor = 0xFF660000;
    //canvas
    private Canvas drawCanvas;
    //canvas bitmap
    private Bitmap canvasBitmap;
    //check if the user is currently erasing or not
    private boolean erase = false;

    //check if the user is currently using Circle brush
    private boolean isCircleBrush = false;
    //for brush sizes
    private int brushSize, lastBrushSize;
    //
    private String shapeType="Dot";

    public Drawing(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    public void setShapeType(String setShape){
        shapeType = setShape;
    }

    public String getShapeType(){
        return shapeType;
    }

    private void setupDrawing() {
        //get drawing area setup for interaction
        drawPath = new Path();
        drawPaint = new Paint();
        //set initial color
        drawPaint.setColor(paintColor);
        //set initial path properties
        //Antialias and StrokeCap for smooth drawing
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        brushSize = getResources().getInteger(R.integer.medium_size);
        lastBrushSize = brushSize;

        //instantiate canvas Paint object
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    //Override onSizeChanged method
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//view given size
        super.onSizeChanged(w, h, oldw, oldh);
        //instantiate drawing Canvas and bitmap
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);


    }

    //Override onDraw method to ensure that DrawingView class works as a custom drawing View
    @Override
    protected void onDraw(Canvas canvas) {
//draw view
        //draw canvas and drawing path
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//detect user touch
        //retrieve x and y positions on touch
      // Log.v(event.getButtonState());
        float touchX = event.getX();
        float touchY = event.getY();
        //MotionEvent parameter to respond to touch events

            if (getShapeType().matches("Circle")){
                drawCanvas.drawCircle(touchX, touchY, 50, drawPaint);
            }
            else {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        drawPath.moveTo(touchX, touchY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        drawPath.lineTo(touchX, touchY);
                        break;
                    case MotionEvent.ACTION_UP:
                        drawCanvas.drawPath(drawPath, drawPaint);
                        drawPath.reset();
                        break;
                    default:
                        return false;
                }
            }

        //invalidate view - Calling invalidate causes execution of onDraw method.
        invalidate();
        return true;
    }

    //allow user to toggle between colors
    public void setColor(String newColor) {
//set color
        invalidate();
        //parse and set color for drawing
        paintColor = Color.parseColor(newColor);
        drawPaint.setColor(paintColor);

    }


    //check if the user is erasing
    public void setErase(boolean isErase) {
//set erase true or false
        erase = isErase;
        if (erase) drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        else drawPaint.setXfermode(null);
    }

    //for brush size
    public void setBrushSize(int newSize) {
//update size
        int pixelAmount = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, getResources().getDisplayMetrics());
        brushSize = pixelAmount;
        drawPaint.setStrokeWidth(brushSize);


    }

    public void startNew(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }

    public void setLastBrushSize(int lastSize){
        lastBrushSize=lastSize;
    }
    public int getLastBrushSize(){
        return lastBrushSize;
    }

    public int getBrushSize(){
        return brushSize;
    }


}
