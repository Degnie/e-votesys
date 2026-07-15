-- E-VoteSys - PKG_SALES_CONTRACTING
-- Cotizacion, registro de contrato y moneda (RN07: montos en multiples monedas)

CREATE OR REPLACE PACKAGE PKG_SALES_CONTRACTING AS

    -- Da de alta una moneda soportada por la plataforma comercial
    PROCEDURE register_currency(
        p_id_currency IN CURRENCY.ID_CURRENCY%TYPE,
        p_name        IN CURRENCY.NAME%TYPE,
        p_symbol      IN CURRENCY.SYMBOL%TYPE
    );

    -- Registra un contrato firmado con un cliente institucional, en la moneda indicada (RN07)
    PROCEDURE register_contract(
        p_client_name   IN  CONTRACT.CLIENT_NAME%TYPE,
        p_total_amount  IN  CONTRACT.TOTAL_AMOUNT%TYPE,
        p_currency_id   IN  CONTRACT.CURRENCY_ID_CURRENCY%TYPE,
        p_date_signed   IN  CONTRACT.DATE_SIGNED%TYPE DEFAULT SYSDATE,
        p_id_contract   OUT CONTRACT.ID_CONTRACT%TYPE
    );

END PKG_SALES_CONTRACTING;
/

CREATE OR REPLACE PACKAGE BODY PKG_SALES_CONTRACTING AS

    PROCEDURE register_currency(
        p_id_currency IN CURRENCY.ID_CURRENCY%TYPE,
        p_name        IN CURRENCY.NAME%TYPE,
        p_symbol      IN CURRENCY.SYMBOL%TYPE
    ) IS
    BEGIN
        INSERT INTO CURRENCY (ID_CURRENCY, NAME, SYMBOL)
        VALUES (p_id_currency, p_name, p_symbol);
    END register_currency;

    PROCEDURE register_contract(
        p_client_name   IN  CONTRACT.CLIENT_NAME%TYPE,
        p_total_amount  IN  CONTRACT.TOTAL_AMOUNT%TYPE,
        p_currency_id   IN  CONTRACT.CURRENCY_ID_CURRENCY%TYPE,
        p_date_signed   IN  CONTRACT.DATE_SIGNED%TYPE DEFAULT SYSDATE,
        p_id_contract   OUT CONTRACT.ID_CONTRACT%TYPE
    ) IS
        v_dummy PLS_INTEGER;
    BEGIN
        -- Validacion basica: la moneda debe existir (la FK ya lo exige, esto da un mensaje claro)
        SELECT 1 INTO v_dummy FROM CURRENCY WHERE ID_CURRENCY = p_currency_id;

        SELECT SEQ_CONTRACT.NEXTVAL INTO p_id_contract FROM DUAL;

        INSERT INTO CONTRACT (ID_CONTRACT, CLIENT_NAME, TOTAL_AMOUNT, DATE_SIGNED, CURRENCY_ID_CURRENCY)
        VALUES (p_id_contract, p_client_name, p_total_amount, p_date_signed, p_currency_id);

        PKG_AUDIT.log_event('REGISTER_CONTRACT', 'CONTRACT', TO_CHAR(p_id_contract));
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20001, 'Moneda inexistente: ' || p_currency_id);
    END register_contract;

END PKG_SALES_CONTRACTING;
/
