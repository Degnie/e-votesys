# Changelog

## [Sin publicar] - 2026-07-15 (iteración 6: boleta más visual + fix de "Run Project")

Quería que la boleta se sintiera más parecida a una vista real en vez de solo mostrar resultados en
consola. Antes de tocar el diseño encontré la causa de fondo: había dejado `exec.mainClass=com.evotesys.Main`
en el `pom.xml`, así que "Run Project" siempre abría la consola aunque `VotingFrame` ya existiera. Lo cambié
a `com.evotesys.ui.VotingFrame`.

Decidí enfocarme en mejorar `VotingFrame` en vez de armar un shell completo con `CardLayout`, así que le
subí el nivel a la boleta:

- Agregué `dao/ElectionDAO.java`/`ElectionDAOImpl.java` (lectura simple de `ELECTION` con
  `STATUS='PUBLISHED'`) para que la ventana cargue las elecciones disponibles solo al abrir, en vez de
  pedirte escribir el `ID_ELECTION` a ciegas en un campo de texto.
- Cambié el combo de candidatos por un `JList`: se siente más a boleta (una columna de opciones) que a un
  desplegable de formulario.
- Agregué confirmación (`JOptionPane`) antes de votar y mensajes de resultado en verde/rojo en vez de un
  área de texto plana.
- Verifiqué `ElectionDAOImpl` contra Oracle real con una clase descartable (borrada después): trajo la
  elección de prueba (`1 - Eleccion de prueba de carga [PUBLISHED]`) correctamente.

## [Sin publicar] - 2026-07-14 (iteración 5: simulador de votación con Swing)

Quería algo más visual que la mini consola de `Main.java` para simular la votación, así que agregué una
ventana Swing sin tocar la capa de negocio existente.

- `model/Candidate.java` + `dao/CandidateDAO.java`/`CandidateDAOImpl.java`: lectura simple de `CANDIDATE`
  por elección. La dejo fuera de `PKG_VOTING` a propósito — es solo para poblar el combo de la UI, no
  participa de ninguna regla de negocio, así que no tiene sentido pasarla por un stored procedure.
- `ui/VotingFrame.java`: carga los candidatos de una elección en un `JComboBox` y, al votar, arma un `Vote`
  y llama a `VoteDAOImpl.registerVote` — el mismo camino que ya usaba `Main.java` (`SP_REGISTER_VOTE` →
  `PKG_VOTING.cast_vote`). No dupliqué ninguna validación en la UI: si el votante ya votó o no está en el
  padrón, el mensaje `ORA-2000x` que regresa Oracle se muestra tal cual en el área de estado.

Antes de escribir la ventana verifiqué `CandidateDAOImpl` por consola (clase descartable, borrada después)
contra la elección de prueba: devolvió los 5 candidatos esperados con el `toString()` que alimenta el combo.

## [Sin publicar] - 2026-07-14 (iteración 4: migración a proyecto Maven)

Al revisar cómo había quedado armado el proyecto en NetBeans, encontré una carpeta fuente plana
(`src/com/evotesys/...`) con classpath manual al `ojdbc11.jar` del Oracle Home — funcional, pero poco
portable. Migré todo a un proyecto Maven de verdad:

- Muevo el código a `src/main/java/com/evotesys/...` y el `config.properties.example` a
  `src/main/resources/`, la convención estándar de Maven.
- Pongo el `pom.xml` en la raíz del repo, con `groupId=com.evotesys`, `artifactId=evotesys-backend`.
- Declaro `com.oracle.database.jdbc:ojdbc11` como dependencia Maven en vez de apuntar a una ruta fija de
  driver — así el proyecto es portable a otra máquina sin editar el classpath a mano.
- Actualizo `.gitignore` para `target/` (Maven) en vez de `build/`.

Verifiqué que la migración no rompió nada: recompilé manualmente contra el mismo `ojdbc11.jar` y corrí
`Main` dos veces — con un votante nuevo (`VOTER_ID_VOTER=2107`) registró el voto (`idVote=3990`); repetir
un votante que ya había votado en la iteración anterior siguió devolviendo `ORA-20002` (RN02).

## [Sin publicar] - 2026-07-14 (iteración 3: smoke test manual de VoteDAOImpl)

