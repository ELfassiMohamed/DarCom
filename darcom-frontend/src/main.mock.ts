import { bootstrap } from './bootstrap';
import { startMockApi } from './mocks/browser';

startMockApi()
  .then(() => bootstrap())
  .catch((err) => console.error(err));
