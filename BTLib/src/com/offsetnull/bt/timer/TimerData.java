package com.offsetnull.bt.timer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.offsetnull.bt.responder.TriggerResponder;
import com.offsetnull.bt.responder.ack.AckResponder;
import com.offsetnull.bt.responder.notification.NotificationResponder;
import com.offsetnull.bt.responder.toast.ToastResponder;

//import android.util.Log;

public class TimerData {

	private String name;
	private Integer ordinal;
	private Integer seconds;
	private boolean repeat;
	private boolean playing;
	private long startTime;
	private int remainingTime;


	//data that is not serialized, but should still be parcelable.
	//private long ttf;
	//private Long pauseLocation;

	private List<TriggerResponder> responders;

	public TimerData() {
		name="";
		ordinal=0;
		seconds=30;
		repeat=true;
		playing = false;
		//ttf = seconds*1000;
		responders = new ArrayList<TriggerResponder>();
		//pauseLocation = 0l;

	}

	public void reset() {
		//ttf = seconds*1000;
		//pauseLocation = 0l;
	}

	public TimerData copy() {

		TimerData tmp = new TimerData();
		tmp.name = this.name;
		tmp.ordinal = this.ordinal;
		tmp.seconds = this.seconds;
		tmp.repeat = this.repeat;
		tmp.playing = this.playing;
		tmp.remainingTime =  this.remainingTime;
		for(TriggerResponder responder : this.responders) {
			tmp.responders.add(responder.copy());
		}

		return tmp;

	}

	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof TimerData)) return false;
		TimerData test = (TimerData)o;
		if(!test.name.equals(this.name)) return false;
		if(test.ordinal != this.ordinal) return false;
		if(test.seconds != this.seconds) return false;
		if(test.repeat != this.repeat) return false;
		if(test.playing != this.playing) return false;
		//ttf shouldn't be considered for equality. it seems wrong.
		Iterator<TriggerResponder> test_responders = test.responders.iterator();
		Iterator<TriggerResponder> my_responders = this.responders.iterator();
		while(test_responders.hasNext()) {
			TriggerResponder test_responder = test_responders.next();
			TriggerResponder my_responder = my_responders.next();
			if(!test_responder.equals(my_responder)) return false;
		}

		return true;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setOrdinal(Integer ordinal) {
		this.ordinal = ordinal;
	}

	public Integer getOrdinal() {
		return ordinal;
	}

	public void setSeconds(Integer seconds) {
		this.seconds = seconds;
		//ttf = seconds*1000;
	}

	public Integer getSeconds() {
		return seconds;
	}

	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	public boolean isRepeat() {
		return repeat;
	}

	public void setResponders(List<TriggerResponder> responders) {
		this.responders = responders;
	}

	public List<TriggerResponder> getResponders() {
		return responders;
	}

	public void setPlaying(boolean playing) {
		this.playing = playing;
	}

	public boolean isPlaying() {
		return playing;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public int getRemainingTime() {
		return remainingTime;
	}

	public void setRemainingTime(int remainingTime) {
		this.remainingTime = remainingTime;
	}

}
