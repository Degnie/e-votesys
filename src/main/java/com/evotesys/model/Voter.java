package com.evotesys.model;

/** Maps to the VOTER table. */
public class Voter {

    private int idVoter;
    private String documentId;
    private String fullName;
    private String email;

    public Voter() {
    }

    public Voter(int idVoter, String documentId, String fullName, String email) {
        this.idVoter = idVoter;
        this.documentId = documentId;
        this.fullName = fullName;
        this.email = email;
    }

    public int getIdVoter() {
        return idVoter;
    }

    public void setIdVoter(int idVoter) {
        this.idVoter = idVoter;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
