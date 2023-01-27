/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.uefs.larsid.dlt.iot.soft.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;

/**
 *
 * @author Emers
 */
public class Credential {

    private String id;
    private CredentialDefinition credentialDefinition;
    private Boolean autoRemovable;
    private Map<String, String> values;

    public Credential() {

    }

    public Credential(CredentialDefinition credentialDefinition, Boolean autoRemovable) {
        this.credentialDefinition = credentialDefinition;
        this.autoRemovable = autoRemovable;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CredentialDefinition getCredentialDefinition() {
        return credentialDefinition;
    }

    public void setCredentialDefinition(CredentialDefinition credentialDefinition) {
        this.credentialDefinition = credentialDefinition;
    }

    public Boolean isAutoRemovable() {
        return autoRemovable;
    }

    public void setAutoRemovable(Boolean autoRemovable) {
        this.autoRemovable = autoRemovable;
    }

    public Map<String, String> addValue(String key, String value) {
        if (values == null) {
            values = new HashMap<>();
        }
        if (key != null && value != null) {
            values.put(key, value);
        }
        return values;
    }

    public Map<String, String> addValues(Map<String, String> values) {
        if (this.values == null) {
            this.values = new HashMap<>();
        }
        this.values.putAll(values);
        return this.values;
    }

    private CredentialPreview getCredentialPreview() {
        List<CredentialAttributes> attributes = new LinkedList<>();
        for (String key : values.keySet()) {
            attributes.add(new CredentialAttributes(key, values.get(key)));
        }
        return new CredentialPreview(attributes);
    }

    public V1CredentialProposalRequest buildV1(String connectionID) {
        if (credentialDefinition == null || credentialDefinition.getId() == null || autoRemovable == null || values == null) {
            return null;
        }
        return V1CredentialProposalRequest.builder()
                .connectionId(connectionID)
                .credentialDefinitionId(credentialDefinition.getId())
                .autoRemove(autoRemovable)
                .credentialProposal(getCredentialPreview())
                .build();
    }
}
