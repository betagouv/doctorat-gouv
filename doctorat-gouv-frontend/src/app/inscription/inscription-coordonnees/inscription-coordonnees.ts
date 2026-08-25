import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-inscription-coordonnees',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main id="main-content" class="inscription-page">
      <div class="inscription-layout fr-grid-row">
        <div class="fr-col-12 fr-col-lg-6 login-column">
          <div class="login-card">
            <p class="fr-badge fr-badge--info fr-mb-2w">Étape 1 sur 2</p>
            <h2 class="fr-text--bold fr-text--lg">Coordonnées</h2>
            <p class="fr-text--sm">Placeholder étape 1 (Inc 1).</p>
          </div>
        </div>
      </div>
    </main>
  `,
  styles: [`:host { display: block; }`]
})
export class InscriptionCoordonnees {}
