package com.example.applicationrftg;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginTask extends AsyncTask<String, Void, String> {

    private LoginTaskListener listener;

    public interface LoginTaskListener {
        void onLoginSuccess(String token);
        void onLoginError(String error);
    }

    public LoginTask(LoginTaskListener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        String username = params[0];
        String password = params[1];

        try {
            // URL de connexion bouchon (10.0.2.2 = localhost pour émulateur Android, port 8180)
            URL url = new URL("http://10.0.2.2:8180/api/bouchon/login");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Créer le JSON avec username et password (format AuthRequest du backend)
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("password", password);

            // Envoyer le JSON
            OutputStream os = connection.getOutputStream();
            os.write(jsonBody.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = connection.getResponseCode();
            Log.d("mydebug", ">>> LoginTask - Response Code: " + responseCode);

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String ligne;
                while ((ligne = reader.readLine()) != null) {
                    response.append(ligne);
                }
                reader.close();

                Log.d("mydebug", ">>> LoginTask - Response: " + response.toString());

                // Parser la réponse JSON pour extraire le token
                JSONObject jsonResponse = new JSONObject(response.toString());
                String token = jsonResponse.getString("token");

                return "SUCCESS:" + token;

            } else if (responseCode == 401) {
                return "ERROR:Email ou mot de passe incorrect";
            } else {
                return "ERROR:Erreur serveur (code " + responseCode + ")";
            }

        } catch (Exception e) {
            Log.d("mydebug", ">>> LoginTask - Exception: " + e.toString());
            return "ERROR:Erreur de connexion : " + e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result.startsWith("SUCCESS:")) {
            String token = result.substring(8); // Enlever "SUCCESS:"
            listener.onLoginSuccess(token);
        } else if (result.startsWith("ERROR:")) {
            String error = result.substring(6); // Enlever "ERROR:"
            listener.onLoginError(error);
        }
    }
}
