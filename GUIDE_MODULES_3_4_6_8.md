# Guide d'intégration : modules 3, 4, 6 et 8 (développeur B)

Document pour les développeurs backend (modules 5 et 7, modules 1 et 2) et frontend, et pour les agents IA qui les assistent.
Il décrit ce qui **existe réellement dans le code** (routes, corps de requête, permissions, statuts, règles métier), vérifié par
146 tests unitaires et 111 requêtes Postman (244 vérifications) sans échec. En cas de doute entre ce document et le PDF de spécification (non versionné)
`DTO_gestion_budgetaire.pdf`, la section 10 liste les écarts connus.

Complément : `backend/docs/INTEGRATION_MODULES_5_7.md` (services Java à appeler par les modules 5 et 7).

## 1. Qui possède quoi

| Module | Contenu | Package | Propriétaire |
|---|---|---|---|
| 1 | Sécurité, utilisateurs, rôles, entités, structures, audit | `security` | Développeur A |
| 2 | Budget : plans, objectifs, allocations, révisions, journal | `budget` | Développeur A |
| 3 | Stocks : articles, demandes magasin, mouvements | `stock` | Développeur B |
| 4 | Achats : fournisseurs, demandes d'achat (DA), bons de commande (BC) | `achat` | Développeur B |
| 6 | Abonnements et échéanciers | `abonnement` | Développeur B |
| 8 | Moteur de workflow générique | `workflow` | Développeur B |
| 5, 7 | Réceptions, comptabilité | à créer | à venir |

Les modules de B s'appuient sur ceux de A en lecture seule : ils utilisent leurs entités (`Utilisateur`, `Structure`, `Entite`,
`BudgetAllocationStructure`) sans les modifier. Pour changer une route ou une entité d'un module, voir son propriétaire.

## 2. Démarrer en local

1. PostgreSQL (testé en 18), base `gestion_budget`, paramètres dans `backend/src/main/resources/application.yml`.
2. Tables des modules 1 et 2 d'abord, puis les scripts de `backend/sql/`, dans cet ordre :
   `module2_colonnes_calculees.sql`, `module3_stock.sql`, `module4_achats.sql`, `module6_abonnements.sql`, `module8_workflow.sql`.
3. **Base déjà créée avant la correction des quantités décimales** : appliquer aussi `module4_quantites_decimales.sql`
   (`quantite` et `quantite_commandee` passent en `NUMERIC(15,3)`). Sans lui l'application refuse de démarrer
   (`ddl-auto: validate`). Une base neuve n'en a pas besoin.
4. `cd backend && ./mvnw spring-boot:run` (port 8080). Au démarrage sont créés : l'utilisateur `admin@uab.bf` / `Password123!`,
   les permissions des modules 3, 4, 6, 8 et le workflow `WORKFLOW_DA`. Sur une base vierge, redémarrer une fois pour que
   l'administrateur reçoive toutes les permissions.
5. Tests : `./mvnw test` (unitaires) et la collection `backend/postman/` (voir `backend/postman/README.md`, 111 requêtes). La collection
   écrit des données de test en base : utiliser une base de développement.

## 3. Conventions communes

**URL.** Toutes les routes sont sous `/api/<ressource>`. Le PDF prévoit `/api/v1/...` : ce préfixe n'existe encore dans aucun
module (voir section 10).

**Authentification.** `POST /api/auth/login` avec `{"email": "...", "password": "..."}` renvoie
`{token, type:"Bearer", id, email, nom, prenom, role, permissions:[...]}`. Envoyer ensuite `Authorization: Bearer <token>`.
Le jeton dure 24 h. La liste `permissions` sert au frontend à afficher ou masquer les actions.

**Permissions.** Format `ressource.action` (ex. `article.creer`). Une route sans la permission requise répond **403 sans corps**.
Les permissions de chaque route sont dans les tableaux ci-dessous.

**Pagination.** Les listes des modules 3, 4, 6, 8 acceptent `page` (défaut 0), `size` (défaut 20, plafonné à 200) et
`sort=champ,asc|desc`. Réponse :
```json
{"content": [...], "number": 0, "size": 20, "totalElements": 134, "totalPages": 7, "first": true, "last": false}
```
Un `sort` sur un champ inexistant renvoie 400 avec un message lisible.

