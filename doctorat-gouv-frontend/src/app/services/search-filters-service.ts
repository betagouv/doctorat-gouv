import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SearchFiltersService {
  
private storageKey = 'searchFilters';

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
}
