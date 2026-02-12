package com.example.applicationrftg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity
        implements LoginTask.LoginTaskListener, AdapterView.OnItemSelectedListener {

    private TextInputEditText editEmail, editPassword;
    private MaterialButton btnConnexion;
    private ProgressBar progressBar;
    private TextView txtMessage;
    private String[] listeURLs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("mydebug", ">>> MainActivity (Login) - DEBUT");

        setContentView(R.layout.activity_main);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnConnexion = findViewById(R.id.btnConnexion);
        progressBar = findViewById(R.id.progressBarLogin);
        txtMessage = findViewById(R.id.txtMessageLogin);

        // Configurer le Spinner de sélection d'URL
        listeURLs = getResources().getStringArray(R.array.listeURLs);
        Spinner spinnerURLs = findViewById(R.id.spinnerURLs);
        spinnerURLs.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapterListeURLs = ArrayAdapter.createFromResource(
                this, R.array.listeURLs, android.R.layout.simple_spinner_item);
        adapterListeURLs.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapterListeURLs);

        btnConnexion.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                txtMessage.setText("Veuillez remplir tous les champs");
                return;
            }

            if (DonneesPartagees.getURLConnexion().isEmpty()) {
                txtMessage.setText("Veuillez sélectionner un serveur");
                return;
            }

            // Afficher le loader
            progressBar.setVisibility(View.VISIBLE);
            txtMessage.setText("");
            btnConnexion.setEnabled(false);

            Log.d("mydebug", ">>> MainActivity - Tentative de connexion avec: " + email);
            Log.d("mydebug", ">>> MainActivity - URL serveur: " + DonneesPartagees.getURLConnexion());

            // Lancer la tâche de connexion
            LoginTask loginTask = new LoginTask(MainActivity.this, this);
            loginTask.execute(email, password);
        });

        Log.d("mydebug", ">>> MainActivity (Login) - FIN");
    }

    // Callback quand une URL est sélectionnée dans le Spinner
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        DonneesPartagees.setURLConnexion(listeURLs[position]);
        Log.d("mydebug", ">>> URL sélectionnée: " + listeURLs[position]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onLoginSuccess(int customerId) {
        Log.d("mydebug", ">>> MainActivity - Login réussi ! CustomerId: " + customerId);

        progressBar.setVisibility(View.GONE);
        btnConnexion.setEnabled(true);

        // Sauvegarder le customerId pour le panier
        PanierActivity.setCustomerId(customerId);

        // Aller au menu principal avec le customerId
        Intent intent = new Intent(MainActivity.this, MenuActivity.class);
        intent.putExtra("customerId", customerId);
        startActivity(intent);
        finish();
    }

    @Override
    public void onLoginError(String error) {
        Log.d("mydebug", ">>> MainActivity - Login échoué: " + error);

        progressBar.setVisibility(View.GONE);
        btnConnexion.setEnabled(true);
        txtMessage.setText(error);
    }
}
