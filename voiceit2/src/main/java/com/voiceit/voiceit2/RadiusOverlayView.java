package com.voiceit.voiceit2;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.nfc.Tag;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import java.util.Vector;

public class RadiusOverlayView extends LinearLayout {
    private Bitmap windowFrame;

    private final String mTAG = "RadiusOverlayView";

    private String displayText = "";

    private double progressCircleEndAngle; // Start at the top and go clockwise
    private double progressCircleStartAngle = 270;
    private int progressCircleColor = getResources().getColor(R.color.green);

    private boolean drawWaveform = false;
    private int mAmpGoal = 0;
    private int amplitude = 0;

    public boolean mLockDisplay = false;

    private boolean drawingProgressCircle = false;
    double recordingDuration = 4800; // ~4.8 seconds
    double startTime;

    public void unlockDisplay() {
        mLockDisplay = false;
    }

    public void updateDisplayText(String str) {
        if(!mLockDisplay) {
            displayText = str;
            this.invalidate();
        }
    }

    public void updateDisplayTextAndLock(String str) {
        displayText = str;
        mLockDisplay = true;
        this.invalidate();
    }

    public void startDrawingProgressCircle(int duration) {
        recordingDuration = duration;
        startDrawingProgressCircle();
    }

    public void startDrawingProgressCircle() {
        drawingProgressCircle = true;
        this.startTime = System.currentTimeMillis();
    }

    public void setProgressCircleAngle(double startAngle, double endAngle) {
        progressCircleStartAngle = startAngle; // 270 is at top of circle
        progressCircleEndAngle = endAngle;
        this.invalidate();
    }

    public void setProgressCircleColor(int colorId){
        progressCircleColor = colorId;
        this.invalidate();
    }

    public void setSineWaveAmpGoal(int ampGoal) {
        // Clamp amp value
        mAmpGoal = (int)(((double)ampGoal / 25000.0) * (getHeight() / 2));
        this.invalidate();
    }

    void setStyle(TypedArray attr){
        String type = attr.getString(R.styleable.RadiusOverlayView_view_type);
        if(type.equals("voice")) {
            drawWaveform = true;
        }
        attr.recycle();
    }

    public RadiusOverlayView(Context context) {
        super(context);
    }

