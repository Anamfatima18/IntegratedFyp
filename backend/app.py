import subprocess
from flask import Flask, request, jsonify
import database
import email_service
from google.cloud import speech
from google.cloud.speech import RecognitionAudio, RecognitionConfig
import os
import random
import logging
import hashlib
import tempfile
from ScreeningRoutes import screening_results
app = Flask(__name__)

# # Print environment variables
# # print("MAIL_SERVER:", os.getenv('MAIL_SERVER'))
# # print("MAIL_PORT:", os.getenv('MAIL_PORT'))
# # print("MAIL_USERNAME:", os.getenv('MAIL_USERNAME'))
# # print("MAIL_PASSWORD:", os.getenv('MAIL_PASSWORD'))
# # print("MAIL_USE_TLS:", os.getenv('MAIL_USE_TLS'))
# # print("MAIL_USE_SSL:", os.getenv('MAIL_USE_SSL'))

# app.config['MAIL_SERVER'] = os.getenv('MAIL_SERVER')
# app.config['MAIL_PORT'] = 465  # Using SSL
# app.config['MAIL_USERNAME'] = os.getenv('MAIL_USERNAME')
# app.config['MAIL_PASSWORD'] = os.getenv('MAIL_PASSWORD')
# app.config['MAIL_USE_TLS'] = os.getenv('MAIL_USE_TLS') == 'True'
# app.config['MAIL_USE_SSL'] = os.getenv('MAIL_USE_SSL') == 'True'

# # print("Configured MAIL_PORT:", app.config['MAIL_PORT'])
# # print("Configured MAIL_USE_TLS:", app.config['MAIL_USE_TLS'])
# # print("Configured MAIL_USE_SSL:", app.config['MAIL_USE_SSL'])

# email_service.init_email_service(app)  # Initialize email service with app
# # print("Email service initialized")

# @app.route('/signup', methods=['POST'])
# def signup():
#     print("Received signup request")
#     data = request.json
#     print("Request data:", data)

#     email = data.get('email')
#     name = data.get('name')
#     username = data.get('username')
#     age = data.get('age')
#     password = data.get('password')

#     print("Signup Data - Email:", email, "Name:", name, "Username:", username, "Age:", age)

#     # Hash password
#     hashed_password = hashlib.sha256(password.encode()).hexdigest()
#     # print("Hashed Password:", hashed_password)

#     if database.add_user(email, name, username, age, hashed_password):
#         otp = random.randint(100000, 999999)
#         print("Generated OTP:", otp)
#         database.store_otp(email, str(otp))  # Store OTP
#         email_service.send_otp_email(app, email, otp)  # Send OTP via email
#         print("OTP sent via email")
#         return jsonify({'message': 'OTP sent to email'}), 200
#     else:
#         print("User already exists or invalid data")
#         return jsonify({'message': 'User already exists or invalid data'}), 400

# @app.route('/verify', methods=['POST'])
# def verify():
#     print("Received verify request")
#     data = request.json
#     print("Verify Request Data:", data)

#     email = data.get('email')
#     otp = data.get('otp')

#     print("Verify Data - Email:", email, "OTP:", otp)

#     if database.verify_user(email, otp):
#         print("User verified successfully")
#         return jsonify({'message': 'User verified successfully'}), 200
#     else:
#         print("Invalid OTP or email")
#         return jsonify({'message': 'Invalid OTP or email'}), 400
    

# @app.route('/signin', methods=['POST'])
# def signin():
#     data = request.json
#     email = data.get('email')
#     password = data.get('password')

#     # Hash the input password to compare with the stored hash
#     hashed_password = hashlib.sha256(password.encode()).hexdigest()

#     user_id = database.authenticate_user(email, hashed_password)
#     if user_id:
#         # Assuming authenticate_user now returns user_id upon successful authentication
#         return jsonify({'message': 'Sign in successful', 'userId': user_id}), 200
#     else:
#         return jsonify({'message': 'Invalid credentials or user not verified'}), 401

    


# @app.route('/get_paragraph', methods=['POST'])
# def get_paragraph():
#     data = request.json
#     age = data.get('age')

#     # Determine age group based on age
#     if age:
#         if 4 <= age <= 6:
#             age_group = '4-6'
#         elif 7 <= age <= 8:
#             age_group = '7-8'
#         elif 9 <= age <= 12:
#             age_group = '9-12'
#         else:
#             return jsonify({'error': 'Age not in valid range'}), 400

#         paragraph, word_count = database.get_paragraph(age_group)
#         if paragraph:
#             return jsonify({'paragraph': paragraph, 'word_count': word_count})
#         else:
#             return jsonify({'error': 'No paragraph found for this age group'}), 404
#     else:
#         return jsonify({'error': 'Age is required'}), 400


