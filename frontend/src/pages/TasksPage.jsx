import { useEffect, useState } from 'react';
import apiClient from '../api/apiClient';

function TasksPage() {
  const [tasks, setTasks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [status, setStatus] = useState('TODO');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createErrorMessage, setCreateErrorMessage] = useState('');
  const [statusErrorMessage, setStatusErrorMessage] = useState('');

  const [editingTaskId, setEditingTaskId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editDueDate, setEditDueDate] = useState('');
  const [editPriority, setEditPriority] = useState('MEDIUM');
  const [editStatus, setEditStatus] = useState('TODO');
  const [editErrorMessage, setEditErrorMessage] = useState('');

  useEffect(() => {
    async function loadTasks() {
      try {
        const response = await apiClient.get('/api/tasks');
        setTasks(response.data);
      } catch (error) {
        setErrorMessage(error.response?.data?.message || 'Something went wrong');
      } finally {
        setIsLoading(false);
      }
    }

    loadTasks();
  }, []);

  async function handleCreateTask(event) {
    event.preventDefault();
    setCreateErrorMessage('');
    setIsSubmitting(true);

    try {
      const response = await apiClient.post('/api/tasks', {
        title,
        description,
        dueDate: dueDate || null,
        priority,
        status,
      });

      setTasks([...tasks, response.data]);
      setTitle('');
      setDescription('');
      setDueDate('');
      setPriority('MEDIUM');
      setStatus('TODO');
    } catch (error) {
      setCreateErrorMessage(error.response?.data?.message || 'Something went wrong');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleStatusChange(taskId, newStatus) {
    setStatusErrorMessage('');

    try {
      const response = await apiClient.patch(`/api/tasks/${taskId}/status`, { status: newStatus });
      setTasks(tasks.map((task) => (task.id === taskId ? response.data : task)));
    } catch (error) {
      setStatusErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  function handleEditClick(task) {
    setEditErrorMessage('');
    setEditingTaskId(task.id);
    setEditTitle(task.title);
    setEditDescription(task.description || '');
    setEditDueDate(task.dueDate || '');
    setEditPriority(task.priority);
    setEditStatus(task.status);
  }

  function handleCancelEdit() {
    setEditingTaskId(null);
  }

  async function handleSaveEdit(taskId) {
    setEditErrorMessage('');

    try {
      const response = await apiClient.put(`/api/tasks/${taskId}`, {
        title: editTitle,
        description: editDescription,
        dueDate: editDueDate || null,
        priority: editPriority,
        status: editStatus,
      });

      setTasks(tasks.map((task) => (task.id === taskId ? response.data : task)));
      setEditingTaskId(null);
    } catch (error) {
      setEditErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  async function handleDeleteTask(taskId) {
    if (!window.confirm('Delete this task?')) {
      return;
    }

    setEditErrorMessage('');

    try {
      await apiClient.delete(`/api/tasks/${taskId}`);
      setTasks(tasks.filter((task) => task.id !== taskId));
    } catch (error) {
      setEditErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  if (isLoading) {
    return (
      <div>
        <h2>Tasks</h2>
        <p>Loading...</p>
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div>
        <h2>Tasks</h2>
        <p className="form-error">{errorMessage}</p>
      </div>
    );
  }

  return (
    <div>
      <h2>Tasks</h2>

      <form className="task-form" onSubmit={handleCreateTask}>
        <h3>Add a Task</h3>

        <label htmlFor="title">Title</label>
        <input
          id="title"
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />

        <label htmlFor="description">Description</label>
        <input
          id="description"
          type="text"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <label htmlFor="dueDate">Due Date</label>
        <input
          id="dueDate"
          type="date"
          value={dueDate}
          onChange={(e) => setDueDate(e.target.value)}
        />

        <label htmlFor="priority">Priority</label>
        <select id="priority" value={priority} onChange={(e) => setPriority(e.target.value)}>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
        </select>

        <label htmlFor="status">Status</label>
        <select id="status" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="TODO">To Do</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="COMPLETED">Completed</option>
        </select>

        {createErrorMessage && <p className="form-error">{createErrorMessage}</p>}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Adding...' : 'Add Task'}
        </button>
      </form>

      {statusErrorMessage && <p className="form-error">{statusErrorMessage}</p>}
      {editErrorMessage && <p className="form-error">{editErrorMessage}</p>}

      {tasks.length === 0 ? (
        <p>No tasks yet.</p>
      ) : (
        <ul className="task-list">
          {tasks.map((task) =>
            editingTaskId === task.id ? (
              <li key={task.id} className="task-item">
                <label htmlFor="editTitle">Title</label>
                <input
                  id="editTitle"
                  type="text"
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                />

                <label htmlFor="editDescription">Description</label>
                <input
                  id="editDescription"
                  type="text"
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                />

                <label htmlFor="editDueDate">Due Date</label>
                <input
                  id="editDueDate"
                  type="date"
                  value={editDueDate}
                  onChange={(e) => setEditDueDate(e.target.value)}
                />

                <label htmlFor="editPriority">Priority</label>
                <select
                  id="editPriority"
                  value={editPriority}
                  onChange={(e) => setEditPriority(e.target.value)}
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                </select>

                <label htmlFor="editStatus">Status</label>
                <select
                  id="editStatus"
                  value={editStatus}
                  onChange={(e) => setEditStatus(e.target.value)}
                >
                  <option value="TODO">To Do</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="COMPLETED">Completed</option>
                </select>

                <div className="task-edit-actions">
                  <button type="button" onClick={() => handleSaveEdit(task.id)}>
                    Save
                  </button>
                  <button type="button" onClick={handleCancelEdit}>
                    Cancel
                  </button>
                </div>
              </li>
            ) : (
              <li key={task.id} className="task-item">
                <div className="task-title">{task.title}</div>
                {task.description && <div className="task-description">{task.description}</div>}
                <div className="task-meta">
                  {task.dueDate && <span>Due {task.dueDate}</span>}
                  <span>{task.priority} priority</span>
                  <select
                    className="task-status-select"
                    value={task.status}
                    onChange={(e) => handleStatusChange(task.id, e.target.value)}
                  >
                    <option value="TODO">To Do</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="COMPLETED">Completed</option>
                  </select>
                  <button type="button" onClick={() => handleEditClick(task)}>
                    Edit
                  </button>
                  <button type="button" onClick={() => handleDeleteTask(task.id)}>
                    Delete
                  </button>
                </div>
              </li>
            )
          )}
        </ul>
      )}
    </div>
  );
}

export default TasksPage;
