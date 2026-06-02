package com.example.applicationtftgbpa;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class Detailfilms extends AppCompatActivity {

    TextView titre, description, annee, note, txtNbComments;
    Button btnAjouterPanier;
    LinearLayout commentsContainer;
    ProgressBar progressComments;
    EditText editCommentaire;
    MaterialButton btnEnvoyerCommentaire;

    private int filmIdInt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailfilm);

        titre                  = findViewById(R.id.txtTitre);
        description            = findViewById(R.id.txtDescription);
        annee                  = findViewById(R.id.txtAnnee);
        note                   = findViewById(R.id.txtNote);
        btnAjouterPanier       = findViewById(R.id.btnAjouterPanier);
        commentsContainer      = findViewById(R.id.commentsContainer);
        progressComments       = findViewById(R.id.progressComments);
        txtNbComments          = findViewById(R.id.txtNbComments);
        editCommentaire        = findViewById(R.id.editCommentaire);
        btnEnvoyerCommentaire  = findViewById(R.id.btnEnvoyerCommentaire);

        final String filmIdStr = getIntent().getStringExtra("id");
        final String filmTitle = getIntent().getStringExtra("title");

        try {
            filmIdInt = Integer.parseInt(filmIdStr);
        } catch (Exception e) {
            filmIdInt = -1;
        }

        titre.setText(filmTitle);
        description.setText(getIntent().getStringExtra("description"));
        annee.setText("Année : " + getIntent().getStringExtra("releaseYear"));
        note.setText(getIntent().getStringExtra("rating"));

        // Ajouter au panier
        btnAjouterPanier.setOnClickListener(v -> {
            HashMap<String, String> filmPanier = new HashMap<>();
            filmPanier.put("filmId", filmIdStr);
            filmPanier.put("title", filmTitle);
            PanierActivity.ajouterAuPanier(Detailfilms.this, filmPanier);
            Toast.makeText(Detailfilms.this, "Film ajouté au panier ! 🎫", Toast.LENGTH_SHORT).show();
        });

        // Envoyer un commentaire
        btnEnvoyerCommentaire.setOnClickListener(v -> envoyerCommentaire());

        // Charger les commentaires existants
        chargerCommentaires();
    }

    // ─── Chargement des commentaires ────────────────────────────────────────────

    private void chargerCommentaires() {
        if (filmIdInt == -1) {
            afficherCommentaires(null);
            return;
        }
        new CommentsTask(filmIdInt, json -> afficherCommentaires(json)).execute();
    }

    private void afficherCommentaires(String json) {
        progressComments.setVisibility(View.GONE);
        commentsContainer.setVisibility(View.VISIBLE);
        commentsContainer.removeAllViews();

        if (json == null || json.isEmpty()) {
            ajouterMessageVide();
            return;
        }

        try {
            JSONArray array = new JSONArray(json);
            if (array.length() == 0) {
                ajouterMessageVide();
                return;
            }

            txtNbComments.setText(array.length() + " avis");

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                // Gestion souple des noms de champs selon le backend
                String nom = obj.optString("customerName",
                             obj.optString("name",
                             obj.optString("firstName", "Anonyme")));
                String texte = obj.optString("commentText",
                               obj.optString("comment",
                               obj.optString("text", "")));

                if (!texte.isEmpty()) {
                    ajouterCommentaire(nom, texte, i);
                }
            }

        } catch (Exception e) {
            Log.d("mydebug", "Erreur parsing commentaires: " + e.getMessage());
            ajouterMessageVide();
        }
    }

    // ─── Envoi d'un commentaire ──────────────────────────────────────────────────

    private void envoyerCommentaire() {
        String texte = editCommentaire.getText().toString().trim();

        if (texte.isEmpty()) {
            Toast.makeText(this, "Veuillez écrire un commentaire.", Toast.LENGTH_SHORT).show();
            return;
        }

        int customerId = DonneesPartagees.getCustomerId();
        if (customerId == -1) {
            Toast.makeText(this, "Vous devez être connecté pour commenter.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEnvoyerCommentaire.setEnabled(false);
        btnEnvoyerCommentaire.setText("Envoi...");

        new AddCommentTask(filmIdInt, customerId, texte, success -> {
            btnEnvoyerCommentaire.setEnabled(true);
            btnEnvoyerCommentaire.setText("Envoyer");

            if (success) {
                editCommentaire.setText("");
                // Fermer le clavier
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(editCommentaire.getWindowToken(), 0);
                Toast.makeText(this, "Commentaire publié !", Toast.LENGTH_SHORT).show();
                // Recharger la liste
                progressComments.setVisibility(View.VISIBLE);
                commentsContainer.setVisibility(View.GONE);
                chargerCommentaires();
            } else {
                Toast.makeText(this, "Erreur lors de l'envoi.", Toast.LENGTH_SHORT).show();
            }
        }).execute();
    }

    // ─── Helpers d'affichage ─────────────────────────────────────────────────────

    private void ajouterCommentaire(String nom, String texte, int index) {
        // Séparateur entre commentaires
        if (index > 0) {
            View sep = new View(this);
            sep.setBackgroundColor(Color.parseColor("#2A2A3E"));
            LinearLayout.LayoutParams pSep = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
            pSep.topMargin    = dpToPx(10);
            pSep.bottomMargin = dpToPx(10);
            commentsContainer.addView(sep, pSep);
        }

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);

        // Ligne avatar + nom
        LinearLayout headerItem = new LinearLayout(this);
        headerItem.setOrientation(LinearLayout.HORIZONTAL);
        headerItem.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView avatar = new TextView(this);
        avatar.setText(nom.isEmpty() ? "?" : nom.substring(0, 1).toUpperCase());
        avatar.setTextSize(13);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setTextColor(Color.WHITE);
        avatar.setGravity(android.view.Gravity.CENTER);
        int size = dpToPx(32);
        LinearLayout.LayoutParams pAvatar = new LinearLayout.LayoutParams(size, size);
        pAvatar.setMarginEnd(dpToPx(10));
        avatar.setLayoutParams(pAvatar);
        avatar.setBackground(cercleColore(index));

        TextView tvNom = new TextView(this);
        tvNom.setText(nom);
        tvNom.setTextSize(13);
        tvNom.setTypeface(null, Typeface.BOLD);
        tvNom.setTextColor(Color.parseColor("#6C63FF"));

        headerItem.addView(avatar);
        headerItem.addView(tvNom);

        // Texte
        TextView tvTexte = new TextView(this);
        tvTexte.setText(texte);
        tvTexte.setTextSize(13);
        tvTexte.setTextColor(Color.parseColor("#CCCCCC"));
        tvTexte.setLineSpacing(0, 1.3f);
        LinearLayout.LayoutParams pTexte = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pTexte.topMargin    = dpToPx(4);
        pTexte.setMarginStart(dpToPx(42));
        tvTexte.setLayoutParams(pTexte);

        item.addView(headerItem);
        item.addView(tvTexte);
        commentsContainer.addView(item);
    }

    private void ajouterMessageVide() {
        txtNbComments.setText("0 avis");
        TextView tv = new TextView(this);
        tv.setText("Aucun commentaire pour ce film.");
        tv.setTextSize(13);
        tv.setTextColor(Color.parseColor("#777777"));
        tv.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin    = dpToPx(8);
        p.bottomMargin = dpToPx(8);
        commentsContainer.addView(tv, p);
    }

    private android.graphics.drawable.GradientDrawable cercleColore(int index) {
        int[] couleurs = {
            Color.parseColor("#6C63FF"),
            Color.parseColor("#FF6363"),
            Color.parseColor("#00D9A3"),
            Color.parseColor("#FF9900"),
            Color.parseColor("#2E75B6"),
        };
        android.graphics.drawable.GradientDrawable shape =
                new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(couleurs[index % couleurs.length]);
        return shape;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
