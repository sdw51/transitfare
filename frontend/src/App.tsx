import { Routes, Route } from 'react-router-dom';
import { AppShell, Title } from '@mantine/core';
import TripListPage from './pages/TripListPage';
import TripPage from './pages/TripPage';
import PrintPage from './pages/PrintPage';
import AdminPage from './pages/admin/AdminPage';

export default function App() {
  return (
    <AppShell header={{ height: 56 }} padding="md">
      <AppShell.Header px="md" style={{ display: 'flex', alignItems: 'center' }}>
        <Title order={3}>여행 교통비 계산기</Title>
      </AppShell.Header>
      <AppShell.Main>
        <Routes>
          <Route path="/" element={<TripListPage />} />
          <Route path="/trips/:id" element={<TripPage />} />
          <Route path="/trips/:id/print" element={<PrintPage />} />
          <Route path="/admin" element={<AdminPage />} />
        </Routes>
      </AppShell.Main>
    </AppShell>
  );
}
