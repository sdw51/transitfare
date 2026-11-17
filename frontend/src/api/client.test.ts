import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiGet } from './client';

describe('apiGet', () => {
  afterEach(() => vi.restoreAllMocks());

  it('실패 응답이면 status·code·message·field가 담긴 ApiRequestError를 던진다', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          status: 400,
          code: 'ITINERARY_OUT_OF_PERIOD',
          message: '여행 기간 밖의 날짜입니다',
          field: 'travelDate',
        }),
        { status: 400 },
      ),
    );

    await expect(apiGet('/trips/1')).rejects.toMatchObject({
      status: 400,
      code: 'ITINERARY_OUT_OF_PERIOD',
      message: '여행 기간 밖의 날짜입니다',
      field: 'travelDate',
    });
  });
});
