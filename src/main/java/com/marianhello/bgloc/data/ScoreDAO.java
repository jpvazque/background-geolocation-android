package com.marianhello.bgloc.data;

import com.marianhello.bgloc.data.Score;

import java.util.Collection;
import java.util.Date;


public interface ScoreDAO {
    Collection<Score> getAllScores();
    Score getScoreById(long id);
    Collection<Score> getTodayScores();
    Score getScoreByHour(String date, Integer hour);
    long persistScore(Score score);
    long persistOrUpdate(Score score);
    void deleteScoreById(long scoreId);
    int deleteAllScores();
    int deleteScores();
    int deleteScoresByDate(Date date);
    int deleteScoresByStringDate(String date);
}
