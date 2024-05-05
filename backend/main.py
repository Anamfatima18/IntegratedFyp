import secrets
import sqlite3
from flask import Blueprint, Flask, request, jsonify
import database
import email_service
import os
import random
import hashlib
import csv
import os
import cv2
import numpy as np
from datetime import datetime
from flask import Flask, request, jsonify
from gaze_tracking import GazeTracking  # Assuming this is your gaze tracking library
from joblib import load
import numpy as np
import pandas as pd
from Features import extract_features
from Screening import process_image
from werkzeug.utils import secure_filename
from ScreeningRoutes import screening_results
from flask import jsonify, request
import hashlib
from ScreeningTable import insert_screening_data , insert_raw_screening_data
db_path = 'sqlite:///dyslexiadetect.db' 


app = Flask(__name__)


Initial_bp = Blueprint('main', __name__)
app.config['MAIL_SERVER'] = os.getenv('MAIL_SERVER')
app.config['MAIL_PORT'] = 465  # Using SSL
app.config['MAIL_USERNAME'] = os.getenv('MAIL_USERNAME')
app.config['MAIL_PASSWORD'] = os.getenv('MAIL_PASSWORD')
app.config['MAIL_USE_TLS'] = os.getenv('MAIL_USE_TLS') == 'True'
app.config['MAIL_USE_SSL'] = os.getenv('MAIL_USE_SSL') == 'True'
app.register_blueprint(screening_results)


email_service.init_email_service(app)  # Initialize email service with app
# print("Email service initialized")

@Initial_bp.route('/signup', methods=['POST'])
def signup():
    print("Received signup request")
    data = request.json
    print("Request data:", data)

    email = data.get('email')
    name = data.get('name')
    username = data.get('username')
    age = data.get('age')
    password = data.get('password')

    print("Signup Data - Email:", email, "Name:", name, "Username:", username, "Age:", age)

    # Hash password
    hashed_password = hashlib.sha256(password.encode()).hexdigest()
    # print("Hashed Password:", hashed_password)

    if database.add_user(email, name, username, age, hashed_password):
        otp = random.randint(100000, 999999)
        print("Generated OTP:", otp)
        database.store_otp(email, str(otp))  # Store OTP
        email_service.send_otp_email(app, email, otp)  # Send OTP via email
        print("OTP sent via email")
        return jsonify({'message': 'OTP sent to email'}), 200
    else:
        print("User already exists or invalid data")
        return jsonify({'message': 'User already exists or invalid data'}), 400

@Initial_bp.route('/verify', methods=['POST'])
def verify():
    print("Received verify request")
    data = request.json
    print("Verify Request Data:", data)

    email = data.get('email')
    otp = data.get('otp')

    print("Verify Data - Email:", email, "OTP:", otp)

    if database.verify_user(email, otp):
        print("User verified successfully")
        return jsonify({'message': 'User verified successfully'}), 200
    else:
        print("Invalid OTP or email")
        return jsonify({'message': 'Invalid OTP or email'}), 400
    


def generate_secure_token(user_id):
    # Generate a secure random token
    secure_token = secrets.token_hex(16)
    # Concatenate user_id and token
    token = f"{user_id}:{secure_token}"
    return token

####### SIGN IN #####

@Initial_bp.route('/signin', methods=['POST'])
def signin():
    data = request.json
    email = data.get('email')
    password = data.get('password')

    # Hash the password
    hashed_password = hashlib.sha256(password.encode()).hexdigest()

    # Authenticate the user and get the user_id
    user_id, auth_success = database.authenticate_user(email, hashed_password)  # Assuming this function is modified to return both user_id and authentication status

    if auth_success:
        # Generate a secure token that includes the user ID
        token = generate_secure_token(user_id)
        
        # Store the token in the database
        database.store_user_token(email, user_id, token)
        print(token)

        # Return the token and user ID to the client
        return jsonify({'message': 'Sign in successful', 'token': token, 'userId': user_id}), 200
    else:
        # If authentication fails, return an error message
        return jsonify({'message': 'Invalid credentials or user not verified'}), 401



@Initial_bp.route('/get_paragraph_initial', methods=['POST'])
def get_paragraph_initial():
    data = request.json
    age = data.get('age')
    print(age)
    # Determine age group based on age
    if age:
        if 4 <= age <= 6:
            age_group = '4-6'
        elif 7 <= age <= 9:
            age_group = '7-9'
        elif 10 <= age <= 12:
            age_group = '10-12'
        else:
            return jsonify({'error': 'Age not in valid range'}), 400

        paragraph, word_count = database.get_paragraph_initial(age_group)
        if paragraph:
            return jsonify({'paragraph': paragraph, 'word_count': word_count})
        else:
            return jsonify({'error': 'No paragraph found for this age group'}), 404
    else:
        return jsonify({'error': 'Age is required'}), 400


