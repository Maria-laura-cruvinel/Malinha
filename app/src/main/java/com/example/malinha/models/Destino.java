package com.example.malinha.models;
/** Resource IDs keep the initial catalog local and translated through strings.xml. */
public class Destino {
 public final int name, description, image;
 public final int region;
 public Destino(int name,int description,int image){this(name,description,image,0);}
 public Destino(int name,int description,int image,int region){this.name=name;this.description=description;this.image=image;this.region=region;}
}
