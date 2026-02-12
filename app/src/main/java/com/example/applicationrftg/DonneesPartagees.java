package com.example.applicationrftg;

/**
 * Classe pour stocker les données partagées entre les activités
 * Notamment l'URL de connexion au serveur sélectionnée par l'utilisateur
 */
public class DonneesPartagees {

    private static String URLConnexion = "";

    public static String getURLConnexion() {
        return URLConnexion;
    }

    public static void setURLConnexion(String url) {
        URLConnexion = url;
    }
}
