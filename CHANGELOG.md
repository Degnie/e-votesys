# Changelog

## [2.0.0] - 2026-07-16
### Añadido (Added)
* `pom.xml`: agregado `HikariCP` (pool de conexiones), `slf4j-api` + `logback-classic` (logging transaccional) y `maven-shade-plugin` (Fat JAR ejecutable en `package`).
* `service/VotingService.java`: intermediario obligatorio entre la UI y `VoteDAO`/`ElectionDAO`/`CandidateDAO`.
* Excepciones de negocio legibles que traducen los códigos de `PKG_VOTING`: `NotEnrolledException` (RN01/ORA-20001), `AlreadyVotedException` (RN02/ORA-20002), `InvalidElectionStateException` (ORA-20003), `InvalidCandidateException` (ORA-20004). Cada intento de voto (aceptado o rechazado) se registra con slf4j.
* `service/VoteRequestDTO.java`: transporta la intención del usuario (elección, candidato, secure token) desde la vista hacia el servicio.
* `controller/VotingController.java`: concentra toda la orquestación (llamar al servicio, decidir qué mostrar), extraída de `VotingFrame`.
* `ui/VotingView` (interfaz): `VotingFrame` la implementa y queda como vista puramente declarativa (`showElections`, `showCandidates`, `showVoteSuccess`, `showError`, `setLoading`).
* `CompositionRoot.java`: único lugar del proyecto donde se instancian los DAOs (`new *DAOImpl()` eliminado de `VotingFrame` y `VotingService`).
* Ejecución asíncrona de `loadElections`, `loadCandidatesForElection` y `castVote` vía `SwingWorker` (nunca en el Event Dispatch Thread); `view.setLoading(true, "...")` deshabilita combo/lista/botón/token y muestra progreso mientras corren, reactivando todo en `done()`.
* Campo "Secure Token" (`SVT-<voterId>-<nonce>`) en `VotingFrame.java`, en reemplazo del campo libre "ID Votante"; `service/SecureTokenParser.java` es el único punto que extrae el `voterId` del token, evitando que la vista pase un ID crudo sin verificar a la capa de servicio.

### Cambiado (Changed)
* `maven.compiler.source`/`target` subido de 8 a 17.
* `util/DatabaseConnection.java`: reemplazado `DriverManager` por `HikariDataSource`; credenciales leídas primero de variables de entorno (`DB_URL`/`DB_USER`/`DB_PASSWORD`), con `config.properties` como fallback solo para desarrollo local.
* `sql/plsql/03_pkg_voting.sql`: `PKG_VOTING.cast_vote` generaba el `HASH_CODE` con `DBMS_RANDOM.STRING` (PRNG de propósito general, no apto para uso criptográfico); reemplazado por `DBMS_CRYPTO.RANDOMBYTES(16)` vía `RAWTOHEX` para dar entropía de grado criptográfico a la sal del `STANDARD_HASH`. Requiere `GRANT EXECUTE ON DBMS_CRYPTO TO EVOTESYS` una sola vez (documentado en el propio script).
* Alcance explícito de esta iteración: no se tocó `PKG_VOTING`/`TRG_VOTE_IMMUTABLE` ni ninguna otra regla de negocio; RN01-RN07 siguen validándose 100% del lado de Oracle.

### Arreglado (Fixed)
* `VotingFrame` ya no captura `SQLException` directo ni muestra el mensaje `ORA-2000x` crudo en pantalla: toda excepción pasa ahora por `VotingService`, que la traduce a una excepción de negocio legible.

### Rechazado / Descartado (Rejected/Discarded)
* Rate limiting sobre `castVote`/`SP_REGISTER_VOTE` (mitigación de fuerza bruta/DoS) — pospuesto para una futura ronda de arquitectura avanzada; hoy no hay límite de intentos por token/IP a nivel aplicativo.
* Merkle Tree para encadenar los votos de una elección y probar su integridad agregada de forma verificable — pospuesto; hoy solo existe el hash individual por voto que genera `PKG_VOTING.cast_vote`.

