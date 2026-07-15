-- E-VoteSys - Ejecutar como SYSTEM (o cualquier usuario con privilegio CREATE ROLE)
-- Prefijo EVOTESYS_ para evitar colisiones con roles de otros proyectos en la misma instancia XE.

CREATE ROLE EVOTESYS_ADMIN;
CREATE ROLE EVOTESYS_OPERATOR;
CREATE ROLE EVOTESYS_AUDITOR;