Agregué `Main.java` (`src/com/evotesys/Main.java`) como smoke test manual: toma `voterId`, `electionId` y
`candidateId` por argumentos y llama a `VoteDAOImpl.registerVote`, sin depender de una UI todavía inexistente.

Lo corrí contra Oracle real (no solo que compile) con un votante pendiente real de la carga de prueba
(`VOTER_ID_VOTER=2101`, elección 1): la primera corrida registró el voto (`idVote=3989`); repetir el mismo
comando con el mismo votante lo rechazó con `ORA-20002` (RN02), confirmando que la cadena completa
Java → JDBC `CallableStatement` → `SP_REGISTER_VOTE` → `PKG_VOTING.cast_vote` funciona de punta a punta, no
solo el PL/SQL de forma aislada.

## [Sin publicar] - 2026-07-14 (iteración 2: capa de integración Java)

Arranqué el backend Java que va a consumir el esquema Oracle, siguiendo MVC/DAO/Singleton para NetBeans.

### Conexión y modelos (`src/com/evotesys/util/`, `src/com/evotesys/model/`)

- `DatabaseConnection`: Singleton que lee `db.url`/`db.user`/`db.password` de un `config.properties` cargado
  desde el classpath (no del working directory), para no depender de dónde se lance la JVM. Dejo
  `src/config.properties.example` versionado y agrego `src/config.properties` al `.gitignore`, para que las
  credenciales reales nunca terminen en el repo.
- `Voter`, `Election`, `Vote`: POJOs uno a uno con `VOTER`, `ELECTION` y `VOTE`. La única excepción es
  `Vote.voterId`: no hay columna `VOTER_ID` en la tabla `VOTE` (así preservo el anonimato del voto, ver
  RN03 en el README), así que ese campo vive solo en el objeto Java — nunca se persiste — y existe
  únicamente para que el DAO pueda pasarle la identidad del votante a `SP_REGISTER_VOTE` y validar RN01/RN02.

### DAO del flujo de votación (`src/com/evotesys/dao/`)

`VoteDAOImpl.registerVote(Vote)` abre su propia conexión (try-with-resources) e invoca
`{call SP_REGISTER_VOTE(?, ?, ?, ?)}` vía `CallableStatement`. Decidí no envolver el `SQLException` en un
`catch` que solo lo relanzaba sin agregar nada — lo dejo propagar tal cual, con el mensaje `ORA-2000x`
original de `PKG_VOTING` intacto, para que quien llame decida si lo traduce o lo muestra.

### PL/SQL: punto de entrada para JDBC (`sql/plsql/04_sp_register_vote.sql`)

Agregué `SP_REGISTER_VOTE` como procedimiento standalone (no dentro de un paquete) porque
`{call SP_REGISTER_VOTE(?,?,?,?)}` es más simple de invocar desde JDBC que un procedimiento de paquete.
Es un envoltorio delgado: toda la lógica de negocio sigue en `PKG_VOTING.cast_vote`, al que además le agregué
un parámetro `p_id_vote OUT` (antes el ID generado quedaba solo en una variable local) para poder devolverle
el ID del voto recién creado a la capa Java. Actualicé también la llamada existente en
`sql/data/01_load_test_data.sql` a la nueva firma.

Verifiqué `SP_REGISTER_VOTE` contra Oracle real (no solo que compile): tomé un votante pendiente de la carga
de datos de prueba, lo hice votar a través del procedimiento, y confirmó el mismo `ID_VOTE` que hubiera
generado `PKG_VOTING.cast_vote` directamente.

## [Sin publicar] - 2026-07-14 (iteración 1: esquema físico, paquetes PL/SQL, seguridad y datos de prueba)

Arranqué la fase de implementación a partir del diseño de datos ya aprobado (Entregable Parcial II) y dejé
el esquema completo corriendo y verificado contra una instancia Oracle XE real (esquema `EVOTESYS`), no solo
escrito en archivos.

### Esquema físico (`sql/ddl/`)

Creé las 9 tablas del diccionario de datos en orden de dependencia (`01_create_tables.sql`), separé las
restricciones en `02_constraints.sql` (PK, FK y el `CHECK` de RN04: `END_DATE > START_DATE`) y agregué
`03_sequences_indexes.sql` con una secuencia por tabla de PK simple y un índice por cada columna FK, ya que
Oracle no las indexa automáticamente y sin esto los `JOIN`/reportes de conteo escanearían las tablas completas.

