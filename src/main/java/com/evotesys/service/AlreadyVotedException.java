package com.evotesys.service;

/** RN02: the voter already cast a vote in this election (ORA-20002). */
public class AlreadyVotedException extends VotingException {

    public AlreadyVotedException(String message) {
        super(message);
    }
}
