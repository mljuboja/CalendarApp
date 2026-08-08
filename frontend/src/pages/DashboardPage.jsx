import { useEffect, useState } from 'react';
import apiClient from '../api/apiClient';

function DashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    async function loadDashboard() {
      try {
        const response = await apiClient.get('/api/dashboard');
        setDashboard(response.data);
      } catch (error) {
        setErrorMessage(error.response?.data?.message || 'Something went wrong');
      } finally {
        setIsLoading(false);
      }
    }

    loadDashboard();
  }, []);

  if (isLoading) {
    return (
      <div>
        <h2>Dashboard</h2>
        <p>Loading...</p>
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div>
        <h2>Dashboard</h2>
        <p className="form-error">{errorMessage}</p>
      </div>
    );
  }

  return (
    <div>
      <h2>Dashboard</h2>

      <section className="dashboard-section">
        <h3>Today's Events</h3>
        {dashboard.todaysEvents.length === 0 ? (
          <p>No events today</p>
        ) : (
          <ul className="dashboard-list">
            {dashboard.todaysEvents.map((event) => (
              <li key={`${event.id}-${event.startTime}`}>
                <strong>{event.title}</strong> — {event.startTime} to {event.endTime}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="dashboard-section">
        <h3>Upcoming Tasks</h3>
        {dashboard.upcomingTasks.length === 0 ? (
          <p>No upcoming tasks</p>
        ) : (
          <ul className="dashboard-list">
            {dashboard.upcomingTasks.map((task) => (
              <li key={task.id}>
                <strong>{task.title}</strong> — due {task.dueDate}, {task.priority} priority, {task.status}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="dashboard-section">
        <h3>Completed Tasks</h3>
        <p>{dashboard.completedTaskCount}</p>
      </section>

      <section className="dashboard-section">
        <h3>Scheduled Hours Today</h3>
        <p>{dashboard.scheduledHoursToday}</p>
      </section>
    </div>
  );
}

export default DashboardPage;
