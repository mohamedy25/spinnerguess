package com.example.spinnerguess;
public class ModelUser
{
    private String userName;
    private int score;
    private long id;

    public ModelUser(String userName) {
        this.userName = userName;
        this.score = 0;
        this.id=0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ModelUser(String userName, int score, long id) {
        this.userName = userName;
        this.score = score;
        this.id = id;
    }

    public void setScore(int s)
    {
        this.score = s;
    }
    public String getUserName() {
        return userName;
    }

    public int getScore() {
        return score;
    }

    public void updateUser(int score)
    {
       this.score=score;

    }

}
