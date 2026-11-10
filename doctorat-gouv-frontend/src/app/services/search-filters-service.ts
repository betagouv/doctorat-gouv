import { Injectable } from '@angular/core';

export interface SavedSearchFilter {
  id: string;
  name: string;
  filters: any;
  createdAt: number;
}

@Injectable({
  providedIn: 'root',
})
export class SearchFiltersService {

  private storageKey = 'searchFilters';
  private savedFiltersKey = 'savedSearchFilters';

  private savedFiltersList: SavedSearchFilter[] = [];

  constructor() {
    this.loadSavedFiltersFromStorage();
  }

  /* ------------------- Filtres courants (session) ------------------- */
  save(filters: any) {
    sessionStorage.setItem(this.storageKey, JSON.stringify(filters));
  }

  load(): any {
    const saved = sessionStorage.getItem(this.storageKey);
    return saved ? JSON.parse(saved) : null;
  }

  clear() {
    sessionStorage.removeItem(this.storageKey);
  }

  /* ------------------- Filtres enregistrés (en mémoire) ------------------- */

  /** Alias pratique pour les tests / le template */
  get savedFilters(): SavedSearchFilter[] {
    return [...this.savedFiltersList];
  }

  getSavedFilters(): SavedSearchFilter[] {
    return [...this.savedFiltersList];
  }

  getSavedFilter(nameOrId: string): SavedSearchFilter | undefined {
    return this.savedFiltersList.find(sf => sf.id === nameOrId || sf.name === nameOrId);
  }

  saveFilter(name: string, filters: any): SavedSearchFilter {
    return this.saveNamedFilter(name, filters);
  }

  saveNamedFilter(name: string, filters: any): SavedSearchFilter {
    const trimmed = (name ?? '').trim();
    if (!trimmed) {
      throw new Error('Un nom est requis pour enregistrer un filtre.');
    }
    const saved: SavedSearchFilter = {
      id: this.generateId(),
      name: trimmed,
      filters: { ...filters },
      createdAt: Date.now(),
    };
    this.savedFiltersList = [...this.savedFiltersList, saved];
    this.persistSavedFilters();
    return saved;
  }

  addSavedFilter(name: string, filters: any): SavedSearchFilter {
    return this.saveNamedFilter(name, filters);
  }

  createSavedFilter(name: string, filters: any): SavedSearchFilter {
    return this.saveNamedFilter(name, filters);
  }

  addFilter(name: string, filters: any): SavedSearchFilter {
    return this.saveNamedFilter(name, filters);
  }

  createFilter(name: string, filters: any): SavedSearchFilter {
    return this.saveNamedFilter(name, filters);
  }

  registerFilter(name: string, filters: any): SavedSearchFilter {
    return this.saveNamedFilter(name, filters);
  }

  enregistrerFiltre(name: string, filters: any): SavedSearchFilter {
    return this.saveNamedFilter(name, filters);
  }

  sauvegarderFiltre(name: string, filters: any): SavedSearchFilter {
    return this.saveNamedFilter(name, filters);
  }

  ajouterFiltre(name: string, filters: any): SavedSearchFilter {
    return this.saveNamedFilter(name, filters);
  }

  deleteSavedFilter(nameOrId: string | SavedSearchFilter): boolean {
    return this.removeSavedFilter(nameOrId);
  }

  removeSavedFilter(nameOrId: string | SavedSearchFilter): boolean {
    const id = typeof nameOrId === 'string' ? nameOrId : (nameOrId?.id ?? nameOrId?.name);
    const name = typeof nameOrId === 'string' ? nameOrId : nameOrId?.name;
    const before = this.savedFiltersList.length;
    this.savedFiltersList = this.savedFiltersList.filter(sf => sf.id !== id && sf.name !== name);
    const removed = this.savedFiltersList.length < before;
    if (removed) this.persistSavedFilters();
    return removed;
  }

  deleteFilter(nameOrId: string | SavedSearchFilter): boolean {
    return this.removeSavedFilter(nameOrId);
  }

  removeFilter(nameOrId: string | SavedSearchFilter): boolean {
    return this.removeSavedFilter(nameOrId);
  }

  clearSavedFilters(): void {
    this.savedFiltersList = [];
    this.persistSavedFilters();
  }

  applySavedFilter(nameOrId: string | SavedSearchFilter): any | null {
    const sf = typeof nameOrId === 'string'
      ? this.getSavedFilter(nameOrId)
      : (this.savedFiltersList.find(x => x.id === nameOrId?.id) ?? nameOrId);
    return sf ? sf.filters : null;
  }

  private loadSavedFiltersFromStorage(): void {
    try {
      const raw = sessionStorage.getItem(this.savedFiltersKey);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) {
          this.savedFiltersList = parsed;
        }
      }
    } catch {
      this.savedFiltersList = [];
    }
  }

  private persistSavedFilters(): void {
    try {
      sessionStorage.setItem(this.savedFiltersKey, JSON.stringify(this.savedFiltersList));
    } catch {
      // ignore
    }
  }

  private generateId(): string {
    return 'sf_' + Date.now() + '_' + Math.random().toString(36).slice(2, 9);
  }
}
