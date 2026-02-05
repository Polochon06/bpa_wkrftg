package com.example.applicationrftg;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AsyncTask pour récupérer la liste des catégories de films
 */
public class CategoriesTask extends AsyncTask<URL, Integer, String> {

    public interface Listener {
        void onCategoriesLoaded(String resultatJson);
    }

    private volatile Listener listener;

    public CategoriesTask(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        Log.d("mydebug", ">>> CategoriesTask - Début du chargement des catégories");
    }

    @Override
    protected String doInBackground(URL... urls) {
        String sResultatAppel = null;
        URL urlAAppeler = urls[0];
        sResultatAppel = appelerServiceRestHttp(urlAAppeler);
        return sResultatAppel;
    }

    @Override
    protected void onPostExecute(String resultat) {
        Log.d("mydebug", ">>> CategoriesTask - Catégories reçues");

        if (listener != null) {
            listener.onCategoriesLoaded(resultat);
        }
    }

    private String appelerServiceRestHttp(URL urlAAppeler) {
        HttpURLConnection urlConnection = null;
        int responseCode = -1;
        StringBuilder sResultatAppel = new StringBuilder();

        try {
            urlConnection = (HttpURLConnection) urlAAppeler.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("User-Agent", System.getProperty("http.agent"));
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(15000);

            responseCode = urlConnection.getResponseCode();
            Log.d("mydebug", ">>> CategoriesTask - Response Code: " + responseCode);

            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(urlConnection.getInputStream())
                );

                String ligne;
                while ((ligne = reader.readLine()) != null) {
                    sResultatAppel.append(ligne);
                }
                reader.close();

                Log.d("mydebug", ">>> CategoriesTask - Données reçues : " + sResultatAppel.length() + " caractères");
            } else {
                Log.d("mydebug", ">>> CategoriesTask - Erreur HTTP: " + responseCode);
            }

        } catch (java.net.SocketTimeoutException ste) {
            Log.d("mydebug", ">>> CategoriesTask - TIMEOUT: " + ste.toString());
        } catch (Exception e) {
            Log.d("mydebug", ">>> CategoriesTask - Exception: " + e.toString());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return sResultatAppel.toString();
    }
}
