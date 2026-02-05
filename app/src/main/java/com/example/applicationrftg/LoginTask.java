package com.example.applicationrftg;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginTask extends AsyncTask<String, Void, String> {

    private LoginTaskListener listener;
    private Context context;

    public interface LoginTaskListener {
        void onLoginSuccess(int customerId);
        void onLoginError(String error);
    }

    public LoginTask(Context context, LoginTaskListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... params) {
        String email = params[0];
        String password = params[1];

        try {
            // URL de connexion (IP du PC pour téléphone réel)
            URL url = new URL("http://192.168.30.124:8180/customers/verify");
            Log.d("mydebug", ">>> LoginTask - URL: " + url.toString());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Créer le JSON avec email et password (format LoginRequest du backend)
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
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

                // Parser la réponse JSON pour extraire le customerId
                JSONObject jsonResponse = new JSONObject(response.toString());
                int customerId = jsonResponse.getInt("customerId");

                // Si customerId == -1, c'est un échec de connexion
                if (customerId == -1) {
                    return "ERROR:Email ou mot de passe incorrect";
                }

                return "SUCCESS:" + customerId;

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
            String customerIdStr = result.substring(8); // Enlever "SUCCESS:"
            int customerId = Integer.parseInt(customerIdStr);
            listener.onLoginSuccess(customerId);
        } else if (result.startsWith("ERROR:")) {
            String error = result.substring(6); // Enlever "ERROR:"
            listener.onLoginError(error);
        }
    }
}
