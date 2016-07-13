package me.wangyuwei.trend.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.List;

import me.wangyuwei.trend.common.TouchGestureDetector;
import me.wangyuwei.trend.common.TrendAreaPaint;
import me.wangyuwei.trend.common.VerticalLinePaint;
import me.wangyuwei.trend.entity.DateValueEntity;
import me.wangyuwei.trend.entity.LineEntity;

/**
 * 作者： 巴掌 on 16/4/25 23:24
 */
public class TrendChart extends Chart {


    private Paint mTrendAreaPaint;
    private Paint mVerticalLinePaint;
    private Paint mCrossPaint;
    private Paint mPaintStock;
    /* Y的最大表示值 */
    private double mMaxValue;
    /* Y的最小表示值 */
    private double mMinValue;

    private float mCircleX;
    private float mCircleY;

    private List<LineEntity<DateValueEntity>> mLinesData;

    private TouchGestureDetector touchGestureDetector = new TouchGestureDetector();

    public TrendChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TrendChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView() {
        super.initView();
        mTrendAreaPaint = new TrendAreaPaint(getMeasuredHeight());
        mVerticalLinePaint = new VerticalLinePaint();
        mCrossPaint = new Paint();
        mCrossPaint.setStyle(Paint.Style.FILL);
        mCrossPaint.setAntiAlias(true);
        mPaintStock = new Paint();
        mPaintStock.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calcDataValueRange();

        drawLines(canvas);
        drawAreas(canvas);

        drawVerticalLine(canvas);
        drawCrossCircle(canvas);

    }

    protected void drawLines(Canvas canvas) {
        if (null == this.getLinesData()) {
            return;
        }
        float lineLength;
        float startX;

        for (int i = 0; i < getLinesData().size(); i++) {
            LineEntity<DateValueEntity> line = getLinesData().get(i);
            if (line == null) {
                continue;
            }
            List<DateValueEntity> lineData = line.getLineData();
            if (lineData == null) {
                continue;
            }

            mPaintStock.setColor(line.getLineColor());
            mPaintStock.setStrokeWidth(line.getLineWidth());
            lineLength = (dataQuadrant.getQuadrantPaddingWidth() / mDisplayNumber);
            startX = dataQuadrant.getQuadrantPaddingStartX() + lineLength / 2;
            PointF ptFirst = null;
            for (int j = mDisplayFrom; j < mDisplayFrom + mDisplayNumber; j++) {

                float value = lineData.get(j).getValue();
                float valueY = (float) ((1f - (value - mMinValue)
                        / (mMaxValue - mMinValue)) * dataQuadrant.getQuadrantPaddingHeight())
                        + dataQuadrant.getQuadrantPaddingStartY();

                if (j > mDisplayFrom) {
                    if (ptFirst == null) continue;
                    canvas.drawLine(ptFirst.x, ptFirst.y, startX, valueY,
                            mPaintStock);
                }

                ptFirst = new PointF(startX, valueY);
                startX = startX + lineLength;
            }
        }
    }

    protected void drawAreas(Canvas canvas) {
        if (null == getLinesData()) {
            return;
        }

        for (int i = 0; i < getLinesData().size(); i++) {

            LineEntity<DateValueEntity> line = getLinesData().get(i);

            if (line.getTitle().equals("TREND")) {
                List<DateValueEntity> lineData = line.getLineData();

                if (lineData == null) {
                    continue;
                }

                float lineLength = (dataQuadrant.getQuadrantPaddingWidth() / mDisplayNumber);
                float startX = dataQuadrant.getQuadrantPaddingStartX() + lineLength / 2;

                Path linePath = new Path();
                for (int j = mDisplayFrom; j < mDisplayFrom + mDisplayNumber; j++) {
                    if (j < 0) continue;
                    float value = lineData.get(j).getValue();
                    float valueY = (float) ((1f - (value - mMinValue)
                            / (mMaxValue - mMinValue)) * dataQuadrant.getQuadrantPaddingHeight())
                            + dataQuadrant.getQuadrantPaddingStartY();
                    if (j == mDisplayFrom) {
                        linePath.moveTo(startX, dataQuadrant.getQuadrantPaddingEndY());
                        linePath.lineTo(startX, valueY);
                    } else if (j == mDisplayFrom + mDisplayNumber - 1) {
                        linePath.lineTo(startX, valueY);
                        linePath.lineTo(startX, dataQuadrant.getQuadrantPaddingEndY());
                    } else {
                        linePath.lineTo(startX, valueY);
                    }
                    startX = startX + lineLength;
                }
                linePath.close();
                canvas.drawPath(linePath, mTrendAreaPaint);
            }

        }
    }

