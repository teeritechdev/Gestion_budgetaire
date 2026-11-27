/** Message lisible d'une erreur HTTP du backend (`{message}`), sinon un texte générique. */
export function errorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'error' in error) {
    const body = (error as { error?: { message?: string } }).error;
    if (body?.message) return body.message;
  }
  return 'Opération impossible. Vérifiez vos droits et les données saisies.';
}
