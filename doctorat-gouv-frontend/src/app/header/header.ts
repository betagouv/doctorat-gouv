import { Component } from '@angular/core';
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
export class Header {

	showBanner = true;

	constructor() { }

	onSearchForHeader(event: Event) {
		event.preventDefault();
	}

	closeBanner() {
		this.showBanner = false;
	}
}
