# Tests Postman : modules 3, 4, 6 et 8

## Fichiers
- `Gestion_Budgetaire_B.postman_collection.json` : 107 requêtes en 7 dossiers, avec 236 vérifications automatiques.
- `Gestion_Budgetaire_local.postman_environment.json` : variables `baseUrl` (par défaut `http://localhost:8080`), `adminEmail`, `adminPassword`.

## Mise en route
1. Base PostgreSQL `gestion_budget` avec les tables des modules 1 et 2 puis les scripts de `backend/sql/`
   (`module2_colonnes_calculees`, `module3_stock`, `module4_achats`, `module6_abonnements`, `module8_workflow`).
2. Démarrer l'application : `./mvnw spring-boot:run` dans `backend/` (port 8080). Au démarrage elle crée l'utilisateur
   `admin@uab.bf` / `Password123!` et le workflow `WORKFLOW_DA`. Au tout premier démarrage d'une base vierge, redémarrer
   une fois pour que l'administrateur reçoive toutes les permissions.
3. Postman : Import des deux fichiers, choisir l'environnement « Gestion budgétaire - local ».
4. Lancer toute la collection : clic droit sur la collection, Run collection, ordre par défaut. Le dossier 0 doit passer en premier.

## Ce que fait la collection
- Chaque exécution commence par réinitialiser ses variables (`suffixe`, exercice...) : tous les codes uniques sont nouveaux,
  donc on peut la relancer autant de fois que voulu sans erreur de doublon.
- Les identifiants créés (article, DA, BC, abonnement...) sont mémorisés dans des variables de collection et réutilisés
  par les requêtes suivantes : exécuter les dossiers dans l'ordre. Une requête isolée peut échouer si ses variables manquent.
- Le dossier 0 crée un second utilisateur (copie du rôle de l'administrateur) : la validation d'une demande d'achat
  exige quelqu'un d'autre que le demandeur.
- Le dossier 1 utilise des routes du Module 2 (développeur A) pour créer la ligne budgétaire que les achats et abonnements engagent.
- Les cas d'erreur sont testés aussi (400 attendu) : un test vert ne signifie pas toujours un 200.

## À savoir
- Les données restent en base (il n'y a pas de route de suppression). Pour repartir propre : recréer la base ou utiliser une base de test.
- Les dates d'abonnement suivent l'année courante ; l'exercice budgétaire utilise une année fictive (2100 et plus) pour ne jamais
  entrer en collision avec de vraies données.
- Ligne de commande (sans Postman) : `npx newman run Gestion_Budgetaire_B.postman_collection.json -e Gestion_Budgetaire_local.postman_environment.json`.
- Les listes renvoient `{ content, number, size, totalElements, totalPages, first, last }` (paramètres `page`, `size`, `sort`).
