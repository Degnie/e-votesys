-- E-VoteSys - Punto de entrada para la capa Java (JDBC CallableStatement)
-- Envoltorio delgado sobre PKG_VOTING.cast_vote: toda la validacion de negocio
-- (RN01, RN02, hash, auditoria) vive en el paquete; este procedimiento standalone
-- solo existe porque es mas simple de invocar desde JDBC que un procedimiento de paquete.

CREATE OR REPLACE PROCEDURE SP_REGISTER_VOTE (
    p_voter_id     IN  VOTER_ELECTION_STATUS.VOTER_ID_VOTER%TYPE,
    p_election_id  IN  ELECTION.ID_ELECTION%TYPE,
    p_candidate_id IN  CANDIDATE.ID_CANDIDATE%TYPE,
    p_id_vote      OUT VOTE.ID_VOTE%TYPE
) AS
BEGIN
    PKG_VOTING.cast_vote(p_voter_id, p_election_id, p_candidate_id, p_id_vote);
END SP_REGISTER_VOTE;
/
