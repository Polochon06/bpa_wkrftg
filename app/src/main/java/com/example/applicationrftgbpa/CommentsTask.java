package com.example.applicationtftgbpa;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AsyncTask POST /films/commentaire — récupère les commentaires d'un film.
 * Body : { "filmId": <id> }
 * Réponse attendue : tableau JSON de commentaires.
 */
public class CommentsTask extends AsyncTask<Void, Void, String> {

    public interface Listener {
        void onCommentsLoaded(String json);
    }

    private final int filmId;
    private final Listener listener;

    public CommentsTask(int filmId, Listener listener) {
        this.filmId   = filmId;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Void... voids) {
        HttpURLConnection conn = null;
        StringBuilder result   = new StringBuilder();
        try {
            URL url = new URL(DonneesPartagees.getURLConnexion() + "/films/commentaire");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String jwt = DonneesPartagees.getJwt();
            if (jwt != null && !jwt.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + jwt);
            }

            JSONObject body = new JSONObject();
            body.put("filmId", filmId);
            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();

            int code = conn.getResponseCode();
            Log.d("mydebug", "CommentsTask response: " + code);

            if (code == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                reader.close();
            }
        } catch (Exception e) {
            Log.d("mydebug", "CommentsTask error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return result.toString();
    }

    @Override
    protected void onPostExecute(String json) {
        if (listener != null) listener.onCommentsLoaded(json);
    }
}