## [1.5.0] - 2026-07-15
### Añadido (Added)
* `dao/ElectionDAO.java` / `ElectionDAOImpl.java`: lectura de `ELECTION` con `STATUS='PUBLISHED'`, para que `VotingFrame` cargue las elecciones disponibles al abrir en vez de pedir el `ID_ELECTION` a ciegas por teclado.
* Confirmación (`JOptionPane`) antes de votar y mensajes de resultado en verde/rojo en `VotingFrame`, en vez de un área de texto plana.
* Verificación manual: `ElectionDAOImpl` probado contra Oracle real con una clase descartable (borrada después) — trajo correctamente la elección de prueba (`1 - Eleccion de prueba de carga [PUBLISHED]`).

### Cambiado (Changed)
* Combo de candidatos reemplazado por un `JList` en `VotingFrame`: se siente más a boleta real (columna de opciones) que a un desplegable de formulario.

### Arreglado (Fixed)
* `pom.xml`: causa raíz de que "Run Project" siempre abriera la consola — `exec.mainClass` apuntaba a `com.evotesys.Main` aunque `VotingFrame` ya existía. Corregido a `com.evotesys.ui.VotingFrame`.

### Rechazado / Descartado (Rejected/Discarded)
* Armar un shell completo con `CardLayout` — descartado en favor de enfocar el esfuerzo en mejorar `VotingFrame` directamente.

## [1.4.0] - 2026-07-14
### Añadido (Added)
* `model/Candidate.java` + `dao/CandidateDAO.java`/`CandidateDAOImpl.java`: lectura simple de `CANDIDATE` por elección.
* `ui/VotingFrame.java`: primera ventana Swing para simular la votación. Carga los candidatos de una elección en un `JComboBox` y, al votar, arma un `Vote` y llama a `VoteDAOImpl.registerVote` — el mismo camino que ya usaba `Main.java` (`SP_REGISTER_VOTE` → `PKG_VOTING.cast_vote`). No duplica validaciones: si el votante ya votó o no está en el padrón, el `ORA-2000x` que regresa Oracle se muestra tal cual en el área de estado.
* Verificación manual: `CandidateDAOImpl` probado por consola (clase descartable, borrada después) contra la elección de prueba — devolvió los 5 candidatos esperados con el `toString()` que alimenta el combo.

### Cambiado (Changed)
* Ninguno en esta iteración.

### Arreglado (Fixed)
* Ninguno en esta iteración.

### Rechazado / Descartado (Rejected/Discarded)
* Pasar `CandidateDAO` por un stored procedure de `PKG_VOTING` — descartado a propósito: es solo lectura para poblar el combo de la UI, no participa de ninguna regla de negocio, así que no tiene sentido pasarla por un paquete PL/SQL.

## [1.3.0] - 2026-07-14
### Añadido (Added)
* Proyecto Maven real: código movido a `src/main/java/com/evotesys/...`, `config.properties.example` a `src/main/resources/`.
* `pom.xml` en la raíz del repo (`groupId=com.evotesys`, `artifactId=evotesys-backend`), con `com.oracle.database.jdbc:ojdbc11` declarado como dependencia Maven en vez de apuntar a una ruta fija del driver en el Oracle Home — portable a otra máquina sin editar el classpath a mano.
* `.gitignore` actualizado para `target/` (Maven) en vez de `build/`.
* Verificación: recompilado manualmente contra el mismo `ojdbc11.jar` y corrido `Main` dos veces tras la migración — con un votante nuevo (`VOTER_ID_VOTER=2107`) registró el voto (`idVote=3990`); repetir un votante que ya había votado en la iteración anterior siguió devolviendo `ORA-20002` (RN02), confirmando que la migración no rompió nada.

### Cambiado (Changed)
* Abandonada la carpeta fuente plana (`src/com/evotesys/...`) con classpath manual al `ojdbc11.jar` del Oracle Home: funcional pero poco portable.

