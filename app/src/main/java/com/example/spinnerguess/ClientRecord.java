package com.example.spinnerguess;

public class ClientRecord {
    public final String documentId;
    public final String name;
    public final double balance;

    public ClientRecord(String documentId, String name, double balance) {
        this.documentId = documentId;
        this.name = name;
        this.balance = balance;
    }
}