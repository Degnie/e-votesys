package com.evotesys.dao;

import com.evotesys.model.Vote;
import java.sql.SQLException;

public interface VoteDAO {

    /**
     * Registers a vote. Delegates the whole transaction (enrollment check, anti-duplicate check,
     * hash generation, audit log) to the SP_REGISTER_VOTE stored procedure — this method never
     * runs an INSERT itself. On success, {@code vote.getIdVote()} is populated with the generated ID.
     */
    void registerVote(Vote vote) throws SQLException;
}
