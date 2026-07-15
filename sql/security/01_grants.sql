-- E-VoteSys - Privilegios sobre los objetos de EVOTESYS (ejecutar conectado como EVOTESYS)
-- Requiere que los roles EVOTESYS_ADMIN/EVOTESYS_OPERATOR/EVOTESYS_AUDITOR ya existan
-- (ver 00_create_roles_as_system.sql).
--
-- Regla clave (RN03): ningun rol recibe UPDATE/DELETE sobre VOTE. El acceso de negocio
-- se hace via EXECUTE sobre los paquetes, no via privilegios de tabla directos, para que
-- toda escritura pase por las validaciones de PL/SQL (padron, anti-duplicidad, hash, auditoria).

-- ADMIN: gestiona contratos, elecciones, candidatos y ODS; consulta todo; nunca escribe VOTE directo.
GRANT EXECUTE ON PKG_SALES_CONTRACTING TO EVOTESYS_ADMIN;
GRANT EXECUTE ON PKG_ELECTION_MGMT     TO EVOTESYS_ADMIN;
GRANT SELECT ON CURRENCY               TO EVOTESYS_ADMIN;
GRANT SELECT ON CONTRACT               TO EVOTESYS_ADMIN;
GRANT SELECT ON ELECTION               TO EVOTESYS_ADMIN;
GRANT SELECT ON CANDIDATE              TO EVOTESYS_ADMIN;
GRANT SELECT ON SDG                    TO EVOTESYS_ADMIN;
GRANT SELECT ON ELECTION_SDG           TO EVOTESYS_ADMIN;
GRANT SELECT ON VOTER                  TO EVOTESYS_ADMIN;
GRANT SELECT ON VOTER_ELECTION_STATUS  TO EVOTESYS_ADMIN;
GRANT SELECT ON VOTE                   TO EVOTESYS_ADMIN; -- solo lectura, jamas UPDATE/DELETE

-- OPERATOR: opera el flujo de sufragio (inscripcion y emision de voto) via el paquete.
GRANT EXECUTE ON PKG_VOTING            TO EVOTESYS_OPERATOR;
GRANT SELECT ON ELECTION               TO EVOTESYS_OPERATOR;
GRANT SELECT ON CANDIDATE              TO EVOTESYS_OPERATOR;
GRANT SELECT ON VOTER                  TO EVOTESYS_OPERATOR;
GRANT SELECT ON VOTER_ELECTION_STATUS  TO EVOTESYS_OPERATOR;

-- AUDITOR: solo lectura sobre todo, incluida la bitacora de auditoria.
GRANT SELECT ON CURRENCY               TO EVOTESYS_AUDITOR;
GRANT SELECT ON CONTRACT               TO EVOTESYS_AUDITOR;
GRANT SELECT ON ELECTION               TO EVOTESYS_AUDITOR;
GRANT SELECT ON CANDIDATE              TO EVOTESYS_AUDITOR;
GRANT SELECT ON SDG                    TO EVOTESYS_AUDITOR;
GRANT SELECT ON ELECTION_SDG           TO EVOTESYS_AUDITOR;
GRANT SELECT ON VOTER                  TO EVOTESYS_AUDITOR;
GRANT SELECT ON VOTER_ELECTION_STATUS  TO EVOTESYS_AUDITOR;
GRANT SELECT ON VOTE                   TO EVOTESYS_AUDITOR;
GRANT SELECT ON AUDIT_LOG              TO EVOTESYS_AUDITOR;
