


import os
import csv
from PIL import Image
import pytesseract
from sqlalchemy import create_engine, MetaData
from sqlalchemy.orm import sessionmaker
from pytesseract import Output


# def perform_ocr(left_eye_x, left_eye_y, right_eye_x, right_eye_y, screenshot_path):
#     combined_x = (left_eye_x + right_eye_x) / 2
#     combined_y = (left_eye_y + right_eye_y) / 2
    
#     image_data = Image.open(screenshot_path)
#     data = pytesseract.image_to_data(image_data, output_type=Output.DICT)
#     line = extract_line_at_point(data, combined_x, combined_y)
#     return line
def perform_ocr(left_eye_x, left_eye_y, right_eye_x, right_eye_y, screenshot_path):
    combined_x = (left_eye_x + right_eye_x) / 2
    combined_y = (left_eye_y + right_eye_y) / 2
    
    image_data = Image.open(screenshot_path)
    data = pytesseract.image_to_data(image_data, output_type=Output.DICT)
    line, bbox = extract_line_at_point(data, combined_x, combined_y)
    
    if bbox:
        draw_box_around_text(image_data, bbox)  # Modify the image_data in place

    return line, image_data


def draw_box_around_text(image, bbox):
    draw = ImageDraw.Draw(image)
    draw.rectangle(bbox, outline='red', width=2)  # Draw the rectangle with a red outline
    image.show()  # Display the image
    # Optionally, save the image if needed
    image.save('output_image_with_box.png')





# def extract_line_at_point(data, x, y):
#     for i in range(len(data['text'])):
#         if data['text'][i].strip():  # Check if there's actual text
#             (x1, y1, w, h) = (data['left'][i], data['top'][i], data['width'][i], data['height'][i])
#             x2 = x1 + w
#             y2 = y1 + h
#             if x1 <= x <= x2 and y1 <= y <= y2:
#                 return data['text'][i]
#     return "Text not found at the given point"
def extract_line_at_point(data, x, y):
    for i in range(len(data['text'])):
        if data['text'][i].strip():  # Make sure the OCR result is not just whitespace
            x1, y1, w, h = data['left'][i], data['top'][i], data['width'][i], data['height'][i]
            x2, y2 = x1 + w, y1 + h
            if x1 <= x <= x2 and y1 <= y <= y2:
                return data['text'][i], (x1, y1, x2, y2)
    return "Text not found at the given point", None

# Function to determine if a given point (x, y) lies within a bounding box defined by (x1, y1, x2, y2).
def line_contains_point(x1, y1, x2, y2, x, y):
    return x1 <= x <= x2 and y1 <= y <= y2

# def get_fixation_data(user_id, db_path):
#     engine = create_engine(f"sqlite:///{db_path}")
#     Session = sessionmaker(bind=engine)
#     session = Session()
    
#     metadata = MetaData()
#     metadata.reflect(bind=engine)
#     print(user_id)
#     table_name = f"SCR_RAW_DATA_USER_{user_id}"
#     table = metadata.tables.get(table_name)
    
#     if table is None:
#         print(f"Warning: Table {table_name} does not exist in the database.")
#         return []

#     # query = session.query(table).filter(
#     #     (table.c.category_left == 'Fixation') | (table.c.category_right == 'Fixation')
#     # )
#     query = session.query(table).filter(
#         ((table.c.category_left == 'Fixation') | (table.c.category_right == 'Fixation')) &
#         (table.c.point_of_regard_left_x != -1) & (table.c.point_of_regard_left_y != -1) &
#         (table.c.point_of_regard_right_x != -1) & (table.c.point_of_regard_right_y != -1)
#     )
    
