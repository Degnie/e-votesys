package com.evotesys.dao;

import com.evotesys.model.Candidate;
import com.evotesys.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CandidateDAOImpl implements CandidateDAO {

    private static final String SELECT_BY_ELECTION =
        "SELECT ID_CANDIDATE, NAME, PARTY_NAME, ELECTION_ID_ELECTION FROM CANDIDATE WHERE ELECTION_ID_ELECTION = ? ORDER BY ID_CANDIDATE";

    @Override
    public List<Candidate> findByElection(int electionId) throws SQLException {
        List<Candidate> candidates = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ELECTION)) {

            stmt.setInt(1, electionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    candidates.add(new Candidate(
                        rs.getInt("ID_CANDIDATE"),
                        rs.getString("NAME"),
                        rs.getString("PARTY_NAME"),
                        rs.getInt("ELECTION_ID_ELECTION")
                    ));
                }
            }
        }
        return candidates;
    }
}
