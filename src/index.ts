import { registerPlugin } from '@capacitor/core';

import type { PIPPlugin } from './definitions';

const PIP = registerPlugin<PIPPlugin>('PIP', {});

export * from './definitions';
export { PIP };