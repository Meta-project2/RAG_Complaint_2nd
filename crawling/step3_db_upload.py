import pandas as pd
import psycopg2
from psycopg2.extras import execute_values
import glob
import os
from tqdm import tqdm

# DB 정보
DB_CONFIG = {
    "host": "localhost",
    "database": "complaint_db",
    "user": "postgres",
    "password": "0000",
    "port": "5432"
}

def upload_to_db_bulk():
    conn = None
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        cursor = conn.cursor()
        print("[*] DB 연결 성공. Bulk 업로드를 시작합니다.")

        vector_files = glob.glob("data/step2_vectors/*.parquet")

        for file_path in vector_files:
            df = pd.read_parquet(file_path)
            # 벡터를 리스트 형태로 변환 (DB 저장용)
            df['embedding'] = df['embedding'].apply(lambda x: x.tolist())
            
            print(f"\n[*] {os.path.basename(file_path)} 처리 중 (총 {len(df)}건)...")

            # 대량 처리를 위해 데이터를 일정한 크기(예: 100개)로 나눕니다.
            chunk_size = 100
            for i in tqdm(range(0, len(df), chunk_size), desc="  └ Bulk Inserting"):
                chunk = df.iloc[i : i + chunk_size]
                
                # 1. complaints 테이블에 대량 삽입하고 생성된 ID들을 받아옴
                # RETURNING id를 사용하면 삽입된 순서대로 ID가 반환됩니다.
                complaints_data = [
                    (row['req_title'], row['req_content'], row['resp_content'], row['resp_dept'])
                    for _, row in chunk.iterrows()
                ]
                
                insert_complaints_query = """
                    INSERT INTO complaints (received_at, title, body, answer, district, status, created_at, updated_at)
                    VALUES (NOW(), %s, %s, %s, %s, 'CLOSED', NOW(), NOW())
                    RETURNING id;
                """
                
                # execute_values 대신 여기서는 ID를 순서대로 받기 위해 가공된 쿼리 사용
                ids = []
                for data in complaints_data:
                    cursor.execute(insert_complaints_query, data)
                    ids.append(cursor.fetchone()[0])

                # 2. 받아온 ID들과 함께 complaint_normalizations 테이블에 대량 삽입
                # 이 부분은 ID와 매칭된 정보를 한꺼번에 넣습니다.
                norm_data = []
                for idx, (_, row) in enumerate(chunk.iterrows()):
                    norm_data.append((
                        ids[idx],              # 위에서 받은 complaint_id
                        row['processed_body'], # 요약
                        row['embedding'],      # 벡터
                        True                   # is_current
                    ))

                insert_norm_query = """
                    INSERT INTO complaint_normalizations (complaint_id, neutral_summary, embedding, is_current, created_at)
                    VALUES %s
                """
                
                # execute_values는 진짜 Bulk Insert 기능을 수행합니다.
                execute_values(cursor, insert_norm_query, norm_data)
                
                # 한 덩어리(chunk) 처리할 때마다 커밋
                conn.commit()

            print(f"[+] {os.path.basename(file_path)} 완료!")

    except Exception as e:
        print(f"[!] 오류 발생: {e}")
        if conn: conn.rollback()
    finally:
        if conn: conn.close()

if __name__ == "__main__":
    upload_to_db_bulk()