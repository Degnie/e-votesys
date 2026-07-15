package com.evotesys.model;

/** Maps to the CANDIDATE table. */
public class Candidate {

    private int idCandidate;
    private String name;
    private String partyName;
    private int electionId;

    public Candidate() {
    }

    public Candidate(int idCandidate, String name, String partyName, int electionId) {
        this.idCandidate = idCandidate;
        this.name = name;
        this.partyName = partyName;
        this.electionId = electionId;
    }

    public int getIdCandidate() {
        return idCandidate;
    }

    public void setIdCandidate(int idCandidate) {
        this.idCandidate = idCandidate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public int getElectionId() {
        return electionId;
    }

    public void setElectionId(int electionId) {
        this.electionId = electionId;
    }

    // Drives how the candidate shows up in the JComboBox.
    @Override
    public String toString() {
        return idCandidate + " - " + name + " (" + partyName + ")";
    }
}
