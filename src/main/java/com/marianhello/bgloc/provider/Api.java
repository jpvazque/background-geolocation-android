package com.marianhello.bgloc.provider;

import android.content.Context;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.Score;
import com.marianhello.bgloc.data.ScoreDAO;
import com.marianhello.bgloc.data.sqlite.SQLiteScoreContract.ScoreEntry;
import com.marianhello.bgloc.HttpPostService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.ListIterator;
import java.util.TimeZone;

public class Api {
    private Context mContext;
    private Config mConfig;

    private static final String API_ENDPOINT = "http://3.22.195.65:5000";
    private static final String CREATE_REGISTRY_URL = API_ENDPOINT + "/api/integracion/table/insert";
    private static final String UPDATE_REGISTRY_URL = API_ENDPOINT + "/api/integracion/table/update";

    public Api(Config mConfig, Context mContext) {
        this.mContext = mContext;
        this.mConfig = mConfig;
    }

    public void sendPendingScoresToServer(ArrayList<Score> scores) {
        for(Score score: scores) {
            sendPostRequest(score);
        }

        ScoreDAO scoreDAO = DAOFactory.createScoreDAO(mContext, mConfig);
        scoreDAO.deleteScores();
    }

    public ArrayList<Score> getPendingScores() {
        ArrayList<Score> pendingScores = new ArrayList<Score>();

        ScoreDAO scoreDAO = DAOFactory.createScoreDAO(mContext, mConfig);
        ArrayList<Score> scoresDB = new ArrayList<Score>(scoreDAO.getAllScores());

        Calendar prevDate = Calendar.getInstance();
        
        ListIterator<Score> li = scoresDB.listIterator(scoresDB.size());
        while(li.hasPrevious()) {
            Score score = li.previous();
            SimpleDateFormat format = new SimpleDateFormat(ScoreEntry.DATE_FORMAT);
            Calendar scoreDate = Calendar.getInstance();
            try{
                scoreDate.setTime(format.parse(score.getDate()));
                scoreDate.set(Calendar.HOUR_OF_DAY, score.getHour());
            }catch (Exception e) {
                e.printStackTrace();
            }
            prevDate = scoreDate;

            long diffDates = prevDate.getTime().getTime() - scoreDate.getTime().getTime();
            long diffHours = diffDates / (60 * 60 * 1000) % 24;

            if(diffHours > 1) {
                for(int x = 1; x < diffHours; x++) {
                    scoreDate.add(Calendar.HOUR, 1);
                    Score missingScore = score;
                    missingScore.setHour(scoreDate.get(Calendar.HOUR_OF_DAY));
                    missingScore.setDate(scoreDate.getTime());
                    missingScore.appendLocation(score.getLastLocation());
                    pendingScores.add(missingScore);
                }
            }
            pendingScores.add(score);
        }
        return pendingScores;
    }

    public void sendPostRequest(Score score){        
        try {
            JSONObject data = generateUpdateScoreBody(score);
            JSONObject response = HttpPostService.postJSON(UPDATE_REGISTRY_URL, data.toString(), null);
            int updated = response.getJSONObject("data").getInt("rows_updated");

            if(updated == 0) {
                JSONObject insertBody = generateInsertScoreBody(score);
                HttpPostService.postJSON(CREATE_REGISTRY_URL, insertBody, null);
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject generateUpdateScoreBody(Score score) {
        try {
            JSONObject values = new JSONObject();
            values.put("score_"+score.getHour(), score.getValue());
            values.put("gps_point", score.getLocations());

            JSONObject conditionTelf = generateCondition("telefono_id", "==", score.getUser());
            JSONObject conditionDay = generateCondition("dia", "==", score.getDate());

            JSONArray conditions = generateConditions(conditionTelf, conditionDay);
            
            JSONObject data = new JSONObject();
            data.put("tabla", "integracion_score_diario");
            data.put("operador", "and");
            data.put("valores", values);
            data.put("condiciones", conditions);

            return data;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject generateCondition(String columna, String comparador, String valor) {
        try {
            JSONObject condition = new JSONObject();
            condition.put("columna", columna);
            condition.put("comparador", comparador);
            condition.put("valor", valor);

            return condition;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONArray generateConditions(JSONObject... conditions) {
        try{
            JSONArray jsonConditions = new JSONArray();
            for(int x = 0; x < conditions.length; x++) {
                jsonConditions.put(conditions[x]);
            }

            return jsonConditions;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject generateInsertScoreBody(Score score){
        try{
            JSONObject values = new JSONObject();
            values.put("telefono_id", score.getUser());
            values.put("dia", score.getDate());
            values.put("score_"+score.getHour(), score.getValue());
            values.put("gps_point", score.getLocations());

            JSONArray datos = new JSONArray();
            datos.put(values);

            JSONObject data = new JSONObject();
            data.put("tabla", "integracion_score_diario");
            data.put("datos", datos);

            return data;
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}