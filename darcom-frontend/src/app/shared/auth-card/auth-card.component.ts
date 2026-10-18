import { Component, input } from '@angular/core';
import { DotMapComponent } from './dot-map.component';

/**
 * Auth shell ported from `Connect Signin.txt` (21st.dev "Travel Connect"):
 * full-screen gradient background, split card with the animated dot map on the
 * left and the projected form content on the right.
 */
@Component({
  selector: 'app-auth-card',
  standalone: true,
  imports: [DotMapComponent],
  template: `
    <div
      class="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 p-4"
    >
      <div
        class="auth-card w-full max-w-4xl md:min-h-[600px] overflow-hidden rounded-2xl flex bg-white shadow-xl"
      >
        <!-- Left side: dot map -->
        <div class="hidden md:block w-1/2 relative overflow-hidden border-r border-gray-100">
          <div class="absolute inset-0 bg-gradient-to-br from-blue-50 to-indigo-100">
            <app-dot-map />

            <!-- Brand overlay -->
            <div class="absolute inset-0 flex flex-col items-center justify-center p-8 z-10">
              <div class="auth-fade-down auth-delay-1 mb-6">
                <div
                  class="h-12 w-12 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-200"
                >
                  <svg
                    class="text-white h-6 w-6"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                  >
                    <path d="M5 12h14" />
                    <path d="m12 5 7 7-7 7" />
                  </svg>
                </div>
              </div>
              <h2
                class="auth-fade-down auth-delay-2 text-3xl font-bold mb-2 text-center text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-indigo-600"
              >
                DarCom
              </h2>
              <p class="auth-fade-down auth-delay-3 text-sm text-center text-gray-600 max-w-xs">
                {{ tagline() }}
              </p>
            </div>
          </div>
        </div>

        <!-- Right side: form -->
        <div class="w-full md:w-1/2 p-8 md:p-10 flex flex-col justify-center bg-white">
          <div class="auth-fade-up">
            <ng-content />
          </div>
        </div>
      </div>
    </div>
  `,
  styles: `
    .auth-card {
      animation: auth-card-in 0.5s ease-out backwards;
    }
    .auth-fade-up {
      animation: auth-fade-up 0.5s ease-out backwards;
    }
    .auth-fade-down {
      animation: auth-fade-down 0.5s ease-out backwards;
    }
    .auth-delay-1 {
      animation-delay: 0.6s;
    }
    .auth-delay-2 {
      animation-delay: 0.7s;
    }
    .auth-delay-3 {
      animation-delay: 0.8s;
    }

    @keyframes auth-card-in {
      from {
        opacity: 0;
        transform: scale(0.95);
      }
      to {
        opacity: 1;
        transform: scale(1);
      }
    }
    @keyframes auth-fade-up {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: none;
      }
    }
    @keyframes auth-fade-down {
      from {
        opacity: 0;
        transform: translateY(-20px);
      }
      to {
        opacity: 1;
        transform: none;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .auth-card,
      .auth-fade-up,
      .auth-fade-down {
        animation: none;
      }
    }
  `,
})
export class AuthCardComponent {
  readonly tagline = input.required<string>();
}
