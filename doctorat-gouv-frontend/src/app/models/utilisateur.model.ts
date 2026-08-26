/** Modèle utilisateur retourné par le backend (sans le mot de passe). */
export interface Utilisateur {
  id: string;
  email: string;
  prenom: string;
  nom: string;
  role: string;
  sourceAuth: string;
  actif: boolean;
  dateCreation: string;
}

/** Réponse du backend après connexion ou inscription : token JWT + infos utilisateur. */
export interface ConnexionResponse {
  utilisateur: Utilisateur;
  token: string;
  expiresIn: number;
}

/** Données envoyées au backend pour l'inscription d'un nouvel utilisateur. */
export interface InscriptionRequest {
  email: string;
  motDePasse: string;
  prenom: string;
  nom: string;
  role: string;
}

/** Données envoyées au backend pour la connexion (email + mot de passe). */
export interface ConnexionRequest {
  email: string;
  motDePasse: string;
}