#     try:
#         results = query.all()
#         return results
#     except Exception as e:
#         session.rollback()
#         raise
#     finally:
#         session.close()
def get_fixation_data(user_id, db_path):
    try:
        print(f"Fetching fixation data for user: {user_id}")
        engine = create_engine(f"sqlite:///{db_path}")
        Session = sessionmaker(bind=engine)
        session = Session()
        
        metadata = MetaData()
        metadata.reflect(bind=engine)
        
        table_name = f"SCR_RAW_DATA_USER_{user_id}"
        table = metadata.tables.get(table_name)
        
        if table is None:
            print(f"Table {table_name} does not exist in the database.")
            return []
        
        print(f"Table found: {table_name}, preparing to query.")
        query = session.query(table).filter(
            (table.c.category_left == 'Fixation') | (table.c.category_right == 'Fixation')
        )
        
        results = query.all()
        print(f"Query returned {len(results)} results.")
        
        return results
    except Exception as e:
        print(f"Exception occurred in get_fixation_data: {e}")
        raise
    finally:
        if 'session' in locals():
            session.close()
            print("Database session closed.")


# def process_screenshots_for_ocr(data_folder, user_id, fixation_data):
#     print(f"Processing OCR for {len(fixation_data)} fixation data entries.")
#     user_ocr_data = []
#     user_folder = os.path.join(data_folder, str(user_id))

#     for entry in fixation_data:
#         id, timestamp, left_x, left_y, right_x, right_y, gaze_direction, category_left, category_right = entry
#         screenshot_filename = f"{timestamp}.jpg"
#         screenshot_path = os.path.join(user_folder, screenshot_filename)

#         if os.path.isfile(screenshot_path):
#             ocr_text = perform_ocr(left_x, left_y, right_x, right_y, screenshot_path)
#             user_ocr_data.append([timestamp, ocr_text])
#         else:
#             print(f"Screenshot not found for timestamp {timestamp}")

#     csv_filename = f"{user_id}_OCR_Data.csv"
#     csv_path = os.path.join(data_folder, csv_filename)
#     with open(csv_path, mode='w', newline='', encoding='utf-8') as file:
#         writer = csv.writer(file)
#         writer.writerow(['Timestamp', 'OCR_Text'])
#         writer.writerows(user_ocr_data)
def process_screenshots_for_ocr(data_folder, user_id, fixation_data):
    user_ocr_data = []
    user_folder = os.path.join(data_folder, str(user_id))

    # Directory to save modified images
    image_directory = os.path.join(data_folder, f"{user_id}_OCR_Images")
    if not os.path.exists(image_directory):
        os.makedirs(image_directory)

    for entry in fixation_data:
        id, timestamp, left_x, left_y, right_x, right_y, gaze_direction, category_left, category_right = entry
        screenshot_filename = f"{timestamp}.jpg"
        screenshot_path = os.path.join(user_folder, screenshot_filename)

        if os.path.isfile(screenshot_path):
            ocr_text, modified_image = perform_ocr(left_x, left_y, right_x, right_y, screenshot_path)
            user_ocr_data.append([timestamp, ocr_text])
            # Save the modified image
            modified_image_path = os.path.join(image_directory, screenshot_filename)
            modified_image.save(modified_image_path)
        else:
            print(f"Screenshot not found for timestamp {timestamp}")

    csv_filename = f"{user_id}_OCR_Data.csv"
    csv_path = os.path.join(data_folder, csv_filename)
    with open(csv_path, mode='w', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)
        writer.writerow(['Timestamp', 'OCR_Text'])
        writer.writerows(user_ocr_data)


from PIL import Image, ImageDraw

def debug_draw_boxes(image_path, data, points):
    image = Image.open(image_path)
    draw = ImageDraw.Draw(image)
    
    # Draw rectangles for text areas
    for i in range(len(data['text'])):
        if data['text'][i].strip():
            (x, y, w, h) = (data['left'][i], data['top'][i], data['width'][i], data['height'][i])
            draw.rectangle([x, y, x+w, y+h], outline='red')
    
    # Draw circles for gaze points
    for x, y in points:
        draw.ellipse([x-5, y-5, x+5, y+5], fill='blue')
    
    image.show()

# Use this function in your existing code after extracting data with pytesseract
# Pass the path to the screenshot and the points you want to check
#debug_draw_boxes(screenshot_path, pytesseract.image_to_data(image, output_type=pytesseract.Output.DICT), [(combined_x, combined_y)])


