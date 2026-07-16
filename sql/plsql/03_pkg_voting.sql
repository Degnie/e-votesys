-- E-VoteSys - PKG_VOTING (paquete mas critico para la seguridad del sistema)
-- RN01: valida padron (VOTER_ELECTION_STATUS) antes de habilitar el voto
-- RN02: un votante emite un solo voto por eleccion (anti-duplicidad)
-- VOTE no referencia al votante (anonimato); el vinculo voto-votante nunca se persiste
--
-- Requiere (ejecutar una vez como SYSTEM/DBA si el esquema EVOTESYS no tiene ya el privilegio):
--   GRANT EXECUTE ON DBMS_CRYPTO TO EVOTESYS;

CREATE OR REPLACE PACKAGE PKG_VOTING AS

    -- Inscribe a un votante en el padron de una eleccion (RN01: prerrequisito para poder votar)
    PROCEDURE enroll_voter(
        p_voter_id    IN VOTER_ELECTION_STATUS.VOTER_ID_VOTER%TYPE,
        p_election_id IN VOTER_ELECTION_STATUS.ELECTION_ID_ELECTION%TYPE
    );

    -- Emite el voto: valida padron + anti-duplicidad, inserta el voto anonimo y marca al votante.
    -- p_id_vote devuelve el ID generado (util para integraciones, ej. SP_REGISTER_VOTE / capa Java).
    PROCEDURE cast_vote(
        p_voter_id     IN  VOTER_ELECTION_STATUS.VOTER_ID_VOTER%TYPE,
        p_election_id  IN  ELECTION.ID_ELECTION%TYPE,
        p_candidate_id IN  CANDIDATE.ID_CANDIDATE%TYPE,
        p_id_vote      OUT VOTE.ID_VOTE%TYPE
    );

END PKG_VOTING;
/

CREATE OR REPLACE PACKAGE BODY PKG_VOTING AS

    PROCEDURE enroll_voter(
        p_voter_id    IN VOTER_ELECTION_STATUS.VOTER_ID_VOTER%TYPE,
        p_election_id IN VOTER_ELECTION_STATUS.ELECTION_ID_ELECTION%TYPE
    ) IS
    BEGIN
        INSERT INTO VOTER_ELECTION_STATUS (VOTER_ID_VOTER, ELECTION_ID_ELECTION, HAS_VOTED)
        VALUES (p_voter_id, p_election_id, 'N');
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            NULL; -- ya estaba inscrito, operacion idempotente
    END enroll_voter;

    PROCEDURE cast_vote(
        p_voter_id     IN  VOTER_ELECTION_STATUS.VOTER_ID_VOTER%TYPE,
        p_election_id  IN  ELECTION.ID_ELECTION%TYPE,
        p_candidate_id IN  CANDIDATE.ID_CANDIDATE%TYPE,
        p_id_vote      OUT VOTE.ID_VOTE%TYPE
    ) IS
        v_has_voted        VOTER_ELECTION_STATUS.HAS_VOTED%TYPE;
        v_election_status  ELECTION.STATUS%TYPE;
        v_candidate_count  PLS_INTEGER;
        v_hash             VOTE.HASH_CODE%TYPE;
    BEGIN
        -- Bloquea la fila del padron para esta (votante, eleccion): evita que dos votos
        -- concurrentes del mismo votante pasen juntos la validacion RN02 (condicion de carrera).
        SELECT HAS_VOTED INTO v_has_voted
        FROM VOTER_ELECTION_STATUS
        WHERE VOTER_ID_VOTER = p_voter_id AND ELECTION_ID_ELECTION = p_election_id
        FOR UPDATE;

        IF v_has_voted = 'S' THEN
            RAISE_APPLICATION_ERROR(-20002, 'RN02: el votante ' || p_voter_id || ' ya emitio su voto en la eleccion ' || p_election_id);
        END IF;

        SELECT STATUS INTO v_election_status FROM ELECTION WHERE ID_ELECTION = p_election_id;
        IF v_election_status != 'PUBLISHED' THEN
            RAISE_APPLICATION_ERROR(-20003, 'La eleccion ' || p_election_id || ' no esta publicada (estado: ' || v_election_status || ')');
        END IF;

        SELECT COUNT(*) INTO v_candidate_count
        FROM CANDIDATE
        WHERE ID_CANDIDATE = p_candidate_id AND ELECTION_ID_ELECTION = p_election_id;
        IF v_candidate_count = 0 THEN
            RAISE_APPLICATION_ERROR(-20004, 'El candidato ' || p_candidate_id || ' no pertenece a la eleccion ' || p_election_id);
        END IF;

        SELECT SEQ_VOTE.NEXTVAL INTO p_id_vote FROM DUAL;
        -- RN: la entropia del hash debe ser criptografica, no el PRNG de proposito general DBMS_RANDOM
        SELECT STANDARD_HASH(p_id_vote || p_election_id || p_candidate_id || SYSTIMESTAMP
                             || RAWTOHEX(DBMS_CRYPTO.RANDOMBYTES(16)), 'SHA256')
        INTO v_hash FROM DUAL;

        INSERT INTO VOTE (ID_VOTE, VOTE_TIMESTAMP, HASH_CODE, ELECTION_ID_ELECTION, CANDIDATE_ID_CANDIDATE)
        VALUES (p_id_vote, SYSTIMESTAMP, v_hash, p_election_id, p_candidate_id);

        UPDATE VOTER_ELECTION_STATUS
        SET HAS_VOTED = 'S', VOTE_DATE = SYSDATE
        WHERE VOTER_ID_VOTER = p_voter_id AND ELECTION_ID_ELECTION = p_election_id;

        PKG_AUDIT.log_event('CAST_VOTE', 'VOTE', TO_CHAR(p_id_vote));
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20001, 'RN01: el votante ' || p_voter_id || ' no esta habilitado (padron) para la eleccion ' || p_election_id);
    END cast_vote;

END PKG_VOTING;
/

-- RN03: el voto confirmado es inmutable, ni siquiera el administrador puede alterarlo
CREATE OR REPLACE TRIGGER TRG_VOTE_IMMUTABLE
BEFORE UPDATE OR DELETE ON VOTE
FOR EACH ROW
BEGIN
    RAISE_APPLICATION_ERROR(-20099, 'RN03: la tabla VOTE es de solo insercion, no se permite UPDATE ni DELETE');
END;
/
