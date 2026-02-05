package com.example.applicationrftg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity implements LoginTask.LoginTaskListener {

    private TextInputEditText editEmail, editPassword;
    private MaterialButton btnConnexion, btnSettings;
    private ProgressBar progressBar;
    private TextView txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("mydebug", ">>> MainActivity (Login) - DEBUT");

        setContentView(R.layout.activity_main);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnConnexion = findViewById(R.id.btnConnexion);
        btnSettings = findViewById(R.id.btnSettings);
        progressBar = findViewById(R.id.progressBarLogin);
        txtMessage = findViewById(R.id.txtMessageLogin);

        // Masquer le bouton paramètres
        btnSettings.setVisibility(View.GONE);

        btnConnexion.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                txtMessage.setText("Veuillez remplir tous les champs");
                return;
            }

            // Afficher le loader
            progressBar.setVisibility(View.VISIBLE);
            txtMessage.setText("");
            btnConnexion.setEnabled(false);

            Log.d("mydebug", ">>> MainActivity - Tentative de connexion avec: " + email);

            // Lancer la tâche de connexion
            LoginTask loginTask = new LoginTask(MainActivity.this, this);
            loginTask.execute(email, password);
        });

        Log.d("mydebug", ">>> MainActivity (Login) - FIN");
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
        finish(); // Fermer l'écran de login
    }

    @Override
    public void onLoginError(String error) {
        Log.d("mydebug", ">>> MainActivity - Login échoué: " + error);

        progressBar.setVisibility(View.GONE);
        btnConnexion.setEnabled(true);
        txtMessage.setText(error);
    }
}




