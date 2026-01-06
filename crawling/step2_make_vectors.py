import pandas as pd
import glob
import os
import torch
import warnings
from tqdm import tqdm
from sentence_transformers import SentenceTransformer

warnings.filterwarnings("ignore")

class VectorGenerator:
    def __init__(self):
        # 모델 로딩 (CPU 사용 시 truncate_dim 옵션 추천할 수도 있으나 일단 기본 유지)
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        print(f"[*] 장치: {self.device} / 모델 로딩 중 (mxbai-embed-large-v1)...")
        # 1024차원 모델
        self.model = SentenceTransformer("mixedbread-ai/mxbai-embed-large-v1", device=self.device)

    def make_combined_text(self, row):
        """
        [전략 변경]
        본문, 부서, 답변을 하나의 텍스트로 합쳐서 문맥을 풍부하게 만듭니다.
        """
        summary = row.get('processed_body', '')
        dept = row.get('resp_dept', '정보없음')
        answer = row.get('processed_answer', '')
        
        # 사용자가 원한 포맷대로 문자열 결합
        combined = f"[민원 요약]: {summary} [담당 부서]: {dept} [처리 결과]: {answer}"
        return combined

    def generate_vectors(self, file_path, output_dir):
        try:
            # Step 1 결과물 읽기
            df = pd.read_csv(file_path)
            df = df.fillna('')
            
            # 파일명 설정 (.parquet 사용)
            file_name = os.path.basename(file_path).replace(".csv", ".parquet")
            save_path = os.path.join(output_dir, file_name)

            print(f"\n[*] 벡터 생성 시작: {os.path.basename(file_path)} (총 {len(df)}건)")

            # 통합 텍스트 컬럼 생성
            # (apply 함수를 써서 미리 텍스트를 다 만들어둡니다)
            df['combined_text'] = df.apply(self.make_combined_text, axis=1)

            # 배치 사이즈 (CPU면 16, GPU면 64 추천)
            batch_size = 16
            
            all_embeddings = []

            # 텍스트 리스트 추출
            text_list = df['combined_text'].tolist()

            # 벡터 생성 루프
            for i in tqdm(range(0, len(text_list), batch_size), desc="  └ 통합 임베딩 계산 중"):
                batch_texts = text_list[i : i + batch_size]
                
                # ★ 변경: 하나의 텍스트 덩어리만 인코딩하면 됩니다.
                embeddings = self.model.encode(batch_texts, show_progress_bar=False)
                all_embeddings.extend(embeddings.tolist())

            # 데이터프레임에 벡터 및 통합 텍스트 저장
            df['embedding'] = all_embeddings
            
            # 불필요한 컬럼은 제거하고 저장해도 되지만, 일단 다 저장
            df.to_parquet(save_path, index=False)
            print(f"[+] 저장 완료: {save_path}")

        except Exception as e:
            print(f"[!] 오류 발생 ({file_path}): {e}")

if __name__ == "__main__":
    generator = VectorGenerator()
    output_dir = "data/step2_vectors"
    os.makedirs(output_dir, exist_ok=True)

    target_files = glob.glob("data/step1_output/*.csv")
    
    if not target_files:
        print("[!] Step 1 파일이 없습니다.")
    else:
        for file_path in target_files:
            generator.generate_vectors(file_path, output_dir)

    print("\n[+] 2단계(통합 벡터) 생성 완료!")