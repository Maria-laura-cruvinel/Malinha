package com.example.malinha.models;

import java.io.Serializable;
import java.util.UUID;

/** One manually entered activity. Times are minutes from midnight; cost is decimal text. */
public class Atividade implements Serializable {
    private static final long serialVersionUID = 1L;
    public String id = UUID.randomUUID().toString();
    public int day, startMinutes, endMinutes, category;
    public String name = "", description = "", cost = "0";
}
