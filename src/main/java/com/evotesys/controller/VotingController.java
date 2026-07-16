package com.evotesys.controller;

import com.evotesys.model.Candidate;
import com.evotesys.model.Election;
import com.evotesys.service.VoteRequestDTO;
import com.evotesys.service.VotingException;
import com.evotesys.service.VotingService;
import com.evotesys.ui.VotingView;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

/**
 * Orchestrates VotingFrame's user intents against VotingService, keeping every DB/network call
 * off the Event Dispatch Thread via SwingWorker. VotingFrame only calls these methods and
 * implements {@link VotingView} to receive results; it never touches VotingService or the DAOs.
 */
public class VotingController {

    private final VotingService votingService;
    private final VotingView view;

    public VotingController(VotingService votingService, VotingView view) {
        this.votingService = votingService;
        this.view = view;
    }

    public void loadElections() {
        view.setLoading(true, "Cargando elecciones...");
        new SwingWorker<List<Election>, Void>() {
            @Override
            protected List<Election> doInBackground() throws VotingException {
                return votingService.findPublishedElections();
            }

            @Override
            protected void done() {
                view.setLoading(false, null);
                try {
                    view.showElections(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    view.showError(messageOf(e));
                }
            }
        }.execute();
    }

    public void loadCandidatesForElection(int electionId) {
        view.setLoading(true, "Cargando candidatos...");
        new SwingWorker<List<Candidate>, Void>() {
            @Override
            protected List<Candidate> doInBackground() throws VotingException {
                return votingService.findCandidates(electionId);
            }

            @Override
            protected void done() {
                view.setLoading(false, null);
                try {
                    view.showCandidates(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    view.showError(messageOf(e));
                }
            }
        }.execute();
    }

    public void castVote(VoteRequestDTO request) {
        view.setLoading(true, "Registrando voto...");
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws VotingException {
                return votingService.castVote(request);
            }

            @Override
            protected void done() {
                view.setLoading(false, null);
                try {
                    view.showVoteSuccess(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    view.showError(messageOf(e));
                }
            }
        }.execute();
    }

    private static String messageOf(ExecutionException e) {
        Throwable cause = e.getCause();
        return cause != null && cause.getMessage() != null
            ? cause.getMessage()
            : "Ocurrio un error inesperado.";
    }
}
