package com.example.applicationrftg;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AsyncTask pour appeler le service REST GET
 * Modèle conforme au cours du 13/11/2025 (pages 7-14)
 */
public class ListefilmsTask extends AsyncTask<URL, Integer, String> {

    public interface Listener {
        void onTaskCompleted(String resultatJson);
    }

    private volatile Listener listener;
    private String token;

    public ListefilmsTask(String token, Listener listener) {
        this.token = token;
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        // Prétraitement de l'appel (ex: afficher un loader)
        Log.d("mydebug", ">>> onPreExecute - Début de l'appel REST");
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
        Log.d("mydebug", ">>> onPostExecute / resultat=" +
            (resultat != null ? resultat.substring(0, Math.min(200, resultat.length())) : "null"));

        // IMPORTANT : on appelle ici la méthode de mise à jour de l'activity appelante
        if (listener != null) {
            listener.onTaskCompleted(resultat);
        }
    }

    /**
     * Méthode privée qui fait l'appel HTTP effectif
     * Modèle conforme au cours du 13/11/2025
     */
    private String appelerServiceRestHttp(URL urlAAppeler) {
        HttpURLConnection urlConnection = null;
        int responseCode = -1;
        StringBuilder sResultatAppel = new StringBuilder();

        try {
            // Exemple pour un appel GET
            urlConnection = (HttpURLConnection) urlAAppeler.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("User-Agent", System.getProperty("http.agent"));

            // OPTIMISATION : Ajouter des timeouts pour éviter l'attente infinie
            urlConnection.setConnectTimeout(10000); // 10 secondes pour se connecter
            urlConnection.setReadTimeout(15000);     // 15 secondes pour lire les données

            // Ajouter le token JWT si présent
            if (token != null && !token.isEmpty()) {
                urlConnection.setRequestProperty("Authorization", "Bearer " + token);
            }

            responseCode = urlConnection.getResponseCode();
            Log.d("mydebug", ">>> Response Code: " + responseCode);

            if (responseCode == 200) {
                // OPTIMISATION : Utiliser BufferedReader au lieu de lire caractère par caractère
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(urlConnection.getInputStream())
                );

                String ligne;
                while ((ligne = reader.readLine()) != null) {
                    sResultatAppel.append(ligne);
                }
                reader.close();

                Log.d("mydebug", ">>> Données reçues : " + sResultatAppel.length() + " caractères");
            } else {
                Log.d("mydebug", ">>> Erreur HTTP: " + responseCode);
            }

        } catch (java.net.SocketTimeoutException ste) {
            Log.d("mydebug", ">>> TIMEOUT - Le serveur met trop de temps à répondre: " + ste.toString());
        } catch (IOException ioe) {
            Log.d("mydebug", ">>> Pour appelerServiceRestHttp - IOException ioe=" + ioe.toString());
        } catch (Exception e) {
            Log.d("mydebug", ">>> Pour appelerServiceRestHttp - Exception=" + e.toString());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return sResultatAppel.toString();
    }
}
