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

export interface ConnexionResponse {
  utilisateur: Utilisateur;
  token: string;
  expiresIn: number;
}

export interface InscriptionRequest {
  email: string;
  motDePasse: string;
  prenom: string;
  nom: string;
  role: string;
}

export interface ConnexionRequest {
  email: string;
  motDePasse: string;
}
