import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { MantineProvider } from '@mantine/core';
import App from './App';

describe('App', () => {
  it('제목 "여행 교통비 계산기"를 보여준다', () => {
    render(
      <MantineProvider>
        <MemoryRouter>
          <App />
        </MemoryRouter>
      </MantineProvider>,
    );
    expect(screen.getByText('여행 교통비 계산기')).toBeInTheDocument();
  });
});
