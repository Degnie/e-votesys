package com.evotesys.service;

/** Base type for business-rule failures surfaced by {@link VotingService}. Never carries ORA-XXXX text. */
public class VotingException extends Exception {

    public VotingException(String message) {
        super(message);
    }

    public VotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
