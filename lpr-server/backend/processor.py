import torch
import cv2
import numpy as np
from ultralytics import YOLO
from paddleocr import PaddleOCR
import logging

# Set logging to suppress unnecessary info messages
logging.getLogger("ppocr").setLevel(logging.ERROR)

# PyTorch 2.6+ Security Fix
try:
    from ultralytics.nn.tasks import DetectionModel

    torch.serialization.add_safe_globals([DetectionModel])
except Exception:
    pass

# CLEAN INITIALIZATION
# Removed 'use_gpu' and 'show_log' to fix Version 3.0+ errors
print("Loading AI Models... this may take a moment.")
ocr = PaddleOCR(use_angle_cls=True)
model = YOLO("weights/best.pt")
print("Models loaded successfully.")

def process_image(image_bytes):
    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    
    if img is None:
        return "DECODE_ERROR"

    # YOLO Detection
    results = model(img)
    
    for r in results:
        if len(r.boxes) > 0:
            box = r.boxes[0]
            x1, y1, x2, y2 = map(int, box.xyxy[0])
            
            # Crop logic
            h, w, _ = img.shape
            crop = img[max(0, y1-2):min(h, y2+2), max(0, x1-2):min(w, x2+2)]
            cv2.imwrite("data/crops/crop.jpg", crop)
            # OCR logic (No arguments here either)
            result = ocr.predict(crop)
            print(f"OCR Result: {result[0]['rec_texts']}")
            # Ensure data directory exists

         
            if result:
                text = ''.join(result[0]["rec_texts"])
                return text

    return "NOT_FOUND"