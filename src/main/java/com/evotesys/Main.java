package com.evotesys;

import com.evotesys.dao.CandidateDAOImpl;
import com.evotesys.dao.ElectionDAOImpl;
import com.evotesys.dao.VoteDAOImpl;
import com.evotesys.service.VoteRequestDTO;
import com.evotesys.service.VotingException;
import com.evotesys.service.VotingService;

/**
 * Manual smoke test for VotingService / SP_REGISTER_VOTE.
 * Usage: java com.evotesys.Main <secureToken> <electionId> <candidateId>
 * secureToken format: SVT-<voterId>-<nonce>, e.g. SVT-1042-ab12cd
 */
public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java com.evotesys.Main <secureToken> <electionId> <candidateId>");
            System.exit(1);
        }

        VoteRequestDTO request = new VoteRequestDTO(
            Integer.parseInt(args[1]),
            Integer.parseInt(args[2]),
            args[0]
        );

        VotingService votingService = new VotingService(
            new ElectionDAOImpl(), new CandidateDAOImpl(), new VoteDAOImpl());
        try {
            int idVote = votingService.castVote(request);
            System.out.println("Vote registered. idVote=" + idVote);
        } catch (VotingException e) {
            System.err.println("Vote rejected: " + e.getMessage());
        }
    }
}