### Arreglado (Fixed)
* Ninguno en esta iteración.

### Rechazado / Descartado (Rejected/Discarded)
* Ninguno en esta iteración.

## [1.2.0] - 2026-07-14
### Añadido (Added)
* `Main.java` (`src/com/evotesys/Main.java`) como smoke test manual: toma `voterId`, `electionId` y `candidateId` por argumentos y llama a `VoteDAOImpl.registerVote`, sin depender de una UI todavía inexistente.
* Verificación contra Oracle real: con un votante pendiente real de la carga de prueba (`VOTER_ID_VOTER=2101`, elección 1), la primera corrida registró el voto (`idVote=3989`); repetir el mismo comando con el mismo votante lo rechazó con `ORA-20002` (RN02), confirmando que la cadena completa Java → JDBC `CallableStatement` → `SP_REGISTER_VOTE` → `PKG_VOTING.cast_vote` funciona de punta a punta, no solo el PL/SQL de forma aislada.

### Cambiado (Changed)
* Ninguno en esta iteración.

### Arreglado (Fixed)
* Ninguno en esta iteración.

### Rechazado / Descartado (Rejected/Discarded)
* Ninguno en esta iteración.

## [1.1.0] - 2026-07-14
### Añadido (Added)
* `util/DatabaseConnection.java`: Singleton que lee `db.url`/`db.user`/`db.password` de un `config.properties` cargado desde el classpath (no del working directory), para no depender de dónde se lance la JVM.
* `src/config.properties.example` versionado; `src/config.properties` agregado al `.gitignore` para que las credenciales reales nunca terminen en el repo.
* `Voter`, `Election`, `Vote`: POJOs uno a uno con `VOTER`, `ELECTION` y `VOTE`. Excepción: `Vote.voterId` no tiene columna `VOTER_ID` en la tabla `VOTE` (preserva el anonimato del voto, RN03) — vive solo en el objeto Java, nunca se persiste, y existe únicamente para que el DAO le pase la identidad del votante a `SP_REGISTER_VOTE` y valide RN01/RN02.
* `dao/VoteDAOImpl.registerVote(Vote)`: abre su propia conexión (try-with-resources) e invoca `{call SP_REGISTER_VOTE(?, ?, ?, ?)}` vía `CallableStatement`.
* `sql/plsql/04_sp_register_vote.sql`: `SP_REGISTER_VOTE` como procedimiento standalone (no dentro de un paquete), porque `{call SP_REGISTER_VOTE(?,?,?,?)}` es más simple de invocar desde JDBC que un procedimiento de paquete. Envoltorio delgado: toda la lógica de negocio sigue en `PKG_VOTING.cast_vote`, al que se le agregó el parámetro `p_id_vote OUT` (antes el ID generado quedaba solo en una variable local) para devolver el ID del voto recién creado a la capa Java.
* Verificación: `SP_REGISTER_VOTE` probado contra Oracle real con un votante pendiente de la carga de datos de prueba — confirmó el mismo `ID_VOTE` que hubiera generado `PKG_VOTING.cast_vote` directamente.

### Cambiado (Changed)
* `sql/data/01_load_test_data.sql`: actualizada la llamada existente a la nueva firma de `PKG_VOTING.cast_vote` (parámetro `p_id_vote OUT`).

### Arreglado (Fixed)
* `VoteDAOImpl`: el `SQLException` se deja propagar tal cual, con el mensaje `ORA-2000x` original de `PKG_VOTING` intacto, para que quien llame decida si lo traduce o lo muestra.

### Rechazado / Descartado (Rejected/Discarded)
* Envolver el `SQLException` en un `catch` que solo lo relanzaba sin agregar nada — descartado en favor de dejarlo propagar sin envoltorio.

