# Intégration des modules 5 et 7 avec les modules 3, 4, 6 et 8

Ce document liste ce qui existe déjà et que les modules 5 (réceptions) et 7 (comptabilité) peuvent appeler,
sans modifier le code des modules 3, 4, 6 et 8. Tout est dans les packages `stock`, `achat`, `abonnement`, `workflow`.

## 1. Mise en place de la base locale
Appliquer dans cet ordre (après les tables des modules 1 et 2) : `backend/sql/module2_colonnes_calculees.sql`,
`module3_stock.sql`, `module4_achats.sql`, `module6_abonnements.sql`, `module8_workflow.sql`.
Chaque module écrit son propre script (tables, séquences, colonnes `GENERATED`) : faire de même pour 5 et 7
(`module5_receptions.sql`, `module7_comptabilite.sql`). `ddl-auto: validate` exige que les tables existent.

## 2. Services à appeler

| Besoin | Appel | Remarques |
|---|---|---|
| Entrée en stock à la validation d'un PV conforme (M5) | `StockMovementService.entreeStock(articleId, quantite, prixUnitaire, referenceDocument, auteur)` | Recalcule le PUMP si `prixUnitaire` est fourni (prendre `prix_unitaire_ht` de la ligne de BC). `referenceDocument` = code du PV. Transactionnel : à appeler dans la transaction du PV. |
| Liquidation à la validation du BAP (M7) | `BudgetReservationApi.liquider(allocationId, montant, referenceDocument, annee, auteurId)` | Fait passer le montant de `montant_engage` à `montant_liquide`, journalise `LIQUIDATION_FACTURE`. Refuse si le montant dépasse l'engagement. `allocationId` = `activite_id` des lignes de la DA. |
| Facturer un bon de commande (M7) | `BonCommandeService.imputerFacture(bcId, montantTtc)` | Cumule `montant_facture_ttc`, statut `PARTIELLEMENT_FACTURE` puis `SOLDE`. Refuse un BC non envoyé (`EMIS`), annulé, soldé, ou un dépassement du solde. |
| Facture issue d'un abonnement (M7) | `EcheanceAbonnementService.modifier(echeanceId, dto)` | `FACTURE_RECUE` (référence + montant facturé obligatoires) puis `PAYE`. Les échéances sont dans `echeances_abonnement`. |
| Numéros de documents | `NumeroDocumentService.prochainNumero("BL", "seq_bon_livraison")` | Donne `BL-2026-0001`. Créer la séquence dans votre script SQL (`CREATE SEQUENCE seq_...`). Préfixe en majuscules, séquence `seq_` + minuscules. |
| Utilisateur connecté | `CurrentUserService.utilisateurConnecte()` | Ne jamais lire l'auteur dans le corps de la requête. |
| Trace d'audit | `AuditHelper.tracer(auteur, INSERTION/MODIFICATION/VALIDATION, "table", id, avant, apres)` | `AuditHelper.valeurs("cle", valeur, ...)` ignore les null. Seulement dans une requête web (pas dans une tâche planifiée). |
| Résumé d'utilisateur dans les réponses | `UtilisateurSummaryDto.from(utilisateur)` | N'expose jamais le hash du mot de passe. |

## 3. Circuit de validation (PV, BAP)
Le moteur est `WorkflowEngineApi` : `demarrer(workflowCode, dossierId, montant)` à la création du dossier, puis
`prochaineEtape(workflowCode, dossierId, montant, approuve, utilisateur)` à chaque décision.
Pour ajouter un workflow :
1. Le créer au démarrage comme `WorkflowDaSeed` (ou par `POST /api/workflows/definitions`) : étapes, `permissionRequise`,
   plage de montants (bornes incluses), renvois par `etapeSuivanteSuccesCode` / `etapeSuivanteRejetCode`.
2. Enregistrer les permissions des étapes avec un initialiseur `@Order(1)` (voir `AchatPermissionInitializer`).
3. Pour que `POST /api/workflows/instances/execute` fonctionne sur vos dossiers : implémenter `DossierMontantProvider`
   (voir `DemandeAchatMontantProvider`) et écouter `WorkflowTransitionEvent` pour appliquer vos effets métier
   (voir `DemandeAchatService.surDecisionWorkflow`). Une exception levée dans l'écouteur annule la transition.

## 4. Données déjà disponibles
- `bons_commande` (`BonCommande`, `LigneBonCommande`) : statuts `EMIS`, `ENVOYE_FOURNISSEUR`, `CADRE_EN_COURS`,
  `PARTIELLEMENT_FACTURE`, `SOLDE`, `ANNULE`. Un BC est créé depuis une DA `APPROUVE` (la DA passe à `COMMANDE`).
- `fournisseurs`, `articles`, `mouvements_stock`, `echeances_abonnement`, `demandes_achat`.
- Les lignes budgétaires (`activite_id`) pointent vers `budget_allocation_structure` (module 2).

## 5. Conventions à suivre
- Package `com.budget.backend.<module>` avec `controller`, `dto`, `entity`, `repository`, `service`.
- Erreurs métier : une exception propre au module + un `@RestControllerAdvice(basePackages = "...")` qui renvoie un 400 avec
  `{"message": ...}` (voir `StockExceptionHandler`) ; sans cela les erreurs sortent en 403 sans corps.
- URL `/api/<ressource>`, DTO `...RequestDto` / `...ResponseDto`, permissions `ressource.action`, statuts en `String`.
- Listes paginées (PDF : `page` défaut 0, `size` défaut 20, `sort`) : le contrôleur reçoit un `Pageable`
  (`@PageableDefault(size = 20, sort = "createdAt", direction = DESC)`), le service appelle
  `repository.findAll(specification, PageUtils.limiter(pageable))` et renvoie
  `PageResponseDto.from(page, XxxResponseDto::from)` (package `com.budget.backend.common`, format
  `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`). La taille est plafonnée à 200.
  Ajouter dans l'`@RestControllerAdvice` du module les deux handlers de tri invalide (voir `StockExceptionHandler`) :
  sans eux, `sort=nimporte` sort en 403 sans corps.
- Verrou pessimiste (`@Lock(PESSIMISTIC_WRITE)`) sur tout ce qui cumule des montants ou des quantités ; après un
  verrou, relire l'état (`em.refresh`) si l'entité a pu être chargée plus tôt dans la même transaction.

## 6. Pas encore fait (à décider avec le développeur A)
- Les modules 3, 4, 6 et 8 sont paginés ; les modules 1 et 2 (développeur A) renvoient encore de simples tableaux :
  le frontend verra deux formats tant qu'ils ne sont pas alignés (même classe `PageResponseDto` à réutiliser).
- Préfixe `/api/v1` et fichier OpenAPI : aucun module ne les a.
- Le service d'engagement budgétaire (`BudgetReservationProvisoire`) est provisoire : A pourra fournir le sien
  (annoté `@Primary`) sans changer les appelants.
- Écarts volontaires avec le PDF (DTO) : `PUT /api/echeances/{id}` exige `montantFactureTtc` pour passer à `FACTURE_RECUE`
  (le PDF ne le prévoit pas) afin de calculer l'écart facturé/attendu ; `BonCommandeCreateDto.typeBc` est obligatoire ;
  `codeFournisseur`, `numeroDemande` et `codeAbonnement` sont générés s'ils sont absents ; les quantités des lignes de DA
  et de BC sont décimales (script `module4_quantites_decimales.sql` pour une base existante).

Voir aussi `GUIDE_MODULES_3_4_6_8.md` à la racine du dépôt : toutes les routes, permissions, statuts et règles des modules 3, 4, 6 et 8.