**Erreurs.**
| Cas | Code | Corps |
|---|---|---|
| Règle métier violée | 400 | `{"message": "..."}` |
| Validation du corps | 400 | `{"message": "Données invalides", "erreurs": {"champ": "message"}}` |
| Ressource introuvable par `GET /{id}` | 404 | vide |
| Pas de jeton, permission manquante, **ou route inexistante** | 403 | vide |

Attention : une URL mal écrite répond 403, pas 404. En cas de 403 inattendu, vérifier d'abord l'URL.

**Types.** Identifiants en UUID. Dates `AAAA-MM-JJ`, horodatages ISO. Montants et quantités en `BigDecimal` (nombres JSON, pas de
chaînes). TVA de 18 % par défaut (`achat.tva-taux`). Statuts et types sont des chaînes en MAJUSCULES, acceptées sans tenir compte
de la casse en entrée.

**Utilisateur connecté.** Jamais lu dans le corps : le serveur prend l'utilisateur du jeton (demandeur, valideur, magasinier,
auteur d'un mouvement...). Les réponses exposent les personnes sous la forme
`{id, matricule, nom, prenom, email, roleMetier}`.

**Numéros de documents** (générés si absents) : `BS-AAAA-NNNN` (demande magasin), `DA-...`, `BC-...`, `FOURN-...`, `ABN-...`.

**Traçabilité.** Les créations, modifications et validations sont écrites dans `audit_logs` (route `GET /api/audit-logs` du module 1).

## 4. Module 3 : stocks

Permissions : `article.{lire,creer,modifier}`, `demande_magasin.{lire,creer,valider,servir}`, `mouvement_stock.{lire,creer}`.

| Méthode | Route | Permission | Paramètres / corps |
|---|---|---|---|
| GET | `/api/articles` | `article.lire` | filtres `codeArticle`, `designation`, `sousSeuil` (stock ≤ seuil) |
| GET | `/api/articles/{id}` | `article.lire` | |
| POST | `/api/articles` | `article.creer` | `codeArticle`*, `designation`*, `uniteMesure`*, `description`, `seuilReapprovisionnement`, `prixUnitaireMoyenPondere`, `actif` |
| PUT | `/api/articles/{id}` | `article.modifier` | mêmes champs |
| GET | `/api/demandes-magasin` | `demande_magasin.lire` | filtres `statut`, `demandeurId`, `entiteCode` |
| GET | `/api/demandes-magasin/{id}` | `demande_magasin.lire` | |
| POST | `/api/demandes-magasin` | `demande_magasin.creer` | `serviceId`*, `entiteCode`*, `objet`*, `lignes`* `[{articleId, quantiteDemande>0}]`, `numeroDemande` (optionnel) |
| PUT | `/api/demandes-magasin/{id}/valider-n1` | `demande_magasin.valider` | `{decision: "APPROUVE"\|"REJETE", commentaire}` |
| PUT | `/api/demandes-magasin/{id}/servir` | `demande_magasin.servir` | `{lignes: [{ligneId, quantiteServie>=0}]}` |
| GET | `/api/mouvements-stock` | `mouvement_stock.lire` | filtres `articleId`, `typeMouvement`, `dateDebut`, `dateFin` |
| POST | `/api/mouvements-stock` | `mouvement_stock.creer` | `articleId`*, `typeMouvement`* (`ENTREE`\|`SORTIE`\|`AJUSTEMENT`), `quantite`*, `referenceDocument`*, `prixUnitaire` (ENTREE seulement) |

(*) obligatoire.

**Règles métier.**
- `quantiteStock` d'un article ne change que par les mouvements (démarre à 0). Le PUMP (prix moyen pondéré) est recalculé à
  chaque `ENTREE` avec `prixUnitaire`. Une sortie supérieure au stock est refusée (jamais de stock négatif).
- `AJUSTEMENT` : `quantite` est le **stock physique compté** (≥ 0) ; le mouvement enregistre l'écart absolu, le sens se lit avec
  `stockAnterieur` et `nouveauStock`.
- Demande magasin : `SOUMIS` → `VALIDE_N1` → `PARTIELLEMENT_SERVIE` → `SERVIE`, ou `SOUMIS` → `REJETEE`. Le service peut se faire en
  plusieurs fois (les quantités se cumulent, jamais au-delà du reste à servir) ; chaque service crée une sortie de stock.
- **Le demandeur ne peut ni valider ni servir sa propre demande.** Si le demandeur a un responsable (`manager` dans le module 1),
  **seul ce responsable** peut valider ; sinon n'importe quel autre utilisateur ayant la permission.
- Un article inactif ne peut pas être demandé.

