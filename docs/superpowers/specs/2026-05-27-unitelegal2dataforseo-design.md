# unitelegal2dataforseo — Design

**Date** : 2026-05-27
**Status** : Approved (brainstorming)

## Objectif

Nouveau module Maven `unitelegal2dataforseo` qui transforme un CSV INSEE Sirene (Stock Unités Légales) en fichier `dataforseo-queries.yml` consommable par le module `seo-news-search`.

Pour chaque ligne CSV, le module dérive un ou plusieurs *keywords* à partir des colonnes de dénomination, applique des défauts communs (langue, profondeur, code de localisation, préfixe de fichier) lus depuis un YAML de config, et émet une entrée YAML par keyword.

## Contexte

Le module `seo-news-search` consomme un YAML statique listant des recherches Google News (via DataForSEO). Aujourd'hui ces queries sont écrites à la main (`banque transformation digitale 2026`, `assurance dématérialisation conformité 2026`). Pour cibler des entreprises spécifiques (ex: enrichissement INSEE sortant des étapes `01-siren`/`02-siren-line` du module `pipeline`), il faut générer ces queries automatiquement depuis les dénominations légales officielles.

Les fichiers INSEE (`sources/insee/echantillon.csv`, et plus tard `StockUniteLegale_utf8.csv` ≈ 3 GB) exposent 5 colonnes de dénomination dont certaines sont vides ou redondantes. Le module doit déduire un keyword pertinent et déduplicaté par entité légale.

## Architecture

Module Maven dans le multi-module racine, au même niveau que `mailing-pipeline`, `seo-news-search`, `seo-news-parse`, `pipeline`.

- **Package racine** : `com.synapsedx.mailing.unitelegal2dataforseo`
- **Sous-packages** : `batch/` (reader/processor/writer + JobConfig), `config/` (`@ConfigurationProperties`), `model/` (records)
- **Stack** : Java 21, Spring Boot 3.4.5, Spring Batch, Jackson YAML, H2 in-memory (JobRepository)
- **Build** : `pom.xml` hérité de la racine ; `Taskfile.yml` avec tâche `default` qui lance le job via `mvnw spring-boot:run`

### Job Spring Batch

Un seul job `unitelegal2dataforseo` avec un step unique `convertStep` chunk-oriented :

```
InseeCsvReader  →  KeywordDedupProcessor  →  DataForSeoYamlWriter
   (FlatFile)        (dédup intelligent)        (Jackson YAML)
```

- **Chunk size** : 100 (le CSV cible peut dépasser plusieurs millions de lignes)
- **Reader** : `FlatFileItemReader` standard Spring Batch avec `LineMapper` qui extrait `siren` + les 5 colonnes de dénomination
- **Processor** : applique l'algo de dédup → renvoie `KeywordBatch` (ou `null` pour skip si aucune valeur exploitable)
- **Writer** : `ItemStreamWriter` custom — `open()` ouvre le fichier de sortie et écrit l'en-tête `queries:` une fois ; chaque appel `write(chunk)` aplatit la chunk en `List<DataForSeoQuery>` (1 par keyword), applique les défauts, et append les items au YAML ; `close()` ferme le flux. Pas de chargement intégral en mémoire.

## Modèle de données

```java
// CSV row — seules les 6 colonnes utiles
record InseeUniteLegale(
    String siren,
    String sigle,
    String denomination,
    String denominationUsuelle1,
    String denominationUsuelle2,
    String denominationUsuelle3
) {}

// Sortie processor
record KeywordBatch(String siren, List<String> keywords) {}

// Defaults YAML (@ConfigurationProperties)
record QueryDefaults(
    String languageCode,
    int depth,
    int locationCode,
    String locationName,
    String filePrefix
) {}

// Entrée YAML générée (1 par keyword)
record DataForSeoQuery(
    String keyword,
    String languageCode,
    int depth,
    int locationCode,
    String locationName,
    String filePrefix
) {}
```

## Algorithme de dédup

Entrée : les 5 valeurs des colonnes `sigleUniteLegale`, `denominationUniteLegale`, `denominationUsuelle1UniteLegale`, `denominationUsuelle2UniteLegale`, `denominationUsuelle3UniteLegale`.