def get_paragraph_from_database(age_group, level):
    complexity = 'Easy' if level in [1, 2] else 'Difficult' if level in [3, 4] else None
    if complexity is None:
        return None

    query = """
    SELECT paragraph, word_count, theme
    FROM paragraphs
    WHERE age_group = ? AND complexity = ?
    ORDER BY RANDOM()
    LIMIT 1
    """
    
    try:
        # Open a new connection to the SQLite database
        conn = sqlite3.connect('dyslexiadetect.db')  # Update this with the actual database path
        cursor = conn.cursor()
        cursor.execute(query, (age_group, complexity))
        result = cursor.fetchone()
        cursor.close()
        conn.close()

        if result:
            return result
        else:
            return None
    except sqlite3.Error as e:
        print(f"An error occurred: {e}")
        return None

@Initial_bp.route('/get_paragraph', methods=['POST'])
def get_paragraph():
    data = request.json
    age = data.get('age')
    level = data.get('level')

    if not level or level not in [1, 2, 3, 4]:
        return jsonify({'error': 'Invalid or missing level'}), 400

    if age:
        age_group = ''
        if 4 <= age <= 6:
            age_group = '4-6'
        elif 7 <= age <= 9:
            age_group = '7-9'
        elif 10 <= age <= 12:
            age_group = '10-12'
        else:
            return jsonify({'error': 'Age not in valid range'}), 400

        paragraph_data = get_paragraph_from_database(age_group, level)
        if paragraph_data:
            paragraph, word_count, theme = paragraph_data
            return jsonify({'paragraph': paragraph, 'word_count': word_count, 'theme': theme})
        else:
            return jsonify({'error': 'No paragraph found for this age group and level'}), 404
    else:
        return jsonify({'error': 'Age is required'}), 400


    

#####   MAIN SCREENING #####


# Global variables for device screen width and height
device_screen_width = None
prev_pupil_data = {'left': None, 'right': None}
device_screen_height = None

# Directory setup for CSV storage
csv_directory = 'data'
csv_filename = 'new_gaze_data.csv'
csv_filepath = os.path.join(csv_directory, csv_filename)
if not os.path.exists(csv_directory):
    os.makedirs(csv_directory)

def write_to_csv(data):
    """Write the provided data to the CSV file."""
    file_exists = os.path.isfile(csv_filepath)
    is_empty = os.stat(csv_filepath).st_size == 0 if file_exists else False

    with open(csv_filepath, 'a', newline='') as csvfile:
        fieldnames = ['RecordingTime [ms]', 'Point of Regard Left X [px]', 'Point of Regard Left Y [px]', 'Point of Regard Right X [px]', 'Point of Regard Right Y [px]', 'gaze_direction', 'Category left', 'Category right']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        if not file_exists or is_empty:
            writer.writeheader()
        writer.writerow(data)
    
from datetime import datetime



def get_timestamp_in_milliseconds(request):
    """
    Extracts and converts the timestamp in milliseconds from the request body.

    Args:
        request (flask.Request): The Flask request object.

    Returns:
        int: The timestamp in milliseconds or None if not found in the body.
    """
    try:
        # Extract timestamp from the request body directly
        timestamp = request.get('timestamp')
        if timestamp is None:
            # Use current time as fallback if timestamp not found
            return int(datetime.now().timestamp() * 1000)
        return int(timestamp)
    except ValueError:
        print(f"Error: Invalid timestamp format.")
        return None


# Example usage (assuming your route has access to the request object)



@Initial_bp.route('/sendDeviceMetrics', methods=['POST'])
def receive_device_metrics():
    """Endpoint to receive device metrics like screen width and height."""
    global device_screen_width, device_screen_height
    data = request.json
    device_screen_width = data['width']
    device_screen_height = data['height']
    return jsonify({"message": "Metrics received successfully"}), 200

def calculate_euclidean_distance(p1, p2):
    if not p1 or not p2:
        return np.inf  # Return infinity if either point is missing
    return np.sqrt((p1[0] - p2[0]) ** 2 + (p1[1] - p2[1]) ** 2)

def calculate_status(current, previous, threshold=5):
    if not previous:
        return "Unknown"
    distance = calculate_euclidean_distance(current, previous)
    return "Fixation" if distance <= threshold else "Saccade"

