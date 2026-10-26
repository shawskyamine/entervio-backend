# Entrevio

> **AI-assisted hiring for candidates and recruiters.**
> Deterministic matching, explainable rankings, cited AI search.

Entrevio is a two-sided recruitment platform that reads real CVs instead of forms, parses job descriptions automatically, ranks candidates with a deterministic scoring engine, and lets recruiters search their talent pool in plain English — with every answer grounded in the actual CV.

Built as a portfolio-grade full-stack product demonstrating clean architecture, disciplined LLM integration, and end-to-end feature ownership.

---

## Table of Contents

- [The Problem](#the-problem)
- [What Entrevio Does](#what-entrevio-does)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Design Decisions](#design-decisions)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running the App](#running-the-app)
- [Testing the Flows](#testing-the-flows)
- [Project Structure](#project-structure)
- [The Matching Pipeline](#the-matching-pipeline)
- [The RAG Pipeline](#the-rag-pipeline)
- [What's Not Built (and Why)](#whats-not-built-and-why)
- [Roadmap](#roadmap)
- [License](#license)

---

## The Problem

Recruiters spend hours reading PDFs that could be structured data. Candidate CVs are:

- **Unstructured** — scanned PDFs, tables, layout-heavy documents
- **Inconsistent** — no two CVs use the same format
- **Semantically dense** — "J2EE" and "Java" mean the same thing, but a keyword search won't catch it

Entrevio solves all three. Every uploaded CV becomes structured JSON. Every job description becomes structured requirements. Matching is deterministic and auditable. Semantic search bridges terminology gaps.

---

## What Entrevio Does

**For candidates:**
- Upload a PDF CV → get it parsed into a structured profile (skills, years, education)
- Receive a quality score (ATS-friendliness, clarity, impact, completeness) with actionable improvement suggestions
- Browse jobs, apply in one click, track application status
- Prepare for interviews with AI-generated, tailored questions

**For recruiters:**
- Post jobs → job descriptions auto-parsed into required skills, seniority, education
- See every candidate in the pool scored against a specific role with a component breakdown
- Search CVs in plain English with cited answers ("candidates who've led a migration to microservices")
- Manage applications through a status pipeline (PENDING → REVIEWING → ACCEPTED/REJECTED)
- Track hiring KPIs, funnel, and top jobs on a live analytics dashboard

---

## Features

### Core

- ✅ **JWT authentication** with `CANDIDATE` / `RECRUITER` role claims
- ✅ **Role-scoped routing** — Next.js App Router route groups enforce access at the folder level
- ✅ **CV upload and parsing** — PDFBox extraction → LLM → structured JSON
- ✅ **CV quality scoring** — 4 weighted dimensions + prioritized improvement suggestions
- ✅ **Job posting and parsing** — LLM extracts requirements, seniority, education, responsibilities
- ✅ **Deterministic matching engine** — weighted multi-criteria scoring, never LLM-ranked
- ✅ **Match explanations** — LLM writes per-candidate reasoning, grounded in the score
- ✅ **Applications pipeline** — full state machine with role-scoped operations
- ✅ **Interview preparation** — AI-generated questions, red flags, strengths; PDF export
- ✅ **Analytics dashboard** — KPIs, timeline chart, pipeline funnel, top jobs

### AI / RAG

- ✅ **Semantic search (RAG)** — pgvector + hybrid retrieval over CV chunks
- ✅ **Hybrid search** — BM25 (Postgres `ts_rank_cd`) + vector similarity, 60/40 weighted
- ✅ **Cited answers** — every claim links back to a specific CV excerpt
- ✅ **Similarity threshold** — low-relevance chunks filtered before the LLM sees them
- ✅ **Local embeddings** — Ollama `nomic-embed-text`, no cloud dependency for vectorization

---

## Architecture
