package com.example.malinha.models;

import java.io.Serializable;
import java.util.ArrayList;

/** Offline trip aggregate. Activity order is the order of this ArrayList. */
public class Viagem implements Serializable {
    private static final long serialVersionUID = 1L;
    public long id = -1, start, end;
    public int travelers = 1, pace = 1;
    public String destination = "", budget = "0";
    public ArrayList<String> interests = new ArrayList<>();
    public ArrayList<Atividade> activities = new ArrayList<>();
}
