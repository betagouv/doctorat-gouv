import { Directive, Input, Output, EventEmitter, OnChanges, OnDestroy, ElementRef, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Directive qui charge une photo de profil via HttpClient (porteur du JWT)
 * et l'affiche en blob: URL. Remplace [src] direct sur les images de photos,
 * car un tag <img> ne passe pas par l'intercepteur d'authentification.
 *
 * Cache-busting : un paramètre ?t= est ajouté pour éviter que le navigateur
 * mette en cache l'ancienne photo après un remplacement (URL déterministe).
 *
 * Usage : <img [appPhotoSrc]="url" (appPhotoSrcError)="showFallback = true" />
 */
@Directive({
  selector: 'img[appPhotoSrc]',
  standalone: true,
})
export class PhotoSrcDirective implements OnChanges, OnDestroy {

  @Input() appPhotoSrc: string | null = null;
  @Output() appPhotoSrcError = new EventEmitter<void>();

  private readonly img = inject(ElementRef<HTMLImageElement>);
  private readonly http = inject(HttpClient);
  private sub: Subscription | null = null;
  private objectUrl: string | null = null;

  ngOnChanges(): void {
    this.sub?.unsubscribe();
    this.revoke();

    if (!this.appPhotoSrc) {
      return;
    }

    // Les data URIs (avatar par défaut) sont affichés directement
    if (!this.appPhotoSrc.startsWith('/api/')) {
      this.img.nativeElement.src = this.appPhotoSrc;
      return;
    }

    // L'URL du backend est relative (/api/...) → on la préfixe avec apiUrl
    // pour atteindre le backend directement (pas de proxy en dev).
    const base = environment.apiUrl.replace(/\/api\/?$/, '');
    const url = `${base}${this.appPhotoSrc}?t=${Date.now()}`;

    this.sub = this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.objectUrl = URL.createObjectURL(blob);
        this.img.nativeElement.src = this.objectUrl;
      },
      error: () => {
        this.appPhotoSrcError.emit();
      },
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.revoke();
  }

  private revoke(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
