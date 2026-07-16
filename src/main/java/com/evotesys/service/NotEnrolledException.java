package com.evotesys.service;

/** RN01: the voter has no VOTER_ELECTION_STATUS entry for this election (ORA-20001). */
public class NotEnrolledException extends VotingException {

    public NotEnrolledException(String message) {
        super(message);
    }
}
