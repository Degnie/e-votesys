package com.evotesys.service;

/** The election is not in PUBLISHED state (ORA-20003). */
public class InvalidElectionStateException extends VotingException {

    public InvalidElectionStateException(String message) {
        super(message);
    }
}
