package com.tenforwardconsulting.bgloc;

import android.content.Context;
import android.location.Location;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.Score;
import com.marianhello.bgloc.data.ScoreDAO;
import com.marianhello.bgloc.data.sqlite.SQLiteScoreContract.ScoreEntry;

import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class LocationScore {
    private Context mContext;
    private Config mConfig;
    private DistanceScore distanceScore;
    private WifiScore wifiScore;
    private int nroNetworksAvailable;
    private TimeAwayScore timeAwayScore;
    private DensityScore densityScore; 
    private double alpha;
    private double beta;
    private double theta;
    private int hour;
    private String date;
    private double score;

    LocationScore(Config mConfig, Context mContext, int nroNetworksAvailable) {
        this.nroNetworksAvailable = nroNetworksAvailable;
        this.mContext = mContext;
        this.mConfig = mConfig;
        alpha = beta = theta = 0.33;
    }

    public Score calculateAndSaveScore(Location location) { //time given in minutes
        calculatePartialScores(location);
        score = distanceScore.getScore() * ((alpha * wifiScore.getScore()) + (beta * densityScore.getScore()) + (theta * timeAwayScore.getScore()));
        Score scoreDB = getScoreDB(location);
        saveToDatabase(scoreDB);
        return scoreDB;
    }

    public void calculatePartialScores(Location location) {
        distanceScore = new DistanceScore(mConfig, location);
        wifiScore = new WifiScore(mConfig, nroNetworksAvailable);
        timeAwayScore = new TimeAwayScore();
        densityScore = new DensityScore();
    }

    public void saveToDatabase(Score scoreDB) {
        ScoreDAO scoreDAO = DAOFactory.createScoreDAO(mContext, mConfig);
        scoreDAO.persistOrUpdate(scoreDB);
    }

    public Score getScoreDB(Location location) {
        hour = getHour(location);
        date = getDate(location);

        ScoreDAO scoreDAO = DAOFactory.createScoreDAO(mContext, mConfig);
        Score scoreDB = scoreDAO.getScoreByHour(date, hour);
        if(scoreDB == null) {
            scoreDB = Score.getDefault();
            scoreDB.setUser(mConfig.getUser());
            scoreDB.setHour(hour);
            scoreDB.setDate(new Date(location.getTime()));
        }
        scoreDB.appendLocation(location);
        scoreDB.setValue(score);
        return scoreDB;
    }

    public String getDate(Location location) {
        Date date = new Date(location.getTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        SimpleDateFormat formatter = new SimpleDateFormat(ScoreEntry.DATE_FORMAT);
        try{
            return formatter.format(calendar.getTime());
        }catch (Exception e) {
            return null;
        }
    }

    public int getHour(Location location) {
        Date date = new Date(location.getTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar.HOUR_OF_DAY;
    }

    public double getscore() {
        return score;
    }

    class WifiScore {
        private Config mConfig;
        private int nroHomeNetworks;
        private int nroNetworksAvailable;
        private double X;
        private double score;

        WifiScore(Config config, int nroNetworksAvailable) {
            this.mConfig = config;
            this.nroHomeNetworks = mConfig.getHomeNetworks();
            this.nroNetworksAvailable = nroNetworksAvailable;
            X = 1.5;
            calculateWifiScore();
        }

        WifiScore(Config config, int nroNetworksAvailable, double X) {
            this.mConfig = config;
            this.nroHomeNetworks = mConfig.getHomeNetworks();
            this.nroNetworksAvailable = nroNetworksAvailable;
            this.X = X;
            calculateWifiScore();
        }

        public void calculateWifiScore() {
            long max_networks_allowed = Math.round(nroHomeNetworks * X);
            if((nroNetworksAvailable > 0) && (nroNetworksAvailable < max_networks_allowed)){
                score = (nroNetworksAvailable / max_networks_allowed);
            }
            score =1;
        }

        public double getScore() {
            return score;
        }
    }

    class TimeAwayScore {
        private double score;
        private int timeAway;

        TimeAwayScore() {
            score = 1;
            timeAway = 0;
        }

        public double getScore() {
            return score;
        }

        public int getTimeAway() {
            return timeAway;
        }
    }

    class DensityScore {
        private double score;

        DensityScore(){
            score = 1;
        }
        
        public double getScore(){
            return score;
        }
    }
}