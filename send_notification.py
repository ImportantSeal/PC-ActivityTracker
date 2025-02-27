import json
import requests
import os
from google.oauth2 import service_account
import google.auth.transport.requests
from dotenv import load_dotenv

# Ladataan .env-tiedoston muuttujat
load_dotenv()

SCOPES = ["https://www.googleapis.com/auth/firebase.messaging"]

def get_access_token(service_account_file):
    """
    Lukee service account -tiedoston ja palauttaa OAuth2-access tokenin.
    """
    credentials = service_account.Credentials.from_service_account_file(
        service_account_file, scopes=SCOPES
    )
    auth_req = google.auth.transport.requests.Request()
    credentials.refresh(auth_req)
    return credentials.token

def send_fcm_notification_v1(service_account_file, project_id, device_token):
    """
    Lähettää push-notifikaation FCM HTTP v1 API:n kautta.
    """
    access_token = get_access_token(service_account_file)
    url = f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"
    message = {
        "message": {
            "token": device_token,
            "notification": {
                "title": "Server Started",
                "body": "PC Activity Tracker server is now online!"
            }
        }
    }
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json; UTF-8"
    }
    response = requests.post(url, headers=headers, json=message)
    print("Response status:", response.status_code)
    print("Response text:", response.text)

if __name__ == "__main__":
    service_account_file = os.getenv("SERVICE_ACCOUNT_FILE")
    project_id = os.getenv("PROJECT_ID")
    device_token = os.getenv("DEVICE_TOKEN")
    send_fcm_notification_v1(service_account_file, project_id, device_token)
