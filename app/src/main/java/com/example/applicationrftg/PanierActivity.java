package com.example.applicationrftg;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class PanierActivity extends AppCompatActivity {

    public static ArrayList<HashMap<String, String>> panier = new ArrayList<>();
    private static PanierDBHelper dbHelper;

    // Classe interne pour gérer SQLite
    static class PanierDBHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "panier.db";
        private static final int DATABASE_VERSION = 1;
        private static final String TABLE_PANIER = "panier";
        private static final String COL_ID = "id";
        private static final String COL_FILM_ID = "film_id";
        private static final String COL_TITLE = "title";
        private static final String COL_RENTAL_RATE = "rental_rate";

        public PanierDBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + TABLE_PANIER + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_FILM_ID + " TEXT, " +
                    COL_TITLE + " TEXT, " +
                    COL_RENTAL_RATE + " TEXT)";
            db.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANIER);
            onCreate(db);
        }

        // Sauvegarder un film dans le panier
        public void ajouterFilm(HashMap<String, String> film) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_FILM_ID, film.get("filmId"));
            values.put(COL_TITLE, film.get("title"));
            values.put(COL_RENTAL_RATE, film.get("rentalRate"));
            db.insert(TABLE_PANIER, null, values);
            db.close();
        }

        // Charger le panier depuis la BDD
        public ArrayList<HashMap<String, String>> chargerPanier() {
            ArrayList<HashMap<String, String>> liste = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PANIER, null);

            if (cursor.moveToFirst()) {
                do {
                    HashMap<String, String> film = new HashMap<>();
                    film.put("filmId", cursor.getString(cursor.getColumnIndexOrThrow(COL_FILM_ID)));
                    film.put("title", cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
                    film.put("rentalRate", cursor.getString(cursor.getColumnIndexOrThrow(COL_RENTAL_RATE)));
                    liste.add(film);
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
            return liste;
        }

        // Vider le panier
        public void viderPanier() {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_PANIER, null, null);
            db.close();
        }

        // Supprimer un film spécifique du panier
        public void supprimerFilm(String filmId) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_PANIER, COL_FILM_ID + " = ?", new String[]{filmId});
            db.close();
        }
    }

    // Méthode statique pour ajouter un film et le sauvegarder
    public static void ajouterAuPanier(Context context, HashMap<String, String> film) {
        if (dbHelper == null) {
            dbHelper = new PanierDBHelper(context);
        }
        panier.add(film);
        dbHelper.ajouterFilm(film);
    }

    private ListView listViewPanier;
    private TextView txtTotal, txtMessagePanier;
    private Button btnCommander;

    // Méthode pour supprimer un film du panier
    private void supprimerFilm(HashMap<String, String> film) {
        if (dbHelper != null && film.get("filmId") != null) {
            dbHelper.supprimerFilm(film.get("filmId"));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        // Initialiser la base de données
        if (dbHelper == null) {
            dbHelper = new PanierDBHelper(this);
        }

        // Charger le panier depuis SQLite si vide en mémoire
        if (panier.isEmpty()) {
            panier = dbHelper.chargerPanier();
        }

        listViewPanier = findViewById(R.id.listViewPanier);
        txtTotal = findViewById(R.id.txtTotal);
        txtMessagePanier = findViewById(R.id.txtMessagePanier);
        btnCommander = findViewById(R.id.btnCommander);

        if (panier.isEmpty()) {
            txtMessagePanier.setText("Votre panier est vide");
            txtTotal.setText("Total : 0.0 €");
            listViewPanier.setAdapter(null);
            btnCommander.setEnabled(false);
            return;
        }

        btnCommander.setEnabled(true);

        txtMessagePanier.setText("");

        // Adapter personnalisé avec layout créé dynamiquement
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                panier,
                android.R.layout.simple_list_item_2,
                new String[]{"title", "rentalRate"},
                new int[]{android.R.id.text1, android.R.id.text2}
        ) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                // Créer un layout horizontal personnalisé
                android.widget.LinearLayout container = new android.widget.LinearLayout(PanierActivity.this);
                container.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                container.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                container.setGravity(android.view.Gravity.CENTER_VERTICAL);
                container.setPadding(16, 16, 16, 16);

                // Layout vertical pour le titre et prix
                android.widget.LinearLayout textContainer = new android.widget.LinearLayout(PanierActivity.this);
                textContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
                android.widget.LinearLayout.LayoutParams textParams = new android.widget.LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                textContainer.setLayoutParams(textParams);

                // Titre du film
                TextView txtTitre = new TextView(PanierActivity.this);
                txtTitre.setText(panier.get(position).get("title"));
                txtTitre.setTextSize(16);
                txtTitre.setTextColor(0xFFFFFFFF);
                txtTitre.setMaxLines(2);
                textContainer.addView(txtTitre);

                // Prix du film
                TextView txtPrix = new TextView(PanierActivity.this);
                txtPrix.setText(panier.get(position).get("rentalRate") + " €");
                txtPrix.setTextSize(14);
                txtPrix.setTextColor(0xFF00D9A3);
                txtPrix.setPadding(0, 4, 0, 0);
                textContainer.addView(txtPrix);

                container.addView(textContainer);

                // Bouton supprimer
                Button btnSupprimer = new Button(PanierActivity.this);
                android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                btnSupprimer.setLayoutParams(btnParams);
                btnSupprimer.setText("❌");
                btnSupprimer.setTextSize(20);
                btnSupprimer.setBackgroundColor(0x00000000); // Transparent
                btnSupprimer.setPadding(16, 8, 16, 8);

                // Gérer le clic sur supprimer
                btnSupprimer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Supprimer de la BDD
                        HashMap<String, String> filmASupprimer = panier.get(position);
                        supprimerFilm(filmASupprimer);

                        // Supprimer de la liste
                        panier.remove(position);

                        // Rafraîchir l'affichage
                        notifyDataSetChanged();

                        // Recalculer le total
                        double nouveauTotal = 0.0;
                        for (HashMap<String, String> ligne : panier) {
                            try {
                                nouveauTotal += Double.parseDouble(ligne.get("rentalRate"));
                            } catch (Exception ignored) { }
                        }
                        txtTotal.setText("Total : " + nouveauTotal + " €");

                        // Si panier vide
                        if (panier.isEmpty()) {
                            txtMessagePanier.setText("Votre panier est vide");
                            txtTotal.setText("Total : 0.0 €");
                            listViewPanier.setAdapter(null);
                            btnCommander.setEnabled(false);
                        }

                        Toast.makeText(PanierActivity.this, "Film supprimé ❌", Toast.LENGTH_SHORT).show();
                    }
                });

                container.addView(btnSupprimer);

                return container;
            }
        };

        listViewPanier.setAdapter(adapter);

        double total = 0.0;
        for (HashMap<String, String> ligne : panier) {
            try {
                total += Double.parseDouble(ligne.get("rentalRate"));
            } catch (Exception ignored) { }
        }

        final double totalFinal = total;
        txtTotal.setText("Total : " + total + " €");

        // Gestion du clic sur le bouton Commander
        btnCommander.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Afficher un message de confirmation
                Toast.makeText(PanierActivity.this,
                    "Commande validée ! Total : " + totalFinal + " €",
                    Toast.LENGTH_LONG).show();

                // Vider le panier après la commande (mémoire + BDD)
                panier.clear();
                dbHelper.viderPanier();

                // Rafraîchir l'affichage
                txtMessagePanier.setText("Votre panier est vide");
                txtTotal.setText("Total : 0.0 €");
                listViewPanier.setAdapter(null);
                btnCommander.setEnabled(false);
            }
        });
    }
}

