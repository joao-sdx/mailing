# Stratégie Outreach Postmaster — Synthèse

> Conversation du 18 mai 2026
> Contexte : SynapseDX / Postmaster — automatisation du courrier entrant pour entreprises régulées

---

## 1. Objectif

- **4 RDV qualifiés / mois** minimum (objectif initial)
- Cible : décideurs métier (COO, Directeur Conformité, Dir. Opérations) puis DSI/CTO en validation
- Marché : entreprises régulées avec flux courrier entrant massif
- Approche **signal-first** : actualités → entreprises → contacts → outreach

---

## 2. Stack outils retenu — Pipeline custom

### Briques principales

| Brique                      | Rôle                                          | Outil                                       | 
|-----------------------------|-----------------------------------------------|---------------------------------------------|
| Signal actualité (entrée)   | Identifier entreprises avec actualité récente | **DataForSEO**                              |
| Recherche contacts          | Trouver le décideur dans l'entreprise         | **Apollo API**                              |
| Enrichissement société      | Forme juridique, actionnariat (FR)            | **Pappers API**                             |
| Génération personnalisation | Accroche email contextualisée                 | **Claude API**                              |
| CRM                         | Stockage contacts + tracking                  | **EspoCRM**                                 |
| Envoi séquences             | 3 emails espacés                              | **Lemlist**                                 |
| Orchestration               | Pipeline global                               | **Apache Camel / Quarkus**                  |

### Pourquoi DataForSEO plutôt que SerpAPI

- ~25× moins cher (~$0.002/requête vs abonnement $75/mois)
- Pay-as-you-go adapté à 240-900 queries/mois
- Pas de risque DMCA (Google a poursuivi SerpAPI en déc. 2025)

### Pourquoi pipeline custom plutôt que Clay ou n8n

