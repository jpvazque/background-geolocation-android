package com.marianhello.bgloc.data.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.marianhello.bgloc.Config;
import com.marianhello.bgloc.data.Score;
import com.marianhello.bgloc.data.ScoreDAO;
import com.marianhello.bgloc.data.sqlite.SQLiteScoreContract.ScoreEntry;
import com.marianhello.utils.Encryption;

import org.json.JSONArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class SQLiteScoreDAO implements ScoreDAO {
  private SQLiteDatabase db;
  private Config config;

  public SQLiteScoreDAO(Context context, Config config) {
    SQLiteOpenHelper helper = SQLiteOpenHelper.getHelper(context);
    this.db = helper.getWritableDatabase();
    this.config = config;
  }

  public SQLiteScoreDAO(SQLiteDatabase db) {
    this.db = db;
  }

  /**
   * Get all scores that match whereClause
   *
   * @param whereClause
   * @param whereArgs
   * @return collection of scores
   */
  private Collection<Score> getScores(String whereClause, String[] whereArgs) {
    Collection<Score> scores = new ArrayList<Score>();

    String[] columns = queryColumns();
    String groupBy = null;
    String having = null;
    String orderBy = ScoreEntry.COLUMN_NAME_DATE + " ASC, " + ScoreEntry.COLUMN_NAME_HOUR + " ASC";
    Cursor cursor = null;

    try {
      cursor = db.query(
          ScoreEntry.TABLE_NAME,  // The table to query
          columns,                   // The columns to return
          whereClause,               // The columns for the WHERE clause
          whereArgs,                 // The values for the WHERE clause
          groupBy,                   // don't group the rows
          having,                    // don't filter by row groups
          orderBy                    // The sort order
      );
      while (cursor.moveToNext()) {
        scores.add(hydrate(cursor));
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }

    return scores;
  }

  public Collection<Score> getAllScores() {
    String whereClause = ScoreEntry.COLUMN_NAME_USER + " = ?";
    String[] whereArgs = { config.getUser() }; 
    return getScores(whereClause, whereArgs);
  }

  public Collection<Score> getTodayScores() {
    String whereClause = ScoreEntry.COLUMN_NAME_USER + " = ? " + ScoreEntry.COLUMN_NAME_DATE + " = ?";
    String todayDate = getFormattedDate(new Date());
    String[] whereArgs = { config.getUser(), todayDate };
    return getScores(whereClause, whereArgs);
  }

  public Score getScoreById(long id) {
    String[] columns = queryColumns();
    String whereClause = ScoreEntry._ID + " = ?";
    String[] whereArgs = { String.valueOf(id) };

    Score score = null;
    Cursor cursor = null;
    try {
      cursor = db.query(
              ScoreEntry.TABLE_NAME,  // The table to query
              columns,                   // The columns to return
              whereClause,               // The columns for the WHERE clause
              whereArgs,                 // The values for the WHERE clause
              null,              // don't group the rows
              null,               // don't filter by row groups
              null               // The sort order
      );
      while (cursor.moveToNext()) {
        score = hydrate(cursor);
        if (!cursor.isLast()) {
          throw new RuntimeException("Score " + id + " is not unique");
        }
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }

    return score;
  }

  public Score getScoreByHour(String date, Integer hour) {
    String[] columns = queryColumns();
    String whereClause = ScoreEntry.COLUMN_NAME_USER + " = ? " + ScoreEntry.COLUMN_NAME_DATE + " = ? " + ScoreEntry.COLUMN_NAME_HOUR + " = ?";
    String[] whereArgs = { config.getUser(), date, String.valueOf(hour) };

    Score score = null;
    Cursor cursor = null;
    try {
      cursor = db.query(
              ScoreEntry.TABLE_NAME,  // The table to query
              columns,                   // The columns to return
              whereClause,               // The columns for the WHERE clause
              whereArgs,                 // The values for the WHERE clause
              null,              // don't group the rows
              null,               // don't filter by row groups
              null               // The sort order
      );
      while (cursor.moveToNext()) {
        score = hydrate(cursor);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }

    return score;
  }

  /**
   * Persist score into database
   *
   * @param score
   * @return rowId or -1 when error occured
   */
  public long persistScore(Score score) {
    ContentValues values = getContentValues(score);
    long rowId = db.insertOrThrow(ScoreEntry.TABLE_NAME, ScoreEntry.COLUMN_NAME_NULLABLE, values);

    return rowId;
  }

  /**
   * Update score into database
   *
   * @param score
   * @return number of rows updated
   */
  public long updateScore(Score score) {
    String[] values = getContentValues(score);
    String whereClause = ScoreEntry.COLUMN_NAME_USER + " = ? " + ScoreEntry.COLUMN_NAME_DATE + " = ? " + ScoreEntry.COLUMN_NAME_HOUR + " = ?";
    String[] whereArgs = { score.getUser(), score.getDate(), String.valueOf(score.getHour()) };

    return db.update(
        ScoreEntry.TABLE_NAME,     // The table to query
        values,                   // The values to update
        whereClause,               // The columns for the WHERE clause
        whereArgs                  // The values for the WHERE clause
    );
  }

  /**
   * Persist score into database
   *
   * @param score
   * @return rowId or -1 when error occured
   * @return rowId or -2 when not updated
   */
  public long mergeScoresAndUpdate(Score currentScore, Score newScore) {
    Float value = Math.max(currentScore.getValue(), newScore.getValue());
    newScore.setValue(value);

    Float distanceToHome = Math.max(currentScore.getDistanceToHome(), newScore.getDistanceToHome());
    newScore.setDistanceToHome(distanceToHome);

    Float timeAway = Math.max(currentScore.getTimeAway(), newScore.getTimeAway());
    newScore.setTimeAway(timeAway);

    return updateScore(newScore);
  }

  /**
   * Persist score if abset, update if present
   *
   * @param score
   */
  public long persistOrUpdate(Score score) {
    Score currentScore = getScoreByHour(score.getDate(), score.getHour());

    if(currentScore == null) {
        return persistScore(score);
    } else {
        return mergeScoresAndUpdate(currentScore, score);
    }
  }

  /**
   * Delete score by given scoreId
   *
   * Note: score is not actually deleted only flagged as non valid
   * @param scoreId
   */
  public void deleteScoreById(long scoreId) {
    if (scoreId < 0) {
      return;
    }
    String whereClause = ScoreEntry._ID + " = ?";
    String[] whereArgs = { String.valueOf(scoreId) };
    return db.delete(ScoreEntry.TABLE_NAME, whereClause, whereArgs);
  }

  /**
   * Delete all scores
   * @return number of rows deleted
   */
  public int deleteAllScores() {
    String whereClause = ScoreEntry.COLUMN_NAME_USER + " = ?";
    String[] whereArgs = { config.getUser() };
    return db.delete(ScoreEntry.TABLE_NAME, whereClause, whereArgs);
  }

  /**
   * Delete scores, except the last one
   * Query: DELETE FROM SCORE WHERE USER = CONFIG.USER AND ID <> (SELECT MAX(ID) FROM SCORE WHERE USER = CONFIG.USER)
   * @return number of rows deleted
   */
  public int deleteScores() {
    String whereClause = "? = ? AND ID <> (SELECT MAX(?) FROM ? WHERE ? = ?)";
    String[] whereArgs = {
        ScoreEntry.COLUMN_NAME_USER,
        config.getUser(),
        ScoreEntry._ID,
        ScoreEntry._ID,
        ScoreEntry.TABLE_NAME,
        ScoreEntry.COLUMN_NAME_USER,
        config.getUser()
    };

    return db.delete(ScoreEntry.TABLE_NAME, whereClause, whereArgs);
  }

  /**
   * Delete all of a specific date
   * @param date is Date type
   * @return number of rows deleted
   */
  public int deleteScoresByDate(Date date) {
    String whereClause = ScoreEntry.COLUMN_NAME_USER + " = ? " + ScoreEntry.COLUMN_NAME_DATE + " = ?";
    String formattedDate = getFormattedDate(date);
    String[] whereArgs = { config.getUser(), formattedDate };
    return db.delete(ScoreEntry.TABLE_NAME, whereClause, whereArgs);
  }

  /**
   * Delete all of a specific date
   * @param date is String type
   * @return number of rows deleted
   */
  public int deleteScoresByDate(String date) {
    String whereClause = ScoreEntry.COLUMN_NAME_USER + " = ? " + ScoreEntry.COLUMN_NAME_DATE + " = ?";
    String[] whereArgs = { config.getUser(), date };
    return db.delete(ScoreEntry.TABLE_NAME, whereClause, whereArgs);
  }

  private Score hydrate(Cursor c) {
    Score s = Score.getDefault();
    s.setUser(c.getString(c.getColumnIndex(ScoreEntry.COLUMN_NAME_USER)));
    s.setValue(c.getDouble(c.getColumnIndex(ScoreEntry.COLUMN_NAME_VALUE)));
    s.setDistanceToHome(c.getDouble(c.getColumnIndex(ScoreEntry.COLUMN_NAME_DISTANCE_TO_HOME)));
    s.setTimeAway(c.getInt(c.getColumnIndex(ScoreEntry.COLUMN_NAME_TIME_AWAY)));
    s.setHour(c.getInt(c.getColumnIndex(ScoreEntry.COLUMN_NAME_HOUR)));
    s.setDate(c.getString(c.getColumnIndex(ScoreEntry.COLUMN_NAME_DATE)));
    s.setLocations(decryptLocations(c.getString(c.getColumnIndex(ScoreEntry.COLUMN_NAME_LOCATIONS))));

    return s;
  }

  private ContentValues getContentValues(Score s) {
    ContentValues values = new ContentValues();
    values.put(ScoreEntry.COLUMN_NAME_USER, s.getUser());
    values.put(ScoreEntry.COLUMN_NAME_VALUE, s.getValue());
    values.put(ScoreEntry.COLUMN_NAME_DISTANCE_TO_HOME, s.getDistanceToHome());
    values.put(ScoreEntry.COLUMN_NAME_TIME_AWAY, s.getTimeAway());
    values.put(ScoreEntry.COLUMN_NAME_HOUR, s.getHour());
    values.put(ScoreEntry.COLUMN_NAME_DATE, s.getDate());
    values.put(ScoreEntry.COLUMN_NAME_LOCATIONS, encryptLocations(s.getLocations()));

    return values;
  }

  private String encryptLocations(JSONArray locations) {
      return Encryption.encrypt(locations.toString(), config.getUser());
  }

  private JSONArray decryptLocations(String locations) {
      return new JSONArray(Encryption.decrypt(locations, config.getUser()));
  }

  private String[] queryColumns() {
    String[] columns = {
        ScoreEntry._ID,
        ScoreEntry.COLUMN_NAME_USER,
        ScoreEntry.COLUMN_NAME_VALUE,
        ScoreEntry.COLUMN_NAME_DISTANCE_TO_HOME,
        ScoreEntry.COLUMN_NAME_TIME_AWAY,
        ScoreEntry.COLUMN_NAME_HOUR,
        ScoreEntry.COLUMN_NAME_DATE,
        ScoreEntry.COLUMN_NAME_LOCATIONS
    };

    return columns;
  }

  private String getFormattedDate(Date date) {
    SimpleDateFormat formatter = new SimpleDateFormat(ScoreEntry.DATE_FORMAT);
    try {
        return formatter.format(date);
    } catch(IllegalArgumentException e) {
        e.printStackTrace();
        return null;
    }
  }

  private String getDateFromFormattedString(String date) {
    SimpleDateFormat formatter = new SimpleDateFormat(ScoreEntry.DATE_FORMAT);
    try {
        return formatter.parse(date);
    } catch(ParseException e) {
        e.printStackTrace();
        return null;
    }
  }
}
