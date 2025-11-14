package com.example.applicationrftg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Activity pour afficher la liste des films
 * Utilise Gson selon le cours du 13/11/2025 (page 15)
 */
public class ListefilmsActivity extends AppCompatActivity implements ListefilmsTask.Listener {

    private ListView listView;
    private TextView msg;
    private ProgressBar progressBar;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listefilm);

        Log.d("mydebug", ">>> ListefilmsActivity.onCreate - DEBUT");

        listView = findViewById(R.id.listViewFilms);
        msg = findViewById(R.id.txtMessage);
        progressBar = findViewById(R.id.progressBar);

        token = getIntent().getStringExtra("token");
        Log.d("mydebug", ">>> Token reçu: " + (token != null ? token.substring(0, 20) + "..." : "NULL"));

        // Appel du service REST via AsyncTask (modèle du cours)
        URL urlAAppeler = null;
        try {
            // 10.0.2.2 = adresse spéciale pour l'émulateur (équivalent de localhost)
            // Limiter à 20 films pour tester
            urlAAppeler = new URL("http://10.0.2.2:8180/films?limit=20");
            Log.d("mydebug", ">>> URL créée: " + urlAAppeler.toString());
            Log.d("mydebug", ">>> Lancement de ListefilmsTask...");
            new ListefilmsTask(token, this).execute(urlAAppeler);
            Log.d("mydebug", ">>> ListefilmsTask lancée");
        } catch (MalformedURLException mue) {
            Log.d("mydebug", ">>> MalformedURLException: " + mue.toString());
            msg.setText("Erreur : URL invalide");
            msg.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Méthode appelée automatiquement par ListefilmsTask après l'appel REST
     * (Modèle du cours du 13/11/2025)
     */
    @Override
    public void onTaskCompleted(String resultatJson) {
        progressBar.setVisibility(View.GONE);

        Log.d("mydebug", ">>> onTaskCompleted - resultat reçu");
        Log.d("mydebug", ">>> Contenu JSON: " + (resultatJson != null ? resultatJson : "NULL"));

        if (resultatJson == null || resultatJson.isEmpty()) {
            msg.setText("Aucune donnée reçue du serveur");
            msg.setVisibility(View.VISIBLE);
            return;
        }

        // Conversion du JSON en ArrayList<HashMap> avec Gson (modèle du cours page 15)
        ArrayList<HashMap<String, Object>> listeFilms = null;
        try {
            listeFilms = convertirListeFilmsEnArrayList(resultatJson);
        } catch (Exception e) {
            Log.d("mydebug", ">>> ERREUR conversion JSON: " + e.getMessage());
            msg.setText("Erreur de conversion JSON: " + e.getMessage());
            msg.setVisibility(View.VISIBLE);
            return;
        }

        if (listeFilms == null || listeFilms.isEmpty()) {
            msg.setText("Aucun film trouvé");
            msg.setVisibility(View.VISIBLE);
            return;
        }

        msg.setVisibility(View.GONE);

        // Affichage dans la liste
        afficherListeFilms(listeFilms);
    }

    /**
     * Conversion de la chaîne JSON en ArrayList avec Gson
     * (Modèle du cours du 13/11/2025 - page 15)
     */
    private ArrayList<HashMap<String, Object>> convertirListeFilmsEnArrayList(String filmsJson) {
        Gson gson = new Gson();

        Type filmListType = new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType();
        ArrayList<HashMap<String, Object>> filmArray = gson.fromJson(filmsJson, filmListType);

        // Contrôle (debug)
        Log.d("mydebug", ">>> Les films >>> DEBUT");
        if (filmArray != null) {
            for(HashMap<String, Object> film : filmArray) {
                Log.d("mydebug", "Film: " + film.get("title") + " (" + film.get("releaseYear") + ")");
            }
        }
        Log.d("mydebug", ">>> Les films >>> FIN");

        return filmArray;
    }

    /**
     * Affiche la liste des films dans le ListView
     */
    private void afficherListeFilms(ArrayList<HashMap<String, Object>> films) {
        // Conversion pour SimpleAdapter
        ArrayList<HashMap<String, String>> listeData = new ArrayList<>();

        for (HashMap<String, Object> film : films) {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", String.valueOf(film.get("filmId")));
            map.put("title", String.valueOf(film.get("title")));
            // Afficher l'année + la note
            String yearAndRating = String.valueOf(film.get("releaseYear")) + " - Note: " + String.valueOf(film.get("rating"));
            map.put("year", yearAndRating);
            map.put("rating", String.valueOf(film.get("rating")));
            listeData.add(map);
        }

        // Créer un adaptateur personnalisé avec un layout custom pour avoir du texte blanc
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                listeData,
                android.R.layout.simple_list_item_2,
                new String[]{"title", "year"},
                new int[]{android.R.id.text1, android.R.id.text2}
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                // Forcer le texte en blanc
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                if (text1 != null) text1.setTextColor(0xFFFFFFFF); // Blanc
                if (text2 != null) text2.setTextColor(0xFFCCCCCC); // Gris clair
                return view;
            }
        };

        listView.setAdapter(adapter);

        // Clic sur un film → afficher le détail
        listView.setOnItemClickListener((parent, view, position, id) -> {
            HashMap<String, Object> film = films.get(position);

            Intent intent = new Intent(ListefilmsActivity.this, Detailfilms.class);
            intent.putExtra("id", String.valueOf(film.get("filmId")));
            intent.putExtra("title", String.valueOf(film.get("title")));
            intent.putExtra("description", String.valueOf(film.get("description")));
            intent.putExtra("releaseYear", String.valueOf(film.get("releaseYear")));
            intent.putExtra("rating", String.valueOf(film.get("rating")));
            intent.putExtra("price", String.valueOf(film.get("rentalRate")));
            startActivity(intent);
        });
    }
}
