from openai import OpenAI
import json
import os
from datetime import datetime, date
from typing import Optional, List
from sqlalchemy import create_engine, Column, Integer, String, Date, DateTime, ForeignKey, Text, and_, exists
from sqlalchemy.orm import declarative_base, sessionmaker, relationship, Session
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from dotenv import load_dotenv
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.pagesizes import LETTER
from reportlab.lib.units import inch

# Load environment variables from .env file
load_dotenv()


# --- JSON schema for strict dict output ---
DAILY_QUESTIONS_DICT_SCHEMA = {
    "type": "json_schema",
    "json_schema": {
        "name": "DailyQuestionSet",
        "schema": {
            "type": "object",
            "properties": {},
            "patternProperties": {
                r"^Q[1-9][0-9]*$": {"type": "string"}   # Q1, Q2, Q3, ...
            },
            "additionalProperties": False
        },
        "strict": True
    }
}

def generate_daily_questions(patient_description: str):
    import os
    KEY = os.getenv("OPENAI_API_KEY")
    client = OpenAI(api_key=KEY)

    system_prompt = """
You are a compassionate, observant, and medically knowledgeable AI assistant working alongside a physician.

Context:
A patient will provide a description of their medical condition, lifestyle, or ongoing symptoms. Based on this initial description, your goal is to design a personalized daily question set that can be used to monitor the patient’s health and detect meaningful changes over time.

Your responsibilities:
1. Generate between 6 daily questions that are absolutely essential. !!THE QUESTIONS MUST NOT BE TOO NUANCED!!
2. Focus on the most relevant physiological, behavioral, or symptom-based aspects of the condition described.
3. Ensure that the questions are clear, concise, and patient-friendly — phrased in a natural way that a patient could answer comfortably every day.
4. Maintain clinical value: each question should be useful for trend detection (e.g., worsening pain, increased fatigue, fluctuations in appetite, changes in sleep quality, emotional wellbeing, etc.).
5. Use empathetic and non-alarming language, encouraging honest and consistent responses.
6. Avoid medical jargon unless absolutely necessary, and prefer phrasing like “Have you noticed…” or “How often have you…” instead of rigid clinical terms.

Output expectations:
Return your result in valid JSON format.
Each question should be keyed as “Q1”, “Q2”, etc.
Example:
{
  "Q1": "Have you experienced any pain or discomfort since yesterday?",
  "Q2": "Did you sleep better, worse, or about the same compared to the previous night?",
  "Q3": "How would you rate your energy level today on a scale of 1 to 10?",
  "Q4": "Have you noticed any changes in your appetite or eating patterns?"
}

Your goal:
Produce questions that can be used daily to track the progression, improvement, or stability of the patient’s condition. You are not diagnosing — you are supporting consistent data collection for long-term medical monitoring.
"""

    user_prompt = f"""
The patient described their current condition as follows:

{patient_description}

Based on this information, generate 6 personalized daily health monitoring questions.
"""

    resp = client.chat.completions.create(
        model="gpt-5-mini",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ],
        response_format=DAILY_QUESTIONS_DICT_SCHEMA,
        max_completion_tokens=5000
    )

    payload = json.loads(resp.choices[0].message.content)
    return payload




# JSON schema for stable output
FOLLOWUP_SCHEMA = {
    "type": "json_schema",
    "json_schema": {
        "name": "FollowupOutput",
        "schema": {
            "type": "object",
            "properties": {
                "questions": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "id":  {"type": "string", "description": "Q-id like Q1, Q2, ..."},
                            "text":{"type": "string", "description": "Follow-up question text"}
                        },
                        "required": ["id", "text"],
                        "additionalProperties": False
                    }
                }
            },
            "required": ["questions"],
            "additionalProperties": False
        },
        "strict": True
    }
}

