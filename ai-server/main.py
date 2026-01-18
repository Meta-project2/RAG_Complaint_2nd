from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from app.services import llm_service
from app import database
from app.services.llm_service import LLMService
from fastapi.middleware.cors import CORSMiddleware
import requests
import os
import uuid
import os
import re
import json
import uuid
import requests
import textwrap
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from datetime import datetime
import google.generativeai as genai
from sqlalchemy import Integer, create_engine, Column, BigInteger, String, Text, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from sqlalchemy.dialects.postgresql import JSONB

app = FastAPI(title="Complaint Analyzer AI")
llm_service = LLMService()

# (CORS 설정)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],      # 모든 곳에서 접속 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 테스트
@app.get("/")
async def root():
    return {"message": "서버 연결 성공 "}

# 요청 데이터 구조 정의
class ChatRequest(BaseModel):
    query: str

# 민원 상세 화면 진입 시 (자동 분석 & 가이드)
@app.get("/api/complaints/{complaint_id}/ai-analysis")
async def get_ai_analysis(complaint_id: int):
    """
    [자동 모드]
    공무원이 민원을 클릭했을 때, DB에 있는 민원 내용을 바탕으로
    유사 사례 요약과 처리 방향 가이드를 자동으로 생성
    """
    try:
        # query 인자 없이 호출 -> llm_service 내부에서 '자동 모드'로 동작
        response = await llm_service.generate_rag_response(complaint_id)
        return {"status": "success", "result": response}
    except Exception as e:
        return {"status": "error", "message": f"AI 분석 실패: {str(e)}"}

# 챗봇에게 추가 질문하기 (Q&A)
@app.post("/api/complaints/{complaint_id}/chat")
async def chat_with_ai(complaint_id: int, request: ChatRequest):
    """
    [수동 모드]
    공무원이 채팅창에 질문(query)을 입력하면,
    해당 질문을 법률 용어로 변환 후 검색하여 답변
    """
    try:
        # query 인자 포함 호출 -> llm_service 내부에서 '수동 질문 모드'로 동작
        response = await llm_service.generate_rag_response(complaint_id, request.query)
        return {"status": "success", "result": response}
    except Exception as e:
        return {"status": "error", "message": f"답변 생성 실패: {str(e)}"}
    
@app.post("/api/complaints/analyze")
async def analyzeComplaints(title:str, body:str):
    api_key = 'sk-QoIqcyDiLSdNT-c7OBhfLV6WbkGNhVt1cdDuTzzrGyw'
    url = "http://localhost:7860/api/v1/run/59369f82-0d62-414e-bd20-9bc5f9aa8a50"  # The complete API endpoint URL for this flow

    print("Title: ", title)
    print("Body: ", body)

    # Request payload configuration
    payload = {
        "output_type": "chat",
        "input_type": "text",
        # [수정] 최상위 input_value를 채워주면 RAG 검색 정확도가 올라갑니다
        "input_value": f"TITLE: {title}\BODY: {body}", 
        "tweaks": {
            # 찾으신 ID를 정확히 매핑합니다
            "TextInput-MBAG3": {
                "input_value": title
            },
            "TextInput-NNDwa": {
                "input_value": body
            }
        }
    }
    payload["session_id"] = str(uuid.uuid4())

    headers = {"x-api-key": api_key}

    try:
        # Send API request
        response = requests.request("POST", url, json=payload, headers=headers)
        response.raise_for_status()  # Raise exception for bad status codes

        # Print response
        json_string_compact = json.dumps(response.text)
        print("--- 기본 출력 ---")
        print(json_string_compact)

    except requests.exceptions.RequestException as e:
        print(f"Error making API request: {e}")
    except ValueError as e:
        print(f"Error parsing response: {e}")
        

# DB 설정 (사용자, 비밀번호, 호스트, DB이름 수정 필요)
DATABASE_URL = "postgresql://postgres:sanghpw@localhost:5432/postgres"
engine = create_engine(DATABASE_URL)
try:
    with engine.connect() as conn:
        print("✅ DB 연결 성공! 주소:", DATABASE_URL)
except Exception as e:
    print("❌ DB 연결 실패! 주소를 확인하세요.")
    print("에러 내용:", e)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# Gemini 설정
genai.configure(api_key="AIzaSyCfF0yXHFw-WDVy-VSdJaZaAaIaWpLuSeA")
model = genai.GenerativeModel('gemini-2.0-flash', generation_config={"response_mime_type": "application/json"})

OLLAMA_URL = "http://localhost:11434/api/generate"
OLLAMA_MODEL = "gemma2:2b"

# --- DB 테이블 모델 ---
class ComplaintNormalization(Base):
    __tablename__ = "complaint_normalizations"

    id = Column(BigInteger, primary_key=True, index=True)
    complaint_id = Column(BigInteger, nullable=False)
    district_id = Column(Integer, nullable=True)
    neutral_summary = Column(Text)
    core_request = Column(Text)
    core_cause = Column(Text)
    target_object = Column(String(120))
    keywords_jsonb = Column(JSONB)
    location_hint = Column(String(255))
    resp_dept = Column(String(100))
    routing_rank = Column(JSONB)
    created_at = Column(DateTime, default=datetime.now)

# 테이블 생성
Base.metadata.create_all(bind=engine)

