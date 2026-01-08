export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number; // numéro de la page courante
  size: number;   // taille de la page
}
