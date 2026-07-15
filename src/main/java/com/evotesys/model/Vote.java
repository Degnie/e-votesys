package com.evotesys.model;

import java.sql.Timestamp;

/**
 * Maps to the VOTE table. VOTE never stores who cast it (ballot secrecy, RN03's sibling rule);
 * {@link #voterId} carries the voter's identity only in memory, so VoteDAO can pass it to
 * SP_REGISTER_VOTE for the enrollment/anti-duplicate check (RN01/RN02) — it is never persisted.
 */
public class Vote {

    private int idVote;
    private Timestamp voteTimestamp;
    private String voteHash;
    private int electionId;
    private int candidateId;
    private int voterId;

    public Vote() {
    }

    public Vote(int electionId, int candidateId, int voterId) {
        this.electionId = electionId;
        this.candidateId = candidateId;
        this.voterId = voterId;
    }

    public int getIdVote() {
        return idVote;
    }

    public void setIdVote(int idVote) {
        this.idVote = idVote;
    }

    public Timestamp getVoteTimestamp() {
        return voteTimestamp;
    }

    public void setVoteTimestamp(Timestamp voteTimestamp) {
        this.voteTimestamp = voteTimestamp;
    }

    public String getVoteHash() {
        return voteHash;
    }

    public void setVoteHash(String voteHash) {
        this.voteHash = voteHash;
    }

    public int getElectionId() {
        return electionId;
    }

    public void setElectionId(int electionId) {
        this.electionId = electionId;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public int getVoterId() {
        return voterId;
    }

    public void setVoterId(int voterId) {
        this.voterId = voterId;
    }
}
