package com.example.applicationrftg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MenuActivity extends AppCompatActivity {

    private CardView btnVoirFilms, btnVoirPanier, btnDeconnexion;
    private TextView txtPanierCount;
    private int customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("mydebug", ">>> MenuActivity.onCreate - DEBUT");

        setContentView(R.layout.activity_menu);

        // Récupérer le customerId depuis l'intent
        customerId = getIntent().getIntExtra("customerId", -1);
        Log.d("mydebug", ">>> MenuActivity - CustomerId reçu: " + customerId);

        btnVoirFilms = findViewById(R.id.btnVoirFilms);
        btnVoirPanier = findViewById(R.id.btnVoirPanier);
        btnDeconnexion = findViewById(R.id.btnDeconnexion);
        txtPanierCount = findViewById(R.id.txtPanierCount);

        // Afficher le nombre d'articles dans le panier
        int panierSize = PanierActivity.panier.size();
        txtPanierCount.setText(String.valueOf(panierSize));

        btnVoirFilms.setOnClickListener(v -> {
            Log.d("mydebug", ">>> MenuActivity - Clic sur btnVoirFilms");
            Intent intent = new Intent(MenuActivity.this, ListefilmsActivity.class);
            intent.putExtra("customerId", customerId);
            startActivity(intent);
        });

        btnVoirPanier.setOnClickListener(v -> {
            Log.d("mydebug", ">>> MenuActivity - Clic sur btnVoirPanier");
            Intent intent = new Intent(MenuActivity.this, PanierActivity.class);
            startActivity(intent);
        });

        btnDeconnexion.setOnClickListener(v -> {
            Log.d("mydebug", ">>> MenuActivity - Déconnexion");
            // Le panier reste sauvegardé dans SQLite lors de la déconnexion
            // On vide juste la mémoire
            PanierActivity.panier.clear();
            // Retourner à l'écran de connexion
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        Log.d("mydebug", ">>> MenuActivity.onCreate - FIN");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mettre à jour le compteur du panier quand on revient sur cet écran
        int panierSize = PanierActivity.panier.size();
        txtPanierCount.setText(String.valueOf(panierSize));
    }
}
