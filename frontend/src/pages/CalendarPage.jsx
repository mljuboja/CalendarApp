import { useEffect, useState } from 'react';
import apiClient from '../api/apiClient';

function CalendarPage() {
  const [events, setEvents] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const [calendars, setCalendars] = useState([]);
  const [categories, setCategories] = useState([]);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [location, setLocation] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [allDay, setAllDay] = useState(false);
  const [recurrenceType, setRecurrenceType] = useState('NONE');
  const [reminderOffsetMinutes, setReminderOffsetMinutes] = useState('');
  const [calendarId, setCalendarId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createErrorMessage, setCreateErrorMessage] = useState('');

  const [editingEventId, setEditingEventId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editLocation, setEditLocation] = useState('');
  const [editStartTime, setEditStartTime] = useState('');
  const [editEndTime, setEditEndTime] = useState('');
  const [editAllDay, setEditAllDay] = useState(false);
  const [editRecurrenceType, setEditRecurrenceType] = useState('NONE');
  const [editReminderOffsetMinutes, setEditReminderOffsetMinutes] = useState('');
  const [editCalendarId, setEditCalendarId] = useState('');
  const [editCategoryId, setEditCategoryId] = useState('');
  const [eventActionErrorMessage, setEventActionErrorMessage] = useState('');

  const [newCalendarName, setNewCalendarName] = useState('');
  const [newCalendarColor, setNewCalendarColor] = useState('#4a90e2');
  const [editingCalendarId, setEditingCalendarId] = useState(null);
  const [editCalendarNameValue, setEditCalendarNameValue] = useState('');
  const [editCalendarColorValue, setEditCalendarColorValue] = useState('#4a90e2');
  const [calendarActionErrorMessage, setCalendarActionErrorMessage] = useState('');

  const [newCategoryName, setNewCategoryName] = useState('');
  const [newCategoryColor, setNewCategoryColor] = useState('#4a90e2');
  const [editingCategoryId, setEditingCategoryId] = useState(null);
  const [editCategoryNameValue, setEditCategoryNameValue] = useState('');
  const [editCategoryColorValue, setEditCategoryColorValue] = useState('#4a90e2');
  const [categoryActionErrorMessage, setCategoryActionErrorMessage] = useState('');

  useEffect(() => {
    async function loadEvents() {
      try {
        const response = await apiClient.get('/api/events');
        setEvents(response.data);
      } catch (error) {
        setErrorMessage(error.response?.data?.message || 'Something went wrong');
      } finally {
        setIsLoading(false);
      }
    }

    async function loadCalendars() {
      const response = await apiClient.get('/api/calendars');
      setCalendars(response.data);
      if (response.data.length > 0) {
        setCalendarId(String(response.data[0].id));
      }
    }

    async function loadCategories() {
      const response = await apiClient.get('/api/categories');
      setCategories(response.data);
    }

    loadEvents();
    loadCalendars();
    loadCategories();
  }, []);

  async function handleCreateEvent(event) {
    event.preventDefault();
    setCreateErrorMessage('');
    setIsSubmitting(true);

    try {
      const response = await apiClient.post('/api/events', {
        title,
        description,
        location,
        startTime,
        endTime,
        allDay,
        recurrenceType,
        reminderOffsetMinutes: reminderOffsetMinutes ? Number(reminderOffsetMinutes) : null,
        calendarId: Number(calendarId),
        categoryId: categoryId ? Number(categoryId) : null,
      });

      setEvents([...events, response.data]);
      setTitle('');
      setDescription('');
      setLocation('');
      setStartTime('');
      setEndTime('');
      setAllDay(false);
      setRecurrenceType('NONE');
      setReminderOffsetMinutes('');
      setCategoryId('');
    } catch (error) {
      setCreateErrorMessage(error.response?.data?.message || 'Something went wrong');
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleEditClick(event) {
    setEventActionErrorMessage('');
    setEditingEventId(event.id);
    setEditTitle(event.title);
    setEditDescription(event.description || '');
    setEditLocation(event.location || '');
    setEditStartTime(event.startTime.slice(0, 16));
    setEditEndTime(event.endTime.slice(0, 16));
    setEditAllDay(event.allDay);
    setEditRecurrenceType(event.recurrenceType);
    setEditReminderOffsetMinutes(
      event.reminderOffsetMinutes === null || event.reminderOffsetMinutes === undefined
        ? ''
        : String(event.reminderOffsetMinutes)
    );
    setEditCalendarId(String(event.calendarId));
    setEditCategoryId(event.categoryId ? String(event.categoryId) : '');
  }

  function handleCancelEdit() {
    setEditingEventId(null);
  }

  async function handleSaveEdit(id) {
    setEventActionErrorMessage('');

    try {
      const response = await apiClient.put(`/api/events/${id}`, {
        title: editTitle,
        description: editDescription,
        location: editLocation,
        startTime: editStartTime,
        endTime: editEndTime,
        allDay: editAllDay,
        recurrenceType: editRecurrenceType,
        reminderOffsetMinutes: editReminderOffsetMinutes ? Number(editReminderOffsetMinutes) : null,
        calendarId: Number(editCalendarId),
        categoryId: editCategoryId ? Number(editCategoryId) : null,
      });

      setEvents(events.map((event) => (event.id === id ? response.data : event)));
      setEditingEventId(null);
    } catch (error) {
      setEventActionErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  async function handleDeleteEvent(id) {
    const confirmed = window.confirm('Delete this event?');
    if (!confirmed) {
      return;
    }

    setEventActionErrorMessage('');

    try {
      await apiClient.delete(`/api/events/${id}`);
      setEvents(events.filter((event) => event.id !== id));
    } catch (error) {
      setEventActionErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  async function handleCreateCalendar(event) {
    event.preventDefault();
    setCalendarActionErrorMessage('');

    try {
      const response = await apiClient.post('/api/calendars', {
        name: newCalendarName,
        color: newCalendarColor,
      });

      setCalendars([...calendars, response.data]);
      setNewCalendarName('');
      setNewCalendarColor('#4a90e2');
    } catch (error) {
      setCalendarActionErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  function handleEditCalendarClick(calendar) {
    setCalendarActionErrorMessage('');
    setEditingCalendarId(calendar.id);
    setEditCalendarNameValue(calendar.name);
    setEditCalendarColorValue(calendar.color);
  }

  function handleCancelCalendarEdit() {
    setEditingCalendarId(null);
  }

  async function handleSaveCalendarEdit(id) {
    setCalendarActionErrorMessage('');

    try {
      const response = await apiClient.put(`/api/calendars/${id}`, {
        name: editCalendarNameValue,
        color: editCalendarColorValue,
      });

      setCalendars(calendars.map((calendar) => (calendar.id === id ? response.data : calendar)));
      setEditingCalendarId(null);
    } catch (error) {
      setCalendarActionErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  async function handleDeleteCalendar(id) {
    const confirmed = window.confirm('Delete this calendar?');
    if (!confirmed) {
      return;
    }

    setCalendarActionErrorMessage('');

    try {
      await apiClient.delete(`/api/calendars/${id}`);
      setCalendars(calendars.filter((calendar) => calendar.id !== id));
    } catch (error) {
      setCalendarActionErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  async function handleCreateCategory(event) {
    event.preventDefault();
    setCategoryActionErrorMessage('');

    try {
      const response = await apiClient.post('/api/categories', {
        name: newCategoryName,
        color: newCategoryColor,
      });

      setCategories([...categories, response.data]);
      setNewCategoryName('');
      setNewCategoryColor('#4a90e2');
    } catch (error) {
      setCategoryActionErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  function handleEditCategoryClick(category) {
    setCategoryActionErrorMessage('');
    setEditingCategoryId(category.id);
    setEditCategoryNameValue(category.name);
    setEditCategoryColorValue(category.color);
  }

  function handleCancelCategoryEdit() {
    setEditingCategoryId(null);
  }

  async function handleSaveCategoryEdit(id) {
    setCategoryActionErrorMessage('');

    try {
      const response = await apiClient.put(`/api/categories/${id}`, {
        name: editCategoryNameValue,
        color: editCategoryColorValue,
      });

      setCategories(categories.map((category) => (category.id === id ? response.data : category)));
      setEditingCategoryId(null);
    } catch (error) {
      setCategoryActionErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  async function handleDeleteCategory(id) {
    const confirmed = window.confirm('Delete this category?');
    if (!confirmed) {
      return;
    }

    setCategoryActionErrorMessage('');

    try {
      await apiClient.delete(`/api/categories/${id}`);
      setCategories(categories.filter((category) => category.id !== id));
    } catch (error) {
      setCategoryActionErrorMessage(error.response?.data?.message || 'Something went wrong');
    }
  }

  if (isLoading) {
    return (
      <div>
        <h2>Calendar</h2>
        <p>Loading...</p>
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div>
        <h2>Calendar</h2>
        <p className="form-error">{errorMessage}</p>
      </div>
    );
  }

  return (
    <div>
      <h2>Calendar</h2>

      {calendars.length === 0 ? (
        <p>Create a calendar before adding an event.</p>
      ) : (
        <form className="event-form" onSubmit={handleCreateEvent}>
          <h3>Add an Event</h3>

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

          <label htmlFor="location">Location</label>
          <input
            id="location"
            type="text"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />

          <label htmlFor="startTime">Start Time</label>
          <input
            id="startTime"
            type="datetime-local"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
          />

          <label htmlFor="endTime">End Time</label>
          <input
            id="endTime"
            type="datetime-local"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
          />

          <label htmlFor="allDay">
            <input
              id="allDay"
              type="checkbox"
              checked={allDay}
              onChange={(e) => setAllDay(e.target.checked)}
            />{' '}
            All Day
          </label>

          <label htmlFor="recurrenceType">Recurrence</label>
          <select
            id="recurrenceType"
            value={recurrenceType}
            onChange={(e) => setRecurrenceType(e.target.value)}
          >
            <option value="NONE">None</option>
            <option value="DAILY">Daily</option>
            <option value="WEEKLY">Weekly</option>
            <option value="MONTHLY">Monthly</option>
          </select>

          <label htmlFor="reminderOffsetMinutes">Reminder (minutes before)</label>
          <input
            id="reminderOffsetMinutes"
            type="number"
            value={reminderOffsetMinutes}
            onChange={(e) => setReminderOffsetMinutes(e.target.value)}
          />

          <label htmlFor="calendarId">Calendar</label>
          <select
            id="calendarId"
            value={calendarId}
            onChange={(e) => setCalendarId(e.target.value)}
          >
            {calendars.map((calendar) => (
              <option key={calendar.id} value={calendar.id}>
                {calendar.name}
              </option>
            ))}
          </select>

          <label htmlFor="categoryId">Category</label>
          <select
            id="categoryId"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
          >
            <option value="">No category</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>

          {createErrorMessage && <p className="form-error">{createErrorMessage}</p>}

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Adding...' : 'Add Event'}
          </button>
        </form>
      )}

      {eventActionErrorMessage && <p className="form-error">{eventActionErrorMessage}</p>}

      {events.length === 0 ? (
        <p>No events yet.</p>
      ) : (
        <ul className="event-list">
          {events.map((event) =>
            editingEventId === event.id ? (
              <li key={event.id} className="event-item">
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

                <label htmlFor="editLocation">Location</label>
                <input
                  id="editLocation"
                  type="text"
                  value={editLocation}
                  onChange={(e) => setEditLocation(e.target.value)}
                />

                <label htmlFor="editStartTime">Start Time</label>
                <input
                  id="editStartTime"
                  type="datetime-local"
                  value={editStartTime}
                  onChange={(e) => setEditStartTime(e.target.value)}
                />

                <label htmlFor="editEndTime">End Time</label>
                <input
                  id="editEndTime"
                  type="datetime-local"
                  value={editEndTime}
                  onChange={(e) => setEditEndTime(e.target.value)}
                />

                <label htmlFor="editAllDay">
                  <input
                    id="editAllDay"
                    type="checkbox"
                    checked={editAllDay}
                    onChange={(e) => setEditAllDay(e.target.checked)}
                  />{' '}
                  All Day
                </label>

                <label htmlFor="editRecurrenceType">Recurrence</label>
                <select
                  id="editRecurrenceType"
                  value={editRecurrenceType}
                  onChange={(e) => setEditRecurrenceType(e.target.value)}
                >
                  <option value="NONE">None</option>
                  <option value="DAILY">Daily</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option>
                </select>

                <label htmlFor="editReminderOffsetMinutes">Reminder (minutes before)</label>
                <input
                  id="editReminderOffsetMinutes"
                  type="number"
                  value={editReminderOffsetMinutes}
                  onChange={(e) => setEditReminderOffsetMinutes(e.target.value)}
                />

                <label htmlFor="editCalendarId">Calendar</label>
                <select
                  id="editCalendarId"
                  value={editCalendarId}
                  onChange={(e) => setEditCalendarId(e.target.value)}
                >
                  {calendars.map((calendar) => (
                    <option key={calendar.id} value={calendar.id}>
                      {calendar.name}
                    </option>
                  ))}
                </select>

                <label htmlFor="editCategoryId">Category</label>
                <select
                  id="editCategoryId"
                  value={editCategoryId}
                  onChange={(e) => setEditCategoryId(e.target.value)}
                >
                  <option value="">No category</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>

                <div className="event-edit-actions">
                  <button type="button" onClick={() => handleSaveEdit(event.id)}>
                    Save
                  </button>
                  <button type="button" onClick={handleCancelEdit}>
                    Cancel
                  </button>
                </div>
              </li>
            ) : (
              <li key={event.id} className="event-item">
                <div className="event-title">{event.title}</div>
                <div className="event-meta">
                  {new Date(event.startTime).toLocaleString()} to{' '}
                  {new Date(event.endTime).toLocaleString()}
                </div>
                {event.description && <div className="event-description">{event.description}</div>}
                {event.location && <div className="event-meta">Location: {event.location}</div>}
                <div className="event-meta">Calendar: {event.calendarName}</div>
                {event.categoryName && <div className="event-meta">Category: {event.categoryName}</div>}
                <div className="event-meta">Recurrence: {event.recurrenceType}</div>
                <div className="event-meta">
                  <button type="button" onClick={() => handleEditClick(event)}>
                    Edit
                  </button>
                  <button type="button" onClick={() => handleDeleteEvent(event.id)}>
                    Delete
                  </button>
                </div>
              </li>
            )
          )}
        </ul>
      )}

      <section className="management-section">
        <h3>Calendars</h3>

        <form className="management-form" onSubmit={handleCreateCalendar}>
          <label htmlFor="newCalendarName">Name</label>
          <input
            id="newCalendarName"
            type="text"
            value={newCalendarName}
            onChange={(e) => setNewCalendarName(e.target.value)}
          />

          <label htmlFor="newCalendarColor">Color</label>
          <input
            id="newCalendarColor"
            type="color"
            value={newCalendarColor}
            onChange={(e) => setNewCalendarColor(e.target.value)}
          />

          <button type="submit">Add Calendar</button>
        </form>

        {calendarActionErrorMessage && <p className="form-error">{calendarActionErrorMessage}</p>}

        {calendars.length === 0 ? (
          <p>No calendars yet.</p>
        ) : (
          <ul className="management-list">
            {calendars.map((calendar) =>
              editingCalendarId === calendar.id ? (
                <li key={calendar.id} className="management-item">
                  <label htmlFor="editCalendarNameValue">Name</label>
                  <input
                    id="editCalendarNameValue"
                    type="text"
                    value={editCalendarNameValue}
                    onChange={(e) => setEditCalendarNameValue(e.target.value)}
                  />

                  <label htmlFor="editCalendarColorValue">Color</label>
                  <input
                    id="editCalendarColorValue"
                    type="color"
                    value={editCalendarColorValue}
                    onChange={(e) => setEditCalendarColorValue(e.target.value)}
                  />

                  <div className="management-actions">
                    <button type="button" onClick={() => handleSaveCalendarEdit(calendar.id)}>
                      Save
                    </button>
                    <button type="button" onClick={handleCancelCalendarEdit}>
                      Cancel
                    </button>
                  </div>
                </li>
              ) : (
                <li key={calendar.id} className="management-item">
                  <span
                    className="color-swatch"
                    style={{ backgroundColor: calendar.color }}
                  ></span>
                  <span>{calendar.name}</span>
                  <span className="management-actions">
                    <button type="button" onClick={() => handleEditCalendarClick(calendar)}>
                      Edit
                    </button>
                    <button type="button" onClick={() => handleDeleteCalendar(calendar.id)}>
                      Delete
                    </button>
                  </span>
                </li>
              )
            )}
          </ul>
        )}
      </section>

      <section className="management-section">
        <h3>Categories</h3>

        <form className="management-form" onSubmit={handleCreateCategory}>
          <label htmlFor="newCategoryName">Name</label>
          <input
            id="newCategoryName"
            type="text"
            value={newCategoryName}
            onChange={(e) => setNewCategoryName(e.target.value)}
          />

          <label htmlFor="newCategoryColor">Color</label>
          <input
            id="newCategoryColor"
            type="color"
            value={newCategoryColor}
            onChange={(e) => setNewCategoryColor(e.target.value)}
          />

          <button type="submit">Add Category</button>
        </form>

        {categoryActionErrorMessage && <p className="form-error">{categoryActionErrorMessage}</p>}

        {categories.length === 0 ? (
          <p>No categories yet.</p>
        ) : (
          <ul className="management-list">
            {categories.map((category) =>
              editingCategoryId === category.id ? (
                <li key={category.id} className="management-item">
                  <label htmlFor="editCategoryNameValue">Name</label>
                  <input
                    id="editCategoryNameValue"
                    type="text"
                    value={editCategoryNameValue}
                    onChange={(e) => setEditCategoryNameValue(e.target.value)}
                  />

                  <label htmlFor="editCategoryColorValue">Color</label>
                  <input
                    id="editCategoryColorValue"
                    type="color"
                    value={editCategoryColorValue}
                    onChange={(e) => setEditCategoryColorValue(e.target.value)}
                  />

                  <div className="management-actions">
                    <button type="button" onClick={() => handleSaveCategoryEdit(category.id)}>
                      Save
                    </button>
                    <button type="button" onClick={handleCancelCategoryEdit}>
                      Cancel
                    </button>
                  </div>
                </li>
              ) : (
                <li key={category.id} className="management-item">
                  <span
                    className="color-swatch"
                    style={{ backgroundColor: category.color }}
                  ></span>
                  <span>{category.name}</span>
                  <span className="management-actions">
                    <button type="button" onClick={() => handleEditCategoryClick(category)}>
                      Edit
                    </button>
                    <button type="button" onClick={() => handleDeleteCategory(category.id)}>
                      Delete
                    </button>
                  </span>
                </li>
              )
            )}
          </ul>
        )}
      </section>
    </div>
  );
}

export default CalendarPage;
