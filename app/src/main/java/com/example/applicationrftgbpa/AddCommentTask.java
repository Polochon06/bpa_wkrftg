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
 * AsyncTask POST /films/commentaire/add — ajoute un commentaire.
 * Body : { "filmId": <id>, "customerId": <id>, "commentText": "..." }
 */
public class AddCommentTask extends AsyncTask<Void, Void, Boolean> {

    public interface Listener {
        void onCommentAdded(boolean success);
    }

    private final int filmId;
    private final int customerId;
    private final String commentText;
    private final Listener listener;

    public AddCommentTask(int filmId, int customerId, String commentText, Listener listener) {
        this.filmId      = filmId;
        this.customerId  = customerId;
        this.commentText = commentText;
        this.listener    = listener;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(DonneesPartagees.getURLConnexion() + "/films/commentaire/add");
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
            body.put("filmId",      filmId);
            body.put("customerId",  customerId);
            body.put("commentText", commentText);

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();

            int code = conn.getResponseCode();
            Log.d("mydebug", "AddCommentTask code: " + code);
            Log.d("mydebug", "AddCommentTask filmId=" + filmId + " customerId=" + customerId + " text=" + commentText);

            // Lire le corps de la réponse (erreur ou succès)
            try {
                java.io.InputStream is = code >= 200 && code < 300
                        ? conn.getInputStream() : conn.getErrorStream();
                if (is != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String l;
                    while ((l = r.readLine()) != null) sb.append(l);
                    r.close();
                    Log.d("mydebug", "AddCommentTask body: " + sb.toString());
                }
            } catch (Exception ignored) {}

            return code == 200 || code == 201;

        } catch (Exception e) {
            Log.d("mydebug", "AddCommentTask error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (listener != null) listener.onCommentAdded(success);
    }
}