    protected void drawVerticalLine(Canvas canvas) {

        if (mTouchPoint == null) return;

        calcCircleCoordinate(mTouchPoint.x);

        if (mCircleX < 0) return;

        float lineVLength = dataQuadrant.getQuadrantHeight() + getBorderWidth();

        canvas.drawLine(mCircleX, getBorderWidth(), mCircleX, lineVLength, mVerticalLinePaint);

    }

    protected void drawCrossCircle(Canvas canvas) {

        if (mTouchPoint == null) return;

        if (mCircleX < 0) return;

        mCrossPaint.setColor(Color.parseColor("#323232"));
        canvas.drawCircle(mCircleX, mCircleY, 14, mCrossPaint);
        mCrossPaint.setColor(Color.parseColor("#ffffff"));
        canvas.drawCircle(mCircleX, mCircleY, 7, mCrossPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return touchGestureDetector.onTouchEvent(event, this);
    }

    private void calcCircleCoordinate(float positionX) {
        if (positionX < dataQuadrant.getQuadrantPaddingStartX())
            positionX = dataQuadrant.getQuadrantPaddingStartX();
        else if (positionX > dataQuadrant.getQuadrantPaddingEndX())
            positionX = dataQuadrant.getQuadrantPaddingEndX();

        mCircleX = positionX;

        //TODO mLinesData.get(0) 这里是个坑，勿踩，大家自己根据业务来
        List<DateValueEntity> lineData = mLinesData.get(0).getLineData();
        float value = lineData.get(getAxisxIndex(mCircleX)).getValue();

        mCircleY = (float) ((1f - (value - mMinValue)
                / (mMaxValue - mMinValue)) * dataQuadrant.getQuadrantPaddingHeight())
                + dataQuadrant.getQuadrantPaddingStartY();
    }

    private int getAxisxIndex(Object value) {

        float graduate = Float.valueOf(getAxisXGraduate(value));
        int index = (int) Math.floor(graduate * mDisplayNumber);

        if (index >= mDisplayNumber) {
            index = mDisplayNumber - 1;
        } else if (index < 0) {
            index = 0;
        }
        index = index + mDisplayNumber;

        return index;

    }

    /* 计算X轴上显示的坐标刻度值 */
    private String getAxisXGraduate(Object value) {
        float valueLength = ((Float) value).floatValue() - dataQuadrant.getQuadrantPaddingStartX();
        return String.valueOf(valueLength / dataQuadrant.getQuadrantPaddingWidth());
    }

    private void calcDataValueRange() {

        double maxValue = Double.MIN_VALUE;
        double minValue = Double.MAX_VALUE;

        for (int i = 0; i < this.getLinesData().size(); i++) {
            LineEntity<DateValueEntity> line = this.getLinesData().get(i);

            if (null != line && line.getLineData().size() > 0) {

                for (int j = mDisplayNumber; j < mDisplayNumber + mDisplayNumber; j++) {

                    DateValueEntity lineData = line.getLineData().get(j);

                    if (lineData.getValue() < 0) continue;

                    if (lineData.getValue() < minValue) {
                        minValue = lineData.getValue();
                    }

                    if (lineData.getValue() > maxValue) {
                        maxValue = lineData.getValue();
                    }
                }

            }
        }

        if (Math.abs(maxValue - mPrevClosePrice) > Math.abs(minValue - mPrevClosePrice)) {
            minValue = mPrevClosePrice - Math.abs(maxValue - mPrevClosePrice);
        } else {
            maxValue = mPrevClosePrice + Math.abs(minValue - mPrevClosePrice);
        }

        this.mMaxValue = maxValue;
        this.mMinValue = minValue;

    }

    public List<LineEntity<DateValueEntity>> getLinesData() {
        return mLinesData;
    }

    public void setLinesData(List<LineEntity<DateValueEntity>> linesData) {
        this.mLinesData = linesData;
    }

}
