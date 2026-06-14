# Production Deployment Guide

This guide describes the step-by-step process of deploying the Splitwise Clone and Anomaly Detector application to production.

*   **Backend & Database**: Render (Web Service + Managed PostgreSQL)
*   **Frontend**: Vercel (Static Web Hosting)

---

## 1. Relational Database Setup (PostgreSQL on Render)

1.  Log in to your [Render Dashboard](https://dashboard.render.com/).
2.  Click **New +** and select **PostgreSQL**.
3.  Configure your database:
    *   **Name**: `splitwise-db`
    *   **Database Name**: `splitwise`
    *   **User**: `postgres` (or keep Render's auto-generated user)
    *   **Region**: Select the region closest to your users.
4.  Click **Create Database**.
5.  Once initialized, copy the **Internal Database URL** (for backend services running on Render) and **External Connection String** (for local migrations or debugging).

---

## 2. Backend Web Service Setup (Render)

1.  From the Render Dashboard, click **New +** and select **Web Service**.
2.  Connect your GitHub repository.
3.  Configure your Web Service:
    *   **Name**: `splitwise-backend`
    *   **Runtime**: `Java` (Ensure it supports Java 21)
    *   **Build Command**: `./mvnw clean package -DskipTests` (or `mvn clean package -DskipTests`)
    *   **Start Command**: `java -jar target/splitwise-0.0.1-SNAPSHOT.jar`
4.  Expand the **Advanced** section to add the following **Environment Variables**:

| Variable Name | Description | Example Value |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC Connection URL (Use internal URL) | `jdbc:postgresql://dpg-xxxx-a.singapore-postgres.render.com/splitwise` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `your_db_password` |
| `CORS_ALLOWED_ORIGINS` | Allowed origins (frontend Vercel domain URL) | `https://splitwise-client.vercel.app` |
| `JWT_SECRET` | Secure cryptographic key to sign session JWTs | `a-super-secret-key-of-at-least-256-bits-long-for-hmac` |
| `PORT` | Bound port for Spring Boot (automatically set by Render) | `8080` |

5.  Configure Render's health check path to **`/api/health`** to let Render automatically monitor application health.
6.  Click **Create Web Service**.

---

## 3. Frontend Deployment (Vercel)

1.  Log in to [Vercel](https://vercel.com/).
2.  Click **Add New...** and select **Project**.
3.  Import your GitHub repository.
4.  Configure the Vercel build settings:
    *   **Root Directory**: `frontend`
    *   **Framework Preset**: `Vite` (or Other)
    *   **Build Command**: `npm run build`
    *   **Output Directory**: `dist`
5.  Add the following **Environment Variable**:

| Variable Name | Description | Example Value |
| :--- | :--- | :--- |
| `VITE_API_URL` | Root URL of your deployed backend service | `https://splitwise-backend.onrender.com` |

6.  Click **Deploy**.
7.  Once deployed, copy the project URL (e.g., `https://splitwise-client.vercel.app`) and update the `CORS_ALLOWED_ORIGINS` variable in your Render backend settings.

---

## 4. Verification and Operations

*   **Health Status check**: Run a curl request to verify that the service is running:
    ```bash
    curl https://splitwise-backend.onrender.com/api/health
    # Expected: {"status":"UP"}
    ```
*   **Logs Audit**: Monitor deployment and runtime logs in the Render Web Service console to troubleshoot startup database migrations or connection errors.
