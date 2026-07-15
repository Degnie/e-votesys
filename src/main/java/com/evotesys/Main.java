package com.evotesys;

import com.evotesys.dao.VoteDAO;
import com.evotesys.dao.VoteDAOImpl;
import com.evotesys.model.Vote;
import java.sql.SQLException;

/**
 * Manual smoke test for VoteDAOImpl / SP_REGISTER_VOTE.
 * Usage: java com.evotesys.Main <voterId> <electionId> <candidateId>
 */
public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java com.evotesys.Main <voterId> <electionId> <candidateId>");
            System.exit(1);
        }

        Vote vote = new Vote(
            Integer.parseInt(args[1]),
            Integer.parseInt(args[2]),
            Integer.parseInt(args[0])
        );

        VoteDAO voteDAO = new VoteDAOImpl();
        try {
            voteDAO.registerVote(vote);
            System.out.println("Vote registered. idVote=" + vote.getIdVote());
        } catch (SQLException e) {
            System.err.println("Vote rejected: " + e.getMessage());
        }
    }
}
