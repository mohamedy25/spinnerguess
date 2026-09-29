package com.example.spinnerguess;

public class ModelUser {
    private String userName;
    private int score;
    private long id;
    private String password; // متغير كلمة المرور

    // Constructor فارغ أو افتراضي
    public ModelUser(String userName) {
        this.userName = userName;
        this.score = 0;
        this.id = 0;
        this.password = "";
    }

    // Constructor للاستخدام عند التسجيل الجديد
    public ModelUser(String userName, String password) {
        this.userName = userName;
        this.password = password;
        this.score = 0;
        this.id = 0;
    }

    // Constructor الشامل (عند الجلب من قاعدة البيانات)
    public ModelUser(String userName, int score, long id, String password) {
        this.userName = userName;
        this.score = score;
        this.id = id;
        this.password = password;
    }

    // دوال Get و Set لكلمة المرور (هنا كان الخطأ)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setScore(int s) {
        this.score = s;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getScore() {
        return score;
    }

    public void updateUser(int score) {
        this.score = score;
    }
}