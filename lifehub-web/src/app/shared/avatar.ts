import { Component, computed, input } from '@angular/core';

/** A round badge with a person's initials, coloured from their name so each person keeps the same colour. */
@Component({
  selector: 'lh-avatar',
  template: `{{ initials() }}`,
  host: {
    class: 'avatar',
    '[style.--avatar-hue]': 'hue()',
    '[style.width.px]': 'size()',
    '[style.height.px]': 'size()',
    '[style.font-size.px]': 'size() * 0.4',
    'aria-hidden': 'true',
  },
})
export class Avatar {
  /** The name to take initials and colour from. */
  readonly name = input<string | null | undefined>('');
  /** Diameter in pixels. */
  readonly size = input(32);

  protected readonly initials = computed(() => {
    const words = (this.name() || '?').trim().split(/\s+/);
    return ((words[0]?.[0] ?? '?') + (words.length > 1 ? words[words.length - 1][0] : '')).toUpperCase();
  });

  protected readonly hue = computed(() => {
    let hash = 0;
    for (const c of this.name() || '') {
      hash = (hash * 31 + c.charCodeAt(0)) % 360;
    }
    return hash;
  });
}
