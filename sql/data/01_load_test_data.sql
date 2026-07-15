-- E-VoteSys - Carga de datos ficticios para pruebas de rendimiento/concurrencia
-- Volumen sugerido 1000-100000: para cambiarlo, edita v_voter_count mas abajo.
-- Los votantes y el padron se cargan en bloque (bypass de PKG_VOTING) por volumen;
-- el voto en si se emite via PKG_VOTING.cast_vote para ejercitar tambien la logica real.

-- 1. ODS de referencia (17 Objetivos de Desarrollo Sostenible)
INSERT INTO SDG (ID_SDG, DESCRIPTION)
SELECT LEVEL, 'ODS ' || LEVEL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM SDG)
CONNECT BY LEVEL <= 17;

-- 2. Monedas
INSERT INTO CURRENCY (ID_CURRENCY, NAME, SYMBOL)
SELECT 1, 'Sol Peruano', 'S/' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CURRENCY WHERE ID_CURRENCY = 1);
INSERT INTO CURRENCY (ID_CURRENCY, NAME, SYMBOL)
SELECT 2, 'Dolar Americano', '$' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CURRENCY WHERE ID_CURRENCY = 2);
COMMIT;

-- 3. Contrato + eleccion de prueba + 5 candidatos + 3 ODS vinculados + publicacion
DECLARE
    v_id_contract  CONTRACT.ID_CONTRACT%TYPE;
    v_id_election  ELECTION.ID_ELECTION%TYPE;
    v_id_candidate CANDIDATE.ID_CANDIDATE%TYPE;
    v_voter_count  PLS_INTEGER := 5000; -- ajustar aqui para otro volumen (1000-100000)
    v_min_candidate CANDIDATE.ID_CANDIDATE%TYPE;
    v_id_vote_out   VOTE.ID_VOTE%TYPE;
BEGIN
    PKG_SALES_CONTRACTING.register_contract(
        p_client_name  => 'Universidad de Prueba E-VoteSys',
        p_total_amount => 15000,
        p_currency_id  => 1,
        p_id_contract  => v_id_contract
    );

    PKG_ELECTION_MGMT.create_election(
        p_title       => 'Eleccion de prueba de carga',
        p_start_date  => SYSDATE - 1,
        p_end_date    => SYSDATE + 7,
        p_contract_id => v_id_contract,
        p_id_election => v_id_election
    );

    FOR i IN 1..5 LOOP
        PKG_ELECTION_MGMT.add_candidate(
            p_election_id  => v_id_election,
            p_name         => 'Candidato ' || i,
            p_party_name   => 'Partido ' || CHR(64 + i),
            p_id_candidate => v_id_candidate
        );
    END LOOP;

    PKG_ELECTION_MGMT.link_sdg(v_id_election, 1);
    PKG_ELECTION_MGMT.link_sdg(v_id_election, 4);
    PKG_ELECTION_MGMT.link_sdg(v_id_election, 16);

    PKG_ELECTION_MGMT.publish_election(v_id_election);

    -- 4. Votantes en bloque
    INSERT INTO VOTER (ID_VOTER, DOCUMENT_ID, FULL_NAME, EMAIL)
    SELECT SEQ_VOTER.NEXTVAL, 'DNI' || LPAD(LEVEL, 8, '0'), 'Votante de Prueba ' || LEVEL,
           'votante' || LEVEL || '@evotesys.test'
    FROM DUAL
    CONNECT BY LEVEL <= v_voter_count;

    -- 5. Padron: se inscribe a todos los votantes recien creados en la eleccion de prueba
    INSERT INTO VOTER_ELECTION_STATUS (VOTER_ID_VOTER, ELECTION_ID_ELECTION, HAS_VOTED)
    SELECT ID_VOTER, v_id_election, 'N'
    FROM VOTER
    WHERE DOCUMENT_ID LIKE 'DNI%'
      AND ID_VOTER NOT IN (SELECT VOTER_ID_VOTER FROM VOTER_ELECTION_STATUS WHERE ELECTION_ID_ELECTION = v_id_election);

    -- 6. ~80% de los votantes ya vota (para pruebas de concurrencia/reportes); el resto queda pendiente
    SELECT MIN(ID_CANDIDATE) INTO v_min_candidate FROM CANDIDATE WHERE ELECTION_ID_ELECTION = v_id_election;

    FOR c IN (
        SELECT VOTER_ID_VOTER
        FROM VOTER_ELECTION_STATUS
        WHERE ELECTION_ID_ELECTION = v_id_election AND HAS_VOTED = 'N'
          AND ORA_HASH(VOTER_ID_VOTER, 99) < 80
    ) LOOP
        PKG_VOTING.cast_vote(
            p_voter_id     => c.VOTER_ID_VOTER,
            p_election_id  => v_id_election,
            p_candidate_id => v_min_candidate + MOD(c.VOTER_ID_VOTER, 5),
            p_id_vote      => v_id_vote_out
        );
    END LOOP;

    COMMIT;

    DBMS_OUTPUT.PUT_LINE('Eleccion de prueba: ' || v_id_election || ' | Votantes cargados: ' || v_voter_count);
END;
/
