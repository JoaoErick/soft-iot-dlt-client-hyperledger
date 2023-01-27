/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.uefs.larsid.dlt.iot.soft.model;

import com.google.gson.JsonObject;

/**
 *
 * @author Emers
 */
public class AttributeRestriction {

    private final String name;
    private final JsonObject restriction;

    public AttributeRestriction(String name, String nameRestriction, String propertyRestriction) {
        this.name = name;
        this.restriction = new JsonObject();
        this.restriction.addProperty(nameRestriction, propertyRestriction);
    }

    public String getName() {
        return name;
    }

    public JsonObject getRestriction() {
        return restriction;
    }

}
