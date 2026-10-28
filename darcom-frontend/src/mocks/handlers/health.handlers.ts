import { HttpResponse, http } from 'msw';
import { apiPath } from './paths';

export const healthHandlers = [
  http.get(apiPath('/health'), () => HttpResponse.json({ status: 'UP' })),
];
