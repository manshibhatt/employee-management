# PeopleOS frontend

React + Vite frontend for the Spring Boot employee management API in this repository.

## Run locally

1. Start the backend from the project root: `./mvnw spring-boot:run` (it listens on port 8080).
2. In another terminal, run `cd frontend && npm install && npm run dev`.
3. Open the local URL printed by Vite, usually `http://localhost:5173`.

The Vite dev server proxies `/api` requests to `http://localhost:8080`. Sign in with an existing backend account, or choose **Create an account** on the sign-in screen to register and sign in. Employee and department API routes require a JWT, which the app keeps in browser local storage for the session.

The employee form uses the backend's `EmployeeDTO` fields: `firstName`, `lastName`, `email`, `salary`, and `departmentId`. Department choices come from `GET /api/departments`.
