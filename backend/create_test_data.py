from app import SessionLocal, Question, Answer, ChatMessage, today, generate_daily_questions
from datetime import datetime, timedelta
import random

# Sample answers for different question types
SAMPLE_ANSWERS = [
    "I feel good today, no major issues",
    "Slight discomfort in the morning, but improved throughout the day",
    "Energy level is about 7/10",
    "Sleep was restful, about 8 hours",
    "Appetite is normal, eating regularly",
    "No new symptoms observed",
    "Pain level is around 3/10",
    "Feeling better than yesterday",
    "Some fatigue in the afternoon",
    "Overall condition is stable",
]

def create_test_data_for_date(user_id: int, days_ago: int = 1):
    """
    Create test questions and answers for a specific date.
    """
    db = SessionLocal()
    
    try:
        # Calculate the target date
        target_date = (datetime.now() - timedelta(days=days_ago)).date()
        print(f"\nCreating test data for {target_date} (user_id={user_id})...")
        
        # Check if questions already exist for this date
        existing = db.query(Question).filter(
            Question.user_id == user_id,
            Question.q_date == target_date
        ).first()
        
        if existing:
            print(f"Questions already exist for {target_date}. Skipping generation.")
            return
        
        # Generate questions using GPT
        print("Generating questions via GPT...")
        questions_dict = generate_daily_questions("General health monitoring for testing")
        
        # Sort questions by key (Q1, Q2, etc.)
        sorted_keys = sorted(questions_dict.keys(), key=lambda k: int(k[1:]))
        
        # Create questions in database
        question_ids = []
        for idx, key in enumerate(sorted_keys):
            q = Question(
                user_id=user_id,
                text=questions_dict[key],
                q_date=target_date,
                order_index=idx,
                source="daily",
                asked_at=datetime.combine(target_date, datetime.min.time()) + timedelta(hours=9, minutes=idx*5),
                created_at=datetime.combine(target_date, datetime.min.time()) + timedelta(hours=9)
            )
            db.add(q)
            db.flush()  # Get the ID
            question_ids.append((q.id, q.text))
        
        db.commit()
        print(f"  Created {len(question_ids)} questions")
        
        # Create mock answers for each question
        print("\nCreating mock answers...")
        for idx, (q_id, q_text) in enumerate(question_ids):
            # Pick a random answer
            answer_text = random.choice(SAMPLE_ANSWERS)
            
            # Create answer
            answer = Answer(
                user_id=user_id,
                question_id=q_id,
                text=answer_text,
                created_at=datetime.combine(target_date, datetime.min.time()) + timedelta(hours=9, minutes=idx*5+2)
            )
            db.add(answer)
            
            # Create chat messages (question + answer)
            # Question message
            db.add(ChatMessage(
                user_id=user_id,
                role="assistant",
                content=q_text,
                question_id=q_id,
                m_date=target_date,
                created_at=datetime.combine(target_date, datetime.min.time()) + timedelta(hours=9, minutes=idx*5)
            ))
            
            # Answer message
            db.add(ChatMessage(
                user_id=user_id,
                role="user",
                content=answer_text,
                question_id=q_id,
                m_date=target_date,
                created_at=datetime.combine(target_date, datetime.min.time()) + timedelta(hours=9, minutes=idx*5+2)
            ))
            
            print(f"  A{idx+1}: {answer_text}")
        
        db.commit()
        print(f"\n✅ Successfully created test data for {target_date}!")
        print(f"   - {len(question_ids)} questions")
        print(f"   - {len(question_ids)} answers")
        print(f"   - {len(question_ids)*2} chat messages")
        
    except Exception as e:
        db.rollback()
        print(f"❌ Error: {e}")
        raise
    finally:
        db.close()


if __name__ == "__main__":
    import sys
    
    user_id = 1
    
    # Get days_ago from command line or default to 1 (yesterday)
    days_ago = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    
    print("="*60)
    print("Creating Test Data for Patient Health Monitoring")
    print("="*60)
    
    create_test_data_for_date(user_id, days_ago)
    
    print("\n" + "="*60)
    print("Done! You can now test PDF generation with this data.")
    print("="*60)
