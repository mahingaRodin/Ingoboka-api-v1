import { stressOptions } from './lib/config.js';
import { criticalPaths } from './scenarios/critical-paths.js';

export const options = stressOptions;

export default function () {
  criticalPaths();
}