- Stack Quarkus/Camel déjà en place
- Contrôle RGPD total (données ne sortent pas de l'infra)
- ~5 routes HTTP simples, ~200 lignes de Java
- Tourne sur k3s existant

---

## 3. Marché adressable

### Périmètre géographique final

- **Maghreb** : Maroc
- **Ibérique** : Portugal
- **Europe francophone** : France, Belgique, Luxembourg, Suisse romande, **Monaco**

### Secteurs ciblés (8 en Phase test)

1. Banques
2. Assurances
3. Mutuelles
4. Caisses retraite / institutions prévoyance
5. Hôpitaux / Cliniques privées
6. Énergie
7. Télécom
8. E-commerce

### Volume Tier 2/3 (50-500 employés)

| Secteur            | Maroc   | PT      | FR        | BE      | LU     | CH      | MC      | **Total**  |
|--------------------|---------|---------|-----------|---------|--------|---------|---------|------------|
| Banques            | 40      | 30      | 130       | 20      | 50     | 80      | 25      | 375        |
| Assurances         | 25      | 20      | 70        | 15      | 15     | 30      | 10      | 185        |
| Mutuelles          | 10      | 15      | 200       | 20      | 5      | 20      | 3       | 273        |
| Caisses retraite   | 5       | 10      | 120       | 15      | 10     | 40      | –       | 200        |
| Hôpitaux/Cliniques | 20      | 30      | 300       | 30      | 5      | 30      | 5       | 420        |
| Énergie            | 5       | 15      | 60        | 15      | 5      | 15      | –       | 115        |
| Télécom            | 5       | 8       | 20        | 5       | 3      | 5       | –       | 46         |
| E-commerce         | 10      | 15      | 150       | 20      | 5      | 15      | –       | 215        |
| **Total**          | **120** | **143** | **1 050** | **140** | **98** | **235** | **~83** | **~1 869** |

```
1 869 entités × 2 personas = ~3 740 contacts adressables
À 100 contacts/mois = ~37 mois de pipeline
```

---

## 4. Phase test multi-secteurs (mois 1-3)

### Répartition mensuelle (100 contacts/mois)

**Par secteur :**

| Secteur            | Contacts/mois | %   |
|--------------------|---------------|-----|
| Banques            | 15            | 15% |
| Assurances         | 15            | 15% |
| Mutuelles          | 15            | 15% |
| Hôpitaux/Cliniques | 15            | 15% |
| Caisses retraite   | 10            | 10% |
| Énergie            | 10            | 10% |
| Télécom            | 10            | 10% |
| E-commerce         | 10            | 10% |

**Par pays :**

| Pays       | Contacts/mois | Logique                            |
|------------|---------------|------------------------------------|
| France     | 38            | Volume, DORA, langue maternelle    |
| Maroc      | 19            | Réseau personnel, GITEX Africa     |
| Belgique   | 14            | DORA + NIS2, décideurs accessibles |
| Portugal   | 10            | Marché domicile, légitimité        |
| Suisse     | 8             | Marché premium, test               |
| Luxembourg | 8             | Densité, CSSF                      |
| Monaco     | 3             | Premium, effet réseau              |

### Validation à la fin du mois 3

Métriques à analyser par croisement secteur × pays :

- Taux d'ouverture
- Taux de réponse
- Taux de RDV
- Qualité des conversations

---

## 5. Stratégie de ciblage — Métier d'abord

### Pourquoi le métier plutôt que l'IT

| Critère        | IT (DSI/CTO)                  | Métier (COO, Dir. Ops) |
|----------------|-------------------------------|------------------------|
| Cycle de vente | Long (RFP, sécurité)          | Court (ROI visible)    |
| Réflexe        | "On peut le faire en interne" | "Je veux la solution"  |
| Décide seul    | Rarement                      | Souvent                |
| Sensible à     | Spec technique                | Workflow concret       |

**Postmaster = processus métier**, pas une fonction IT. Le métier est payeur et bénéficiaire.

### Personas prioritaires par secteur

| Secteur          | Persona métier                                     |
|------------------|----------------------------------------------------|
| Banques          | Directeur Opérations / COO / Dir. Back-Office      |
| Assurances       | Directeur Sinistres / Dir. Conformité              |
| Mutuelles        | Directeur Adhérents / Dir. Gestion                 |
| Caisses retraite | Directeur Opérations / Dir. Relations Allocataires |
| Hôpitaux         | Directeur Administratif / DRH                      |
| Énergie          | Directeur Service Client / Dir. Conformité         |

### Approche pince à 2 mains

1. Premier contact → Direction Métier
2. Démo orientée workflow + ROI
3. Phase technique → DSI/CTO consulté pour validation
4. Métier signe, IT valide

---

## 6. Configuration anti-spam (CRITIQUE)

### État actuel synapsedx.com

- ❌ **DMARC manquant** — à corriger immédiatement
- À vérifier : SPF, DKIM via mail-tester.com

### Record DMARC à ajouter

| Champ    | Valeur                                             |
|----------|----------------------------------------------------|
| Type     | TXT                                                |
| Nom/Host | `_dmarc`                                           |
| Valeur   | `v=DMARC1; p=none; rua=mailto:dmarc@synapsedx.com` |
| TTL      | 3600                                               |

### Évolution recommandée

1. `p=none` (monitoring 2-4 semaines)
2. `p=quarantine`
3. `p=reject` (objectif final)

### Autres bonnes pratiques

- Sous-domaine dédié pour outreach (ex: `outreach.synapsedx.com`)
- Warm-up progressif sur 4-6 semaines (Lemwarm, Mailreach)
- Max 50-100 emails/jour par domaine
- Vérification emails avant envoi (ZeroBounce)

---

## 7. Queries DataForSEO Phase test

### Paramètres techniques

```json
{
  "language_code": "fr",
  "search_type": "news",
  "date_range": "past_month",
  "depth": 20
}
```

Location codes : 2250 FR, 2620 PT, 2056 BE, 2442 LU, 2756 CH, 2504 MA, 2492 MC

### Queries par secteur (extrait)

**Banques :**

- "banque transformation digitale 2026"
- "banque DORA conformité 2026"
- "banque automatisation back-office 2026"

**Mutuelles :**

- "mutuelle dématérialisation adhérents 2026"
- "mutuelle santé transformation digitale 2026"
- "mutuelle RGPD données santé 2026"

**Caisses retraite :**

- "caisse retraite dématérialisation 2026"
- "institution prévoyance digital 2026"

**Hôpitaux :**

- "hôpital clinique dématérialisation administrative 2026"
- "groupe clinique privée transformation digitale 2026"

### Volume queries

```
8 secteurs × 3 queries × 7 pays = 168 queries/mois
Coût : ~$0.34/mois
```

---

## 8. Pipeline de qualification

```
DataForSEO News (date < 30j)
        ↓
Claude scoring JSON
  → is_target_company ?
  → signal_strength >= 2 ?
  → decision_maker_mentioned ?
        ↓
Apollo company search (si pas de nom)
        ↓
Pappers (FR uniquement) — forme juridique
        ↓
DataForSEO "[nom] interview"
        ↓
Claude génère accroche personnalisée
        ↓
EspoCRM → Lemlist
```

### Output Claude attendu

```json
{
  "is_target_company": true,
  "company_name": "...",
  "sector": "mutuelle|banque|...",
  "country": "FR",
  "signal_type": "...",
  "signal_strength": 1-3,
  "signal_summary": "phrase d'accroche 1 ligne",
  "decision_maker_mentioned": "...",
  "source_url": "..."
}
```

---

## 9. Budget mensuel

| Poste                              | Coût/mois      |
|------------------------------------|----------------|
| Apollo Basic                       | $49            |
| DataForSEO (pay-as-you-go)         | ~$2            |
| Pappers API (FR)                   | ~€19           |
| Claude API (~100 contacts)         | ~$15           |
| Lemlist                            | $39            |
| PhantomBuster (optionnel LinkedIn) | $56            |
| **Total sans LinkedIn**            | **~$125/mois** |
| **Total avec LinkedIn**            | **~$180/mois** |

### Conversion attendue

- Taux email → RDV : **5%** avec séquence personnalisée
- 100 contacts → **5 RDV/mois** (au-dessus de l'objectif initial)
- Temps de révision : ~5h/mois avec pipeline automatisé

---

## 10. CRM — EspoCRM

### Pourquoi EspoCRM

- GPL3 open source, maintenu activement
- API REST standard (`GET/POST/PUT/DELETE /api/v1/{Entity}`) — zéro cérémonie
- Auth par API key header ou OAuth2
- Custom fields natifs via Admin UI
- Tourne sur k3s existant (Docker image officielle)

### Custom fields EspoCRM à créer

```
cf_signal_type        (picklist : DORA, RGPD, transformation, autre)
cf_signal_strength    (integer 1-3)
cf_signal_summary     (text long)
cf_signal_url         (URL)
cf_signal_date        (date)
cf_sector             (picklist)
cf_tier               (picklist : T1, T2, T3)
cf_country            (picklist)
cf_apollo_id          (text, pour déduplication)
```

---

## 11. Prochaines étapes (par priorité)

### Immédiat (cette semaine)

1. ✅ **Ajouter le record DMARC** sur synapsedx.com
2. Vérifier SPF/DKIM via mail-tester.com
3. Créer sous-domaine `outreach.synapsedx.com`
4. ✅ **CRM : EspoCRM retenu**

### Court terme (2 semaines)

5. Créer compte Apollo (free tier pour test)
6. Créer compte DataForSEO
7. Rédiger les **8 templates email** par secteur (français)
8. Écrire le **prompt Claude de qualification** (output JSON)

### Mise en place pipeline (3-4 semaines)

9. Routes Camel : DataForSEO → Claude scoring → Apollo
10. Intégration YetiForce/EspoCRM API
11. Configuration warm-up domaine (Lemwarm 4 semaines)
12. Tests bout-en-bout avec 10 contacts pilotes

### Phase test (mois 1-3)

13. Lancer 100 contacts/mois sur 8 secteurs × 7 pays
14. Analyser métriques croisées secteur × pays
15. Identifier 2-3 segments gagnants pour Phase 2

---

## 12. Décisions ouvertes à trancher

- [x] ~~YetiForce ou EspoCRM ?~~ → **EspoCRM retenu** (API REST plus simple, GPL3 actif)
- [ ] LinkedIn (PhantomBuster) en Phase test ou plus tard ?
- [ ] Premier secteur prioritaire si budget temps personnalisation limité ?
- [ ] Quelle adresse `From:` pour les emails (Joao perso vs nom générique SynapseDX) ?
- [ ] Faut-il un template ES/IT dès Phase test, ou Phase 3 uniquement ?

---

## Annexe — Benchmarks de conversion

### Cold call B2B C-levels

- Décroché : 5-10%
- RDV obtenu : 1-3%
- → Pas optimal pour profil startup niche

### Cold email B2B C-levels (séquence personnalisée)

- Ouverture : 40-60%
- Réponse : 8-15%
- **RDV : 3-7% (cible 5%)**

### Séquence email + LinkedIn + call de suivi

- RDV : 5-8% (meilleur ROI)
