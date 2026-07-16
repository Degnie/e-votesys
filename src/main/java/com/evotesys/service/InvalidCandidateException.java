package com.evotesys.service;

/** The candidate does not belong to the given election (ORA-20004). */
public class InvalidCandidateException extends VotingException {

    public InvalidCandidateException(String message) {
        super(message);
    }
}
