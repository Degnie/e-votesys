package com.evotesys.ui;

import com.evotesys.dao.CandidateDAO;
import com.evotesys.dao.CandidateDAOImpl;
import com.evotesys.dao.ElectionDAO;
import com.evotesys.dao.ElectionDAOImpl;
import com.evotesys.dao.VoteDAO;
import com.evotesys.dao.VoteDAOImpl;
import com.evotesys.model.Candidate;
import com.evotesys.model.Election;
import com.evotesys.model.Vote;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Boleta simulada: elige una elección publicada, selecciona un candidato de la lista y vota como
 * un votante dado. Cada voto sigue pasando por VoteDAOImpl -> SP_REGISTER_VOTE ->
 * PKG_VOTING.cast_vote, así que RN01/RN02 se siguen validando del lado de Oracle; esta ventana no
 * duplica ninguna regla de negocio, solo la presenta.
 */
public class VotingFrame extends javax.swing.JFrame {

    private static final Color COLOR_EXITO = new Color(0, 128, 0);
    private static final Color COLOR_ERROR = new Color(178, 34, 34);

    private final ElectionDAO electionDAO = new ElectionDAOImpl();
    private final CandidateDAO candidateDAO = new CandidateDAOImpl();
    private final VoteDAO voteDAO = new VoteDAOImpl();

    private final JComboBox<Election> electionCombo = new JComboBox<>();
    private final DefaultListModel<Candidate> candidateListModel = new DefaultListModel<>();
    private final JList<Candidate> candidateList = new JList<>(candidateListModel);
    private final JTextField voterIdField = new JTextField(12);
    private final JButton voteButton = new JButton("Emitir voto");
    private final JLabel statusLabel = new JLabel(" ");

    public VotingFrame() {
        super("E-VoteSys - Boleta de votación");
        buildUi();
        wireActions();
        loadElections();
    }

    private void buildUi() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Selecciona tu elección y candidato");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        header.add(title);
        header.add(Box.createVerticalStrut(8));

        JPanel electionRow = new JPanel(new BorderLayout(6, 0));
        electionRow.add(new JLabel("Elección:"), BorderLayout.WEST);
        electionRow.add(electionCombo, BorderLayout.CENTER);
        header.add(electionRow);

        add(header, BorderLayout.NORTH);

        candidateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        candidateList.setFont(candidateList.getFont().deriveFont(14f));
        candidateList.setFixedCellHeight(28);
        candidateList.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane candidateScroll = new JScrollPane(candidateList);
        candidateScroll.setBorder(BorderFactory.createTitledBorder("Candidatos"));
        add(candidateScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JPanel voterRow = new JPanel(new BorderLayout(6, 0));
        voterRow.add(new JLabel("ID Votante:"), BorderLayout.WEST);
        voterRow.add(voterIdField, BorderLayout.CENTER);
        voterRow.add(voteButton, BorderLayout.EAST);
        footer.add(voterRow);

        footer.add(Box.createVerticalStrut(8));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        footer.add(statusLabel);

        add(footer, BorderLayout.SOUTH);

        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        setSize(480, 420);
        setLocationRelativeTo(null);
    }

    private void wireActions() {
        electionCombo.addActionListener(e -> loadCandidatesForSelectedElection());
        voteButton.addActionListener(e -> castVote());
    }

    private void loadElections() {
        try {
            List<Election> elections = electionDAO.findPublished();
            electionCombo.setModel(new DefaultComboBoxModel<>(elections.toArray(new Election[0])));
            if (elections.isEmpty()) {
                setStatus("No hay elecciones publicadas todavía.", COLOR_ERROR);
            }
        } catch (SQLException e) {
            setStatus("Error consultando elecciones: " + e.getMessage(), COLOR_ERROR);
        }
    }

    private void loadCandidatesForSelectedElection() {
        Election selected = (Election) electionCombo.getSelectedItem();
        candidateListModel.clear();
        if (selected == null) {
            return;
        }
        try {
            List<Candidate> candidates = candidateDAO.findByElection(selected.getIdElection());
            candidates.forEach(candidateListModel::addElement);
        } catch (SQLException e) {
            setStatus("Error consultando candidatos: " + e.getMessage(), COLOR_ERROR);
        }
    }

    private void castVote() {
        Election election = (Election) electionCombo.getSelectedItem();
        Candidate candidate = candidateList.getSelectedValue();

        if (election == null) {
            setStatus("Selecciona una elección.", COLOR_ERROR);
            return;
        }
        if (candidate == null) {
            setStatus("Selecciona un candidato de la lista.", COLOR_ERROR);
            return;
        }
        int voterId;
        try {
            voterId = Integer.parseInt(voterIdField.getText().trim());
        } catch (NumberFormatException e) {
            setStatus("ID Votante debe ser numérico.", COLOR_ERROR);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Confirmas tu voto por " + candidate.getName() + "?",
            "Confirmar voto", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Vote vote = new Vote(election.getIdElection(), candidate.getIdCandidate(), voterId);
            voteDAO.registerVote(vote);
            setStatus("Voto registrado correctamente (ID_VOTE=" + vote.getIdVote() + ").", COLOR_EXITO);
        } catch (SQLException e) {
            setStatus("Voto rechazado: " + e.getMessage(), COLOR_ERROR);
        }
    }

    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VotingFrame().setVisible(true));
    }
}
