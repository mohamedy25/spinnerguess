package com.example.spinnerguess;

public class ClientRecord {
    public final long id;
    public final String name;
    public final double balance;

    public ClientRecord(long id, String name, double balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }
}