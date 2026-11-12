import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-activation',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  templateUrl: './activation.html',
  styleUrl: './activation.scss',
})
export class Activation implements OnInit {
  isLoading = true;
  isSuccess = false;
  isError = false;
  errorMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParams['token'];
    if (!token) {
      this.isLoading = false;
      this.isError = true;
      this.errorMessage = 'ACTIVATION.ERROR_MISSING_TOKEN';
      return;
    }

    this.authService.activerCompte(token).subscribe({
      next: () => {
        this.isLoading = false;
        this.isSuccess = true;
        // Redirection après 2 secondes
        setTimeout(() => {
          const user = this.authService.currentUser();
          if (user?.role === 'DIRECTEUR_THESE') {
            this.router.navigate(['/tableau-de-bord-directeur-these']);
          } else {
            this.router.navigate(['/tableau-de-bord-candidat']);
          }
        }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.isError = true;
        if (err.status === 400) {
          this.errorMessage = 'ACTIVATION.ERROR_INVALID_OR_EXPIRED';
        } else {
          this.errorMessage = 'ACTIVATION.ERROR_SERVICE_UNAVAILABLE';
        }
      }
    });
  }
}