## 5. Module 4 : achats

Permissions : `fournisseur.{lire,creer,modifier}`, `demande_achat.{lire,creer,modifier,viser,controler_budget,approuver_dg}`,
`bon_commande.{lire,creer,emettre}`.

| Méthode | Route | Permission | Paramètres / corps |
|---|---|---|---|
| GET | `/api/fournisseurs` | `fournisseur.lire` | filtres `raisonSociale`, `ifu`, `actif`, `query` (recherche libre) |
| GET | `/api/fournisseurs/{id}` | `fournisseur.lire` | |
| POST / PUT | `/api/fournisseurs[/{id}]` | `fournisseur.creer` / `.modifier` | `raisonSociale`*, `ifu`* (unique), `codeFournisseur` (généré), `email`, `telephone`, `adresse`, `actif` |
| GET | `/api/demandes-achat` | `demande_achat.lire` | filtres `codeDossier`, `statut`, `demandeurId`, `exerciceAnnee` |
| GET | `/api/demandes-achat/{id}` | `demande_achat.lire` | |
| POST | `/api/demandes-achat` | `demande_achat.creer` | voir ci-dessous |
| PUT | `/api/demandes-achat/{id}` | `demande_achat.modifier` | mêmes champs ; seul le demandeur, avant la 1re validation |
| POST | `/api/demandes-achat/{id}/approuver` | `viser` ou `controler_budget` ou `approuver_dg` selon l'étape | `{action: "APPROVE"\|"REJECT", commentaire}` |
| GET | `/api/bons-commande` | `bon_commande.lire` | filtres `codeBc`, `fournisseurId`, `statut`, `daId` |
| GET | `/api/bons-commande/{id}` | `bon_commande.lire` | |
| POST | `/api/bons-commande` | `bon_commande.creer` | voir ci-dessous |
| PUT | `/api/bons-commande/{id}/emettre` | `bon_commande.emettre` | aucun corps |
| GET | `/api/bons-commande/{id}/pdf` | `bon_commande.lire` | fichier PDF |

**Corps d'une DA** : `entiteEmettrice`*, `entiteBeneficiaire`*, `typeAchat`* (`STOCKABLE`, `PRESTATION_SERVICE`, `INVESTISSEMENT_IMMO`,
`TRAVAUX`), `objet`*, `justification`*, `directionDemandeurId`* (structure), `exerciceAnnee`*, `codeDossier` (généré),
`lignes`* `[{activiteId*, articleId, description*, uniteMesure*, quantite* (décimal >0), prixUnitaireEstime* (>0)}]`,
`ventilations` `[{entiteImpactee, activiteImpacteeId, montantImpute, pourcentageApplique}]`.
`activiteId` est l'identifiant d'une ligne budgétaire `budget_allocation_structure` (module 2), de l'exercice indiqué.

**Corps d'un BC** : `typeBc`* (`CLASSIQUE` ou `CADRE_ANNUEL`), `daId`*, `fournisseurId`*,
`lignes`* `[{ligneDaId*, articleId, description*, quantiteCommandee (décimal), prixUnitaireHt, montantTotalHt}]`.
Une ligne a soit `quantiteCommandee` **et** `prixUnitaireHt` (le montant est recalculé), soit seulement `montantTotalHt` (forfait).

**Règles métier.**
- À la création, la DA engage les crédits sur les lignes budgétaires (refus si le budget est insuffisant ou si la ligne appartient
  à un autre exercice). Un rejet ou une annulation libère les crédits.
- Statuts DA : `SOUMIS` → `EN_COURS_VALIDATION` → `APPROUVE` → `COMMANDE`, ou `REJETE`. Circuit `WORKFLOW_DA` (section 8) :
  validation N+1, contrôle budgétaire, puis validation DG si le montant dépasse 50 000.
- **Le demandeur ne peut pas valider sa propre DA.** Une DA n'est plus modifiable après la 1re validation.
- Un BC se crée depuis une DA `APPROUVE`, une seule fois par DA. Son montant ne peut pas dépasser l'estimation de la DA ;
  s'il est inférieur, la différence est libérée du budget. La DA passe à `COMMANDE`.
- Statuts BC : `EMIS` → `ENVOYE_FOURNISSEUR` (ou `CADRE_EN_COURS` pour un contrat cadre) → `PARTIELLEMENT_FACTURE` → `SOLDE`, ou `ANNULE`.
  L'imputation des factures est faite par le module 7 (`BonCommandeService.imputerFacture`).

