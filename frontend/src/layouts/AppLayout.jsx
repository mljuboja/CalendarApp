import { Link, Outlet, useNavigate } from 'react-router-dom';

// Basic layout for the authenticated part of the app: a header with
// navigation links, and an Outlet where the routed page renders.
function AppLayout() {
  const navigate = useNavigate();

  function handleLogout() {
    localStorage.removeItem('authToken');
    navigate('/login');
  }

  return (
    <div className="app-layout">
      <header className="app-header">
        <h1 className="app-title">Daymark</h1>
        <nav className="app-nav">
          <Link to="/">Dashboard</Link>
          <Link to="/calendar">Calendar</Link>
          <Link to="/tasks">Tasks</Link>
        </nav>
        <button type="button" className="logout-button" onClick={handleLogout}>
          Log Out
        </button>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}

export default AppLayout;
