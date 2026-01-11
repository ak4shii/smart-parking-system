import base64
import binascii
import os
import time
import uuid
import boto3
from botocore.exceptions import BotoCoreError, ClientError
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv
from processor import process_image
from database import save_plate

load_dotenv()

app = FastAPI()

S3_BUCKET = os.getenv("S3_BUCKET_NAME")
S3_REGION = os.getenv("AWS_REGION", "us-east-1")
S3_PREFIX = os.getenv("S3_PREFIX", "").strip("/")

if not S3_BUCKET:
    raise RuntimeError("S3_BUCKET_NAME is not configured in environment/.env")

s3_client = boto3.client("s3", region_name=S3_REGION)

class Base64ImageRequest(BaseModel):
    image_base64: str


@app.post("/upload")
async def upload_image(payload: Base64ImageRequest):
    try:
        raw_data = payload.image_base64.strip()

        if "," in raw_data:
            raw_data = raw_data.split(",", 1)[1]

        try:
            image_bytes = base64.b64decode(raw_data, validate=True)
        except (binascii.Error, ValueError):
            raise HTTPException(status_code=400, detail="Invalid base64 image data")

        if not image_bytes:
            raise HTTPException(status_code=400, detail="Image data is empty")

        timestamp = int(time.time())
        unique_id = uuid.uuid4().hex[:6]
        object_key = f"{timestamp}_{unique_id}.jpg"
        if S3_PREFIX:
            object_key = f"{S3_PREFIX}/{object_key}"

        try:
            s3_client.put_object(
                Bucket=S3_BUCKET,
                Key=object_key,
                Body=image_bytes,
                ContentType="image/jpeg",
            )
            print(f"Image uploaded to S3: s3://{S3_BUCKET}/{object_key}")
        except (BotoCoreError, ClientError) as s3_error:
            print(f"Failed to upload to S3: {s3_error}")
            raise HTTPException(status_code=500, detail="Failed to upload image to storage")

        plate_text = process_image(image_bytes)

        if plate_text and plate_text != "NOT_FOUND":
            save_plate(plate_text)
            return {
                "status": "success",
                "plate": plate_text,
                "saved_as": object_key,
                "timestamp": timestamp,
            }

        return {
            "status": "failed",
            "reason": "No plate detected",
            "saved_as": object_key,
        }

    except HTTPException:
        raise
    except Exception as e:
        print(f"Error during upload/processing: {e}")
        return {"status": "error", "message": str(e)}