# MODOP — Release v0.3.7

> Date : 2026-07-06
> Branche source : `preparation-version-0.3.7`
> Tag : `v0.3.7`
> Objet : Corrections RGAA accessibilité

---

## Étapes réalisées

### 1. Merge des corrections RGAA dans `develop`

- **PR** : [#45](https://github.com/betagouv/doctorat-gouv/pull/45) — `preparation-version-0.3.7` → `develop`
- **Commits inclus** (9) :
  - `b6548e8` fix(rgaa): associer les labels aux champs du formulaire de contact (RGAA 11.1)
  - `cd8659f` fix(rgaa): relier les messages d'erreur a leur champ avec aria-invalid et aria-describedby (RGAA 11.10)
  - `8cd23ed` fix(rgaa): ajouter h1 et main sur les pages détail et contact (RGAA 9.1, 9.2, 12.7)
  - `ecb0349` fix(rgaa): corriger le lien "Aller au contenu principal"
  - `b41389e` fix(i18n): mettre à jour l'attribut lang du document au changement de langue
  - `e791cf8` fix(rgaa): ajouter des régions live ARIA pour les mises à jour dynamiques (RGAA 7.4, 7.5)
  - `e62d237` fix(rgaa): intégrer la confirmation de contact dans `<main>` avec `<h1>` et role=alert (RGAA 7.5, 9.1, 12.7)
  - `cfddce3` fix(rgaa): accessibilité de la page détail - alertes dynamiques, liens target=_blank (RGAA 7.5, 13.2)
  - `5709118` fix(rgaa): ajouter title nouvelle fenêtre sur le lien mentions légales du contact (RGAA 13.2)

### 2. Mise à jour des versions pour la release

- **Commit** : `90ed8a2` — `chore: mise à jour des versions pour la release 0.3.7`
- **Fichiers modifiés** :
  - `pom.xml` : `0.3.7-SNAPSHOT` → `0.3.7`
  - `doctorat-gouv-backend/pom.xml` : `0.3.7-SNAPSHOT` → `0.3.7`
  - `doctorat-gouv-frontend/pom.xml` : `0.3.7-SNAPSHOT` → `0.3.7`
  - `doctorat-gouv-frontend/package.json` : `0.3.7-beta` → `0.3.7`
  - `doctorat-gouv-frontend/package-lock.json` : `0.3.7-beta` → `0.3.7` (×2)
  - `doctorat-gouv-frontend/src/environments/environment.prod.ts` : `0.3.7-beta` → `0.3.7`

### 3. Merge de release dans `main`

- **PR** : [#46](https://github.com/betagouv/doctorat-gouv/pull/46) — `develop` → `main`
- Titre : *Release v0.3.7 – Corrections RGAA accessibilité*

### 4. Création du tag

```bash
git tag -a v0.3.7 -m "v0.3.7 – Corrections RGAA accessibilité (lang, live regions, confirmation contact, detail liens, nouvelle fenetre)"
git push origin v0.3.7
```

### 5. Création de la GitHub Release

- **URL** : https://github.com/betagouv/doctorat-gouv/releases/tag/v0.3.7
- **Titre** : `v0.3.7 – Corrections RGAA accessibilité`
- Contenu reprenant les 9 correctifs RGAA

### 6. Synchronisation `main` → `develop`

- **PR** : [#47](https://github.com/betagouv/doctorat-gouv/pull/47) — `main` → `develop`
- Permet de reporter les versions release dans `develop`

### 7. Création de la branche pour le cycle suivant

- **Branche** : `preparation-version-0.3.8`
- **Commit** : `abf411d` — `chore: bump version to 0.3.8-SNAPSHOT for next development cycle`
- **Fichiers modifiés** :
  - `pom.xml` × 3 : `0.3.7` → `0.3.8-SNAPSHOT`
  - `package.json` : `0.3.7` → `0.3.8-beta`
  - `package-lock.json` : `0.3.7` → `0.3.8-beta` (×2)
  - `environment.prod.ts` : `0.3.7` → `0.3.8-beta`
  - `environment.ts` : `0.3.7-beta` → `0.3.8-beta`

---

## Arbre git

```
preparation-version-0.3.7 ──► develop ──► main ──► develop
                                       │            │
                                       │            └─ tag v0.3.7
                                       │
                                       └─ preparation-version-0.3.8
```

## Notes

- Le commit `1397797` (chore: bump version to 0.3.7-SNAPSHOT) était déjà présent dans `preparation-version-0.3.7` (hérité du cycle précédent)
- 4 correctifs RGAA étaient déjà présents dans la branche avant le début du cycle (labels de formulaire, messages d'erreur, h1/main, lien d'évitement)
- 5 correctifs RGAA ont été ajoutés durant ce cycle (lang, live regions, confirmation contact, détails liens, nouvelle fenêtre)
- Le fichier `environment.ts` (dev) est resté en `0.3.7-beta` pendant la release ; il a été passé à `0.3.8-beta` uniquement pour le nouveau cycle
- La branche `preparation-version-0.3.8` est prête pour la suite des corrections RGAA
