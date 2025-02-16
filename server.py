import socket
import threading
from flask import Flask, jsonify
import datetime
import time
import win32gui
import win32process
import psutil

app = Flask(__name__)

# Tallennetaan käyttöistunnot, esim.:
# { "process": "chrome.exe", "window": "YouTube - Google Chrome", "start_time": "2025-02-15T16:45:00", "end_time": "2025-02-15T16:50:00", "duration_seconds": 300 }
sessions = []

# Tällä hetkellä aktiivinen käyttöistunto
active_session = None

@app.route('/status', methods=['GET'])
def get_status():
    status = {
        "status": "PC is Online",
        "timestamp": str(datetime.datetime.now())
    }
    if active_session is not None:
        status["current_session"] = {
            "process": active_session["process"],
            "window": active_session["window"],
            "start_time": active_session["start_time"]
        }
    return jsonify(status)

@app.route('/sessions', methods=['GET'])
def get_sessions():
    return jsonify(sessions)

# Hakee paikallisen IP-osoitteen
def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.settimeout(0)
    try:
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
    except Exception:
        local_ip = "127.0.0.1"
    finally:
        s.close()
    return local_ip

# UDP-broadcast: lähetetään säännöllisesti PC:n IP-osoite
def broadcast_ip():
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    while True:
        try:
            ip_address = get_local_ip()
            message = f"PC_IP:{ip_address}"
            udp_socket.sendto(message.encode(), ('<broadcast>', 5001))
            print(f"Broadcasting: {message}")
        except Exception as e:
            print(f"Broadcast error: {e}")
        time.sleep(5)


# Hakee aktiivisen ikkunan tiedot: palauttaa (hwnd, prosessin nimi, ikkunan otsikko)
def get_active_window_info():
    hwnd = win32gui.GetForegroundWindow()
    if hwnd == 0:
        return None, None, None
    window_title = win32gui.GetWindowText(hwnd)
    _, pid = win32process.GetWindowThreadProcessId(hwnd)
    try:
        proc = psutil.Process(pid)
        process_name = proc.name()
    except Exception:
        process_name = "Unknown"
    return hwnd, process_name, window_title

# Seurataan aktiivista ikkunaa ja kirjataan "istunnot"
def monitor_active_window():
    global active_session, sessions
    last_hwnd = None
    while True:
        hwnd, process_name, window_title = get_active_window_info()
        now = datetime.datetime.now()
        # Jos mitään aktiivista ikkunaa ei löydy, odotetaan
        if hwnd is None:
            time.sleep(1)
            continue

        # Jos aktiivinen ikkuna muuttuu (tai ensimmäinen kerta)
        if last_hwnd != hwnd:
            # Jos edellinen istunto on käynnissä, päätetään se
            if active_session is not None:
                active_session["end_time"] = now.isoformat()
                start_time = datetime.datetime.fromisoformat(active_session["start_time"])
                duration = (now - start_time).total_seconds()
                active_session["duration_seconds"] = duration
                sessions.append(active_session)
                print("Session ended:", active_session)
                active_session = None
            # Aloitetaan uusi istunto
            active_session = {
                "process": process_name,
                "window": window_title,
                "start_time": now.isoformat(),
                "end_time": None,
                "duration_seconds": None
            }
            print("New session started:", active_session)
            last_hwnd = hwnd

        time.sleep(1)

if __name__ == '__main__':
    broadcast_thread = threading.Thread(target=broadcast_ip, daemon=True)
    broadcast_thread.start()

    active_window_thread = threading.Thread(target=monitor_active_window, daemon=True)
    active_window_thread.start()

    app.run(host='0.0.0.0', port=5000)
