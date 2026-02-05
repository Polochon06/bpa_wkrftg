package com.example.applicationrftg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Activity pour afficher la liste des films avec filtrage par catégorie
 */
public class ListefilmsActivity extends AppCompatActivity implements ListefilmsTask.Listener, CategoriesTask.Listener {

    private ListView listView;
    private TextView msg;
    private ProgressBar progressBar;
    private Spinner spinnerCategory;
    private Button btnFilter;
    private int customerId;

    private ArrayList<HashMap<String, Object>> categories = new ArrayList<>();
    private Integer selectedCategoryId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listefilm);

        Log.d("mydebug", ">>> ListefilmsActivity.onCreate - DEBUT");

        listView = findViewById(R.id.listViewFilms);
        msg = findViewById(R.id.txtMessage);
        progressBar = findViewById(R.id.progressBar);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnFilter = findViewById(R.id.btnFilter);

        customerId = getIntent().getIntExtra("customerId", -1);
        Log.d("mydebug", ">>> CustomerId reçu: " + customerId);

        // Charger les catégories
        chargerCategories();

        // Charger tous les films au départ
        chargerFilms(null);

        // Listener du bouton Filtrer
        btnFilter.setOnClickListener(v -> {
            int position = spinnerCategory.getSelectedItemPosition();

            if (position == 0) {
                // "Toutes les catégories" sélectionné
                selectedCategoryId = null;
            } else {
                // Récupérer l'ID de la catégorie sélectionnée
                HashMap<String, Object> selectedCategory = categories.get(position - 1);
                selectedCategoryId = ((Double) selectedCategory.get("categoryId")).intValue();
            }

            // Recharger les films avec le filtre
            chargerFilms(selectedCategoryId);
        });
    }

    /**
     * Charge les catégories depuis le serveur
     */
    private void chargerCategories() {
        try {
            URL urlCategories = new URL("http://192.168.30.124:8180/categories");
            new CategoriesTask(this).execute(urlCategories);
        } catch (MalformedURLException e) {
            Log.d("mydebug", ">>> Erreur URL catégories: " + e.toString());
        }
    }

    /**
     * Charge les films depuis le serveur
     * @param categoryId ID de la catégorie (null pour tous les films)
     */
    private void chargerFilms(Integer categoryId) {
        progressBar.setVisibility(View.VISIBLE);
        msg.setVisibility(View.GONE);

        try {
            String urlString;
            if (categoryId != null) {
                urlString = "http://192.168.30.124:8180/films?categoryId=" + categoryId + "&limit=50";
            } else {
                urlString = "http://192.168.30.124:8180/films?limit=20";
            }

            URL urlFilms = new URL(urlString);
            Log.d("mydebug", ">>> URL films: " + urlString);
            new ListefilmsTask(null, this).execute(urlFilms);
        } catch (MalformedURLException e) {
            Log.d("mydebug", ">>> Erreur URL films: " + e.toString());
            msg.setText("Erreur : URL invalide");
            msg.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Callback quand les catégories sont chargées
     */
    @Override
    public void onCategoriesLoaded(String resultatJson) {
        Log.d("mydebug", ">>> Catégories reçues");

        if (resultatJson == null || resultatJson.isEmpty()) {
            Log.d("mydebug", ">>> Aucune catégorie reçue");
            return;
        }

        try {
            Gson gson = new Gson();
            Type categoryListType = new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType();
            categories = gson.fromJson(resultatJson, categoryListType);

            // Créer la liste des noms de catégories pour le Spinner
            List<String> categoryNames = new ArrayList<>();
            categoryNames.add("Toutes les catégories"); // Option par défaut

            for (HashMap<String, Object> category : categories) {
                categoryNames.add(String.valueOf(category.get("name")));
            }

            // Adapter le Spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);

            Log.d("mydebug", ">>> " + categories.size() + " catégories chargées");

        } catch (Exception e) {
            Log.d("mydebug", ">>> Erreur conversion JSON catégories: " + e.getMessage());
        }
    }

    /**
     * Callback quand les films sont chargés
     */
    @Override
    public void onTaskCompleted(String resultatJson) {
        progressBar.setVisibility(View.GONE);

        Log.d("mydebug", ">>> Films reçus");

        if (resultatJson == null || resultatJson.isEmpty()) {
            msg.setText("Aucune donnée reçue du serveur");
            msg.setVisibility(View.VISIBLE);
            return;
        }

        try {
            ArrayList<HashMap<String, Object>> listeFilms = convertirListeFilmsEnArrayList(resultatJson);

            if (listeFilms == null || listeFilms.isEmpty()) {
                msg.setText("Aucun film trouvé pour cette catégorie");
                msg.setVisibility(View.VISIBLE);
                listView.setAdapter(null);
                return;
            }

            msg.setVisibility(View.GONE);
            afficherListeFilms(listeFilms);

        } catch (Exception e) {
            Log.d("mydebug", ">>> ERREUR conversion JSON: " + e.getMessage());
            msg.setText("Erreur de conversion JSON: " + e.getMessage());
            msg.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Conversion de la chaîne JSON en ArrayList avec Gson
     */
    private ArrayList<HashMap<String, Object>> convertirListeFilmsEnArrayList(String filmsJson) {
        Gson gson = new Gson();

        Type filmListType = new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType();
        ArrayList<HashMap<String, Object>> filmArray = gson.fromJson(filmsJson, filmListType);

        Log.d("mydebug", ">>> " + (filmArray != null ? filmArray.size() : 0) + " films chargés");

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
