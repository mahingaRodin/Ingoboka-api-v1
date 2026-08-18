import { smokeOptions } from './lib/config.js';
import { criticalPaths } from './scenarios/critical-paths.js';

export const options = smokeOptions;

export default function () {
  criticalPaths();
}
