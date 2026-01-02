import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage.tsx';
import HomePage from './pages/HomePage';
import DevicesPage from './pages/DevicesPage';
import EntryLogPage from './pages/EntryLogPage';
import RfidPage from './pages/RfidPage';
import DoorPage from './pages/DoorPage';
import LcdPage from './pages/LcdPage';
import AdminPage from './pages/AdminPage';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <HomePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/devices"
            element={
              <ProtectedRoute>
                <DevicesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/entry-logs"
            element={
              <ProtectedRoute>
                <EntryLogPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/rfid"
            element={
              <ProtectedRoute>
                <RfidPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/doors"
            element={
              <ProtectedRoute>
                <DoorPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/lcds"
            element={
              <ProtectedRoute>
                <LcdPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute>
                <AdminPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 3000,
            style: {
              background: '#363636',
              color: '#fff',
            },
            success: {
              duration: 3000,
              iconTheme: {
                primary: '#10b981',
                secondary: '#fff',
              },
            },
            error: {
              duration: 4000,
              iconTheme: {
                primary: '#ef4444',
                secondary: '#fff',
              },
            },
          }}
        />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
