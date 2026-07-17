# E-VoteSys — Plataforma Integral de Gestión de Votación Electrónica

> Handoff técnico (Fase de implementación).
> Motor de base de datos objetivo: **Oracle Database** · Lógica de negocio en **PL/SQL**.

## 1. Contexto del proyecto

**Cliente:** Oficina Nacional de Procesos Electorales Digitales (ONPED), entidad encargada de procesar, supervisar y validar procesos electorales municipales, regionales, universitarias e institucionales, en contextos público o privado.

**Producto:** E-VoteSys, un generador de sistemas de votación electrónica adaptable a distintos tipos de elección, que debe garantizar integridad, seguridad, auditoría, respaldo y disponibilidad de la información electoral, y que además contempla una capa comercial multimoneda (venta del servicio a distintos clientes institucionales) y un enfoque de responsabilidad social vinculado a los Objetivos de Desarrollo Sostenible (ODS).

**Este README resume el diseño de datos ya aprobado (Entregable Parcial II) y define el alcance de la siguiente fase: implementación del esquema físico, objetos de base de datos y paquetes/procedimientos PL/SQL.**

## 2. Actores del sistema

Definidos en inglés por política corporativa del proyecto:

| Actor | Rol |
|---|---|
| `Customer` | Cliente institucional (universidad, municipio, etc.) que contrata el servicio. |
| `Sales Representative` | Asesor comercial de la consultora. |
| `Election Organizer` | Representante del cliente que configura los parámetros de la elección. |
| `Voter` | Usuario final habilitado para sufragar. |
| `Voting System / Management System` | Componentes automatizados del software. |

## 3. Reglas de negocio (deben quedar codificadas como constraints, triggers o procedures)

| ID | Regla | Mecanismo sugerido |
|---|---|---|
| RN01 | Validación de padrón electoral antes de habilitar el voto. | Procedure/Trigger contra `VOTER` y `VOTER_ELECTION_STATUS`. |
| RN02 | Un votante emite **un solo voto por elección** (anti-duplicidad). | Trigger/Procedure que valide `VOTER_ELECTION_STATUS.HAS_VOTED` antes de insertar en `VOTE`. |
| RN03 | El voto confirmado es **inmutable**: no se permite `UPDATE`/`DELETE` sobre `VOTE`, ni siquiera por el administrador. | Permisos restringidos a nivel de rol + tabla de solo inserción. |
| RN04 | `END_DATE` de una elección debe ser estrictamente posterior a `START_DATE`. | `CHECK CONSTRAINT` en `ELECTION`. |
| RN05 | No se puede generar una instancia de votación sin al menos **2 candidatos** registrados. | Procedure de validación antes de "publicar" la elección. |
| RN06 | Toda elección debe estar vinculada a **al menos 1 ODS**. | Validación en `ELECTION_SDG` (regla de negocio, no expresable como constraint simple — requiere trigger/procedure). |
| RN07 | El contrato/cotización debe permitir registrar montos en **múltiples monedas**. | Modelado vía `CONTRACT.CURRENCY_ID_CURRENCY` → `CURRENCY`. |

**Nota de seguridad clave:** la tabla `VOTE` **no contiene referencia a `ID_VOTER`**, para preservar el anonimato del sufragio. La validación de "ya votó" se resuelve exclusivamente contra `VOTER_ELECTION_STATUS`.

## 4. Modelo de datos

### 4.1 Entidades y relaciones (resumen lógico)

- **CURRENCY** ← **CONTRACT** ← **ELECTION** ← **CANDIDATE** (Muchos a Uno en cascada)
- **ELECTION** ↔ **SDG** vía **ELECTION_SDG** (Muchos a Muchos)
- **VOTER** ↔ **ELECTION** vía **VOTER_ELECTION_STATUS** (llave compuesta, controla unicidad del voto — RN02)
- **VOTE** → **ELECTION** y **VOTE** → **CANDIDATE** (Muchos a Uno); **VOTE** deliberadamente desacoplado de **VOTER**

