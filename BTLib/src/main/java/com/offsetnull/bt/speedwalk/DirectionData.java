package com.offsetnull.bt.speedwalk;

public class DirectionData {
    private String direction = "";
    private String command = "";
    private String reverse = "";

    public DirectionData() {}

    public DirectionData(String direction, String command) {
        this.direction = direction;
        this.command = command;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof DirectionData)) return false;
        DirectionData tmp = (DirectionData) o;
        if (!tmp.direction.equals(this.direction)) return false;
        if (!tmp.command.equals(this.command)) return false;
        if (!tmp.reverse.equals(this.reverse)) return false;

        return true;
    }

    public DirectionData copy() {
        DirectionData tmp = new DirectionData();
        tmp.direction = this.direction;
        tmp.command = this.command;
        tmp.reverse = this.reverse;
        return tmp;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getDirection() {
        return direction;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setReverse(String reverse) {
        this.reverse = reverse;
    }

    public String getReverse() {
        return reverse;
    }
}
