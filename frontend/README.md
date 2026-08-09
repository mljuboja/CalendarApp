# Daymark — Frontend

The React frontend for **Daymark**, a full-stack calendar and task management app.

## Tech Stack

- **React** (JavaScript, no TypeScript)
- **Vite**
- **React Router** — client-side routing and protected routes
- **Axios** — API calls to the Spring Boot backend
- **FullCalendar** (`@fullcalendar/react`, `core`, `daygrid`, `interaction`) — visual month calendar with drag/resize

## Setup

1. Copy the environment template and fill in the backend URL:

   ```bash
   cp .env.example .env
   ```

   ```
   VITE_API_BASE_URL=http://localhost:8080
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Run the dev server:

   ```bash
   npm run dev
   ```

   The app runs at `http://localhost:5173` and expects the backend
   (see the root `README.md`) to be running at `VITE_API_BASE_URL`.

## Other Commands

```bash
npm run build    # production build
npm run lint     # run Oxlint
npm run preview  # preview a production build locally
```
