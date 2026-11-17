import type { ApiError } from './types';

const BASE = '/api/v1';

export class ApiRequestError extends Error {
  readonly status: number;
  readonly code: string;
  readonly field: string | null;

  constructor(error: ApiError) {
    super(error.message);
    this.status = error.status;
    this.code = error.code;
    this.field = error.field;
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await fetch(BASE + path, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ApiError | null;
    throw new ApiRequestError(
      error ?? { status: response.status, code: 'UNKNOWN', message: '요청에 실패했습니다', field: null },
    );
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

export const apiGet = <T>(path: string) => request<T>('GET', path);
export const apiSend = <T>(method: 'POST' | 'PUT' | 'DELETE', path: string, body?: unknown) =>
  request<T>(method, path, body);
