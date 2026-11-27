/** Page renvoyée par les modules 3, 4, 6 et 8 du backend. */
export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
