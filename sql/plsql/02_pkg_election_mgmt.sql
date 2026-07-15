-- E-VoteSys - PKG_ELECTION_MGMT
-- Alta de elecciones, candidatos, vinculo con ODS y publicacion (RN05, RN06)
-- STATUS de ELECTION: DRAFT -> PUBLISHED -> CLOSED

CREATE OR REPLACE PACKAGE PKG_ELECTION_MGMT AS

    PROCEDURE create_election(
        p_title        IN  ELECTION.TITLE%TYPE,
        p_start_date   IN  ELECTION.START_DATE%TYPE,
        p_end_date     IN  ELECTION.END_DATE%TYPE,
        p_contract_id  IN  ELECTION.CONTRACT_ID_CONTRACT%TYPE,
        p_id_election  OUT ELECTION.ID_ELECTION%TYPE
    );

    PROCEDURE add_candidate(
        p_election_id   IN  CANDIDATE.ELECTION_ID_ELECTION%TYPE,
        p_name          IN  CANDIDATE.NAME%TYPE,
        p_party_name    IN  CANDIDATE.PARTY_NAME%TYPE,
        p_photo_url     IN  CANDIDATE.PHOTO_URL%TYPE DEFAULT NULL,
        p_id_candidate  OUT CANDIDATE.ID_CANDIDATE%TYPE
    );

    -- Vincula un ODS a una eleccion (RN06 exige al menos 1 antes de publicar)
    PROCEDURE link_sdg(
        p_election_id IN ELECTION_SDG.ELECTION_ID_ELECTION%TYPE,
        p_sdg_id      IN ELECTION_SDG.SDG_ID_SDG%TYPE
    );

    -- Publica la eleccion: valida RN05 (>=2 candidatos) y RN06 (>=1 ODS) antes de habilitarla
    PROCEDURE publish_election(
        p_election_id IN ELECTION.ID_ELECTION%TYPE
    );

END PKG_ELECTION_MGMT;
/

CREATE OR REPLACE PACKAGE BODY PKG_ELECTION_MGMT AS

    PROCEDURE create_election(
        p_title        IN  ELECTION.TITLE%TYPE,
        p_start_date   IN  ELECTION.START_DATE%TYPE,
        p_end_date     IN  ELECTION.END_DATE%TYPE,
        p_contract_id  IN  ELECTION.CONTRACT_ID_CONTRACT%TYPE,
        p_id_election  OUT ELECTION.ID_ELECTION%TYPE
    ) IS
    BEGIN
        SELECT SEQ_ELECTION.NEXTVAL INTO p_id_election FROM DUAL;

        INSERT INTO ELECTION (ID_ELECTION, TITLE, START_DATE, END_DATE, STATUS, CONTRACT_ID_CONTRACT)
        VALUES (p_id_election, p_title, p_start_date, p_end_date, 'DRAFT', p_contract_id);
    END create_election;

    PROCEDURE add_candidate(
        p_election_id   IN  CANDIDATE.ELECTION_ID_ELECTION%TYPE,
        p_name          IN  CANDIDATE.NAME%TYPE,
        p_party_name    IN  CANDIDATE.PARTY_NAME%TYPE,
        p_photo_url     IN  CANDIDATE.PHOTO_URL%TYPE DEFAULT NULL,
        p_id_candidate  OUT CANDIDATE.ID_CANDIDATE%TYPE
    ) IS
    BEGIN
        SELECT SEQ_CANDIDATE.NEXTVAL INTO p_id_candidate FROM DUAL;

        INSERT INTO CANDIDATE (ID_CANDIDATE, NAME, PARTY_NAME, PHOTO_URL, ELECTION_ID_ELECTION)
        VALUES (p_id_candidate, p_name, p_party_name, p_photo_url, p_election_id);
    END add_candidate;

    PROCEDURE link_sdg(
        p_election_id IN ELECTION_SDG.ELECTION_ID_ELECTION%TYPE,
        p_sdg_id      IN ELECTION_SDG.SDG_ID_SDG%TYPE
    ) IS
    BEGIN
        INSERT INTO ELECTION_SDG (ELECTION_ID_ELECTION, SDG_ID_SDG)
        VALUES (p_election_id, p_sdg_id);
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            NULL; -- ya vinculado, operacion idempotente
    END link_sdg;

    PROCEDURE publish_election(
        p_election_id IN ELECTION.ID_ELECTION%TYPE
    ) IS
        v_candidate_count PLS_INTEGER;
        v_sdg_count       PLS_INTEGER;
    BEGIN
        SELECT COUNT(*) INTO v_candidate_count FROM CANDIDATE WHERE ELECTION_ID_ELECTION = p_election_id;
        IF v_candidate_count < 2 THEN
            RAISE_APPLICATION_ERROR(-20010, 'RN05: la eleccion ' || p_election_id || ' necesita al menos 2 candidatos (tiene ' || v_candidate_count || ')');
        END IF;

        SELECT COUNT(*) INTO v_sdg_count FROM ELECTION_SDG WHERE ELECTION_ID_ELECTION = p_election_id;
        IF v_sdg_count < 1 THEN
            RAISE_APPLICATION_ERROR(-20011, 'RN06: la eleccion ' || p_election_id || ' debe vincular al menos 1 ODS');
        END IF;

        UPDATE ELECTION SET STATUS = 'PUBLISHED' WHERE ID_ELECTION = p_election_id;

        PKG_AUDIT.log_event('PUBLISH_ELECTION', 'ELECTION', TO_CHAR(p_election_id));
    END publish_election;

END PKG_ELECTION_MGMT;
/
