package com.tenforwardconsulting.bgloc;

import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.ScoreDAO;
import com.marianhello.bgloc.data.Score;
import android.content.Context;
import com.marianhello.bgloc.Config;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import com.marianhello.bgloc.data.sqlite.SQLiteScoreContract.ScoreEntry;
import com.tenforwardconsulting.bgloc.DistanceScore;

class LocationScore {
    private Context mContext;
    private Config mConfig;
    private Location location;
    private DistanceScore distanceScore; //score & distance
    private WifiScore wifiScore; //score
    private TimeAwayScore timeAwayScore; //score & time
    private DensityScore densityScore; //score
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

    public void calculateAndSaveScore(Location location) { //time given in minutes
        calculatePartialScores(location);
        score = distanceScore.score * ((alpha * wifiScore.score) + (beta * densityScore.score) + (theta * timeAwayScore.score));
        saveToDatabase(getScoreDB(location));
    }

    public void calculatePartialScores(Location location) {
        distanceScore = new DistanceScore(mConfig, location);
        wifiScore = new WifiScore();
        timeScore = new TimeScore();
        densityScore = new DensityScore();
    }

    public void saveToDatabase(Score scoreDB) {
        ScoreDAO scoreDAO = DAOFactory.createScoreDAO(mContext);
        scoreDAO.persistOrUpdate(scoreDB);
    }

    public Score getScoreDB(Location location){
        hour = getHour(location);
        date = getDate(location);
        Score scoreDB = new Score();
        scoreDB.setUser(mConfig.getUser());
        scoreDB.setValue(score);
        scoreDB.setHour(hour);
        scoreDB.setDate(date);

        return scoreDB;
    }

    public String getDate(Location location) {
        Date date = new Date(location.getTime());
        SimpleDateFormat formatter = new SimpleDateFormat(ScoreEntry.DATE_FORMAT);
        return formatter.format(date);
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