######## Rhyming Activity ############
from main import Initial_bp
app.register_blueprint(Initial_bp)
app.register_blueprint(screening_results)

@app.route('/get_rhyming_task/<int:level>', methods=['GET'])
def get_rhyming_task(level):
    word, options = database.get_rhyming_task(level)
    if word:
        response = {'word': word, 'options': options}
        print(f"Sending response: {response}")  # Debugging log
        return jsonify(response), 200
    else:
        print("Sending error response: No task available for this level")  # Debugging log
        return jsonify({'message': 'No task available for this level'}), 404

@app.route('/submit_rhyming_answer', methods=['POST'])
def submit_rhyming_answer():
    data = request.json
    word = data.get('word')
    chosen_option = data.get('chosen_option')

    if database.validate_rhyming_answer(word, chosen_option):
        return jsonify({'result': 'correct'}), 200
    else:
        return jsonify({'result': 'incorrect'}), 200
    

@app.route('/update_score', methods=['POST'])
def update_score():
    # Parse request data
    data = request.json
    user_id = data['user_id']
    level = data['level']
    score = data['score']

    # Update the user score
    try:
        database.update_user_score(user_id, level, score)
        return jsonify({'message': 'Score updated successfully'}), 200
    except Exception as e:
        print(f"Error updating score: {str(e)}")
        return jsonify({'message': 'Failed to update score'}), 500
    

########## Word Harmony Activity ############

@app.route('/get_word_harmony_task/<int:level>', methods=['GET'])
def get_word_harmony_task(level):
    correct_word, sound_path, options = database.get_word_harmony_task(level)
    if correct_word:
        response = {
            'word': correct_word,
            'sound_path': sound_path,
            'options': options  # This is the new line to include options
        }
        return jsonify(response), 200
    else:
        return jsonify({'message': 'No task available for this level'}), 404


@app.route('/submit_word_harmony_answer', methods=['POST'])
def submit_word_harmony_answer():
    data = request.json
    word = data.get('word')
    chosen_word = data.get('chosen_word')

    if word == chosen_word:  # Simple equality check for validation
        return jsonify({'result': 'correct'}), 200
    else:
        return jsonify({'result': 'incorrect'}), 200



@app.route('/submit_word_harmony_score', methods=['POST'])
def submit_word_harmony_score():
    data = request.get_json()
    user_id = data['user_id']
    level_number = data['level']
    score = data['score']

    # Assuming 'Word Harmony' is the name of the activity in your 'activities' table
    activity_name = 'Word Harmony'
    
    success = database.insert_word_harmony_score(user_id, activity_name, level_number, score)
    
    if success:
        return jsonify({'message': 'Score submitted successfully'}), 200
    else:
        return jsonify({'message': 'Failed to submit score'}), 500



# ########## Audio Rhyme Selection Activity ############

# Initialize Google Cloud Speech client

os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = 'C:/Users/Anam/Downloads/dyslexiadetectfyp-a2e09a1eefb2.json'
speech_client = speech.SpeechClient()


@app.route('/audio_rhyme/get_word', methods=['GET'])
def get_audio_rhyme_word_route():
    level = request.args.get('level', default=1, type=int)
    word, options = database.get_audio_rhyme_word_and_options(level)
    if word:
        return jsonify({'word': word, 'options': options}), 200
    else:
        return jsonify({'message': 'No task available for this level'}), 404

    
def convert_aac_to_wav(aac_file_path, wav_file_path):
    """
    Converts an AAC audio file to WAV format using FFmpeg.
    """
    command = ['ffmpeg', '-i', aac_file_path, '-acodec', 'pcm_s16le', '-ar', '16000', '-ac', '1', wav_file_path]
    subprocess.run(command, check=True)


