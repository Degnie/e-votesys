package com.evotesys.service;

/** The Secure Token supplied by the UI is missing, malformed, or does not carry a voter id. */
public class InvalidTokenException extends VotingException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
