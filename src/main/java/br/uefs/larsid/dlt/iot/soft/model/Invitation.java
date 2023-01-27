/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.uefs.larsid.dlt.iot.soft.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;

/**
 *
 * @author Emers
 */
public class Invitation {
    private String type;
    private String id;
    private String label;
    private String serviceEndpoint;
    private List<String> recipientKeys;

    public Invitation(JsonObject invitation){
        this.type = invitation.get("@type").getAsString();
        this.id = invitation.get("@id").getAsString();
        this.label = invitation.get("label").getAsString();
        this.serviceEndpoint = invitation.get("serviceEndpoint").getAsString();
        this.recipientKeys = new ArrayList<>(); 
        
        for(JsonElement recipientKey: invitation.get("recipientKeys").getAsJsonArray()){
            this.recipientKeys.add(recipientKey.getAsString());
        }
    }
    
    public Invitation(String type, String id, String label, String serviceEndpoint, List<String> recipientKeys) {
        this.type = type;
        this.id = id;
        this.label = label;
        this.serviceEndpoint = serviceEndpoint;
        this.recipientKeys = recipientKeys;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }

    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    public List<String> getRecipientKeys() {
        return recipientKeys;
    }

    public void setRecipientKeys(ArrayList<String> recipientKeys) {
        this.recipientKeys = recipientKeys;
    }
    
    public ReceiveInvitationRequest build(){
        return ReceiveInvitationRequest.builder()
            .id(id)
            .type(type)
            .label(label)
            .serviceEndpoint(serviceEndpoint)
            .recipientKeys(recipientKeys)
            .build();
    }
}
