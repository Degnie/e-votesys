package com.evotesys.dao;

import com.evotesys.model.Election;
import com.evotesys.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ElectionDAOImpl implements ElectionDAO {

    private static final String SELECT_PUBLISHED =
        "SELECT ID_ELECTION, TITLE, START_DATE, END_DATE, STATUS, CONTRACT_ID_CONTRACT "
            + "FROM ELECTION WHERE STATUS = 'PUBLISHED' ORDER BY ID_ELECTION";

    @Override
    public List<Election> findPublished() throws SQLException {
        List<Election> elections = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PUBLISHED);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                elections.add(new Election(
                    rs.getInt("ID_ELECTION"),
                    rs.getString("TITLE"),
                    rs.getDate("START_DATE"),
                    rs.getDate("END_DATE"),
                    rs.getString("STATUS"),
                    rs.getInt("CONTRACT_ID_CONTRACT")
                ));
            }
        }
        return elections;
    }
}
