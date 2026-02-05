package com.example.applicationrftg;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * AsyncTask pour valider le panier en appelant l'API /rentals/validate
 */
public class ValidatePanierTask extends AsyncTask<Void, Void, String> {

    public interface Listener {
        void onValidationSuccess(int rentalsCreated);
        void onValidationError(String error);
    }

    private Listener listener;
    private int customerId;
    private List<Integer> filmIds;

    public ValidatePanierTask(Listener listener, int customerId, List<Integer> filmIds) {
        this.listener = listener;
        this.customerId = customerId;
        this.filmIds = filmIds;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            // URL de l'endpoint (IP du PC pour téléphone réel)
            URL url = new URL("http://192.168.30.124:8180/rentals/validate");
            Log.d("mydebug", ">>> ValidatePanierTask - URL: " + url.toString());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Créer le JSON : { "customerId": 123, "filmIds": [1, 2, 3] }
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("customerId", customerId);

            JSONArray filmIdsArray = new JSONArray();
            for (Integer filmId : filmIds) {
                filmIdsArray.put(filmId);
            }
            jsonBody.put("filmIds", filmIdsArray);

            Log.d("mydebug", ">>> ValidatePanierTask - Body: " + jsonBody.toString());

            // Envoyer le JSON
            OutputStream os = connection.getOutputStream();
            os.write(jsonBody.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d("mydebug", ">>> ValidatePanierTask - Response Code: " + responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("mydebug", ">>> ValidatePanierTask - Response: " + response.toString());
                return "SUCCESS:" + response.toString();
            } else {
                return "ERROR:Erreur serveur (code " + responseCode + ")";
            }

        } catch (Exception e) {
            Log.d("mydebug", ">>> ValidatePanierTask - Exception: " + e.toString());
            return "ERROR:" + e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result.startsWith("SUCCESS:")) {
            try {
                String jsonStr = result.substring(8);
                JSONObject json = new JSONObject(jsonStr);
                int rentalsCreated = json.getInt("rentalsCreated");
                listener.onValidationSuccess(rentalsCreated);
            } catch (Exception e) {
                listener.onValidationError("Erreur parsing réponse");
            }
        } else if (result.startsWith("ERROR:")) {
            String error = result.substring(6);
            listener.onValidationError(error);
        }
    }
}