def generate_followup_questions(answers: str, symptoms: str):
    KEY = os.getenv("OPENAI_API_KEY")
    if not KEY:
        raise RuntimeError("OPENAI_API_KEY not set")
    client = OpenAI(api_key=KEY)

    system_prompt = """
    You are a helpful and caring medical assistant AI.

    Your task:
    You will be given a numbered list of questions and their answers. These were written by a doctor to be answered regularly by the patient before each check-up.

    Your goal:
    Generate several intelligent follow-up questions that could help the doctor better understand the patient’s condition and symptom progression before the next visit.

    Guidelines:
    1. Use the provided disease symptom database (given in the user prompt) as your primary knowledge base.
       - The database will be in the following JSON format:
         {
           "disease_1": "1. Symptom one... 2. Symptom two...",
           "disease_2": "1. Symptom one... 2. Symptom two..."
         }

    2. Review the patient’s answers carefully.
       - Identify which symptoms or issues might need clarification or more detail.
       - Consider severity, frequency, duration, and new or changing symptoms.

    3. Generate follow-up questions that:
       - Are medically relevant and connected to the symptoms in the disease database.
       - Help gather context useful for a doctor’s clinical interpretation.
       - Are phrased empathetically and clearly for the patient to understand.

    4. Keep questions concise and specific (avoid generic “how are you feeling?” type questions).

    Output format:
    Return your results as:
    {
      "questions": [
        {"id": "Q1", "text": "..."},
        {"id": "Q2", "text": "..."}
      ]
    }
    """

    user_prompt = f"""
    Here are the doctor’s routine questions and the patient’s answers:

    {answers}

    Here is the symptom database that could be useful for question generation:
    {symptoms}

    Based on this information, generate up to 4 follow-up questions that would help the doctor better understand the patient’s symptom trends and condition.
    """

    resp = client.chat.completions.create(
        model="gpt-5-mini",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user",   "content": user_prompt}
        ],
        response_format=FOLLOWUP_SCHEMA,
        max_completion_tokens=5000
    )

    payload = json.loads(resp.choices[0].message.content)

    # Convert array to dict like {"Q1": "...", "Q2": "..."}
    questions_dict = {item["id"]: item["text"] for item in payload.get("questions", [])}
    return questions_dict




# Dict-only schema: keys must be Q1, Q2, ... ; values are strings
TREND_FOLLOWUP_DICT_SCHEMA = {
    "type": "json_schema",
    "json_schema": {
        "name": "TrendFollowupDict",
        "schema": {
            "type": "object",
            "properties": {},                 # no fixed keys
            "patternProperties": {
                r"^Q[1-9][0-9]*$": { "type": "string" }   # Q1, Q2, Q3, ...
            },
            "additionalProperties": False     # forbid any non-matching key
        },
        "strict": True
    }
}

def generate_trend_followups(answers_over_days: str):
    KEY = os.getenv("OPENAI_API_KEY")
    if not KEY:
        raise RuntimeError("OPENAI_API_KEY not set")
    client = OpenAI(api_key=KEY)
    system_prompt = """
You are a compassionate, observant, and medically knowledgeable AI assistant working alongside a physician.

Context:
You will receive a JSON-formatted record of a patient’s answers to routine monitoring questions collected over multiple days. 
Each question (Q1, Q2, etc.) contains timestamped answers (A1, A2, …) that reflect the patient’s condition and how it has changed over time.

Example structure:
{
  "Question1 text...": {
    "A1": "{Timestamp_1}, Answer text...",
    "A2": "{Timestamp_2}, Answer text...",
    ...
  },
  "Question2 text...": {
    "A1": "{Timestamp_1}, Answer text...",
    "A2": "{Timestamp_2}, Answer text...",
    ...
  }
}

Your goal:
1. Carefully analyze trends, shifts, or anomalies in the patient’s responses across days.
2. Identify anything unusual, worsening, or clinically interesting.
3. Generate 2 **insightful, medically relevant follow-up questions** that would help the doctor understand the change in the patient’s condition.

Guidelines:
- Only generate follow-up questions when there is something meaningful to ask about (e.g., a symptom worsening, new discomfort, or inconsistent responses).
- Each question should be **clear, empathetic, and medically sound** — phrased as if written by a professional nurse or physician’s assistant.
- Avoid repeating the original doctor’s questions.
- If nothing significant is detected, return an empty JSON (no follow-ups).

Output format:
Return your follow-up questions in **valid JSON**, for example:
{
  "Q1": "Have your shortness of breath episodes become more frequent over the last few days?",
  "Q2": "You mentioned dizziness worsening yesterday — has it persisted today?"
}

Be concise, context-aware, and human-centered — your job is to notice subtle medical changes and ask the right next questions.
"""
    user_prompt = f"""
    Patient timeline (multiple days of answers, JSON-structured):

    {answers_over_days}

    If nothing significant is detected, return an empty JSON: {{}}
    Otherwise, return a dictionary mapping Q-ids to follow-up question strings.
    """

    resp = client.chat.completions.create(
        model="gpt-5-mini",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user",   "content": user_prompt}
        ],
        response_format=TREND_FOLLOWUP_DICT_SCHEMA,  # strict dict schema
        max_completion_tokens=5000
    )

    payload = json.loads(resp.choices[0].message.content)  # dict like {"Q1": "..."} or {}
    return payload



