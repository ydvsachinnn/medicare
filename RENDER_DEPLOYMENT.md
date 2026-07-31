# 🐳 Render Docker Deployment Guide — MediCare Plus

This guide walks you through deploying the **MediCare Plus** Spring Boot web application onto **Render** using a **Docker Web Service** configuration.

---

## 📋 Prerequisites
1. **GitHub Repository:** Push your project repository containing the `pepclass` subdirectory (with the newly created `Dockerfile`) to GitHub.
2. **MongoDB Atlas Instance:** A free-tier MongoDB connection URI (e.g. `mongodb+srv://...`).
3. **Google Gemini API Key:** Your personal Gemini API key.

---

## 🛠️ Step 1: Provision a MongoDB Atlas Cluster (Free Tier)
Since Render container storage is ephemeral, you need an external database:
1. Go to **[MongoDB Atlas](https://www.mongodb.com/cloud/atlas)** and sign up.
2. Click **Create** to provision a free shared cluster.
3. In **Network Access**, add IP address `0.0.0.0/0` (allows Render servers to connect).
4. In **Database Access**, create a user (e.g. `dbuser`) and copy the password.
5. Click **Connect** -> **Drivers** to get your connection URI:
   ```text
   mongodb+srv://dbuser:<password>@cluster0.xxxx.mongodb.net/pepclass?retryWrites=true&w=majority
   ```
   *(Be sure to replace `<password>` with your database user's password).*

---

## 🌐 Step 2: Create a Docker Web Service on Render
1. Go to your **[Render Dashboard](https://dashboard.render.com/)**.
2. Click **New +** and select **Web Service**.
3. Connect your GitHub repository.
4. Configure the Web Service settings:
   - **Name:** `medicare-plus`
   - **Region:** Select closest to you (e.g. `Oregon (US West)` or `Singapore`)
   - **Branch:** `main` (or whichever branch you pushed to)
   - **Root Directory:** `pepclass`
   - **Runtime:** `Docker` *(This will automatically detect and build the `Dockerfile` inside the `pepclass` folder).*

---

## 🔒 Step 3: Environment Variables (Critical)
Navigate to the **Environment** tab on Render and add the following keys:

| Key | Value | Description |
|-----|-------|-------------|
| `SPRING_DATA_MONGODB_URI` | `mongodb+srv://...` | Your MongoDB Atlas connection string (from Step 1). |
| `GOOGLE_GEMINI_API_KEY` | `AQ.Ab8RN...` | Your personal Gemini API key. |

---

## 🚀 Step 4: Deploy!
- Click **Create Web Service** at the bottom of the page.
- Render will start building the Docker image from the multi-stage `Dockerfile`, package the JAR, and spin up the container.
- Once completed, you will see a green **"Live"** status badge, and your web app will be available at your custom Render URL (e.g. `https://medicare-plus.onrender.com`).
