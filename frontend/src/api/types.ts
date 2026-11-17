// 에러 응답 모양. 나머지 타입은 각 화면 작업 때 docs/contracts.md를 보고 추가한다.
export interface ApiError {
  status: number;
  code: string;
  message: string;
  field: string | null;
}
