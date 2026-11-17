import '@testing-library/jest-dom/vitest';

// Mantine이 쓰는 브라우저 기능을 테스트 환경(jsdom)에서 대신한다.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: () => {},
    removeEventListener: () => {},
    addListener: () => {},
    removeListener: () => {},
    dispatchEvent: () => false,
  }),
});
