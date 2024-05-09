package com.hka.webshop;

import javax.persistence.*;


// Datenmodell

// Produkt anlegen (Kontrolle ob Kategorie existiert) UND 
// Kategorie löschen (Kontrolle ob Produkt mit Kategorie existiert)
@Entity
public class Category implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;

    public Category() {
    }

    public Category(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }


}

