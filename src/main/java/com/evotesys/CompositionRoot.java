package com.evotesys;

import com.evotesys.controller.VotingController;
import com.evotesys.dao.CandidateDAOImpl;
import com.evotesys.dao.ElectionDAOImpl;
import com.evotesys.dao.VoteDAOImpl;
import com.evotesys.service.VotingService;
import com.evotesys.ui.VotingFrame;

/**
 * The one place allowed to instantiate DAOs and wire the dependency graph by hand
 * (manual DI): VotingFrame and VotingService never call `new *DAOImpl()` themselves.
 */
public final class CompositionRoot {

    private CompositionRoot() {
    }

    public static VotingFrame buildVotingFrame() {
        VotingService votingService = new VotingService(
            new ElectionDAOImpl(), new CandidateDAOImpl(), new VoteDAOImpl());

        VotingFrame frame = new VotingFrame();
        VotingController controller = new VotingController(votingService, frame);
        frame.setController(controller);
        frame.init();
        return frame;
    }
}
