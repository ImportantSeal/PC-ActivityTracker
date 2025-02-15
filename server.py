import socket
import threading
from flask import Flask, jsonify
import datetime
import psutil 
import time
import win32gui
import win32process

app = Flask(__name__)

# lists events like {"process": "notepad.exe", "action": "opened", "timestamp": "2025-02-15 12:34:56"}
events = []

@app.route('/status', methods=['GET'])
def get_status():
    return jsonify({
        "status": "PC is Online",
        "timestamp": str(datetime.datetime.now())
    })

@app.route('/events', methods =['GET'])
def get_events():
    return jsonify(events)

# function to Get Local IP Address
def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.settimeout(0)
    try:
        # connect to an external server to get the correct local IP
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
    except Exception:
        local_ip = "127.0.0.1"
    finally:
        s.close()
    return local_ip

# UDP Broadcast Function
def broadcast_ip():
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

    while True:
        ip_address = get_local_ip()  # get actual local IP
        message = f"PC_IP:{ip_address}"
        udp_socket.sendto(message.encode(), ('<broadcast>', 5001))  
        print(f"Broadcasting: {message}")
        threading.Event().wait(5)  

# for following processes
prev_processes = set()

def get_active_window_info():
    hwnd = win32gui.GetForegroundWindow()
    window_title = win32gui.GetWindowText(hwnd)
    # searching PID from active window
    _, pid = win32process.GetWindowThreadProcessId(hwnd)
    try:
        proc = psutil.Process(pid)
        process_name = proc.name()
    except Exception:
        process_name= "Unknown"
    return process_name, window_title

# background thread that followes opening and closing apps
def monitor_applications():
    global prev_processes, events
    while True:
        current_processes = set()
        #go trough ongoing process and collect names
        for proc in psutil.process_iter(['name']):
            try:
                name = proc.info['name']
                if name: #for testing if name is available
                    current_processes.add(name)
            except Exception:
                continue

        new_processes = current_processes - prev_processes
        closed_processes = prev_processes - current_processes

        for p in new_processes:
            event = {"process": p, "action": "opened", "timestamp": str(datetime.datetime.now())}
            events.append(event)
            print("Application opened", event)
        for p in closed_processes:
            event = {"process": p, "action": "closed", "timestamp": str(datetime.datetime.now())}
            events.append(event)
            print("Application closed", event)

        prev_processes = current_processes
        time.sleep(5)

def monitor_active_window():
    last_active = None
    while True:
        current_active = get_active_window_info()
        if current_active != last_active:
            last_active = current_active
            event = {
                "process": current_active[0],
                "window": current_active[1],
                "action": "active",
                "timestamp": str(datetime.datetime.now())
            }
            events.append(event)
            print("Active window changed:", event)
        time.sleep(5)  

# start UDP broadcaster in a separate thread
broadcast_thread = threading.Thread(target=broadcast_ip, daemon=True)
broadcast_thread.start()

active_window_thread = threading.Thread(target=monitor_active_window, daemon=True)
active_window_thread.start()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
