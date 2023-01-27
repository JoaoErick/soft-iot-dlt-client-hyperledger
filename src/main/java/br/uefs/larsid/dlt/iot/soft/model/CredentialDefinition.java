/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.uefs.larsid.dlt.iot.soft.model;

import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;

/**
 *
 * @author Emers
 */
public class CredentialDefinition {

    private String id;
    private String tag;
    private Boolean revocable;
    private Integer revocationRegistrySize;
    private Schema schema;

    public CredentialDefinition() {

    }

    public CredentialDefinition(String tag, Boolean revocable, Integer revocationRegistrySize, Schema schema) {
        this.tag = tag;
        this.revocable = revocable;
        this.revocationRegistrySize = revocationRegistrySize;
        this.schema = schema;
    }

    public CredentialDefinition(String id, String tag, Boolean revocable, Integer revocationRegistrySize, Schema schema) {
        this.id = id;
        this.tag = tag;
        this.revocable = revocable;
        this.revocationRegistrySize = revocationRegistrySize;
        this.schema = schema;
    } 

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Boolean isRevocable() {
        return revocable;
    }

    public void setRevocable(Boolean revocable) {
        this.revocable = revocable;
    }

    public Integer getRevocationRegistrySize() {
        return revocationRegistrySize;
    }

    public void setRevocationRegistrySize(Integer revocationRegistrySize) {
        this.revocationRegistrySize = revocationRegistrySize;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public CredentialDefinitionRequest build() {
        if (this.tag == null || this.revocable == null || this.revocationRegistrySize == null || this.schema == null || this.schema.getId() == null) {
            return null;
        }
        return CredentialDefinitionRequest.builder()
                .schemaId(schema.getId())
                .supportRevocation(revocable)
                .tag(tag)
                .revocationRegistrySize(revocationRegistrySize)
                .build();
    }
    
    @Override
    public String toString(){
        return 
            "id: "+id+
            "\ntag: "+tag+
            "\nrevocable: "+revocable+
            "\nrevocationRegistrySize: "+revocationRegistrySize+
            "\nschema: "+schema.toString();
    }

}