## 6. Module 6 : abonnements

Permissions : `abonnement.{lire,creer,modifier,generer_echeances}`, `echeance.modifier`.

| Méthode | Route | Permission | Paramètres / corps |
|---|---|---|---|
| GET | `/api/abonnements` | `abonnement.lire` | filtres `fournisseurId`, `statut`, `responsableId` |
| GET | `/api/abonnements/{id}` | `abonnement.lire` | |
| GET | `/api/abonnements/alertes` | `abonnement.lire` | `jours` (défaut 10) : échéances proches et préavis atteints |
| POST / PUT | `/api/abonnements[/{id}]` | `abonnement.creer` / `.modifier` | voir ci-dessous |
| GET | `/api/abonnements/{id}/echeances` | `abonnement.lire` | filtres `statut`, `dateDebut`, `dateFin` |
| POST | `/api/abonnements/{id}/generer-echeances` | `abonnement.generer_echeances` | aucun corps ; idempotent |
| POST | `/api/abonnements/{id}/reconduire` | `abonnement.modifier` | prolonge la fin d'un an |
| GET | `/api/echeances/{id}` | `abonnement.lire` | |
| PUT | `/api/echeances/{id}` | `echeance.modifier` | `{statut*, referenceFacture, bcId, montantFactureTtc}` |

**Corps d'un abonnement** : `libelle`*, `fournisseurId`*, `activiteId`* (ligne budgétaire), `periodicite`* (`MENSUEL`, `TRIMESTRIEL`,
`SEMESTRIEL`, `ANNUEL`), `jourFacturation`* (1 à 31, 31 = dernier jour du mois), `montantEcheanceHt`*, `montantEcheanceTtc`* (>0),
`dateDebut`*, `dateFin`*, `responsableId`*, `taciteReconduction`, `preavisJours`, `statut` (`ACTIF` par défaut ou `BROUILLON`),
`codeAbonnement` (généré).

**Règles métier.**
- Statuts : `BROUILLON` → `ACTIF` ↔ `SUSPENDU` → `RESILIE` ; `ACTIF` → `EXPIRE`. `RESILIE` et `EXPIRE` ne sont plus modifiables.
  Résilier annule toutes les échéances non traitées. Une tâche planifiée (chaque nuit à 2 h) reconduit ou expire les contrats arrivés à terme.
- Un abonnement `ACTIF` génère son échéancier (une échéance par période). Changer le montant met à jour les échéances `A_VENIR` ;
  changer le rythme est refusé dès qu'une échéance est traitée.
- Échéance : `A_VENIR` → `BC_GENERE` (exige `bcId`) → `FACTURE_RECUE` (exige `referenceFacture` **et `montantFactureTtc`**) → `PAYE`.
  `ANNULE` possible depuis `A_VENIR` ou `BC_GENERE`. Aucun saut d'étape. La réponse contient `ecartTtc` (facturé moins attendu).

## 7. Module 8 : moteur de workflow

Permissions : `workflow.lire`, `workflow.creer`. Les deux routes d'instance exigent seulement un utilisateur connecté ; la permission
de l'étape courante est vérifiée par le moteur.

| Méthode | Route | Permission | Paramètres / corps |
|---|---|---|---|
| GET | `/api/workflows/definitions` | `workflow.lire` | filtres `code`, `actif` |
| GET | `/api/workflows/definitions/{id}` | `workflow.lire` | |
| POST | `/api/workflows/definitions` | `workflow.creer` | `code`*, `libelle`*, `actif`, `etapes`* `[{numEtape*, codeEtape*, libelle*, permissionRequise*, montantMinimum, montantMaximum, etapeSuivanteSuccesCode, etapeSuivanteRejetCode}]` |
| GET | `/api/workflows/instances/{dossierId}` | connecté | suivi d'un dossier |
| POST | `/api/workflows/instances/execute` | connecté | `{dossierId*, action* ("APPROVE"\|"REJECT"), commentaire}` |