### 4.2 Diccionario de datos físico (fuente de verdad para los scripts DDL)

Las tablas `VOTE` y `VOTER_ELECTION_STATUS` ya incorporan la corrección de llave primaria/compuesta aplicada en la última revisión del Entregable Parcial II.

#### `CURRENCY`

| Columna | Tipo | PK/FK | Nulabilidad |
|---|---|---|---|
| `ID_CURRENCY` | INTEGER | PK | NOT NULL |
| `NAME` | VARCHAR2 | - | NULL |
| `SYMBOL` | VARCHAR2 | - | NULL |

Constraints:
- `CURRENCY_PK` (PRIMARY KEY) (ID_CURRENCY)

#### `CONTRACT`

| Columna | Tipo | PK/FK | Nulabilidad |
|---|---|---|---|
| `ID_CONTRACT` | INTEGER | PK | NOT NULL |
| `CLIENT_NAME` | VARCHAR2 | - | NULL |
| `TOTAL_AMOUNT` | NUMBER | - | NULL |
| `DATE_SIGNED` | DATE | - | NULL |
| `CURRENCY_ID_CURRENCY` | INTEGER | FK | NOT NULL |

Constraints:
- `CONTRACT_PK` (PRIMARY KEY) (ID_CONTRACT)
- `CONTRACT_CURRENCY_FK` (FOREIGN KEY) (CURRENCY_ID_CURRENCY)

#### `ELECTION`

| Columna | Tipo | PK/FK | Nulabilidad |
|---|---|---|---|
| `ID_ELECTION` | INTEGER | PK | NOT NULL |
| `TITLE` | VARCHAR2 | - | NULL |
| `START_DATE` | DATE | - | NULL |
| `END_DATE` | DATE | - | NULL |
| `STATUS` | VARCHAR2 | - | NULL |
| `CONTRACT_ID_CONTRACT` | INTEGER | FK | NOT NULL |

Constraints:
- `ELECTION_PK` (PRIMARY KEY) (ID_ELECTION)
- `ELECTION_CONTRACT_FK` (FOREIGN KEY) (CONTRACT_ID_CONTRACT)

#### `CANDIDATE`

| Columna | Tipo | PK/FK | Nulabilidad |
|---|---|---|---|
| `ID_CANDIDATE` | INTEGER | PK | NOT NULL |
| `NAME` | VARCHAR2 | - | NULL |
| `PARTY_NAME` | VARCHAR2 | - | NULL |
| `PHOTO_URL` | VARCHAR2 | - | NULL |
| `ELECTION_ID_ELECTION` | INTEGER | FK | NOT NULL |

Constraints:
- `CANDIDATE_PK` (PRIMARY KEY) (ID_CANDIDATE)
- `CANDIDATE_ELECTION_FK` (FOREIGN KEY) (ELECTION_ID_ELECTION)

#### `ELECTION_SDG`

| Columna | Tipo | PK/FK | Nulabilidad |
|---|---|---|---|
| `ELECTION_ID_ELECTION` | INTEGER | PK, FK | NOT NULL |
| `SDG_ID_SDG` | INTEGER | PK, FK | NOT NULL |

Constraints:
- `Relation_4_PK` (PRIMARY KEY) (ELECTION_ID_ELECTION, SDG_ID_SDG)
- `Relation_4_ELECTION_FK` (FOREIGN KEY) (ELECTION_ID_ELECTION)
- `Relation_4_SDG_FK` (FOREIGN KEY) (SDG_ID_SDG)

#### `SDG`

| Columna | Tipo | PK/FK | Nulabilidad |
|---|---|---|---|
| `ID_SDG` | INTEGER | PK | NOT NULL |
| `DESCRIPTION` | VARCHAR2 | - | NULL |

Constraints:
- `SDG_PK` (PRIMARY KEY) (ID_SDG)

#### `VOTER_ELECTION_STATUS`

