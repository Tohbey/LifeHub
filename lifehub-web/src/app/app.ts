import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { UpdateBanner } from './shared/update-banner';
import { Toasts } from './shared/toasts';

/** The root: the routed page, toast messages and the "new version" banner. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toasts, UpdateBanner],
  template: `<router-outlet /><lh-toasts /><lh-update-banner />`,
})
export class App {}