Les renvois entre étapes se décrivent par `etapeSuivanteSuccesCode` / `etapeSuivanteRejetCode` (code d'étape) ou par identifiant.
Les plages de montants ont des bornes incluses ; une plage incohérente, un code en double ou un renvoi vers une étape inexistante
sont refusés. Statuts d'une instance : `EN_ATTENTE_VALIDATION`, `APPROUVE`, `REJETE`, `ANNULE`.

**Workflow `WORKFLOW_DA`** (créé au démarrage) : 1. `VALIDATION_N1` (`demande_achat.viser`) → 2. `CONTROLE_BUDGETAIRE`
(`demande_achat.controler_budget`) → 3. `VALIDATION_DG` (`demande_achat.approuver_dg`, seulement au-dessus de 50 000).

**Brancher un nouveau dossier (PV, BAP...)** : voir `backend/docs/INTEGRATION_MODULES_5_7.md` section 3 (`WorkflowEngineApi`,
`DossierMontantProvider`, `WorkflowTransitionEvent`).

## 8. Formes des réponses (champs renvoyés)

Les listes sont enveloppées dans la page de la section 3 ; les autres routes renvoient l'objet seul. `Personne` =
`{id, matricule, nom, prenom, email, roleMetier}`. Tous les objets ont `createdAt` (et souvent `updatedAt`).

| Ressource | Champs |
|---|---|
| Article | `id, codeArticle, designation, description, uniteMesure, quantiteStock, seuilReapprovisionnement, prixUnitaireMoyenPondere, actif, createdAt, updatedAt` |
| Demande magasin | `id, numeroDemande, demandeur (Personne), serviceId, entiteCode, objet, statut, valideurN1 (Personne), dateValidation, magasinier (Personne), dateServie, lignes, createdAt, updatedAt` |
| Ligne de demande magasin | `id, article (Article complet), quantiteDemande, quantiteServie, createdAt` |
| Mouvement de stock | `id, articleId, typeMouvement, quantite, stockAnterieur, nouveauStock, referenceDocument, auteur (Personne), createdAt` |
| Fournisseur | `id, codeFournisseur, raisonSociale, ifu, email, telephone, adresse, actif, createdAt, updatedAt` |
| Demande d'achat | `id, codeDossier, entiteEmettrice, entiteBeneficiaire, typeAchat, objet, justification, montantEstimeHt, montantEstimeTtc, statut, etapeCourante, demandeur (Personne), directionDemandeurId, exerciceAnnee, lignes, ventilations, historique, createdAt, updatedAt` |
| Ligne de DA | `id, activiteId, articleId, description, uniteMesure, quantite, prixUnitaireEstime, montantTotalHt, createdAt` |
| Ventilation | `id, daId, entiteImpactee, activiteImpacteeId, montantImpute, pourcentageApplique, statutBlocage` |
| Historique de validation | `id, daId, etape, valideur (Personne), decision, commentaire, dateDecision` |
| Bon de commande | `id, codeBc, typeBc, daId, codeDossier, fournisseur (Fournisseur), montantTotalHt, montantTotalTtc, montantFactureTtc, soldeRestantTtc, statut, dateEmission, lignes, createdAt, updatedAt` |
| Ligne de BC | `id, bcId, ligneDaId, articleId, description, quantiteCommandee, prixUnitaireHt, montantTotalHt, createdAt` |
| Abonnement | `id, codeAbonnement, libelle, fournisseur (Fournisseur), activiteId, periodicite, jourFacturation, montantEcheanceHt, montantEcheanceTtc, dateDebut, dateFin, taciteReconduction, preavisJours, dateLimitePreavis, statut, responsable (Personne), createdAt, updatedAt` |
| Échéance | `id, abonnementId, codeAbonnement, numEcheance, dateEcheance, montantAttenduTtc, montantFactureTtc, ecartTtc, statut, bcId, referenceFacture, createdAt, updatedAt` |
| Alertes d'abonnement | `{echeancesProches: [Échéance], preavisAtteints: [{abonnementId, codeAbonnement, libelle, dateFin, dateLimitePreavis, joursAvantFin, taciteReconduction, responsableId}]}` |
| Définition de workflow | `id, code, libelle, actif, etapes, createdAt` |
| Étape de workflow | `id, numEtape, codeEtape, libelle, permissionRequise, montantMinimum, montantMaximum, etapeSuivanteSuccesId, etapeSuivanteRejetId` |
| Instance de workflow | `id, workflowId, workflowCode, dossierId, etapeCourante (Étape, null une fois terminé), statutActuel, createdAt, updatedAt` |

## 9. Routes des modules 1 et 2 utilisées par B (noms réels du code)

Utilisées par la collection Postman pour préparer une ligne budgétaire : `POST /api/plans-strategiques`, `/api/exercices-budgetaires`,
`/api/objectifs-strategiques`, `/api/objectifs-operationnels`, `/api/activites-catalogue`, `/api/allocations-os`,
`/api/allocations-op`, `/api/allocations-activites`, `/api/allocations-structures`, ainsi que `/api/utilisateurs`, `/api/roles`,
`/api/structures`, `/api/entites`, `/api/permissions`, `/api/audit-logs`. Voir la section 10 pour leurs différences avec le PDF.

## 10. Écarts connus et points d'attention

**À signaler au développeur A (modules 1 et 2, constats faits depuis les modules de B) :**
1. **Fuite de données** : `GET /api/utilisateurs` renvoie `motDePasseHash` (les contrôleurs renvoient les entités JPA, pas des DTO).
   À ne jamais afficher ni stocker côté frontend. `POST /api/utilisateurs` reçoit le mot de passe dans l'URL (`?motDePasse=`).
2. **Préfixe `/api/v1`** demandé par le PDF : absent partout.
3. **Pagination** : les modules 1 et 2 renvoient de simples tableaux ; les modules 3, 4, 6, 8 renvoient des pages (section 3).
   Le frontend doit gérer les deux formats tant que ce n'est pas aligné.
4. **Noms de routes différents du PDF** : `/allocations-structures` (PDF : `/allocations-structure`), `/allocations-activites`
   (PDF : `/allocations-activite`), `/repartition-csp` (PDF : `/csp-rules`), `/statistiques-cles` (PDF : `/entites-statistiques`),
   `/revisions-budgetaires` (PDF : `/demandes-revision`).
5. **Routes du PDF absentes** : `GET /structures/tree`, `PUT /exercices-budgetaires/{id}/statut`, import et export Excel des allocations.
6. **Filtres différents** : utilisateurs (`roleId`, `recherche` au lieu de `roleMetier`, `query`), audit (`startDate`, `endDate`
   au lieu de `dateDebut`, `dateFin`), statistiques (`annee` au lieu de `anneeExercice`).

**Écarts volontaires des modules de B avec le PDF :**
- `PUT /api/echeances/{id}` exige `montantFactureTtc` pour `FACTURE_RECUE` (absent du PDF) afin de calculer l'écart facturé/attendu.
- `typeBc` est obligatoire à la création d'un BC ; `codeFournisseur`, `numeroDemande`, `codeAbonnement` sont générés s'ils sont absents.
- Quantités des lignes de DA et de BC décimales (PDF : `BigDecimal`) ; périodicité `SEMESTRIEL` en plus.
- Routes en plus : `GET /{id}` sur plusieurs ressources, `/abonnements/alertes`, `/abonnements/{id}/reconduire`, `POST /mouvements-stock`.

**Autres points :**
- `BudgetReservationProvisoire` (engagement budgétaire de la DA) est provisoire : A fournira le sien, annoté `@Primary`, sans changer les appelants.
- Un `GET /{id}` inconnu renvoie 404 sans corps ; les autres erreurs ont un `message` (section 3).
- `@CrossOrigin(origins = "*")` est présent sur les contrôleurs : à restreindre avant la production.
- Les modules 1 et 2 n'ont pas de tests unitaires.

## 11. Consignes pour les agents IA et nouveaux contributeurs

- Lire ce document et `backend/docs/INTEGRATION_MODULES_5_7.md` avant de coder. Ne pas deviner une route : elle est dans les tableaux ci-dessus.
- Chaque développeur modifie ses propres modules ; pour un changement dans le module d'un autre (routes, entités, DTO), voir d'abord son propriétaire.
- Respecter les conventions : package `com.budget.backend.<module>` (`controller`, `dto`, `entity`, `repository`, `service`),
  DTO `...RequestDto` / `...ResponseDto`, permissions `ressource.action`, exception métier propre au module avec un
  `@RestControllerAdvice` qui renvoie 400 `{"message": ...}` (sans lui, les erreurs sortent en 403 sans corps).
- Toute nouvelle table passe par un script `backend/sql/moduleN_*.sql` (Hibernate ne crée rien) ; toute modification de colonne
  doit aussi avoir son script de migration pour les bases existantes.
- Ne jamais lire l'auteur d'une action dans le corps : utiliser `CurrentUserService.utilisateurConnecte()`.
- Verrou pessimiste sur tout ce qui cumule des montants ou des quantités ; ajouter des tests unitaires et des requêtes Postman
  (cas nominal **et** cas d'erreur 400) pour chaque route.
- Avant de livrer : `./mvnw test` puis la collection Postman complète doivent passer sans échec.
