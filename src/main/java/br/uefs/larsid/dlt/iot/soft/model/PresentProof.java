/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.uefs.larsid.dlt.iot.soft.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentProofRequest.ProofRequest;
import org.hyperledger.aries.api.present_proof.PresentProofRequest.ProofRequest.ProofRequestedAttributes;

/**
 *
 * @author Emers
 */
public class PresentProof {

    private String name;
    private String comment;
    private String version;

    private Map<String, ProofRequestedAttributes> attributes;

    public PresentProof(String name, String comment, String version) {
        this.name = name;
        this.comment = comment;
        this.version = version;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, ProofRequestedAttributes> addAttributeRestriction(AttributeRestriction attributeRestriction) {
        ProofRequestedAttributes proofRequestedAttributes = ProofRequestedAttributes.builder().name(attributeRestriction.getName()).restriction(attributeRestriction.getRestriction()).build();
        this.attributes.put(attributeRestriction.getName(), proofRequestedAttributes);
        return this.attributes;
    }

    public Map<String, ProofRequestedAttributes> addAttributesRestrictions(List<AttributeRestriction> attributesRestricitions) {
        if(this.attributes==null){
            this.attributes = new HashMap<>();
        }
        for (AttributeRestriction attributeRestriction : attributesRestricitions) {
            addAttributeRestriction(attributeRestriction);
        }
        return this.attributes;
    }

    private ProofRequest getProofRequest() {
        return ProofRequest.builder().name(name).requestedAttributes(attributes).version(version).build();
    }

    public PresentProofRequest build(String connectionId) {
        System.out.println("Proof: "+getProofRequest());
        return PresentProofRequest.builder().comment(comment).connectionId(connectionId).proofRequest(getProofRequest()).build();
    }

}
