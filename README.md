# LULU AI — Intelligent Medical Logging Assistant

**LULU AI** is a conversational health companion designed to make patient monitoring seamless, consistent, and intelligent between doctor appointments.
Since daily communication between doctors and patients is usually impossible, the medical information accumulated in this interval is partially lost.
LULU AI bridges the gap between doctor visits by helping users record, track, and understand their ongoing health changes through daily, personalized conversations. 

---

## Purpose
Patients often forget important details about their symptoms or treatment progress by the time they meet their doctor again. **LULU** solves this by keeping a structured, AI-assisted health log that evolves with the user’s condition.

---

## How It Works
- **Daily Check-ins:** Lulu proactively checks in with patients, asking short, health-related questions.  
- **Doctor-Preset Questions:** Physicians can define specific questions based on a diagnosis or treatment plan.  
- **Adaptive Follow-ups:** An integrated **LLM** analyzes user responses and generates intelligent follow-up questions tailored to the patient’s situation.  
- **RAG-Backed Reasoning:** All adaptive questions and inferences are supported by a **Retrieval-Augmented Generation (RAG)** system, ensuring they are grounded in verified medical documentation.  
- **Health Tracking:** Lulu tracks symptom progression over time, identifying trends and health patterns.  
- **PDF Reports for Doctors:** The system generates concise, time-based PDF summaries for healthcare providers — ensuring accurate and efficient follow-ups.

---

## Why It Matters
- Reduces miscommunication between doctors and patients  
- Encourages consistent daily health monitoring  
- Provides structured and reliable medical histories  
- Supports evidence-based, AI-assisted insights  

---

## Tech Stack
- **LLM** for dialogue and question generation  
- **RAG System** for grounded medical reasoning  
- **PDF Generator** for structured report creation  
- **Session Memory** for long-term user tracking  

---

## Vision
**LULU AI** aims to become a trusted companion for patients and healthcare providers — not to replace doctors, but to enhance communication, accuracy, and care continuity by ensuring no detail is ever lost between visits.


-----------------------------------

# System Architecture Overview

The provisioned architecture for this project consists of **two core components**:  
**Azure OpenAI** (for secure LLM inference) and **Azure Cosmos DB** (for data storage and retrieval).  
For demonstration purposes, the current prototype uses **OpenAI’s API** and **SQLite** as substitutes.

---

## Azure OpenAI

Azure OpenAI serves as the project’s **LLM inference endpoint**.  
It offers full compatibility with the OpenAI API while adding enterprise-grade **data protection and compliance** benefits:

- **Data Retention:** Prompted data is automatically deleted after 30 days.  
- **Privacy Guarantee:** No data is shared with OpenAI’s public API, ensuring that confidential patient information is never used for model retraining.  
- **Enterprise Security:** Azure provides built-in encryption, network isolation, and regional compliance support (e.g., HIPAA-ready).

These properties make Azure OpenAI a **secure, compliant, and scalable choice** for our healthcare-oriented AI system.

---

## Cosmos DB for RAG and Data Storage

**Azure Cosmos DB** acts as the system’s **knowledge and retrieval layer**, powering:

- Medical data storage, including patient logs and structured records.  
- Vector-based similarity search for **Retrieval-Augmented Generation (RAG)**.  
- Fast contextual retrieval of medical documentation, diagnostic guidelines, and past patient interactions.
- Native support for **vector search**, grounding LLM responses in verified medical sources.  

---

## AI Agent Implementation (LangGraph)

The AI agent is implemented using **LangGraph**, providing a flexible, node-based workflow for LLM-driven reasoning.  
It is connected to:

- The patient’s **medical history**, and  
- A **diagnostic guideline database** curated by healthcare professionals.

This design **reduces hallucinations** and ensures outputs remain **clinically grounded**, improving safety and trust in AI-driven medical assistance.

---

## Demo Mode and Final Implementation

- **Demo:** The prototype uses pre-fetched text directly injected into prompts for simplicity and reproducibility.  
- **Final Implementation:** The production system will dynamically query the **Cosmos DB vector index** to retrieve only the most relevant context chunks at runtime — enabling **efficient, evidence-backed, and context-aware responses with minimized hallucinations**. Eliminating hallucinations is always important, especially when the LLM is used for sensitive medical purposes.


---
## Future improvements
- The current implementation is only for Android. The app can be developed for any mobile platform and smart watch technology.
- User's interaction with the assistant is only through a chat. Future iterations will include an integrated STT model that will make communication with the LLM more intuitive and convinient.