@app.route('/audio_rhyme/submit', methods=['POST'])
def submit_audio_rhyme():

    level = request.args.get('level', type=int)
    task = request.args.get('task', type=int)
    
    logging.info("Received /audio_rhyme/submit request")
    if 'file' not in request.files or 'word' not in request.form:
        logging.warning("Request missing 'file' or 'word'")
        return jsonify({'message': 'Missing file or word'}), 400

    word = request.form['word']
    file = request.files['file']
    
    if file.filename == '':
        logging.warning("No file selected")
        return jsonify({'message': 'No selected file'}), 400

    # Save the uploaded file temporarily
    temp_dir = tempfile.mkdtemp()
    aac_file_path = os.path.join(temp_dir, file.filename)
    file.save(aac_file_path)

    # Convert the AAC file to WAV format
    wav_file_path = os.path.join(temp_dir, 'converted_audio.wav')
    try:
        convert_aac_to_wav(aac_file_path, wav_file_path)
    except subprocess.CalledProcessError as e:
        logging.error(f"FFmpeg audio conversion failed: {e}", exc_info=True)
        return jsonify({"message": "Audio conversion failed"}), 500
    finally:
        # Clean up the original AAC file
        os.remove(aac_file_path)

    # Read the converted WAV file
    with open(wav_file_path, 'rb') as audio_file:
        audio_data = audio_file.read()

    config = RecognitionConfig(
        encoding=speech.RecognitionConfig.AudioEncoding.LINEAR16,
        sample_rate_hertz=16000,
        language_code='en-US'
    )
    audio = RecognitionAudio(content=audio_data)

    try:
        response = speech_client.recognize(config=config, audio=audio)
        if response.results and response.results[0].alternatives:
            transcript = response.results[0].alternatives[0].transcript
            is_correct_rhyme = database.check_audio_rhyme(word, transcript)
            return jsonify({
                'is_correct_rhyme': is_correct_rhyme, 
                'transcript': transcript,
                'message': "Submission received for level {} and task {}".format(level, task)
            }), 200
        else:
            logging.info("No transcription results")
            return jsonify({'message': 'No transcription results'}), 400
    except Exception as e:
        logging.error(f"Error during speech recognition: {e}", exc_info=True)
        return jsonify({"message": str(e)}), 500
    finally:
        # Clean up the WAV file
        os.remove(wav_file_path)


@app.route('/submit_audio_rhyme_score', methods=['POST'])
def submit_audio_rhyme_score():
    data = request.json
    user_id = data['user_id']
    level = data['level']
    score = data['score']

    success = database.insert_audio_rhyme_score(user_id, level, score)
    if success:
        return jsonify({'message': 'Score submitted successfully'}), 200
    else:
        return jsonify({'message': 'Failed to submit score'}), 500



######### Scores ##########
  
@app.route('/get_user_scores', methods=['GET'])
def get_user_scores_route():
    user_id = request.args.get('user_id')
    if user_id:
        scores = database.get_all_attempts_for_progress_tracking(user_id)
        if scores:
            # Directly return the structured scores if available
            return jsonify(scores), 200
        else:
            # Indicate no scores were found for the user
            return jsonify({'message': 'No scores found for the user'}), 404
    else:
        # Indicate that a User ID is required for this request
        return jsonify({'message': 'User ID is required'}), 400
    

@app.route('/get_max_scores', methods=['GET'])
def get_max_scores_route():
    user_id = request.args.get('user_id')
    if user_id:
        max_scores = database.get_max_scores_by_level(user_id)
        if max_scores:
            return jsonify(max_scores), 200
        else:
            return jsonify({'message': 'No scores found for the user'}), 404
    else:
        return jsonify({'message': 'User ID is required'}), 400
    

######## Progress Tracking ##########

@app.route('/get_user_progress/<int:user_id>', methods=['GET'])
def get_user_progress_route(user_id):
    progress_data = database.get_user_progress(user_id)
    if progress_data:
        # Transform data into a JSON-serializable structure if needed
        return jsonify(progress_data), 200
    else:
        return jsonify({'message': 'No progress found for the user'}), 404



######### User Profile ##########

@app.route('/get_user_profile', methods=['GET'])
def get_user_profile():
    user_id = request.args.get('userId')
    profile = database.get_user_profile(user_id)
    if profile:
        return jsonify(profile), 200
    else:
        return jsonify({"message": "User not found"}), 404



@app.route('/update_user_profile', methods=['POST'])
def update_user_profile():
    data = request.get_json()
    user_id = data.get('userId')
    name = data.get('name')
    username = data.get('username')
    age = data.get('age')
    old_password = data.get('oldPassword')
    new_password = data.get('newPassword')
    
    # Assuming you have a function in database.py to handle profile updates
    success = database.update_user_profile(user_id, name, username, age, old_password, new_password)
    
    if success:
        return jsonify({'success': True}), 200
    else:
        return jsonify({'success': False, 'error': 'Failed to update profile'}), 400

@app.route('/upload_profile_picture', methods=['POST'])
def upload_profile_picture_route():
    user_id = request.form['user_id']
    image_data = request.files['image'].read()  # Assuming image is sent as a file in the request
    return database.upload_profile_picture(user_id, image_data)

@app.route('/fetch_profile_picture/<int:user_id>', methods=['GET'])
def fetch_profile_picture_route(user_id):
    image_data = database.fetch_profile_picture(user_id)
    if image_data:
        return image_data, 200, {'Content-Type': 'image/png'}  # Assuming the image data is PNG format
    else:
        return jsonify({'message': 'Profile picture not found'}), 404

if __name__ == '__main__':

    app.run(debug=True, host='0.0.0.0', port=5000)