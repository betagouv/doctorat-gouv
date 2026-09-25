import { Injectable } from '@angular/core';

/**
 * Contexte de navigation vers le détail d'une demande du DT.
 * L'identifiant transite en mémoire (+ sessionStorage pour survivre au refresh)
 * au lieu d'apparaître dans l'URL (?id=...).
 */
@Injectable({
  providedIn: 'root'
})
export class DemandeDtContextService {

  private demandeId: number | null = null;
  private storageKey = 'demandeDtContext';

  setDemandeId(id: number | null) {
    this.demandeId = id;
    if (id == null) {
      sessionStorage.removeItem(this.storageKey);
    } else {
      sessionStorage.setItem(this.storageKey, JSON.stringify({ id }));
    }
  }

  getDemandeId(): number | null {
    if (this.demandeId != null) {
      return this.demandeId;
    }
    const saved = sessionStorage.getItem(this.storageKey);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed && typeof parsed.id === 'number') {
          this.demandeId = parsed.id;
          return this.demandeId;
        }
      } catch {
        // contexte illisible, on l'ignore
      }
    }
    return null;
  }

  clear() {
    this.demandeId = null;
    sessionStorage.removeItem(this.storageKey);
  }
}
