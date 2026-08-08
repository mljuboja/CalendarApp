import axios from 'axios';

// Central Axios instance. Every API call in the app should import this
// instead of calling axios directly, so the base URL only lives in one place.
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

export default apiClient;
