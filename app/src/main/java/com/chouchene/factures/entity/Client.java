package com.chouchene.factures.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Client {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String clientName;
    public String street;
    public String ville;
    public String codePostale;
    public String pays;
    public String numeroSiren;
    public String numeroTVA;
    public String email;

    public Client(String clientName, String street, String ville, String codePostale, String pays, String numeroSiren,String numeroTVA, String email) {
        this.clientName = clientName;
        this.street = street;
        this.ville = ville;
        this.codePostale = codePostale;
        this.pays = pays;
        this.numeroSiren = numeroSiren;
        this.email = email;
        this.numeroTVA = numeroTVA;
    }

    public String getNumeroTVA() {
        return numeroTVA;
    }

    public void setNumeroTVA(String numeroTVA) {
        this.numeroTVA = numeroTVA;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getCodePostale() {
        return codePostale;
    }

    public void setCodePostale(String codePostale) {
        this.codePostale = codePostale;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public String getNumeroSiren() {
        return numeroSiren;
    }

    public void setNumeroSiren(String numeroSiren) {
        this.numeroSiren = numeroSiren;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
