package com.mark.bluetoothdemo;

/**
 * Created on 2017/05/16
 *
 * @author Mark Hsu
 */

class Obstacle {
    private String road, type, time, comment;
    private double distance;

    Obstacle(String road, String type, String time, String comment, double distance) {
        this.road = road;
        this.type = type;
        this.time = time.substring(0, 5);
        this.comment = comment;
        this.distance = distance;
    }

    String getSpeaking() {
        return "在" + road + "有" + type + "\n";
    }

    String getDetail() {
        return "  Happened at " + time + "\n" +
                "  Distance: " + String.valueOf(distance).substring(0, 6) + "km\n" +
                "  Detail: " + comment + "\n";
    }
}