package com.evotesys.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simulates a secure-token identity flow: the UI never lets the user type a raw voter id, it
 * collects a token (e.g. issued by an SSO/credential provider) of the form "SVT-<voterId>-<nonce>"
 * and this parser is the only place allowed to extract the voter id from it. Swapping this for a
 * real signed-token verifier (JWT, etc.) would not require touching VotingFrame or VotingController.
 */
public final class SecureTokenParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("^SVT-(\\d+)-[A-Za-z0-9]+$");

    private SecureTokenParser() {
    }

    public static int extractVoterId(String secureToken) throws InvalidTokenException {
        if (secureToken == null || secureToken.isBlank()) {
            throw new InvalidTokenException("Se requiere un Secure Token para identificarte.");
        }
        Matcher matcher = TOKEN_PATTERN.matcher(secureToken.trim());
        if (!matcher.matches()) {
            throw new InvalidTokenException("Secure Token invalido. Formato esperado: SVT-<id>-<nonce>.");
        }
        return Integer.parseInt(matcher.group(1));
    }
}
