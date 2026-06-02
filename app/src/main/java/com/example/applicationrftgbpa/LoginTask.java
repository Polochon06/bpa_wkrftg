package com.example.applicationtftgbpa;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;

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
            // URL de connexion depuis DonneesPartagees
            URL url = new URL(DonneesPartagees.getURLConnexion() + "/customers/verify");
            Log.d("mydebug", ">>> LoginTask - URL: " + url.toString());

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Crypter le mot de passe en MD5
            String passwordCrypte = encypterChaineMD5(password);
            Log.d("mydebug", ">>> LoginTask - Password MD5: " + passwordCrypte);

            // Créer le JSON avec email et password hashé en MD5
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", passwordCrypte);

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

                // Parser la réponse JSON pour extraire le customerId et le token
                JSONObject jsonResponse = new JSONObject(response.toString());
                int customerId = jsonResponse.getInt("customerId");

                // Si customerId == -1, c'est un échec de connexion
                if (customerId == -1) {
                    return "ERROR:Email ou mot de passe incorrect";
                }

                // Stocker le JWT pour les requêtes suivantes
                String token = jsonResponse.optString("token", "");
                DonneesPartagees.setJwt(token);

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

    // ENCRYPTAGE EN MD5
    private String encypterChaineMD5(String chaine) {
        byte[] chaineBytes = chaine.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(chaineBytes);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        StringBuffer hashString = new StringBuffer();
        for (int i=0; i<hash.length; ++i ) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length()-1));
            }
            else {
                hashString.append(hex.substring(hex.length()-2));
            }
        }
        return hashString.toString();
    }
}