## [1.0.0] - 2026-07-14
### Añadido (Added)
* Esquema físico (`sql/ddl/`): 9 tablas del diccionario de datos en orden de dependencia (`01_create_tables.sql`), restricciones separadas en `02_constraints.sql` (PK, FK y el `CHECK` de RN04: `END_DATE > START_DATE`), y `03_sequences_indexes.sql` con una secuencia por tabla de PK simple y un índice por cada columna FK (Oracle no las indexa automáticamente; sin esto los `JOIN`/reportes de conteo escanearían las tablas completas).
* `PKG_SALES_CONTRACTING`: registro de monedas y contratos (RN07, montos en múltiples monedas vía `CONTRACT.CURRENCY_ID_CURRENCY`).
* `PKG_ELECTION_MGMT`: alta de elecciones, candidatos y vínculo con ODS; `publish_election` valida RN05 (mínimo 2 candidatos) y RN06 (mínimo 1 ODS) antes de dejar la elección `PUBLISHED`.
* `PKG_VOTING` (el más crítico): `enroll_voter` inscribe al votante en el padrón (`VOTER_ELECTION_STATUS`); `cast_vote` valida RN01 (padrón), RN02 (anti-duplicidad) y que el candidato pertenezca a la elección, antes de insertar en `VOTE` (que nunca referencia al votante, para preservar el anonimato) y marcar `HAS_VOTED='S'`. Usa `SELECT ... FOR UPDATE` sobre la fila del padrón antes de leer `HAS_VOTED`, para que dos llamadas concurrentes del mismo votante no pasen juntas la validación de RN02.
* `TRG_VOTE_IMMUTABLE`: trigger `BEFORE UPDATE OR DELETE ON VOTE` que aborta cualquier intento de alterar un voto ya confirmado (RN03), sin excepción para el rol administrador.
* Auditoría (`sql/audit/`): `AUDIT_LOG` + `PKG_AUDIT.log_event`, invocado desde los tres paquetes de negocio en sus operaciones relevantes (contrato registrado, elección publicada, voto emitido). Usa `PRAGMA AUTONOMOUS_TRANSACTION` para que el registro de auditoría quede confirmado aunque la transacción que lo originó haga rollback después.
* Seguridad (`sql/security/`): creación de roles separada de los `GRANT` sobre los objetos (`00_create_roles_as_system.sql`, requiere un privilegio de sistema que `EVOTESYS` no tiene; `01_grants.sql`, ejecutable como `EVOTESYS`). Roles prefijados `EVOTESYS_ADMIN`/`EVOTESYS_OPERATOR`/`EVOTESYS_AUDITOR` para evitar colisiones de nombre en la instancia XE compartida con otros proyectos. Ningún rol recibe `UPDATE`/`DELETE` sobre `VOTE` bajo ningún caso: el acceso de escritura pasa siempre por `EXECUTE` sobre los paquetes, nunca por privilegio de tabla directo.
* Datos de prueba (`sql/data/01_load_test_data.sql`): carga de 17 ODS y dos monedas de referencia, un contrato y una elección de prueba con 5 candidatos y 3 ODS vinculados (publicada), 5000 votantes generados en bloque (por volumen, sin pasar por el paquete) con ~80% marcado como ya votado a través de `PKG_VOTING.cast_vote`, para ejercitar la lógica real también en la carga de datos.
* Verificación contra la base real: se corrió el flujo completo contra Oracle y se confirmó que las tres reglas críticas bloquean exactamente lo que deben bloquear — votar dos veces con el mismo votante fue rechazado por RN02 (`ORA-20002`); borrar un voto ya insertado fue rechazado por RN03 vía `TRG_VOTE_IMMUTABLE` (`ORA-20099`); votar con un votante no inscrito en el padrón fue rechazado por RN01 (`ORA-20001`).

### Cambiado (Changed)
* Ninguno en esta iteración.

### Arreglado (Fixed)
* Causa raíz descubierta: `STANDARD_HASH` es una función solo-SQL, no se puede asignar directo en PL/SQL; se resuelve con `SELECT STANDARD_HASH(...) INTO v_hash FROM DUAL`.

### Rechazado / Descartado (Rejected/Discarded)
* Ninguno en esta iteración.