| Columna | Tipo | PK/FK | Nulabilidad |
|---|---|---|---|
| `HAS_VOTED` | CHAR(1) | - | NULL |
| `VOTE_DATE` | DATE | - | NULL |
| `VOTER_ID_VOTER` | INTEGER | PK, FK | NOT NULL |
| `ELECTION_ID_ELECTION` | INTEGER | PK, FK | NOT NULL |

Constraints:
- `VOTER_ELECTION_STATUS_PK` (PRIMARY KEY) (VOTER_ID_VOTER, ELECTION_ID_ELECTION)
- `VOTER_ELECTION_STATUS_VOTER_FK` (FOREIGN KEY) (VOTER_ID_VOTER)
- `VOTER_ELECTION_STATUS_ELECTION_FK` (FOREIGN KEY) (ELECTION_ID_ELECTION)

#### `VOTER`

| Columna | Tipo | PK/FK | Nulabilidad |
|---|---|---|---|
| `ID_VOTER` | INTEGER | PK | NOT NULL |
| `DOCUMENT_ID` | VARCHAR2 | - | NULL |
| `FULL_NAME` | VARCHAR2 | - | NULL |
| `EMAIL` | VARCHAR2 | - | NULL |

Constraints:
- `VOTER_PK` (PRIMARY KEY) (ID_VOTER)

#### `VOTE`

| Columna | Tipo | PK/FK | Nulabilidad |
|---|---|---|---|
| `ID_VOTE` | INTEGER | PK | NOT NULL |
| `VOTE_TIMESTAMP` | TIMESTAMP | - | NULL |
| `HASH_CODE` | VARCHAR2 | - | NULL |
| `ELECTION_ID_ELECTION` | INTEGER | FK | NOT NULL |
| `CANDIDATE_ID_CANDIDATE` | INTEGER | FK | NOT NULL |

Constraints:
- `VOTE_PK` (PRIMARY KEY) (ID_VOTE)
- `VOTE_ELECTION_FK` (FOREIGN KEY) (ELECTION_ID_ELECTION)
- `VOTE_CANDIDATE_FK` (FOREIGN KEY) (CANDIDATE_ID_CANDIDATE)

## 5. Alcance de la fase de implementación (lo que sigue)

Con el modelo de datos ya validado, la siguiente fase debe producir:

1. **Scripts de esquema (DDL)**
   - `01_create_tables.sql` — creación de las 9 tablas en el orden de dependencia (`CURRENCY` → `CONTRACT` → `ELECTION` → `CANDIDATE`/`ELECTION_SDG`/`VOTER_ELECTION_STATUS`/`VOTE`; `SDG` y `VOTER` sin dependencias previas).
   - `02_constraints.sql` — PK, FK y `CHECK` (incluye RN04: `END_DATE > START_DATE`).
   - `03_sequences_indexes.sql` — secuencias para IDs autonuméricos y, más adelante, índices de soporte para reportes/conteos.

2. **Objetos PL/SQL**
   - Paquete `PKG_SALES_CONTRACTING` — cotización, registro de contrato, moneda (RN07).
   - Paquete `PKG_ELECTION_MGMT` — alta de elección, candidatos, vínculo con ODS (RN05, RN06).
   - Paquete `PKG_VOTING` — flujo de sufragio: validar padrón (RN01), verificar `VOTER_ELECTION_STATUS` (RN02), insertar en `VOTE`, actualizar estado del votante. Este paquete es el más crítico para la seguridad del sistema.
   - Trigger o `BEFORE INSERT` en `VOTE` que impida cualquier `UPDATE`/`DELETE` posterior (RN03).

3. **Seguridad**
   - Roles: `ADMIN`, `OPERATOR`, `AUDITOR`, aplicados sobre los objetos anteriores.
   - Verificar que ningún rol (incluido `ADMIN`) tenga privilegio de `UPDATE`/`DELETE` directo sobre `VOTE`.

