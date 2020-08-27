package com.marianhello.bgloc.provider;

import com.marianhello.bgloc.data.DAOFactory;
import com.marianhello.bgloc.data.Score;
import com.marianhello.bgloc.data.ScoreDAO;
import com.marianhello.bgloc.HttpPostService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.utils.Collection;

public class Api {
    private static final API_ENDPOINT = "http://3.22.195.65:5000";
    private static final CREATE_REGISTRY_URL = API_ENDPOINT+"/api/integracion/table/insert";
    private static final UPDATE_REGISTRY_URL = API_ENDPOINT+"/api/integracion/table/update";

    
    public void sendPendingScoresToServer(){
        ScoreDAO scoreDAO = DAOFactory.createScoreDAO(mContext, mConfig);
        Collection<Score> scores = scoreDAO.getAllScores();

        for(Score score: scores) {
            sendPostRequest(score);
        }

        scoreDAO.deleteScores();
    }

    public void sendPostRequest(Score score){
        JSONObject data = generateUpdateScoreBody(score);
        
        try {
            JSONObject response = HttpPostService.postJSON(UPDATE_REGISTRY_URL, data.toString(), null);
            int updated = response.getJSONObject("data").getInt("rows_updated");

            if(update == 0) {
                JSONObject insertBody = generateInsertScoreBody(score);
                HttpPostService.postJSON(CREATE_REGISTRY_URL, insertBody, null);
            }

        } catch(IOException e) {
            //None
        }
    }

    public JSONObject generateUpdateScoreBody(Score score){
        JSONObject value = new JSONObject();
        value.put("score_"+score.getHour(), score.getValue());
        value.put("gps_point", score.getLocations());

        JSONObject conditionTelf = generateCondition("telefono_id", "==", score.getUser());
        JSONObject conditionDay = generateCondition("dia", "==", score.getDate());

        JSONArray conditions = generateConditions(conditionTelf, conditionDay);
        
        JSONObject data = new JSONObject();
        data.put("tabla", "integracion_score_diario");
        data.put("operador", "and");
        data.put("valores", values);
        data.put("condiciones", conditions);

        return data;
    }

    public JSONObject generateCondition(String columna, String comparador, String valor) {
        JSONObject condition = new JSONObject();
        condition.put("columna", columna);
        condition.put("comparador", comparador);
        condition.put("valor", valor);

        return condition;
    }

    public JSONArray generateConditions(JSONObject... conditions) {
        JSONArray jsonConditions = new JSONArray();
        for(int x = 0; x < conditions.length; x++) {
            jsonConditions.put(conditions[x]);
        }

        return jsonConditions;
    }

    public JSONObject generateInsertScoreBody(Score score){
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
    }
}