package com.evotesys.dao;

import com.evotesys.model.Vote;
import com.evotesys.util.DatabaseConnection;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class VoteDAOImpl implements VoteDAO {

    private static final String CALL_SP_REGISTER_VOTE = "{call SP_REGISTER_VOTE(?, ?, ?, ?)}";

    // Throws SQLException with the raw ORA-20001/20002/20003/20004 message on business-rule
    // violations (see PKG_VOTING) — the caller decides whether to show it or translate it.
    @Override
    public void registerVote(Vote vote) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             CallableStatement stmt = conn.prepareCall(CALL_SP_REGISTER_VOTE)) {

            stmt.setInt(1, vote.getVoterId());
            stmt.setInt(2, vote.getElectionId());
            stmt.setInt(3, vote.getCandidateId());
            stmt.registerOutParameter(4, Types.INTEGER);

            stmt.execute();

            vote.setIdVote(stmt.getInt(4));
        }
    }
}
