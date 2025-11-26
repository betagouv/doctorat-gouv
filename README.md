# doctorat-gouv

Plateforme de gestion des candidatures doctorales, développée dans le cadre de [beta.gouv.fr](https://beta.gouv.fr).  
Ce projet est un **mono-repo** comprenant un backend en **Spring Boot 4 (Java 21)** et un frontend en **Angular 20**.

---

## 🚀 Technologies

- **Backend** : Spring Boot 4, Spring Data JPA, Spring Security, PostgreSQL (prod), H2 (tests)
- **Frontend** : Angular 20, TypeScript
- **Build & CI/CD** : Maven, GitHub Actions
- **Langage** : Java 21, TypeScript

---

## 📂 Structure du projet

doctorat-gouv/ 

    ├── doctorat-gouv-backend/ # API REST Spring Boot
 
    ├── doctorat-gouv-frontend/ # Application Angular 

    └── pom.xml # Parent Maven (modules + BOM Spring Boot)


---

## ⚙️ Installation & Build

### Backend
```bash
cd doctorat-gouv-backend
mvn clean install
mvn spring-boot:run
```

### Frontend
```bash
cd doctorat-gouv-frontend
npm install
ng serve
```
---
## 🧪 Tests

### Backend
```bash
cd doctorat-gouv-backend
mvn test
```

### Frontend
```bash
cd doctorat-gouv-frontend
ng test
```
---
## 📜 Licence
Ce projet est distribué sous la licence Apache 2.0. Vous pouvez consulter le texte complet de la licence dans le fichier LICENSE.

---
