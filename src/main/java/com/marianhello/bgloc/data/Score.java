/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.bgloc;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.marianhello.bgloc.data.AbstractLocationTemplate;
import com.marianhello.bgloc.data.LocationTemplate;
import com.marianhello.bgloc.data.LocationTemplateFactory;
import com.marianhello.utils.CloneHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Score class
 */
public class Score implements Parcelable
{
    public static final String BUNDLE_KEY = "score";

    public static final int DISTANCE_FILTER_PROVIDER = 0;
    public static final int ACTIVITY_PROVIDER = 1;
    public static final int RAW_PROVIDER = 2;

    // NULL string score option to distinguish between java null
    public static final String NullString = new String();

    private String user;
    private Float value;
    private Float distanceToHome;
    private Integer timeAway;
    private Integer hour;
    private String date;

    public Score () {
    }

    // Copy constructor
    public Score(Score score) {
        this.user = score.user;
        this.value = score.value;
        this.distanceToHome = score.distanceToHome;
        this.timeAway = score.timeAway;
        this.hour = score.hour;
        this.date = score.date;
    }

    private Score(Parcel in) {
        setUser(in.readString());
        setValue(in.readFloat());
        setDistanceToHome(in.readFloat());
        setTimeAway(in.readInt());
        setHour(in.readInt());
        setDate(in.readString());
    }

    public static Score getDefault() {
        Score score = new Score();
        score.user = "";
        score.value = 0f;
        score.distanceToHome = 0f;
        score.timeAway = 0;
        score.hour = 0;
        score.date = "";

        return score;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getUser());
        out.writeFloat(getValue());
        out.writeFloat(getDistanceToHome());
        out.writeInt(getTimeAway());
        out.writeInt(getHour());
        out.writeString(getDate());
    }

    public static final Parcelable.Creator<Score> CREATOR
            = new Parcelable.Creator<Score>() {
        public Score createFromParcel(Parcel in) {
            return new Score(in);
        }

        public Score[] newArray(int size) {
            return new Score[size];
        }
    };

    public boolean hasUser() { return user != null; }

    public String getUser() { return user; }

    public void setUser(String user) { this.user = user; }


    public boolean hasValue() { return value != null; }

    public Float getValue() { return value; }

    public void setValue(float value) { this.value = value; }

    public void setValue(double value) { this.value = (float) value; }


    public boolean hasDistanceToHome() { return distanceToHome != null; }

    public Float getDistanceToHome() { return distanceToHome; }

    public void setDistanceToHome(float distanceToHome) { this.distanceToHome = distanceToHome; }

    public void setDistanceToHome(double distanceToHome) { this.distanceToHome = (float) distanceToHome; }


    public boolean hasTimeAway() { return timeAway != null; }

    public Integer getTimeAway() { return timeAway; }

    public void setTimeAway(Integer timeAway) { this.timeAway = timeAway; }


    public boolean hasHour() { return hour != null; }

    public Integer getHour() { return hour; }

    public void setHour(Integer hour) { this.hour = hour; }


    public boolean hasDate() { return date != null; }

    public String getDate() { return date; }

    public void setDate(String date) { this.date = date; }


    @Override
    public String toString () {
        return new StringBuffer()
                .append(" user=").append(getUser())
                .append(" value=").append(getValue())
                .append(" distanceToHome=").append(getDistanceToHome())
                .append(" timeAway=").append(getTimeAway())
                .append(" hour=").append(getHour())
                .append(" date=").append(getDate())
                .toString();
    }

    /**
     * Returns score as JSON object.
     * @throws JSONException
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("user", user);
        json.put("value", value);
        json.put("distanceToHome", distanceToHome);
        json.put("timeAway", timeAway);
        json.put("hour", hour);
        json.put("date", date);

        return json;
  	}

    public Parcel toParcel () {
        Parcel parcel = Parcel.obtain();
        this.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return parcel;
    }

    public Bundle toBundle () {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_KEY, this);
        return bundle;
    }

    public static Score fromByteArray (byte[] byteArray) {
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(byteArray, 0, byteArray.length);
        parcel.setDataPosition(0);
        return Score.CREATOR.createFromParcel(parcel);
    }
}
