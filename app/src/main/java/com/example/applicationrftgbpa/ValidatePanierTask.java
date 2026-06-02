package com.example.applicationtftgbpa;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            // Étape 1 : s'assurer d'avoir un JWT
            String jwt = DonneesPartagees.getJwt();
            if (jwt == null || jwt.isEmpty()) {
                jwt = fetchStaffJwt();
                DonneesPartagees.setJwt(jwt);
            }

            // Étape 2 : essayer /rentals/validate (nouveau serveur)
            String baseUrl = DonneesPartagees.getURLConnexion();
            Log.d("mydebug", ">>> ValidatePanierTask - Essai /rentals/validate");

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("customerId", customerId);
            JSONArray filmIdsArray = new JSONArray();
            for (Integer filmId : filmIds) filmIdsArray.put(filmId);
            jsonBody.put("filmIds", filmIdsArray);
            Log.d("mydebug", ">>> ValidatePanierTask - Body: " + jsonBody.toString());

            HttpURLConnection conn = openPostConnection(baseUrl + "/rentals/validate", jwt);
            conn.getOutputStream().write(jsonBody.toString().getBytes("UTF-8"));
            int code = conn.getResponseCode();
            Log.d("mydebug", ">>> ValidatePanierTask - Response Code /rentals/validate: " + code);

            if (code == 200) {
                String body = readResponse(conn);
                Log.d("mydebug", ">>> ValidatePanierTask - Response: " + body);
                return "SUCCESS:" + body;
            }

            // Étape 3 : fallback — créer les rentals un par un via POST /rentals
            if (jwt == null || jwt.isEmpty()) {
                return "ERROR:Impossible d'obtenir un token d'authentification";
            }
            Log.d("mydebug", ">>> ValidatePanierTask - Fallback POST /rentals");
            return fallbackPostRentals(baseUrl, jwt);

        } catch (Exception e) {
            Log.d("mydebug", ">>> ValidatePanierTask - Exception: " + e.toString());
            return "ERROR:" + e.getMessage();
        }
    }

    /** Authentification staff via /api/auth/login pour obtenir un JWT */
    private String fetchStaffJwt() {
        try {
            String url = DonneesPartagees.getURLConnexion() + "/api/auth/login";
            Log.d("mydebug", ">>> ValidatePanierTask - fetchStaffJwt: " + url);
            HttpURLConnection conn = openPostConnection(url, null);
            JSONObject body = new JSONObject();
            body.put("username", "matthieu");
            body.put("password", "password");
            conn.getOutputStream().write(body.toString().getBytes("UTF-8"));
            if (conn.getResponseCode() == 200) {
                JSONObject json = new JSONObject(readResponse(conn));
                String token = json.optString("token", "");
                Log.d("mydebug", ">>> ValidatePanierTask - JWT staff obtenu: " + (token.isEmpty() ? "NON" : "OUI"));
                return token;
            }
        } catch (Exception e) {
            Log.d("mydebug", ">>> ValidatePanierTask - fetchStaffJwt exception: " + e.toString());
        }
        return "";
    }

    /**
     * Fallback : charge les inventories, trouve le bon inventoryId par filmId,
     * puis crée un rental par film via POST /rentals
     */
    private String fallbackPostRentals(String baseUrl, String jwt) {
        try {
            // Charger les inventories pour trouver inventoryId par filmId
            Map<Integer, Integer> filmToInventory = fetchInventoryMap(baseUrl, jwt);

            int created = 0;
            for (Integer filmId : filmIds) {
                Integer inventoryId = filmToInventory.get(filmId);
                if (inventoryId == null) {
                    Log.d("mydebug", ">>> ValidatePanierTask - Pas d'inventory pour filmId=" + filmId);
                    continue;
                }
                // Créer le rental
                JSONObject rental = new JSONObject();
                rental.put("inventoryId", inventoryId);
                rental.put("customerId", customerId);
                rental.put("staffId", 1);
                rental.put("statusId", 3);
                String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                rental.put("rentalDate", now);
                rental.put("lastUpdate", now);

                HttpURLConnection conn = openPostConnection(baseUrl + "/rentals", jwt);
                conn.getOutputStream().write(rental.toString().getBytes("UTF-8"));
                int code = conn.getResponseCode();
                Log.d("mydebug", ">>> ValidatePanierTask - POST /rentals filmId=" + filmId + " code=" + code);
                if (code == 200 || code == 201) created++;
            }

            JSONObject result = new JSONObject();
            result.put("rentalsCreated", created);
            result.put("success", created > 0);
            return "SUCCESS:" + result.toString();

        } catch (Exception e) {
            Log.d("mydebug", ">>> ValidatePanierTask - fallbackPostRentals exception: " + e.toString());
            return "ERROR:" + e.getMessage();
        }
    }

    /** Charge tous les inventories et retourne un map filmId → inventoryId */
    private Map<Integer, Integer> fetchInventoryMap(String baseUrl, String jwt) {
        Map<Integer, Integer> map = new HashMap<>();
        try {
            URL url = new URL(baseUrl + "/inventories");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + jwt);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            if (conn.getResponseCode() == 200) {
                JSONArray arr = new JSONArray(readResponse(conn));
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject inv = arr.getJSONObject(i);
                    int filmId = inv.getInt("filmId");
                    int inventoryId = inv.getInt("inventoryId");
                    if (!map.containsKey(filmId)) {
                        map.put(filmId, inventoryId);
                    }
                }
                Log.d("mydebug", ">>> ValidatePanierTask - Inventories chargés: " + map.size() + " films");
            }
        } catch (Exception e) {
            Log.d("mydebug", ">>> ValidatePanierTask - fetchInventoryMap exception: " + e.toString());
        }
        return map;
    }

    private HttpURLConnection openPostConnection(String urlStr, String jwt) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        if (jwt != null && !jwt.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + jwt);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
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
            listener.onValidationError(result.substring(6));
        }
    }
}
