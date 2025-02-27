import os
from dotenv import load_dotenv
import firebase_admin
from firebase_admin import credentials, messaging

load_dotenv() 

service_account_path = os.getenv("FCM_SERVICE_ACCOUNT_PATH")
if not service_account_path:
    raise Exception("FCM_SERVICE_ACCOUNT_PATH environment variable not set.")

cred = credentials.Certificate(service_account_path)
firebase_admin.initialize_app(cred)


def send_push_notification():
    message = messaging.Message(
        notification=messaging.Notification(
            title="Tietokone käynnistyi",
            body="PC on Online"
        ),
        topic="pcStatus"
    )
    response = messaging.send(message)
    print("Viesti lähetetty onnistuneesti:", response)

if __name__ == "__main__":
    send_push_notification()
