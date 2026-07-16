package com.evotesys.service;

import com.evotesys.dao.CandidateDAO;
import com.evotesys.dao.ElectionDAO;
import com.evotesys.dao.VoteDAO;
import com.evotesys.model.Candidate;
import com.evotesys.model.Election;
import com.evotesys.model.Vote;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intermediary between the UI and the DAO layer. Receives DAO instances through its constructor
 * (manual dependency injection) so views never instantiate a DAO themselves. Every ORA-XXXX
 * raised by PKG_VOTING is translated here into a typed, UI-safe {@link VotingException} — no
 * SQLException/ORA text should ever reach VotingFrame.
 */
public class VotingService {

    private static final Logger LOG = LoggerFactory.getLogger(VotingService.class);

    private static final int ORA_NOT_ENROLLED = 20001;
    private static final int ORA_ALREADY_VOTED = 20002;
    private static final int ORA_ELECTION_NOT_PUBLISHED = 20003;
    private static final int ORA_INVALID_CANDIDATE = 20004;

    private final ElectionDAO electionDAO;
    private final CandidateDAO candidateDAO;
    private final VoteDAO voteDAO;

    public VotingService(ElectionDAO electionDAO, CandidateDAO candidateDAO, VoteDAO voteDAO) {
        this.electionDAO = electionDAO;
        this.candidateDAO = candidateDAO;
        this.voteDAO = voteDAO;
    }

    public List<Election> findPublishedElections() throws VotingException {
        try {
            return electionDAO.findPublished();
        } catch (SQLException e) {
            LOG.error("Error consultando elecciones publicadas", e);
            throw new VotingException("No se pudieron cargar las elecciones en este momento.", e);
        }
    }

    public List<Candidate> findCandidates(int electionId) throws VotingException {
        try {
            return candidateDAO.findByElection(electionId);
        } catch (SQLException e) {
            LOG.error("Error consultando candidatos de la eleccion {}", electionId, e);
            throw new VotingException("No se pudieron cargar los candidatos en este momento.", e);
        }
    }

    /** Parses the secure token, runs cast_vote through VoteDAO and returns the generated vote id. */
    public int castVote(VoteRequestDTO request) throws VotingException {
        int voterId = SecureTokenParser.extractVoterId(request.getSecureToken());

        Vote vote = new Vote(request.getElectionId(), request.getCandidateId(), voterId);
        try {
            voteDAO.registerVote(vote);
        } catch (SQLException e) {
            throw translate(e, request.getElectionId(), request.getCandidateId());
        }

        LOG.info("Voto registrado: idVote={}, election={}, candidate={}",
            vote.getIdVote(), request.getElectionId(), request.getCandidateId());
        return vote.getIdVote();
    }

    private VotingException translate(SQLException e, int electionId, int candidateId) {
        int code = Math.abs(e.getErrorCode());
        VotingException translated = switch (code) {
            case ORA_NOT_ENROLLED -> new NotEnrolledException(
                "No estas habilitado para votar en esta eleccion (no figuras en el padron).");
            case ORA_ALREADY_VOTED -> new AlreadyVotedException(
                "Ya emitiste tu voto en esta eleccion.");
            case ORA_ELECTION_NOT_PUBLISHED -> new InvalidElectionStateException(
                "La eleccion ya no esta disponible para votar.");
            case ORA_INVALID_CANDIDATE -> new InvalidCandidateException(
                "El candidato seleccionado no pertenece a esta eleccion.");
            default -> new VotingException("No se pudo registrar tu voto. Intenta nuevamente mas tarde.", e);
        };

        LOG.warn("Voto rechazado (election={}, candidate={}, ORA-{}): {}",
            electionId, candidateId, code, e.getMessage());
        return translated;
    }
}
