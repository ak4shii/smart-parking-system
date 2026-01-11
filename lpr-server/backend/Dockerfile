FROM python:3.9-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Upgrade pip and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Pre-download PaddleOCR models 
# Fix: Removed use_gpu=False (it's now use_cuda or automatic)
# Added env var to skip model source check which often fails in Docker
RUN DISABLE_MODEL_SOURCE_CHECK=True python3 -c "from paddleocr import PaddleOCR; PaddleOCR(lang='en')"

COPY . .

EXPOSE 8000

# WRONG: CMD ["uvicorn", "main.py", "--host", "0.0.0.0", "--port", "8000"]
# CORRECT:
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]