import { registerPlugin } from '@capacitor/core';

import type { NetsEasyPlugin } from './definitions';

const NetsEasy = registerPlugin<NetsEasyPlugin>('NetsEasy', {
  web: () => import('./web').then((m) => new m.NetsEasyWeb()),
});

export * from './definitions';
export { NetsEasy };