# 
@Initial_bp.route('/process_frame', methods=['POST'])
def process_frame():
    try:
        if 'image' not in request.files or 'screenshot' not in request.files:
            return jsonify({'error': 'No file part'}), 400

        # timestamp = get_timestamp_in_milliseconds(request)
        timestamp = int(datetime.now().timestamp() * 1000)
        print("Timestamp generated on the server:", timestamp)

        # Your existing code continues here...
        message = save_screenshot_from_request(request , timestamp)
        print(message) 
        print(timestamp)
        
        frame_file = request.files['image']
        in_memory_file = frame_file.read()
        nparr = np.frombuffer(in_memory_file, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        process_image(frame)
        gaze = GazeTracking()
        gaze.refresh(frame)

        left_pupil = gaze.pupil_left_coords()
        right_pupil = gaze.pupil_right_coords()
        text = "Unable to determine gaze direction"
        if gaze.is_blinking():
            text = "Blinking"
        elif gaze.is_right():
            text = "Looking right"
        elif gaze.is_left():
            text = "Looking left"
        elif gaze.is_center():
            text = "Looking center"
        
        # Mocked device screen metrics
        device_screen_width = 1920
        device_screen_height = 1080
        
        # Scale factors for target resolution
        target_width = 1280
        target_height = 1024
        scaleX = target_width / device_screen_width
        scaleY = target_height / device_screen_height
        
        # Scale pupil coordinates
        scaled_left_x = int(left_pupil[0] * scaleX) if left_pupil else -1
        scaled_left_y = int(left_pupil[1] * scaleY) if left_pupil else -1
        scaled_right_x = int(right_pupil[0] * scaleX) if right_pupil else -1
        scaled_right_y = int(right_pupil[1] * scaleY) if right_pupil else -1
        
        # Calculate the status based on Euclidean distance
        threshold = 30  # Adjust this fixation threshold as needed (in pixels)
        status_L = calculate_status(left_pupil, prev_pupil_data['left'], threshold)
        status_R = calculate_status(right_pupil, prev_pupil_data['right'], threshold)
        
        # At the end of the process_frame function, before returning the response, add:
        prev_pupil_data['left'] = left_pupil
        prev_pupil_data['right'] = right_pupil

        token = request.headers.get('Authorization', '').split(" ")[1]
        user_id = token.split(":")[0]
        print(f"Token: {token}, User ID: {user_id}")
         
        # Raw data before scaling
        raw_data = {
            'recording_time': timestamp,
            'point_of_regard_left_x': left_pupil[0] if left_pupil else -1,  # Raw left pupil X
            'point_of_regard_left_y': left_pupil[1] if left_pupil else -1,  # Raw left pupil Y
            'point_of_regard_right_x': right_pupil[0] if right_pupil else -1,  # Raw right pupil X
            'point_of_regard_right_y': right_pupil[1] if right_pupil else -1,  # Raw right pupil Y
            'gaze_direction': text,
            'category_left': status_L,  # Assuming status_L can be determined without scaling
            'category_right': status_R,  # Assuming status_R can be determined without scaling
        }
   
        data = {
            'recording_time': timestamp,
            'point_of_regard_left_x': scaled_left_x, 
            'point_of_regard_left_y': scaled_left_y,
            'point_of_regard_right_x': scaled_right_x, 
            'point_of_regard_right_y': scaled_right_y,
            'gaze_direction': text,
            'category_left': status_L,
            'category_right': status_R,
        }
        print(f"Attempting to insert data for user {user_id}: {data}")
        insert_screening_data(user_id, data)  # Assuming this function is defined elsewhere
        print(f"Attempting to insert RAW data for user {user_id}: {raw_data}")
        insert_raw_screening_data(user_id, raw_data)  # Assuming this function is defined elsewhere
        return jsonify({'status': 'success', 'timestamp': timestamp})
    except Exception as e:
        print(f"Error: Failed to process frame and insert data: {e}")
        return jsonify({'error': f'Failed to insert data: {str(e)}'}), 500












# ------------------screenshot fucntion ------------------------

from flask import request
import os
from datetime import datetime
import werkzeug

def save_screenshot_from_request(req , timestamp):
    """
    Extracts a screenshot from the request and saves it in a directory based on the user ID.

    :param req: The request object from Flask
    :return: A message indicating the success/failure of the operation
    """
    try:
        # Check if the screenshot part is in the request
        if 'screenshot' not in req.files:
            return "Missing screenshot part in the request.", 400

        # Extract the screenshot file from the request
        screenshot_file = req.files['screenshot']
        
        # Ensure the file is not empty
        if screenshot_file.filename == '':
            return "No selected file.", 400

        # Ensure the file is a valid image
        if not (screenshot_file and allowed_file(screenshot_file.filename)):
            return "Invalid file format.", 400

        # Extract the user ID from the request's header
        token = request.headers.get('Authorization', '').split(" ")[1]
        user_id = token.split(":")[0]
        print(f"Token: {token}, User ID: {user_id}")

        # Define the directory path based on the user ID
        user_dir = os.path.join('data', user_id)
        os.makedirs(user_dir, exist_ok=True)  # Create the directory if it doesn't exist

        # Define the file path for the screenshot
        # timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
        screenshot_path = os.path.join(user_dir, f"{timestamp}.jpg")

        # Save the screenshot
        screenshot_file.save(screenshot_path)

        return f"Screenshot saved successfully at {screenshot_path}.", 200
    except Exception as e:
        return f"Error saving screenshot: {str(e)}", 500

def allowed_file(filename):
    """
    Checks if the uploaded file is allowed based on its extension.

    :param filename: The name of the fil
    :return: True if the file is allowed, False otherwise
    """
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@Initial_bp.before_request
def before_request_func():
    print("Received request: ", request.url, request.method)


# -------------------------------------------------------------------


# if __name__ == '__main__':
#     print("Starting Flask app")
#     database.create_database()  # Ensure the database is set up

#     print("Database created")
#     app.run(debug=True , host='0.0.0.0' , port='5000')