```
1. nonBlank = valeurs.filtre(v != null && !v.trim().isBlank()).map(trim)
2. Tri descendant par longueur de chaîne
3. kept = []
4. Pour chaque v dans nonBlank:
     normalizedV = v.toUpperCase(Locale.ROOT)
     déjàInclus = kept.any { k -> k.toUpperCase().contains(normalizedV) }
     si !déjàInclus:
       kept.add(v)        // valeur originale, casse préservée
5. Retourner kept
```

**Pourquoi tri descendant par longueur** : garantit que la dénomination la plus longue passe en premier. Le sigle court (ex: `SET`) est alors rejeté car déjà substring de la dénomination retenue (ex: `SET - HUILLIER - SOCIETE D'ENTREPOSAGE ET DE TRANSPORTS`).

**Pourquoi cette stratégie** : DataForSEO est un SERP Google News. Chaque entrée YAML = 1 appel API facturé. Une dédup substring-aware maximise la qualité (chaque query est un nom propre distinct que Google reconnaît) tout en évitant les doublons coûteux. Concaténer toutes les valeurs donnerait un keyword interprété comme `AND` par Google → 0 résultat. Prendre la première non vide raterait les sigles connus en tant que marques.

**Exemples** :
- `sigle="SET"`, `denomination="SET - HUILLIER..."` → 1 query : `"SET - HUILLIER..."` (SET dédupliqué)
- `denomination="ETS J VIRLY S A"` seul → 1 query : `"ETS J VIRLY S A"`
- `sigle="EDF"`, `denomination="ELECTRICITE DE FRANCE"` → 2 queries (pas de substring match — deux marques distinctes)

## Configuration

### `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:batchdb
  batch:
    jdbc:
      initialize-schema: always
    job:
      enabled: false   # déclenché explicitement via JobLauncher

unitelegal2dataforseo:
  input-csv: ../sources/insee/echantillon.csv
  output-yml: output/dataforseo-queries.yml

query-defaults:
  language-code: fr
  depth: 2
  location-code: 2250
  location-name: France
  file-prefix: assurance-fr
```

### Surcharge CLI

Convention Spring Batch du projet (cf. `mailing-pipeline`) :

```sh
./mvnw spring-boot:run -pl unitelegal2dataforseo \
  -Dspring-boot.run.arguments="\
    --unitelegal2dataforseo.input-csv=/path/to/big.csv \
    --unitelegal2dataforseo.output-yml=output/big-queries.yml"
```

### Encodage / délimiteur CSV

- Délimiteur : virgule (vérifié sur `echantillon.csv`)
- Encodage : UTF-8
- Quoting : standard (Spring Batch `DelimitedLineTokenizer` par défaut suffit pour ce CSV)

### Format YAML produit

Jackson `YAMLFactory` avec `WRITE_DOC_START_MARKER` désactivé pour matcher le format existant de `seo-news-search/src/main/resources/dataforseo-queries.yml` :

```yaml
queries:
  - keyword: "ETS J VIRLY S A"
    language_code: fr
    depth: 2
    location_code: 2250
    location_name: France
    file_prefix: assurance-fr
  - keyword: "PENNEQUIN INVEST"
    ...
```

## Tests

Convention JUnit 5 + Spring Boot Test (cohérent avec les autres modules).

| Test class | Couverture |
|------------|------------|
| `KeywordDedupProcessorTest` | Cas unitaires : sigle ∈ denomination, sigle ≠ denomination, toutes colonnes vides → `null`, casse mixte, espaces parasites |
| `InseeCsvReaderTest` | Parse `echantillon.csv` (resource test), vérifie mapping des 6 colonnes retenues |
| `DataForSeoYamlWriterTest` | Vérifie format YAML produit (pas de `---` initial, indent 2 espaces, ordre des champs identique aux YAML manuels existants) |
| `Unitelegal2DataforseoJobTest` | Test bout-en-bout avec petit CSV → compare YAML produit à un fichier `expected.yml` |

## Hors-scope

- Ne consomme pas le YAML généré — c'est le job de `seo-news-search`
- Ne filtre pas les entreprises (état administratif, code NAF, etc.) — la sélection en amont relève d'un autre module
- Ne déduplique pas entre lignes CSV (deux entreprises avec la même dénomination produiront deux queries identiques — comportement assumé)
- Pas de gestion de variation orthographique (accents, casse) au-delà de la dédup substring case-insensitive

## Dépendances

- `spring-boot-starter-batch`
- `spring-boot-starter-test`
- `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`
- `com.h2database:h2`
- Lombok (héritage du parent)