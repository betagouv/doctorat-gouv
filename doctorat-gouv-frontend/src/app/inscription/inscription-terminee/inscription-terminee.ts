import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-inscription-terminee',
  standalone: true,
  imports: [CommonModule],
  template: `
    <main id="main-content" class="inscription-page">
      <div class="inscription-layout fr-grid-row">
        <div class="fr-col-12 fr-col-lg-6 login-column">
          <div class="login-card">
            <h2 class="fr-text--bold fr-text--lg">Inscription terminée</h2>
            <p class="fr-text--sm">Placeholder confirmation (Inc 1).</p>
          </div>
        </div>
      </div>
    </main>
  `,
  styles: [`:host { display: block; }`]
})
export class InscriptionTerminee {}
