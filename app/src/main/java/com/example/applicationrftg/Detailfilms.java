package com.example.applicationrftg;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class Detailfilms extends AppCompatActivity {

    TextView titre, description, annee, note, prix;
    Button btnAjouterPanier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailfilm);

        titre = findViewById(R.id.txtTitre);
        description = findViewById(R.id.txtDescription);
        annee = findViewById(R.id.txtAnnee);
        note = findViewById(R.id.txtNote);
        prix = findViewById(R.id.txtPrix);
        btnAjouterPanier = findViewById(R.id.btnAjouterPanier);

        final String filmTitle = getIntent().getStringExtra("title");
        final String filmPrice = getIntent().getStringExtra("price");

        titre.setText(filmTitle);
        description.setText(getIntent().getStringExtra("description"));
        annee.setText("Année : " + getIntent().getStringExtra("releaseYear"));
        note.setText("Note : " + getIntent().getStringExtra("rating"));
        prix.setText("Prix : " + filmPrice + " €");

        // Gestion du clic sur le bouton Ajouter au panier
        btnAjouterPanier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Créer un HashMap avec les infos du film
                HashMap<String, String> filmPanier = new HashMap<>();
                filmPanier.put("filmId", getIntent().getStringExtra("filmId"));
                filmPanier.put("title", filmTitle);
                filmPanier.put("rentalRate", filmPrice);

                // Ajouter au panier avec sauvegarde SQLite
                PanierActivity.ajouterAuPanier(Detailfilms.this, filmPanier);

                // Afficher un message de confirmation
                Toast.makeText(Detailfilms.this,
                    "Film ajouté au panier ! 🎫",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}

