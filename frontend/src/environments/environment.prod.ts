// Configuration utilisée pour la version en ligne (ng build, configuration "production").
// L'API est servie sur le même domaine que les pages (Nginx redirige /api vers le backend).
export const environment = {
  production: true,
  apiUrl: '/api'
};