from fastapi import FastAPI, HTTPException, Depends, Query
from pydantic import BaseModel

app = FastAPI(
    title="Health assistant AI",
    description="Personalized daily health monitoring for patients",
    version="1.0"
)

# CORS for mobile testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Database setup (SQLite by default) ---
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./symptom.db")
engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {},
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


class Question(Base):
    __tablename__ = "questions"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, index=True, nullable=False)
    text = Column(Text, nullable=False)
    q_date = Column(Date, index=True, nullable=False)
    order_index = Column(Integer, nullable=False, default=0)
    source = Column(String(20), nullable=False, default="daily")
    asked_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    answers = relationship("Answer", back_populates="question")


class Answer(Base):
    __tablename__ = "answers"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, index=True, nullable=False)
    question_id = Column(Integer, ForeignKey("questions.id"), nullable=False)
    text = Column(Text, nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    question = relationship("Question", back_populates="answers")


class ChatMessage(Base):
    __tablename__ = "chat_messages"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, index=True, nullable=False)
    role = Column(String(16), nullable=False)  # assistant|user
    content = Column(Text, nullable=False)
    question_id = Column(Integer, ForeignKey("questions.id"), nullable=True)
    m_date = Column(Date, index=True, nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)


Base.metadata.create_all(bind=engine)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def today() -> date:
    return datetime.utcnow().date()


def ensure_todays_questions(db: Session, user_id: int, patient_description: Optional[str] = None):
    existing = (
        db.query(Question)
        .filter(Question.user_id == user_id, Question.q_date == today())
        .order_by(Question.order_index.asc())
        .all()
    )
    if existing:
        return existing

    desc = patient_description or "General daily health check"
    generated = generate_daily_questions(desc)
    items = []
    for idx, k in enumerate(sorted(generated.keys(), key=lambda kk: int(kk[1:]))):
        q = Question(
            user_id=user_id,
            text=generated[k],
            q_date=today(),
            order_index=idx,
            source="daily",
        )
        db.add(q)
        items.append(q)
    db.commit()
    for q in items:
        db.refresh(q)
    return (
        db.query(Question)
        .filter(Question.user_id == user_id, Question.q_date == today())
        .order_by(Question.order_index.asc())
        .all()
    )


def next_unanswered_question(db: Session, user_id: int):
    q = (
        db.query(Question)
        .filter(Question.user_id == user_id, Question.q_date == today())
        .filter(~exists().where(and_(Answer.question_id == Question.id, Answer.user_id == user_id)))
        .order_by(Question.order_index.asc())
        .first()
    )
    return q

# --- Request body model ---
class PatientDescription(BaseModel):
    description: str

    
