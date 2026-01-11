import sqlite3
from datetime import datetime

DB_PATH = "/app/data/plates.db"

def init_db():
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute('''CREATE TABLE IF NOT EXISTS plate_logs 
                        (id INTEGER PRIMARY KEY AUTOINCREMENT, 
                         plate_text TEXT, 
                         timestamp DATETIME)''')

def save_plate(text):
    init_db()
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute("INSERT INTO plate_logs (plate_text, timestamp) VALUES (?, ?)", 
                     (text, datetime.now()))