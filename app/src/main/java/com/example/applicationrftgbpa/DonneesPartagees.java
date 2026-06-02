package com.example.applicationtftgbpa;

/**
 * Classe pour stocker les données partagées entre les activités
 * Notamment l'URL de connexion au serveur sélectionnée par l'utilisateur
 */
public class DonneesPartagees {

    private static String URLConnexion = "";
    private static String jwt = "";
    private static int customerId = -1;

    public static String getURLConnexion() {
        return URLConnexion;
    }

    public static void setURLConnexion(String url) {
        URLConnexion = url;
    }

    public static String getJwt() {
        return jwt;
    }

    public static void setJwt(String token) {
        jwt = token;
    }

    public static int getCustomerId() {
        return customerId;
    }

    public static void setCustomerId(int id) {
        customerId = id;
    }
}