4. **Auditoría y continuidad**
   - Tabla/mecanismo de auditoría de transacciones (quién, cuándo, qué operación). La implementé en `sql/audit/01_audit_log.sql`: tabla `AUDIT_LOG` + `PKG_AUDIT.log_event`, invocado desde los paquetes de negocio (contrato, publicación de elección, emisión de voto). Uso transacción autónoma para que el registro de auditoría persista aunque la transacción llamante haga rollback.
   - Estrategia de backup/restore que dejo definida (no como scripts en esta fase):
     - **RMAN** (`BACKUP DATABASE PLUS ARCHIVELOG`) en modo `ARCHIVELOG`, backup completo diario + incrementales, con foco en poder restaurar la tabla `VOTE` sin pérdida (recovery point-in-time).
     - Exports lógicos (`Data Pump expdp`) del esquema `EVOTESYS` antes de cada carga masiva de datos de prueba, para poder revertir sin depender de RMAN.
     - No genero scripts de backup en esta fase (según alcance ya acordado); la instancia XE local que uso para desarrollo no requiere el mismo nivel de continuidad que un ambiente productivo.

5. **Datos de prueba**
   - Scripts de carga de datos ficticios con volumen representativo (1,000–100,000 votantes por elección, según los supuestos del proyecto) para pruebas de rendimiento y concurrencia.

6. **Consideración de escalabilidad (nota ya incorporada en el documento formal)**
   - `VOTE` debe mantenerse bajo el estándar transaccional ACID de Oracle. En escenarios reales de mayor volumen, la carga de lectura para reportes/conteos en tiempo real podría desacoplarse temporalmente hacia una réplica de solo lectura, consolidándose después contra la base transaccional principal.

## 5-bis. Backend Java (capa de integración, proyecto Maven en la raíz)

Empecé la integración Java-Oracle sobre el esquema ya desplegado, con NetBeans (proyecto Maven) + JDBC (ojdbc) + MVC/DAO/Singleton. El código vive en `src/main/java/com/evotesys/`, siguiendo la convención estándar de Maven (`pom.xml` en la raíz del repo, no un proyecto anidado).

- `util/DatabaseConnection.java` — Singleton, credenciales en `config.properties` (fuera del control de versiones; ver `src/main/resources/config.properties.example`), sin nada hardcodeado.
- `model/Voter.java`, `model/Election.java`, `model/Vote.java` — POJOs que reflejan `VOTER`, `ELECTION` y `VOTE`. `Vote.voterId` es la única excepción: no existe columna `VOTER_ID` en `VOTE` (ver nota de anonimato en la sección 3); ese campo viaja solo en memoria para que el DAO pueda validar RN01/RN02 antes de insertar.
- `dao/VoteDAO.java` + `dao/VoteDAOImpl.java` — `registerVote(Vote)` invoca `SP_REGISTER_VOTE` vía `CallableStatement`; no hago ningún `INSERT` directo desde Java, toda la transacción (padrón, anti-duplicidad, hash, auditoría) queda del lado de Oracle.
- `sql/plsql/04_sp_register_vote.sql` — agregué este procedimiento standalone porque `SP_REGISTER_VOTE(?,?,?,?)` es más simple de invocar desde JDBC que un procedimiento dentro de `PKG_VOTING`; internamente delega en `PKG_VOTING.cast_vote`, que ahora también expone el `ID_VOTE` generado por un parámetro `OUT`.
- `Main.java` — smoke test manual por consola (`java com.evotesys.Main <voterId> <electionId> <candidateId>`) para probar `VoteDAOImpl` sin UI.
- `model/Candidate.java`, `dao/CandidateDAO.java`/`CandidateDAOImpl.java` — lectura simple de `CANDIDATE` por elección (sin reglas de negocio, es solo para poblar la boleta en pantalla).
- `dao/ElectionDAO.java`/`ElectionDAOImpl.java` — lectura simple de las elecciones con `STATUS='PUBLISHED'` (mismo motivo: solo para llenar el combo, no toca reglas de negocio).
- `ui/VotingFrame.java` — boleta simulada con Swing: al abrir carga las elecciones publicadas en un combo; al elegir una, carga sus candidatos en una lista (`JList`, no combo, para que se sienta como una boleta real); botón "Emitir voto" con confirmación (`JOptionPane`) que arma un `Vote` y llama a `VoteDAOImpl.registerVote` — el mismo camino que `Main.java`, así que RN01/RN02 se siguen aplicando del lado de Oracle sin duplicar lógica en la UI. Mensajes de éxito/error en verde/rojo.

