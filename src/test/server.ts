import { setupServer } from 'msw/node';

/** The mocked network for every test; tests add handlers with server.use(...). */
export const server = setupServer();
