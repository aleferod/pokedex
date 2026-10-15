/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL of pokedex-api, absolute or same-origin. Default: /api/v1 */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