El `pom.xml` declara `com.oracle.database.jdbc:ojdbc11` como dependencia Maven (Oracle lo publica en Maven Central) y `exec.mainClass=com.evotesys.ui.VotingFrame`, así que "Run Project" abre la ventana directamente (no la consola de `Main.java`).

### Cómo ejecutar

1. Copia `src/main/resources/config.properties.example` a `src/main/resources/config.properties` y completa tus credenciales de `EVOTESYS`. Ese archivo está en `.gitignore`, nunca se sube.
2. **Desde NetBeans:** File → Open Project → selecciona esta carpeta (la detecta como proyecto Maven por el `pom.xml`).
   - Simulador con ventana: Run Project (F6), o botón derecho sobre `VotingFrame.java` → Run File.
   - Smoke test por consola: botón derecho sobre `Main.java` → Run File, con los 3 argumentos (`voterId electionId candidateId`) en Project Properties → Run → Arguments.
3. **Desde línea de comandos** (con Maven instalado): `mvn compile exec:java -Dexec.mainClass=com.evotesys.ui.VotingFrame` (o `com.evotesys.Main` con `-Dexec.args="<voterId> <electionId> <candidateId>"`).

## 6. Convenciones a mantener en el código

- Nomenclatura de tablas y columnas: `MAYÚSCULAS_CON_GUION_BAJO`, tal como en el diccionario de datos (no renombrar).
- Nombres de constraints ya definidos en el diccionario (`XXX_PK`, `XXX_FK`) deben respetarse tal cual para trazabilidad con el documento formal.
- Los actores/roles del sistema se mantienen en inglés (política corporativa ya establecida): `Customer`, `Sales Representative`, `Election Organizer`, `Voter`, `Voting System`/`Management System`.
- Cualquier cambio al modelo de datos debe reflejarse primero en el JSON de modelos y en el documento Word (`Entregable_Parcial_II.docx`, secciones 10.2/10.3), y solo después en el código, para mantener ambos artefactos sincronizados.

## 7. Estado actual

- ✅ Modelo conceptual, lógico y físico validados (Entregable Parcial II).
- ✅ Reglas de negocio RN01–RN07 documentadas.
- ✅ Diccionario de datos y restricciones físicas corregidos (llaves primaria/compuesta en `VOTE` y `VOTER_ELECTION_STATUS`).
- ✅ Escribí y desplegué los scripts DDL de esquema (`sql/ddl/01_create_tables.sql`, `02_constraints.sql`, `03_sequences_indexes.sql`) en Oracle XE (esquema `EVOTESYS`).
- ✅ Implementé los paquetes y procedimientos PL/SQL (`sql/plsql/`): `PKG_SALES_CONTRACTING`, `PKG_ELECTION_MGMT`, `PKG_VOTING`, y el trigger `TRG_VOTE_IMMUTABLE` (RN03).
- ✅ Configuré roles, privilegios y auditoría (`sql/security/`, `sql/audit/`): roles `EVOTESYS_ADMIN`/`EVOTESYS_OPERATOR`/`EVOTESYS_AUDITOR`, ninguno con `UPDATE`/`DELETE` sobre `VOTE`; tabla `AUDIT_LOG` + `PKG_AUDIT`.
- ✅ Escribí el script de carga de datos ficticios (`sql/data/01_load_test_data.sql`) y verifiqué RN01/RN02/RN03 ejecutando el flujo completo contra Oracle (ver detalle en el [CHANGELOG](CHANGELOG.md)).
- ✅ Backend Java (`src/main/java/com/evotesys/`, proyecto Maven): conexión Singleton, POJOs, `VoteDAO`/`VoteDAOImpl` sobre `SP_REGISTER_VOTE`, y la boleta simulada `ui/VotingFrame.java`, verificados contra Oracle real.
