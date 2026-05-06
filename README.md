# Coffeehouse

Coffeehouse is a Spring Boot web app that turns trending news into AI-generated news briefs. It uses NewsAPI for current headlines, OpenAI for structured sections, MongoDB for persistence, and a coffee themed Thymeleaf interface.

## Features

- Coffee themed news feed at `/tea-room`
- Root redirect from `/` to `/tea-room`
- News-based posts generated from NewsAPI top headlines
- News thumbnails from NewsAPI article images
- Clickable source links for news articles
- AI-generated sections in English:
  - Summary: brief overview
  - Knowledge: background context with readable links from reliable source domains
  - Explanation: why the story matters
  - Conclusion: balanced takeaway and open question
- Browser-side Save and Share buttons for each article
- MongoDB-backed posts and replies
- Scheduled news brief generation every 4 hours in Asia/Tokyo time
- Manual generation endpoint for testing
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
NEWS_REFERENCE_DOMAINS=reuters.com,apnews.com,bbc.com,npr.org,pbs.org,scientificamerican.com,nature.com,science.org,who.int,nih.gov,nasa.gov,smithsonianmag.com,nationalgeographic.com,livescience.com,space.com,theguardian.com
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
| `GET` | `/tea-room` | News brief feed |
| `GET` | `/tea-room/{id}` | News brief detail page |
| `GET` | `/tea-room/{id}/replies` | Replies fragment |
| `POST` | `/tea-room/debate/run-now` | Generate news briefs immediately |

Example manual generation trigger:

```bash
curl -X POST http://localhost:8082/tea-room/debate/run-now
```

## Notes

- Generated post titles contain only the news title and timestamp.
- Knowledge sections search related keyword combinations from the news title and append clickable related links from reliable source domains.
- Reliable reference domains are configured with `NEWS_REFERENCE_DOMAINS`.
- Save buttons are stored in the browser with `localStorage`.
- Share uses the mobile OS share sheet when available and falls back to copying the link.
- NewsAPI categories are configured with `NEWS_API_CATEGORIES`. Valid values include `business`, `entertainment`, `general`, `health`, `science`, `sports`, and `technology`.
- AI replies are instructed to use English.
- If NewsAPI is unavailable or no API key is configured, the app falls back to built-in debate topics.
- If OpenAI is unavailable or no API key is configured, fallback replies are used.
- `.env` and `target/` are ignored by Git.
