package com.tenforwardconsulting.bgloc;

import android.location.Location;
import com.marianhello.bgloc.Config;

class DistanceScore {
    private Location location;
    private float am, bm, cm;
    private float al, bl, cl;
    private float ah, bh, ch;
    private float homeLat;
    private float homeLong;
    private int score;
    private double distance;

    DistanceScore(Config mConfig, Location location){
        this.location = location;
        float homeRadius = mConfig.getHomeRadius();
        float csRadius = mConfig.csRadius();

        al = -10; 
        bl = 0; 
        cl = 2*homeRadius;

        am = homeRadius;
        bm = homeRadius/2;
        cm = csRadius;

        ah = csRadius/2;
        bh = 2*csRadius;
        ch = Integer.MAX_VALUE;

        homeLat = mConfig.getHomeLatitude();
        homeLong = mconfig.getHomeLatitude();

        calculateScore(location);
    }

    public void calculateScore() {
        double max = 0;

        distance = distance(location.latitude, 
                            location.longitude,
                            homeLat,
                            homeLong);
        score = scoreExposure(distance, max);
    }

    public double distance(double lat1, double lon1,
                           double lat2, double lon2) {
        double theta, dist;

        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            theta = lon1 - lon2;
            dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
            dist = Math.acos(dist);
            dist = rad2deg(dist);
            dist = dist * 60 * 1.1515;
            dist = dist * 1.609344;
        }
        return dist*1000;
    }

    public double deg2rad(double deg) {
        return (deg * Math.PI / 180);
    }

    public rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    public double trimf(double x, double a, double b, double c) { // menor que a -> cero, b posicion de pico, mayor que c ->cero
        return (Math.max(Math.min((x-a)/(b-a), (c-x)/(c-b)), 0));
    }

    public double lowExposure(double x) {
      return (trimf(x, al, bl, cl));
    }
    
    public double mediumExposure(double x){
      return (trimf(x, am, bm, cm));
    }

    public double highExposure(double x) {
      return (trimf(x, ah, bh, ch));
    }

    public int scoreExposure(double x, double max) {
        double low = lowExposure(x);
        double mid = mediumExposure(x);
        double high = highExposure(x);

        max = Math.max(high, Math.max(mid, low));
        
        if(max == low)	return (1);
        if(max == mid)	return (2);
        if(max == high)	return (3);
        return 0;
    }

    public int getScore() {
        return score;
    }

    public double getDistance() {
        return distance;
    }
}