    public RadiusOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStyle(context.obtainStyledAttributes(attrs, R.styleable.RadiusOverlayView, 0, 0));
    }

    public RadiusOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setStyle(context.obtainStyledAttributes(attrs, R.styleable.RadiusOverlayView, defStyleAttr, 0));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RadiusOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setStyle(context.obtainStyledAttributes(attrs, R.styleable.RadiusOverlayView, defStyleAttr, 0));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        createWindowFrame(); // Creation of the window frame
        canvas.drawBitmap(windowFrame, 0, 0, null);
    }

    protected void createWindowFrame() {
        windowFrame = Bitmap.createBitmap(Resources.getSystem().getDisplayMetrics().widthPixels, Resources.getSystem().getDisplayMetrics().heightPixels, Bitmap.Config.ARGB_8888);
        Canvas osCanvas = new Canvas(windowFrame); // Create a canvas to draw onto the new image

        int portraitHeight = (int) ((double) getHeight() / 1.3);
        RectF outerRectangle = new RectF(0, 0, getWidth(), getHeight());

        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2.8f;

        // Draw sine wave form inside circle
        if(drawWaveform) {
            osCanvas.drawColor(getResources().getColor(R.color.black));

            Paint sineWavePaint = new Paint();
            sineWavePaint.setAntiAlias(true);
            sineWavePaint.setStyle(Paint.Style.STROKE);
            sineWavePaint.setStrokeCap(Paint.Cap.ROUND);
            sineWavePaint.setStrokeWidth(10);
            sineWavePaint.setColor(getResources().getColor(R.color.yellow));

            // Increment sine wave closer to measured audio amplitude
            if (amplitude < mAmpGoal) {
                amplitude += (mAmpGoal - amplitude) * .75;
            } else if (amplitude > mAmpGoal) {
                amplitude -= (amplitude - mAmpGoal) * .75;
            }

            int periods = 10;
            int midSinePoint = portraitHeight / 2;
            Path mWavePath = new Path();
            mWavePath.moveTo(0, midSinePoint);

            // Size waveform to diameter of circle
            float totalWaveFormWidth = getWidth();
            float sineDiameter = totalWaveFormWidth / periods;

            for (int i = 1; i < periods; i += 4) {
                // Curve over
                mWavePath.quadTo(sineDiameter * i,
                        midSinePoint - amplitude / 2,
                        sineDiameter * (i + 1),
                        midSinePoint);

                // Curve under
                mWavePath.quadTo(sineDiameter * (i + 2),
                        midSinePoint + amplitude / 2,
                        sineDiameter * (i + 3),
                        midSinePoint);
            }
            osCanvas.drawPath(mWavePath, sineWavePaint);

        } else {

            // Draw rect inverted circle
            Paint invertedCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG); // Anti alias allows for smooth corners
            invertedCirclePaint.setColor(getResources().getColor(R.color.black)); // This is the color of the activity background
            invertedCirclePaint.setAlpha(230);
            osCanvas.drawRect(outerRectangle, invertedCirclePaint);

            // Draw inverted circle
            invertedCirclePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)); // A out B http://en.wikipedia.org/wiki/File:Alpha_compositing.svg
            float circleRadius = Math.min(getWidth(), getHeight()) / 2.2f;
            osCanvas.drawCircle(centerX, centerY, circleRadius, invertedCirclePaint);

            // Draw progress circle
            Paint progressCirclePaint = new Paint();
            progressCirclePaint.setAntiAlias(true);
            progressCirclePaint.setStyle(Paint.Style.STROKE);
            progressCirclePaint.setStrokeCap(Paint.Cap.SQUARE);
            progressCirclePaint.setStrokeWidth(30);
            progressCirclePaint.setColor(progressCircleColor);

            if (drawingProgressCircle) {
                double elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime <= recordingDuration) {
                    progressCircleEndAngle = 360 * (elapsedTime / recordingDuration);
                    this.invalidate();
                } else {
                    progressCircleEndAngle = 359.999;
                    drawingProgressCircle = false;
                }
            }

            final RectF oval = new RectF();
            oval.set(centerX - circleRadius, centerY - circleRadius, centerX + circleRadius, centerY + circleRadius);
            Path myPath = new Path();
            // Start at the top and go clockwise
            myPath.arcTo(oval, (float) progressCircleStartAngle, (float) progressCircleEndAngle, true);
            osCanvas.drawPath(myPath, progressCirclePaint);
        }

        // Skip processing text
        if(displayText.isEmpty()){
            return;
        }

        // Setup text
        int textSize = 1;
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #FFFFFF
        textPaint.setColor(Color.rgb(255, 255, 255));
        // text size in pixels
        textPaint.setTextSize((textSize));

        Vector<Rect> textBounds = new Vector<>();
        Vector<String> lines = new Vector<>();
        StringBuilder lineBuilder = new StringBuilder();
        // Assign whole text string to first line
        lines.add(displayText);
        textBounds.add(new Rect());

        // Get textBounds of first line
        textPaint.getTextBounds(lines.get(0), 0, lines.get(0).length(), textBounds.get(0));

        int paddingWidth = getWidth()/10;
        int paddingHeight = getHeight()/8;

        int textArea = textBounds.get(0).height() * textBounds.get(0).width();
        int textBoxArea = (getHeight() - portraitHeight - paddingHeight) * (getWidth() - paddingWidth);

        // To scale for max text size on different phones
        float maxTextSize = getContext().getResources().getDisplayMetrics().density * 40;

        // Rescale text to fit area of text box
        while(textArea < (textBoxArea - (paddingWidth * paddingHeight)) && textSize < maxTextSize ){
            textSize++;
            textPaint.setTextSize(textSize);
            textPaint.getTextBounds(lines.get(0), 0, lines.get(0).length(), textBounds.get(0));
            textArea = textBounds.get(0).height() * textBounds.get(0).width();
        }

        // Move word(s) onto next line if text is wider than the screen
        int line_i = 0;
        while (textBounds.get(line_i).width() >= getWidth() - paddingWidth) {
            // Add textBounds of line
            textBounds.add(new Rect());
            // Move last word to front of line builder
            lineBuilder.insert(0, lines.get(line_i).substring(lines.get(line_i).lastIndexOf(" ") + 1) + " ");
            // Cut word off end of current line
            lines.set(line_i, lines.get(line_i).substring(0, lines.get(line_i).lastIndexOf(" ")));
            // Set textBounds of current line
            textPaint.getTextBounds(lines.get(line_i), 0, lines.get(line_i).length(), textBounds.get(line_i));

            // If current line is not wider than the screen anymore
            if (textBounds.get(line_i).width() < getWidth() - paddingWidth) {
                // Assign line builder string to next line and reset
                lines.add(lineBuilder.toString());
                lineBuilder.setLength(0);
                // Increment and measure next line
                line_i++;
                textBounds.add(new Rect());
                textPaint.getTextBounds(lines.get(line_i), 0, lines.get(line_i).length(), textBounds.get(line_i));
            }
        }

        // Position and draw text
        Vector<Integer> x = new Vector<>();
        Vector<Integer> y = new Vector<>();
        for (int i = 0; i < lines.size(); i++) {
            int verticalOffset = (int)((textBounds.get(0).height() * 1.2) * i);
            // Center width of screen
            x.add((getWidth() - textBounds.get(i).width()) / 2);
            // Center height adjusted for portrait and other lines
            y.add((portraitHeight
                    + textBounds.get(0).height()
                    + (textBounds.get(0).height() / 2) // So not directly touching the bottom of the portrait
                    + verticalOffset));
            // Draw line of text to screen
            osCanvas.drawText(lines.get(i), x.get(i), y.get(i), textPaint);
        }
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        windowFrame = null; // If the layout changes null the frame so it can be recreated with the new width and height
    }
}
