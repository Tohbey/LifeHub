/** Shapes shared by every LifeHub API (lifehub-starter). */

/** The response of every paginated list endpoint. Pages are zero based. */
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

/** An RFC 9457 error body, as every LifeHub service returns it. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Field name to message, for validation errors. */
  errors?: Record<string, string>;
}

export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER';

export const ROLE_RANK: Record<WorkspaceRole, number> = { MEMBER: 1, ADMIN: 2, OWNER: 3 };
