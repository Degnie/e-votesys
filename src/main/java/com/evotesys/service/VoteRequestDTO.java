package com.evotesys.service;

/**
 * Carries the user's voting intent from the view to {@link VotingService}. Unlike {@link
 * com.evotesys.model.Vote}, which is DAO-only and holds a resolved voterId, this DTO holds the raw
 * secureToken exactly as the UI collected it — VotingService is the trust boundary that parses it.
 */
public class VoteRequestDTO {

    private final int electionId;
    private final int candidateId;
    private final String secureToken;

    public VoteRequestDTO(int electionId, int candidateId, String secureToken) {
        this.electionId = electionId;
        this.candidateId = candidateId;
        this.secureToken = secureToken;
    }

    public int getElectionId() {
        return electionId;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public String getSecureToken() {
        return secureToken;
    }
}
