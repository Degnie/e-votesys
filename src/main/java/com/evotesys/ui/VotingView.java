package com.evotesys.ui;

import com.evotesys.model.Candidate;
import com.evotesys.model.Election;
import java.util.List;

/**
 * Narrow contract VotingController uses to update the view. Keeps VotingFrame a passive
 * view (passive MVC): it never talks to VotingService directly, only reacts to these calls.
 */
public interface VotingView {

    void setLoading(boolean loading, String message);

    void showElections(List<Election> elections);

    void showCandidates(List<Candidate> candidates);

    void showVoteSuccess(int idVote);

    void showError(String message);
}