@app.post("/generate_daily_questions")
async def generate_questions(data: PatientDescription):
    """
    POST endpoint for generating daily health monitoring questions.
    """
    try:
        result = generate_daily_questions(data.description)
        return {"status": "success", "questions": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# --- Admin utilities for testing ---
class SeedQuestionsRequest(BaseModel):
    user_id: int
    questions: list[str]
    reset_today: bool = True


@app.post("/admin/seed_questions")
async def admin_seed_questions(payload: SeedQuestionsRequest, db: Session = Depends(get_db)):
    try:
        d = today()
        if payload.reset_today:
            # delete today's questions and their answers/messages for this user
            todays_qs = (
                db.query(Question).filter(Question.user_id == payload.user_id, Question.q_date == d).all()
            )
            q_ids = [q.id for q in todays_qs]
            if q_ids:
                db.query(Answer).filter(Answer.user_id == payload.user_id, Answer.question_id.in_(q_ids)).delete(synchronize_session=False)
                db.query(ChatMessage).filter(ChatMessage.user_id == payload.user_id, ChatMessage.m_date == d).delete(synchronize_session=False)
                db.query(Question).filter(Question.user_id == payload.user_id, Question.q_date == d).delete(synchronize_session=False)
                db.commit()

        # insert new questions
        for idx, text in enumerate(payload.questions):
            db.add(Question(
                user_id=payload.user_id,
                text=text,
                q_date=today(),
                order_index=idx,
                source="daily",
            ))
        db.commit()
        return {"status": "success", "inserted": len(payload.questions)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


class ResetTodayRequest(BaseModel):
    user_id: int


@app.post("/admin/reset_today")
async def admin_reset_today(payload: ResetTodayRequest, db: Session = Depends(get_db)):
    try:
        d = today()
        todays_qs = (
            db.query(Question).filter(Question.user_id == payload.user_id, Question.q_date == d).all()
        )
        q_ids = [q.id for q in todays_qs]
        if q_ids:
            db.query(Answer).filter(Answer.user_id == payload.user_id, Answer.question_id.in_(q_ids)).delete(synchronize_session=False)
        db.query(ChatMessage).filter(ChatMessage.user_id == payload.user_id, ChatMessage.m_date == d).delete(synchronize_session=False)
        db.query(Question).filter(Question.user_id == payload.user_id, Question.q_date == d).delete(synchronize_session=False)
        db.commit()
        return {"status": "success"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# --- Request body model ---
class FollowupRequest(BaseModel):
    answers: str
    symptoms: str


@app.post("/generate_followup_questions")
async def generate_followup(data: FollowupRequest):
    """
    POST endpoint for generating intelligent medical follow-up questions.
    """
    try:
        result = generate_followup_questions(data.answers, data.symptoms)
        return {"status": "success", "followup_questions": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# --- Request body ---
class TrendRequest(BaseModel):
    answers_over_days: str



@app.post("/generate_trend_followups")
async def trend_followups_api(data: TrendRequest):
    try:
        result = generate_trend_followups(data.answers_over_days)
        return {"status": "success", "trend_followup_questions": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))



# --- Session-scoped generation (client persists locally) ---
class SessionQuestionGenRequest(BaseModel):
    patient_description: str | None = None


class SessionQuestionItem(BaseModel):
    order: int
    id: str
    text: str


@app.post("/sessions/{session_id}/generate_daily_questions")
async def session_generate_daily_questions(session_id: str, payload: SessionQuestionGenRequest):
    try:
        desc = payload.patient_description or "General daily health check"
        result = generate_daily_questions(desc)  # dict {"Q1": "...", ...}
        # Return a stable, ordered list so client can store orderIndex
        items = []
        for k in sorted(result.keys(), key=lambda kk: int(kk[1:])):
            order = int(k[1:])
            items.append({"order": order, "id": k, "text": result[k]})
        return {
            "status": "success",
            "session_id": session_id,
            "questions": items,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# --- Initialize daily session (generate + store questions) ---
class InitSessionRequest(BaseModel):
    user_id: int = 1
    patient_description: Optional[str] = None


@app.post("/init_daily_session")
async def init_daily_session(payload: InitSessionRequest, db: Session = Depends(get_db)):
    try:
        user_id = payload.user_id
        d = today()
        
        # Check if questions already exist for today
        existing = db.query(Question).filter(Question.user_id == user_id, Question.q_date == d).first()
        if existing:
            return {"status": "already_initialized", "message": "Questions already exist for today"}
        
        # Generate questions via GPT
        desc = payload.patient_description or "General daily health check"
        generated = generate_daily_questions(desc)
        
        # Store in database
        for idx, key in enumerate(sorted(generated.keys(), key=lambda k: int(k[1:]))):
            db.add(Question(
                user_id=user_id,
                text=generated[key],
                q_date=d,
                order_index=idx,
                source="daily",
            ))
        db.commit()
        
        return {"status": "success", "message": f"Generated and stored {len(generated)} questions"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# --- Chat flow endpoints ---
class NextQuestionRequest(BaseModel):
    user_id: int
    patient_description: Optional[str] = None


class NextQuestionResponse(BaseModel):
    question_id: int
    text: str


@app.post("/chat/next-question", response_model=NextQuestionResponse)
async def get_next_question(payload: NextQuestionRequest, db: Session = Depends(get_db)):
    try:
        ensure_todays_questions(db, payload.user_id, payload.patient_description)
        q = next_unanswered_question(db, payload.user_id)
        if not q:
            raise HTTPException(status_code=204, detail="No more questions for today")

        if not q.asked_at:
            q.asked_at = datetime.utcnow()
            db.add(ChatMessage(
                user_id=payload.user_id,
                role="assistant",
                content=q.text,
                question_id=q.id,
                m_date=today(),
            ))
            db.commit()
            db.refresh(q)

        return NextQuestionResponse(question_id=q.id, text=q.text)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


class AnswerRequest(BaseModel):
    user_id: int
    question_id: int
    answer_text: str


@app.post("/chat/answer")
async def submit_answer(payload: AnswerRequest, db: Session = Depends(get_db)):
    try:
        q = db.query(Question).filter(Question.id == payload.question_id, Question.user_id == payload.user_id).first()
        if not q:
            raise HTTPException(status_code=404, detail="Question not found for user")

        db.add(Answer(user_id=payload.user_id, question_id=q.id, text=payload.answer_text))
        db.add(ChatMessage(user_id=payload.user_id, role="user", content=payload.answer_text, question_id=q.id, m_date=today()))
        db.commit()
        return {"status": "success"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/chat/messages")
async def get_messages(
    user_id: int = Query(...),
    for_date: Optional[str] = Query(None),
    db: Session = Depends(get_db),
):
    try:
        if for_date:
            try:
                d = datetime.strptime(for_date, "%Y-%m-%d").date()
            except ValueError:
                raise HTTPException(status_code=400, detail="Invalid date format. Use YYYY-MM-DD")
        else:
            d = today()

        msgs = (
            db.query(ChatMessage)
            .filter(ChatMessage.user_id == user_id, ChatMessage.m_date == d)
            .order_by(ChatMessage.created_at.asc())
            .all()
        )
        return [
            {
                "id": m.id,
                "role": m.role,
                "content": m.content,
                "question_id": m.question_id,
                "created_at": m.created_at.isoformat() + "Z",
            }
            for m in msgs
        ]
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# --- PDF Report Generation ---
class GenerateReportRequest(BaseModel):
    user_id: int = 1
    start_date: Optional[str] = None  # YYYY-MM-DD
    end_date: Optional[str] = None    # YYYY-MM-DD


@app.post("/generate_report_json")
async def generate_report_json(
    payload: GenerateReportRequest,
    db: Session = Depends(get_db)
):
    """
    Generate JSON structure for PDF report from database.
    Format: {"Question text": {"A1": "timestamp, answer", "A2": ...}, ...}
    """
    try:
        user_id = payload.user_id
        
        # Parse date range
        if payload.start_date:
            start_d = datetime.strptime(payload.start_date, "%Y-%m-%d").date()
        else:
            start_d = date(2000, 1, 1)  # far past
        
        if payload.end_date:
            end_d = datetime.strptime(payload.end_date, "%Y-%m-%d").date()
        else:
            end_d = today()
        
        # Get all questions in date range
        questions = (
            db.query(Question)
            .filter(
                Question.user_id == user_id,
                Question.q_date >= start_d,
                Question.q_date <= end_d
            )
            .order_by(Question.q_date.asc(), Question.order_index.asc())
            .all()
        )
        
        if not questions:
            return {"status": "no_data", "message": "No questions found in date range"}
        
        # Build the structure
        result = {}
        
        for q in questions:
            q_text = q.text
            if q_text not in result:
                result[q_text] = {}
            
            # Get answers for this question
            answers = (
                db.query(Answer)
                .filter(Answer.question_id == q.id, Answer.user_id == user_id)
                .order_by(Answer.created_at.asc())
                .all()
            )
            
            for idx, ans in enumerate(answers, start=1):
                timestamp = ans.created_at.strftime("%Y-%m-%d %H:%M:%S")
                result[q_text][f"A{idx}"] = f"{timestamp}, {ans.text}"
        
        return {
            "status": "success",
            "data": result,
            "date_range": {
                "start": start_d.isoformat(),
                "end": end_d.isoformat()
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/generate_report_pdf")
async def generate_report_pdf(
    payload: GenerateReportRequest,
    db: Session = Depends(get_db)
):
    """
    Generate PDF report from database and return file for download.
    """
    try:
        user_id = payload.user_id
        
        # Parse date range
        if payload.start_date:
            start_d = datetime.strptime(payload.start_date, "%Y-%m-%d").date()
        else:
            start_d = date(2000, 1, 1)
        
        if payload.end_date:
            end_d = datetime.strptime(payload.end_date, "%Y-%m-%d").date()
        else:
            end_d = today()
        
        # Get all questions in date range
        questions = (
            db.query(Question)
            .filter(
                Question.user_id == user_id,
                Question.q_date >= start_d,
                Question.q_date <= end_d
            )
            .order_by(Question.q_date.asc(), Question.order_index.asc())
            .all()
        )
        
        if not questions:
            raise HTTPException(status_code=404, detail="No data found in date range")
        
        # Build the JSON structure organized by DATE first, then questions
        json_data = {}
        
        for q in questions:
            q_date = q.q_date.isoformat()  # Date as key
            q_text = q.text
            
            if q_date not in json_data:
                json_data[q_date] = {}
            
            answers = (
                db.query(Answer)
                .filter(Answer.question_id == q.id, Answer.user_id == user_id)
                .order_by(Answer.created_at.asc())
                .all()
            )
            
            # Store question and its answer for this date
            if answers:
                json_data[q_date][q_text] = answers[0].text  # One answer per question per day
        
        # Generate PDF using GPT analysis
        pdf_filename = f"patient_report_{user_id}_{end_d.isoformat()}.pdf"
        pdf_path = os.path.join(os.getcwd(), pdf_filename)
        
        # Call GPT to analyze and generate report
        KEY = os.getenv("OPENAI_API_KEY")
        if not KEY:
            raise HTTPException(status_code=500, detail="OPENAI_API_KEY not configured")
        
        client = OpenAI(api_key=KEY)
        json_string = json.dumps(json_data, indent=2)
        
        system_prompt = """
You are a compassionate, observant, and medically knowledgeable AI assistant working alongside a physician.

Context:
You will receive a JSON-formatted record of a patient's daily health monitoring answers organized by date. Each date contains the patient's responses to routine health questions for that day.

Your goal:
Generate a concise, medically informative report that summarizes the patient's day-by-day health progression for the physician to review.

Your responsibilities:
- Analyze the patient's condition day by day, noting any changes or patterns
- Highlight worsening, improving, or stable symptoms across days
- Note any concerning trends or significant changes
- Provide clear, structured insight organized chronologically
- Maintain a professional, empathetic tone. No diagnosis.

Output expectations:
Produce structured text ready for PDF, using the following sections:

**Patient Health Monitoring Report**

**1. Overview**
<1-2 short paragraphs summarizing the overall health trend across all days>

**2. Daily Progression**
For each date, provide a brief summary:

**Date: YYYY-MM-DD**
- Key observations from that day's responses
- Notable symptoms or changes
- Overall condition assessment

**3. Trends and Patterns**
<Identify patterns across multiple days>

**4. Recommendations**
<2-4 concise points the physician should address or monitor>

Keep it clear, clinical, and organized by date.
"""
        
        user_prompt = f"""
Patient timeline (JSON):
{json_string}
"""
        
        resp = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": system_prompt.strip()},
                {"role": "user", "content": user_prompt.strip()}
            ],
            max_completion_tokens=8000
        )
        
        report_text = resp.choices[0].message.content.strip()
        
        # Build PDF
        styles = getSampleStyleSheet()
        body = ParagraphStyle(
            "Body",
            parent=styles["BodyText"],
            leading=14,
            spaceAfter=8
        )
        heading = ParagraphStyle(
            "Heading",
            parent=styles["Heading2"],
            spaceBefore=6,
            spaceAfter=6
        )
        title = ParagraphStyle(
            "Title",
            parent=styles["Heading1"],
            alignment=1,
            spaceAfter=12
        )
        
        def to_flow(report: str):
            flow = []
            chunks = [c.strip() for c in report.split("\n\n") if c.strip()]
            for block in chunks:
                if block.startswith("**") and block.endswith("**") and len(block) > 4:
                    flow.append(Paragraph(block.strip("* "), title))
                    flow.append(Spacer(1, 0.15 * inch))
                    continue
                
                lines = block.splitlines()
                if lines and lines[0].startswith("**") and lines[0].endswith("**"):
                    flow.append(Paragraph(lines[0].strip("* "), heading))
                    rest = "\n".join(lines[1:]).strip()
                    if rest:
                        flow.append(Paragraph(rest.replace("\n", "<br/>"), body))
                    flow.append(Spacer(1, 0.12 * inch))
                else:
                    flow.append(Paragraph(block.replace("\n", "<br/>"), body))
            return flow
        
        doc = SimpleDocTemplate(
            pdf_path,
            pagesize=LETTER,
            leftMargin=0.9 * inch,
            rightMargin=0.9 * inch,
            topMargin=0.8 * inch,
            bottomMargin=0.8 * inch,
            title="Patient Summary Report",
            author="Health Assistant AI"
        )
        
        flowables = to_flow(report_text)
        if not flowables:
            flowables = [Paragraph("Patient Summary Report", title)]
        
        doc.build(flowables)
        
        # Return the PDF file for download
        return FileResponse(
            path=pdf_path,
            media_type="application/pdf",
            filename=pdf_filename,
            headers={"Content-Disposition": f"attachment; filename={pdf_filename}"}
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="127.0.0.1", port=8000, reload=True)
