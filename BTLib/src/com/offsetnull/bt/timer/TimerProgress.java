package com.offsetnull.bt.timer;

public class TimerProgress {
	private float percentage;
	private long timeleft;
	private STATE state;
	public enum STATE { STOPPED,PLAYING,PAUSED; };

	public TimerProgress() {
		percentage = 1.0f;
		timeleft = 30000;
		state = STATE.STOPPED;
	}

	public TimerProgress copy() {
		TimerProgress tmp = new TimerProgress();
		tmp.percentage = this.percentage;
		tmp.timeleft = this.timeleft;
		tmp.state = this.state;
		return tmp;
	}

	public void setPercentage(float percentage) {
		this.percentage = percentage;
	}

	public float getPercentage() {
		return percentage;
	}

	public void setTimeleft(long timeleft) {
		this.timeleft = timeleft;
	}

	public long getTimeleft() {
		return timeleft;
	}

	public void setState(STATE state) {
		this.state = state;
	}

	public STATE getState() {
		return state;
	}
}
