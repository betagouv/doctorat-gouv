import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Footer } from './footer/footer';
import { MatomoTrackingService } from './services/matomo-tracking-service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Footer],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('doctorat-gouv-frontend');
  
  constructor(private matomo: MatomoTrackingService) {}

}
