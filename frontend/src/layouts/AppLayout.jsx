import { Link, Outlet } from 'react-router-dom';

// Basic layout for the authenticated part of the app: a header with
// navigation links, and an Outlet where the routed page renders.
function AppLayout() {
  return (
    <div className="app-layout">
      <header className="app-header">
        <h1 className="app-title">Daymark</h1>
        <nav className="app-nav">
          <Link to="/">Dashboard</Link>
          <Link to="/calendar">Calendar</Link>
          <Link to="/tasks">Tasks</Link>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}

export default AppLayout;
