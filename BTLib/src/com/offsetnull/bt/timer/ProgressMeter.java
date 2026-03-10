package com.offsetnull.bt.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class ProgressMeter extends View {

	private float progress;
	private float range;
	private final Paint mDrawPaint = new Paint();
	private final Rect mDrawRect = new Rect();
	private final Paint mBackgroundPaint = new Paint();
	private static final int[] GRADIENT_COLORS = { 0xFFFF0000, 0xFFEDBF24, 0xFF00FF00 };
	private static final float[] GRADIENT_POSITIONS = { 0f, 0.3f, 1f };
	private Shader mGradientShader;

	public ProgressMeter(Context context) {
		super(context);

		init();
	}
	public ProgressMeter(Context context,AttributeSet set) {
		super(context,set);
		init();
	}

	private void init() {
		progress = 25;
		range = 100;
		mBackgroundPaint.setColor(0xFF030303);
	}
	
	//private int indicatorWidth = 10;

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mGradientShader = new LinearGradient(0, 0, getRight(), 0, GRADIENT_COLORS, GRADIENT_POSITIONS, Shader.TileMode.REPEAT);
	}

	public void onDraw(Canvas c) {
		//Log.e("PROGRESS","DRAWING THE PROGRESS BAR");
		
		//float center_x = (this.getRight() - this.getLeft())/2;
		//float center_y = (this.getBottom() - this.getTop())/2;
		int indicator_pos = (int) (this.getWidth()*(progress/range));
		//c.translate(center_x, center_y);
		//this.getP
		mDrawPaint.setStrokeWidth(19*getResources().getDisplayMetrics().density);
		mDrawPaint.setShader(mGradientShader);
		//p.
		mDrawRect.top = this.getTop();
		mDrawRect.bottom = this.getBottom();
		mDrawRect.left = this.getLeft();
		mDrawRect.right = this.getRight();

		c.drawRect(mDrawRect, mBackgroundPaint);
		c.drawLine(0, 0, indicator_pos, 0, mDrawPaint);
		
		
	}
	public void setProgress(float progress) {
		this.progress = progress;
	}
	public float getProgress() {
		return progress;
	}
	public void setRange(float range) {
		this.range = range;
	}
	public float getRange() {
		return range;
	}
	
	

}
