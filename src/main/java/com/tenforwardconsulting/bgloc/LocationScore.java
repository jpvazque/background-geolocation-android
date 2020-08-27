package com.tenforwardconsulting.bgloc;

import android.content.Context;
import android.location.Location;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.Score;
import com.marianhello.bgloc.data.ScoreDAO;
import com.marianhello.bgloc.data.sqlite.SQLiteScoreContract.ScoreEntry;
import com.tenforwardconsulting.bgloc.DistanceScore;

import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

public class LocationScore {
    private Context mContext;
    private Config mConfig;
    private Location location;
    private DistanceScore distanceScore;
    private WifiScore wifiScore;
    private TimeAwayScore timeAwayScore;
    private DensityScore densityScore; 
    private double alpha;
    private double beta;
    private double theta;
    private int hour;
    private String date;
    private double score;

    LocationScore(Config mConfig, Context mContext) {
        alpha = beta = theta = 0.33;

        this.mContext = mContext;
        this.mConfig = mConfig;
    }

    public Score calculateAndSaveScore(Location location) { //time given in minutes
        calculatePartialScores(location);
        score = distanceScore.score * ((alpha * wifiScore.score) + (beta * densityScore.score) + (theta * timeAwayScore.score));
        Score scoreDB = getScoreDB(location);
        saveToDatabase(scoreDB);
        return scoreDB;
    }

    public void calculatePartialScores(Location location) {
        distanceScore = new DistanceScore(mConfig, location);
        wifiScore = new WifiScore();
        timeScore = new TimeScore();
        densityScore = new DensityScore();
    }

    public void saveToDatabase(Score scoreDB) {
        ScoreDAO scoreDAO = DAOFactory.createScoreDAO(mContext, mConfig);
        scoreDAO.persistOrUpdate(scoreDB);
    }

    public Score getScoreDB(Location location) {
        Score scoreDB;
        hour = getHour(location);
        date = getDate(location);
        
        ScoreDAO scoreDAO = DAOFactory.createScoreDAO(mContext, mConfig);
        Score score = scoreDAO.getScoreByHour(date, hour);
        if(score == null) {
            scoreDB = new Score();
            scoreDB.setUser(mConfig.getUser());
            scoreDB.setHour(hour);
            scoreDB.setDate(new Date(location.getTime()));
        }else{
            scoreDB = score;
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

    public int getscore() {
        return score;
    }

    class WifiScore {
        private int nroHomeNetworks;
        private int nroNetworksAvailable;
        private float X;
        private int score;

        WifiScore(int nroHomeNetworks, int nroNetworksAvailable) {
            this.nroHomeNetworks = nroHomeNetworks;
            this.nroNetworksAvailable = nroNetworksAvailable;
            X = 1.5;
            calculateWifiScore();
        }

        WifiScore(int nroNetworksAvailable, float X, int nroHomeNetworks) {
            this.nroHomeNetworks = nroHomeNetworks;
            this.nroNetworksAvailable = nroNetworksAvailable;
            this.X = X;
            calculateWifiScore();
        }

        public int calculateWifiScore() {
            if((nroNetworksAvailable > 0) && (nroNetworksAvailable < max_networks_allowed)){
                int max_networks_allowed = nroHomeNetworks * X;
                score = (int) (nroNetworksAvailable / max_networks_allowed);
            }
            score =1;
        }

        public int getScore() {
            return score;
        }
    }

    class TimeAwayScore {
        private int score;
        private int timeAway;

        TimeScore() {
            score = 1;
            timeAway = 0;
        }

        public int getScore() {
            return score;
        }

        public int getTimeAway() {
            return time;
        }
    }

    class DensityScore {
        private int score;

        DensityScore(){
            score = 1;
        }
        
        public int getScore(){
            return score;
        }
    }
}