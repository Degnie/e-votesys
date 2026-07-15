package com.evotesys.dao;

import com.evotesys.model.Election;
import java.sql.SQLException;
import java.util.List;

public interface ElectionDAO {

    /** Read-only lookup to populate the ballot UI; carries no business rules. */
    List<Election> findPublished() throws SQLException;
}
