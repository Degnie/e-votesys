-- E-VoteSys - Auditoria de transacciones (quien, cuando, que operacion)
-- Registrada explicitamente desde los paquetes de negocio (no via trigger,
-- para mantener un unico punto de logueo por operacion de negocio real).

CREATE SEQUENCE SEQ_AUDIT_LOG START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE AUDIT_LOG (
    ID_AUDIT_LOG    INTEGER NOT NULL,
    DB_USER         VARCHAR2(128) NOT NULL,
    ACTION_NAME     VARCHAR2(50)  NOT NULL,
    TABLE_NAME      VARCHAR2(30)  NOT NULL,
    RECORD_ID       VARCHAR2(50),
    ACTION_TIMESTAMP TIMESTAMP NOT NULL
);

ALTER TABLE AUDIT_LOG ADD CONSTRAINT AUDIT_LOG_PK PRIMARY KEY (ID_AUDIT_LOG);

CREATE OR REPLACE PACKAGE PKG_AUDIT AS
    PROCEDURE log_event(
        p_action_name IN VARCHAR2,
        p_table_name  IN VARCHAR2,
        p_record_id   IN VARCHAR2
    );
END PKG_AUDIT;
/

CREATE OR REPLACE PACKAGE BODY PKG_AUDIT AS
    PROCEDURE log_event(
        p_action_name IN VARCHAR2,
        p_table_name  IN VARCHAR2,
        p_record_id   IN VARCHAR2
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        INSERT INTO AUDIT_LOG (ID_AUDIT_LOG, DB_USER, ACTION_NAME, TABLE_NAME, RECORD_ID, ACTION_TIMESTAMP)
        VALUES (SEQ_AUDIT_LOG.NEXTVAL, SYS_CONTEXT('USERENV','SESSION_USER'), p_action_name, p_table_name, p_record_id, SYSTIMESTAMP);
        COMMIT;
    END log_event;
END PKG_AUDIT;
/
