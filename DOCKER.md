# Run with Docker

1. Copy `.env.example` to `.env` and replace both database password values with strong local values. `.env` is ignored by Git.
2. From the project root, run `docker compose up --build`.
3. Open `http://localhost:5173`. The frontend proxy sends `/api` requests to the Spring Boot service.

The frontend, backend, and MySQL run as separate containers. To stop them, run `docker compose down`. The MySQL data volume remains in place. Use `docker compose down -v` only when you intend to delete that database data.

To enable leave approval email, set `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` in `.env` before starting the backend.

## Publish images from GitHub

The workflow in `.github/workflows/docker-publish.yml` builds and publishes the backend and frontend to GitHub Container Registry after a push to `main` or `master` (and for `v*` tags). GitHub Actions uses the repository's `GITHUB_TOKEN`; no registry password needs to be added to the repository. The images are named `<owner>/<repository>-backend` and `<owner>/<repository>-frontend` under `ghcr.io`.