### Paquetes PL/SQL (`sql/plsql/`)

- **`PKG_SALES_CONTRACTING`**: registro de monedas y contratos (RN07, montos en múltiples monedas vía
  `CONTRACT.CURRENCY_ID_CURRENCY`).
- **`PKG_ELECTION_MGMT`**: alta de elecciones, candidatos y vínculo con ODS, más `publish_election`, que
  valida RN05 (mínimo 2 candidatos) y RN06 (mínimo 1 ODS) antes de dejar la elección `PUBLISHED`.
- **`PKG_VOTING`** (el más crítico): `enroll_voter` inscribe al votante en el padrón
  (`VOTER_ELECTION_STATUS`), y `cast_vote` valida RN01 (padrón), RN02 (anti-duplicidad) y que el candidato
  pertenezca a la elección, antes de insertar en `VOTE` (que nunca referencia al votante, para preservar el
  anonimato) y marcar `HAS_VOTED='S'`.
  - Uso `SELECT ... FOR UPDATE` sobre la fila del padrón antes de leer `HAS_VOTED`, para que dos llamadas
    concurrentes del mismo votante no pasen juntas la validación de RN02 (si no bloqueo la fila, ambas
    transacciones podrían leer `HAS_VOTED='N'` antes de que cualquiera confirme su `UPDATE`, y terminarían
    votando dos veces).
  - Descubrí en el camino que `STANDARD_HASH` es una función solo-SQL: no se puede asignar directo en
    PL/SQL, así que la resuelvo con `SELECT STANDARD_HASH(...) INTO v_hash FROM DUAL`.
- **`TRG_VOTE_IMMUTABLE`**: trigger `BEFORE UPDATE OR DELETE ON VOTE` que aborta cualquier intento de
  alterar un voto ya confirmado (RN03), sin excepción para el rol administrador.

### Auditoría (`sql/audit/`)

Agregué `AUDIT_LOG` + `PKG_AUDIT.log_event`, invocado desde los tres paquetes de negocio en sus operaciones
relevantes (contrato registrado, elección publicada, voto emitido). Uso `PRAGMA AUTONOMOUS_TRANSACTION` para
que el registro de auditoría quede confirmado aunque la transacción que lo originó haga rollback después.

### Seguridad (`sql/security/`)

Separé la creación de roles (`00_create_roles_as_system.sql`, requiere un privilegio de sistema que el
usuario de aplicación `EVOTESYS` no tiene) de los `GRANT` sobre los objetos (`01_grants.sql`, ejecutable como
`EVOTESYS`). Prefijé los roles como `EVOTESYS_ADMIN`/`EVOTESYS_OPERATOR`/`EVOTESYS_AUDITOR` porque la
instancia XE es compartida con otros proyectos y quería evitar colisiones de nombre. Ningún rol recibe
`UPDATE`/`DELETE` sobre `VOTE` bajo ningún caso: el acceso de escritura pasa siempre por `EXECUTE` sobre los
paquetes, nunca por privilegio de tabla directo, para que toda escritura real pase por las validaciones de
negocio.

### Datos de prueba (`sql/data/`)

Escribí `01_load_test_data.sql`: carga los 17 ODS y dos monedas de referencia, registra un contrato y una
elección de prueba con 5 candidatos y 3 ODS vinculados, la publica, genera 5000 votantes en bloque (por
volumen, sin pasar por el paquete) y hace que ~80% ya haya votado, este último tramo sí a través de
`PKG_VOTING.cast_vote`, para ejercitar la lógica real también en la carga de datos.

### Verificación contra la base real

No me quedé con que compilara: corrí el flujo completo contra Oracle y confirmé que las tres reglas críticas
bloquean exactamente lo que deben bloquear:

- Intenté votar dos veces con el mismo votante → RN02 lo rechazó (`ORA-20002`).
- Intenté borrar un voto ya insertado → RN03 lo rechazó (`ORA-20099`, disparado por `TRG_VOTE_IMMUTABLE`).
- Intenté votar con un votante no inscrito en el padrón → RN01 lo rechazó (`ORA-20001`).

