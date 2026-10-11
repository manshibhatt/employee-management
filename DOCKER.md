# Run the full stack with Docker Compose

Install and start Docker Desktop (or another Docker Engine with the Compose plugin), then from the project root create the ignored environment file:

```sh
cp .env.example .env
```

Replace both database password placeholders in `.env` with strong values. Then start all three services:

```sh
docker compose up --build -d
```

Open `http://localhost:5173`. The frontend proxy sends `/api` requests to the Spring Boot service. The backend waits for MySQL's health check before starting.

The frontend, backend, and MySQL run as separate containers. Check startup with `docker compose ps` and view logs with `docker compose logs -f`. To stop them, run `docker compose down`. The MySQL data volume remains in place. Use `docker compose down -v` only when you intend to delete that database data.

To enable leave approval email, set `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` in `.env` before starting the backend.

## Publish images from GitHub

The workflow in `.github/workflows/docker-publish.yml` builds and publishes the backend and frontend images to GitHub Container Registry after a push to the `prod` branch. GitHub Actions uses the repository's `GITHUB_TOKEN`; no registry password needs to be added to the repository. The images are named `<owner>/<repository>-backend` and `<owner>/<repository>-frontend` under `ghcr.io`, with `prod`, commit SHA, and `latest` tags. The workflow publishes images; it does not provision a database or deploy the stack to a hosting provider. Use the Compose setup above for a single-host deployment, or configure a hosting provider to run the images and a managed MySQL database.
