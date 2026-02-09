import { Component, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header implements AfterViewInit {

  showBanner = true;

  constructor() { }

  ngAfterViewInit(): void {
    const burger = document.querySelector(".mobile-burger") as HTMLElement;
    const mobileMenu = document.querySelector(".mobile-menu") as HTMLElement;
    const closeBtn = document.querySelector(".mobile-menu-close") as HTMLElement;

    if (!burger || !mobileMenu) return;

    // Ouvrir le menu
    burger.addEventListener("click", () => {
      mobileMenu.classList.add("open");
    });

    // Fermer via bouton X
    if (closeBtn) {
      closeBtn.addEventListener("click", () => {
        mobileMenu.classList.remove("open");
      });
    }

    // Fermer en cliquant en dehors
    document.addEventListener("click", (e: Event) => {
      if (!mobileMenu.classList.contains("open")) return;

      const target = e.target as HTMLElement;
      const clickedInsideMenu = mobileMenu.contains(target);
      const clickedBurger = burger.contains(target);

      if (!clickedInsideMenu && !clickedBurger) {
        mobileMenu.classList.remove("open");
      }
    });
  }

  onSearchForHeader(event: Event) {
    event.preventDefault();
  }

  closeBanner() {
    this.showBanner = false;
  }
}
