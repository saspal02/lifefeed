# Product Requirements Document: LifeAPI

## 1. Overview

LifeAPI is a personal, self-owned API that pulls data from the services I already use (a sleep wearable, GitHub, a reading-list source, and a workout source) and exposes it through one unified, authenticated endpoint. It includes a chronological "life feed" of small, structured entries (e.g. "Slept 7h12m", "14 commits to repo-x", "Finished chapter 6 of Book Y", "5K run, 28:40"), plus per-source and summary endpoints. Dashboards and other tools can be built on top of it later.

## 2. Problem Statement

- My data is locked in separate apps, each with its own UI, format, and export rules.
- There is no single view of how sleep, work, reading, and exercise relate.
- I don't own the data or the format, so I can't build on it or query it freely.
- Passive social feeds consume attention without giving anything back.

## 3. Goal / Vision

Turn my scattered digital life into one place I control. The feed should feel as rhythmic and rewarding as scrolling, but every item is my own data, structured into something useful instead of algorithmic content from strangers. Long-term, the API becomes the foundation for dashboards, automations, and AI tools that read my life data.

## 4. Target Users

- Primary: me, a developer who tracks their life with several apps and wants ownership and programmability.
- Secondary users (developers and quantified-self enthusiasts who self-host or fork) are a future consideration, not a current target.

## 5. Key Features (MVP)

- Connectors for 4 sources: one sleep wearable, GitHub, one reading-list source, one workout source.
- Scheduled sync (e.g. every few hours) that fetches and refreshes data.
- Normalization into a common event schema: id, source, type, timestamp, summary, data (JSON).
- REST endpoints: GET /v1/feed (unified, reverse-chronological, paginated, filterable); GET /v1/{sleep|commits|reading|workouts}; GET /v1/summary/today and GET /v1/summary/week.
- Personal API key authentication (single user).
- Basic docs (OpenAPI spec) and a README with setup steps.
- Deployed so it is reachable from anywhere.

## 6. Out of Scope

- A polished dashboard or mobile app (the API comes first).
- Multi-user accounts, sign-up, or billing.
- Real-time streaming or webhooks (polling sync only).
- Advanced analytics, correlations, or AI insights.
- Writing data back to the source services.
- More than 4 data sources.
- Social or sharing features.

## 7. Success Metrics

- All 4 sources sync automatically for 14 consecutive days without manual fixes.
- A single GET /v1/feed call returns a correct, merged view of my last 24 hours.
- I check my own feed daily by choice for 2+ weeks.
- I build one thing on top of the API (a simple dashboard or a daily digest) in under a day, without touching the source services.
- Adding a new source takes under half a day because of the common schema.

## 8. Risks and Assumptions

Risks:
- Source API limits, changes, or deprecations (wearable APIs often change access rules or require paid tiers).
- OAuth token expiry breaking syncs silently.
- Data inconsistencies (timezones, duplicate events, missing days).
- Security: health data is sensitive, and a leaked API key exposes it.
- Scope creep into building dashboards before the API is solid.

Assumptions:
- Every chosen source offers an official API or export I can access for personal use.
- A single-user, self-hosted setup is enough.
- Polling every few hours gives fresh enough data.
- A single event schema can represent all four data types without too much loss.

## 9. Open Questions

- Which wearable (Oura, Whoop, Fitbit, Apple Health, Garmin) and which workout and reading apps will be the sources?
- Which stack: language, framework, database, hosting?
- Should the feed show raw events, daily aggregates, or both?
- How long should data be retained, and should raw source payloads be stored?
- How should auth work: static API key, or OAuth/JWT for future sharing?
- Is it open source from day one, or private until stable?
- How should data be backed up and exported?
