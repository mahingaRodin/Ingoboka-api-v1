import { loadOptions } from './lib/config.js';
import { criticalPaths } from './scenarios/critical-paths.js';

export const options = loadOptions;

export default function () {
  criticalPaths();
}
