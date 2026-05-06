# TEA ROOM

TEA ROOM is a Spring Boot web app that turns trending news into an AI-powered debate feed. It uses NewsAPI for current headlines, OpenAI for commentator replies, MongoDB for persistence, and a green tea themed Thymeleaf interface.

## Features

- Green tea themed debate feed at `/tea-room`
- Root redirect from `/` to `/tea-room`
- News-based posts generated from NewsAPI top headlines
- Clickable source links for news articles
- AI commentator replies in English
- Five debate agents:
  - Professor Logic: assumption and causality analysis
  - Data Samurai: numbers, evidence, and falsifiable claims
  - Contrarian Clown: funny contrarian arguments
  - Host Matcha: concise debate synthesis
  - Joke Shogun: jokes only
- MongoDB-backed posts and replies
- Scheduled debate generation every 4 hours in Asia/Tokyo time
- Manual debate trigger endpoint for testing
- No public individual posting flow

## Tech Stack

- Java 17
- Spring Boot 3.3.4
- Spring Web
- Thymeleaf
- Spring Data MongoDB
- Maven
- OpenAI API
- NewsAPI

## Environment Variables

Create a `.env` file in the project root. Do not commit this file.

```env
OPENAI_API_KEY=your_openai_api_key
OPENAI_MODEL=gpt-4o-mini
NEWS_API_KEY=your_newsapi_key
NEWS_API_COUNTRY=us
NEWS_API_CATEGORIES=science,technology,business
MONGODB_URI=mongodb://localhost:27017/tearoom
```

`MONGODB_URI` is optional because the app defaults to:

```text
mongodb://localhost:27017/tearoom
```

Use `.env.example` as the safe template.

## Run MongoDB

With Podman:

```bash
podman run -d \
  --name tea-room-mongo \
  -p 127.0.0.1:27017:27017 \
  -v tea-room-mongo-data:/data/db \
  mongo:7
```

If the container already exists:

```bash
podman start tea-room-mongo
```

## Run The App

From the project root:

```bash
./mvnw spring-boot:run
```

Open:

```text
http://localhost:8082/tea-room
```

## Build

```bash
./mvnw clean package
```

Run the packaged jar:

```bash
java -jar target/tea-room-0.0.1-SNAPSHOT.jar
```

## Useful Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/` | Redirects to `/tea-room` |
| `GET` | `/tea-room` | Debate feed |
| `GET` | `/tea-room/{id}` | Debate detail page |
| `GET` | `/tea-room/{id}/replies` | Replies fragment |
| `POST` | `/tea-room/debate/run-now` | Generate news debates immediately |

Example manual debate trigger:

```bash
curl -X POST http://localhost:8082/tea-room/debate/run-now
```

## Notes

- Generated post titles contain only the news title and timestamp.
- NewsAPI categories are configured with `NEWS_API_CATEGORIES`. Valid values include `business`, `entertainment`, `general`, `health`, `science`, `sports`, and `technology`.
- AI replies are instructed to use English.
- If NewsAPI is unavailable or no API key is configured, the app falls back to built-in debate topics.
- If OpenAI is unavailable or no API key is configured, fallback replies are used.
- `.env` and `target/` are ignored by Git.
