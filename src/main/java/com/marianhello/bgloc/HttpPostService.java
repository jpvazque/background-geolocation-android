package com.marianhello.bgloc;

import android.os.Build;

import com.marianhello.logging.LoggerManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.StringBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStreamWriter;

public class HttpPostService {
    public static final int BUFFER_SIZE = 1024;

    private String mUrl;
    private HttpURLConnection mHttpURLConnection;
    private org.slf4j.Logger logger;
    

    public interface UploadingProgressListener {
        void onProgress(int progress);
    }

    public HttpPostService(String url) {
        mUrl = url;
        this.logger = LoggerManager.getLogger(HttpPostService.class);
        LoggerManager.enableDBLogging();
    }

    public HttpPostService(final HttpURLConnection httpURLConnection) {
        mHttpURLConnection = httpURLConnection;
        this.logger = LoggerManager.getLogger(HttpPostService.class);
        LoggerManager.enableDBLogging();
    }

    private HttpURLConnection openConnection() throws IOException {
        if (mHttpURLConnection == null) {
            mHttpURLConnection = (HttpURLConnection) new URL(mUrl).openConnection();
        }
        return mHttpURLConnection;
    }

    public int postJSON(JSONObject json, Map headers) throws IOException {
        String jsonString = "null";
        if (json != null) {
            jsonString = json.toString();
        }

        return postJSONString(jsonString, headers);
    }

    public int postJSON(JSONArray json, Map headers) throws IOException {
        String jsonString = "null";
        if (json != null) {
            jsonString = json.toString();
        }

        return postJSONString(jsonString, headers);
    }

    public int postJSONString(String body, Map headers) throws IOException {
        return getPostConnection(body, headers).getResponseCode();
    }

    private HttpURLConnection getPostConnection(String body, Map headers) throws IOException {
        if (headers == null) {
            headers = new HashMap();
        }
        
        HttpURLConnection conn = this.openConnection();
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(body.length());
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            conn.setRequestProperty(pair.getKey(), pair.getValue());
        }

        OutputStreamWriter os = null;
        try {
            os = new OutputStreamWriter(conn.getOutputStream());
            os.write(body);

        }catch(Exception e) {
            e.printStackTrace();
            logger.debug("ERROR PAULETTE: " + conn.getResponseCode());
        } finally {
            if (os != null) {
                os.flush();
                os.close();
            }
        }

        return conn;
    }

    public static JSONObject postJSON(String url, String body, Map headers) throws IOException {
        HttpPostService service = new HttpPostService(url);
        HttpURLConnection conn = service.getPostConnection(body, headers);

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        StringBuilder sb = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
            sb.append(output);
        }
        try{
            return new JSONObject(sb.toString());
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int postJSONFile(File file, Map headers, UploadingProgressListener listener) throws IOException {
        return postJSONFile(new FileInputStream(file), headers, listener);
    }

    public int postJSONFile(InputStream stream, Map headers, UploadingProgressListener listener) throws IOException {
        if (headers == null) {
            headers = new HashMap();
        }

        final long streamSize = stream.available();
        HttpURLConnection conn = this.openConnection();

        conn.setDoInput(false);
        conn.setDoOutput(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            conn.setFixedLengthStreamingMode(streamSize);
        } else {
            conn.setChunkedStreamingMode(0);
        }
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            conn.setRequestProperty(pair.getKey(), pair.getValue());
        }

        long progress = 0;
        int bytesRead = -1;
        byte[] buffer = new byte[BUFFER_SIZE];

        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        try {
            is = new BufferedInputStream(stream);
            os = new BufferedOutputStream(conn.getOutputStream());
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                os.flush();
                progress += bytesRead;
                int percentage = (int) ((progress * 100L) / streamSize);
                if (listener != null) {
                    listener.onProgress(percentage);
                }
            }
        } finally {
            if (os != null) {
                os.flush();
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }

        return conn.getResponseCode();
    }

    public static int postJSON(String url, JSONObject json, Map headers) throws IOException {
        HttpPostService service = new HttpPostService(url);
        return service.postJSON(json, headers);
    }

    public static int postJSON(String url, JSONArray json, Map headers) throws IOException {
        HttpPostService service = new HttpPostService(url);
        return service.postJSON(json, headers);
    }

    public static int postJSONFile(String url, File file, Map headers, UploadingProgressListener listener) throws IOException {
        HttpPostService service = new HttpPostService(url);
        return service.postJSONFile(file, headers, listener);
    }
}
