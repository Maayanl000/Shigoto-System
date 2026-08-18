import axios from 'axios';

// הגדרת כתובת הבסיס של שרת ה-Spring Boot שלנו
const api = axios.create({
  baseURL: 'http://localhost:8080/api', 
});

export default api;