# --- 요청 데이터 모델 ---
class ComplaintRequest(BaseModel):
    id: int # 민원 PK
    title: str # 민원 제목
    body: str # 민원 본문
    addressText: str # 도로명 주소 (지도에서 변환된 값)
    # SQL의 DECIMAL(10,7)과 매핑되도록 BigDecimal 사용 권장
    lat: float # 위도
    lon: float # 경도
    # 추가로 필요한 정보들
    applicantId: int # 민원인 ID (Long)
    districtId: int # 발생 구역 ID (Long)

def masking_by_ollama(text):
    if not text or text.strip() == "": return ""
    prompt = f"""
    [Identity]
    당신은 공공기관의 개인정보 보호 전문가입니다. 입력된 민원 본문에서 민원의 핵심 내용(현상, 위치의 성격, 요구사항)은 유지하되, 개인을 식별할 수 있는 정보만 아래 규칙에 따라 마스킹하세요.

    [Masking Rules]
    1. 이름: [성함]으로 변경 (예: 홍길동 -> [성함])
    2. 전화번호: [연락처]로 변경 (예: 010-1234-5678 -> [연락처])
    3. 상세 주소: 구체적인 번지수, 아파트 동/호수는 [상세주소]로 변경 (예: 성내로 25 101동 -> 성내로 [상세주소])
    4. 주민등록번호/계좌번호: [개인식별번호]로 변경
    5. 기타 이메일, 생년월일 등: [개인정보]로 변경

    [Constraints]
    - 민원의 주제(예: 가로등 고장, 불법 주정차, 소음 등)와 관련된 단어는 절대 수정하지 마세요.
    - 인사말이나 감정 표현은 그대로 두되, 그 안의 개인정보만 가리세요.
    - 출력은 마스킹이 완료된 본문만 출력하고, "알겠습니다" 등의 부연 설명은 하지 마세요.

    [Input]
    {text}
    """ # 기존 프롬프트 사용
    try:
        payload = {"model": OLLAMA_MODEL, "prompt": prompt, "stream": False}
        response = requests.post(OLLAMA_URL, json=payload, timeout=40)
        return response.json().get('response', text).strip()
    except:
        return text # 실패 시 원본 혹은 Regex 결과 반환

@app.post("/api/complaints/preprocess")
async def preprocess_complaint(req: ComplaintRequest, request: Request):
    db = SessionLocal()
    body = await request.body()
    print(f"받은 원본 데이터: {body.decode()}")
    try:
        
        safe_title = masking_by_ollama(req.title)
        if safe_title is None: return None
        safe_content = masking_by_ollama(req.body)
        if safe_content is None: return None

        api_key = 'sk-pCYh_S9cW_DoJLmXZVkXgqtdw4yGrU7OJAq6A73eS58'
        url = "http://localhost:7860/api/v1/run/59369f82-0d62-414e-bd20-9bc5f9aa8a50"  # The complete API endpoint URL for this flow

        for i in req:
            print(i)

        # Request payload configuration
        payload = {
            "output_type": "chat",
            "input_type": "text",
            "tweaks": {
                # 찾으신 ID를 정확히 매핑합니다
                "TextInput-MBAG3": {
                    "input_value": safe_title
                },
                "TextInput-NNDwa": {
                    "input_value": safe_content
                }
            }
        }
        payload["session_id"] = str(uuid.uuid4())
        headers = {"x-api-key": api_key}

        # Send API request
        response = requests.request("POST", url, json=payload, headers=headers)
        response.raise_for_status()
        
        # 4. 결과 파싱 (Langflow 응답 구조에서 텍스트만 추출)
        result_json = response.json()
        ai_text = result_json['outputs'][0]['outputs'][0]['results']['message']['data']['text']
        
        print(f"AI 분석 완료: {ai_text}")
        
        # 성공 시 실제 AI 분석 결과를 반환
        return {
            "status": "success",
            "data": ai_text
        }
    except Exception as e:
        print(f"처리 중 오류 발생: {str(e)}")
        return {
            "status": "error",
            "message": str(e)
        }

        '''
        if isinstance(analysis, list):
            if len(analysis) > 0:
                analysis = analysis[0]
            else:
                raise ValueError("Gemini returned an empty list")

        # 3. DB 저장 (complaint_normalizations)
        norm_entry = ComplaintNormalization(
            complaint_id=req.id,
            district_id=3,
            neutral_summary=analysis.get('neutral_summary'),
            core_request=analysis.get('core_request'),
            core_cause=analysis.get('core_cause'),
            target_object=analysis.get('target_object'),
            keywords_jsonb=analysis.get('keywords'),
            location_hint=analysis.get('location_hint'),
            resp_dept=analysis.get('suggested_dept'),
            routing_rank={"primary": analysis.get('suggested_dept'), "confidence": "high"}
        )

        try:
            db.add(norm_entry)
            db.commit()      # 여기서 에러가 나면 except로 빠집니다.
            db.refresh(norm_entry) # DB에서 생성된 ID를 다시 읽어옴

            print(f"--- DB 저장 완료! 생성된 ID: {norm_entry.id}, 참조 민원ID: {req.id}")
        except Exception as e:
            db.rollback()
            # 🚩 에러 내용을 아주 상세하게 출력하도록 수정
            import traceback
            print("!!! DB 저장 에러 발생 !!!")
            print(traceback.format_exc()) 
        
            # 에러 발생 시 성공 응답을 보내지 말고 에러 응답을 보냄
            raise HTTPException(status_code=500, detail=f"DB Error: {str(e)}")

    except Exception as e:
        db.rollback()
        print(f"Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        db.close()
    '''
# 직접 실행을 위한 블록 (python main.py로 실행 가능)
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

