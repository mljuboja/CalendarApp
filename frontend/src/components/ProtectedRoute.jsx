import { Navigate, Outlet } from 'react-router-dom';

// Simple route guard: only checks whether a token exists in localStorage.
// It does not verify the token is valid/unexpired - that check happens on
// the backend for every real request.
function ProtectedRoute() {
  const token = localStorage.getItem('authToken');

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

export default ProtectedRoute;
