package com.evotesys.ui;

import com.evotesys.controller.VotingController;
import com.evotesys.model.Candidate;
import com.evotesys.model.Election;
import com.evotesys.service.VoteRequestDTO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
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
 * Boleta simulada: elige una elección publicada, selecciona un candidato de la lista y vota
 * presentando un Secure Token en lugar de tipear un ID de votante. Esta clase es una vista pasiva:
 * no conoce VotingService ni los DAOs, solo dispara eventos hacia VotingController y expone
 * {@link VotingView} para que el controller la actualice. Cada voto sigue pasando por
 * VoteDAOImpl -> SP_REGISTER_VOTE -> PKG_VOTING.cast_vote, así que RN01/RN02 se siguen validando
 * del lado de Oracle; esta ventana no duplica ninguna regla de negocio, solo la presenta.
 */
public class VotingFrame extends javax.swing.JFrame implements VotingView {

    private static final Color COLOR_EXITO = new Color(0, 128, 0);
    private static final Color COLOR_ERROR = new Color(178, 34, 34);

    private VotingController controller;

    private final JComboBox<Election> electionCombo = new JComboBox<>();
    private final DefaultListModel<Candidate> candidateListModel = new DefaultListModel<>();
    private final JList<Candidate> candidateList = new JList<>(candidateListModel);
    private final JTextField secureTokenField = new JTextField(16);
    private final JButton voteButton = new JButton("Emitir voto");
    private final JLabel statusLabel = new JLabel(" ");

    public VotingFrame() {
        super("E-VoteSys - Boleta de votación");
        buildUi();
        wireActions();
    }

    public void setController(VotingController controller) {
        this.controller = controller;
    }

    /** Composition root calls this once the controller is wired, kicking off the initial load. */
    public void init() {
        controller.loadElections();
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

        JPanel tokenRow = new JPanel(new BorderLayout(6, 0));
        secureTokenField.setToolTipText("Formato: SVT-<id>-<nonce>, provisto por tu proveedor de identidad");
        tokenRow.add(new JLabel("Secure Token:"), BorderLayout.WEST);
        tokenRow.add(secureTokenField, BorderLayout.CENTER);
        tokenRow.add(voteButton, BorderLayout.EAST);
        footer.add(tokenRow);

        footer.add(Box.createVerticalStrut(8));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        footer.add(statusLabel);

        add(footer, BorderLayout.SOUTH);

        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        setSize(480, 420);
        setLocationRelativeTo(null);
    }

    private void wireActions() {
        electionCombo.addActionListener(e -> {
            Election selected = (Election) electionCombo.getSelectedItem();
            candidateListModel.clear();
            if (selected != null) {
                controller.loadCandidatesForElection(selected.getIdElection());
            }
        });
        voteButton.addActionListener(e -> requestVote());
    }

    private void requestVote() {
        Election election = (Election) electionCombo.getSelectedItem();
        Candidate candidate = candidateList.getSelectedValue();

        if (election == null) {
            showError("Selecciona una elección.");
            return;
        }
        if (candidate == null) {
            showError("Selecciona un candidato de la lista.");
            return;
        }
        String secureToken = secureTokenField.getText().trim();
        if (secureToken.isEmpty()) {
            showError("Ingresa tu Secure Token.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Confirmas tu voto por " + candidate.getName() + "?",
            "Confirmar voto", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        VoteRequestDTO request = new VoteRequestDTO(election.getIdElection(), candidate.getIdCandidate(), secureToken);
        controller.castVote(request);
    }

    @Override
    public void setLoading(boolean loading, String message) {
        electionCombo.setEnabled(!loading);
        candidateList.setEnabled(!loading);
        voteButton.setEnabled(!loading);
        secureTokenField.setEnabled(!loading);
        if (loading) {
            setStatus(message != null ? message : "Cargando...", statusLabel.getForeground());
        }
    }

    @Override
    public void showElections(List<Election> elections) {
        electionCombo.setModel(new DefaultComboBoxModel<>(elections.toArray(new Election[0])));
        if (elections.isEmpty()) {
            setStatus("No hay elecciones publicadas todavía.", COLOR_ERROR);
        } else {
            setStatus(" ", COLOR_EXITO);
        }
    }

    @Override
    public void showCandidates(List<Candidate> candidates) {
        candidateListModel.clear();
        candidates.forEach(candidateListModel::addElement);
    }

    @Override
    public void showVoteSuccess(int idVote) {
        setStatus("Voto registrado correctamente (ID_VOTE=" + idVote + ").", COLOR_EXITO);
        secureTokenField.setText("");
    }

    @Override
    public void showError(String message) {
        setStatus(message, COLOR_ERROR);
    }

    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VotingFrame frame = com.evotesys.CompositionRoot.buildVotingFrame();
            frame.setVisible(true);
        });
    }
}
