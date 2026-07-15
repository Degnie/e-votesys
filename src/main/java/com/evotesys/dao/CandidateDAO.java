package com.evotesys.dao;

import com.evotesys.model.Candidate;
import java.sql.SQLException;
import java.util.List;

public interface CandidateDAO {

    /** Read-only lookup to populate the ballot UI; carries no business rules. */
    List<Candidate> findByElection(int electionId) throws SQLException;
}
