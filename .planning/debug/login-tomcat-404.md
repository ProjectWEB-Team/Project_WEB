---
status: resolved
trigger: "GET /login returns the default Apache Tomcat 10.1.59 HTTP 404 page."
created: 2026-10-07T00:00:00+07:00
updated: 2026-10-07T09:15:00+07:00
---

## Symptoms

- expected: "The Spring Boot JSP login page renders at http://localhost:8080/login."
- actual: "The browser shows the default Apache Tomcat 10.1.59 HTTP Status 404 page for [/login]."
- error_messages: "The requested resource [/login] is not available."
- timeline: "Observed after importing/configuring the project in STS for local development."
- reproduction: "Open http://localhost:8080/login in the browser."

## Current Focus

hypothesis: "Resolved: the browser now reaches the Spring Boot login JSP at /login."
next_action: "Continue Phase-01 UAT with a valid local ROLE_USER account."

## Evidence

- timestamp: 2026-10-07T00:00:00+07:00
  observation: "Screenshot identifies Apache Tomcat/10.1.59 default 404 response, not the application's safe error view."
- timestamp: 2026-10-07T09:00:00+07:00
  observation: "Spring Boot started with profile local, but SQL Server rejected login uteexpress_app and startup then failed because port 8080 was already in use."
- timestamp: 2026-10-07T09:15:00+07:00
  observation: "Browser evidence shows the Spring Boot JSP login page rendered successfully at http://localhost:8080/login."

## Eliminated

- hypothesis: "The local profile was not active."
  reason: "The new startup log confirms the local profile is active."

## Resolution

root_cause: "An external Tomcat listener served port 8080 instead of the Spring Boot application."
fix: "Run the embedded Spring Boot application with the local profile and release the conflicting listener."
verification: "The browser now renders the application's JSP login page at /login."
files_changed: []
