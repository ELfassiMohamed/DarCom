import { Component } from '@angular/core';
import { LandingComponent } from '../landing/landing.component';
import { MegaNavComponent } from '../../shared/mega-nav/mega-nav.component';

/**
 * Authenticated home: the public landing hero plus the user-space navigation.
 * The old "Welcome" profile card now lives on the profile settings page.
 */
@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MegaNavComponent, LandingComponent],
  template: `
    <app-mega-nav />
    <app-landing [showCta]="false" />
  `,
})
export class HomeComponent {}
