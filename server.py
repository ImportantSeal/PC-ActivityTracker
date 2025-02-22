import socket
import threading
from flask import Flask, jsonify
import datetime
import time
import win32gui
import win32process
import psutil
import win32ui
import win32con
import base64
import io
from PIL import Image
from PIL import ImageFilter
from PIL import ImageEnhance

app = Flask(__name__)

sessions = []
active_session = None

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
            "start_time": active_session["start_time"],
            "icon_url": active_session.get("icon_url", "")
        }
    return jsonify(status)

@app.route('/sessions', methods=['GET'])
def get_sessions():
    return jsonify(sessions)

def broadcast_ip():
    udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    while True:
        try:
            ip_address = get_local_ip()
            message = f"PC_IP:{ip_address}"
            udp_socket.sendto(message.encode(), ('255.255.255.255', 5001))
            print(f"Broadcasting: {message}")
        except Exception as e:
            print(f"Broadcast error: {e}")
        time.sleep(5)

def get_active_window_info():
    hwnd = win32gui.GetForegroundWindow()
    if hwnd == 0:
        return None, None, None, None
    window_title = win32gui.GetWindowText(hwnd)
    _, pid = win32process.GetWindowThreadProcessId(hwnd)
    try:
        proc = psutil.Process(pid)
        process_name = proc.name()
        process_exe = proc.exe()
    except Exception:
        process_name = "Unknown"
        process_exe = ""
    return hwnd, process_name, window_title, process_exe

def convert_hicon_to_base64(hicon):
    hdc = win32ui.CreateDCFromHandle(win32gui.GetDC(0))
    hbmp = win32ui.CreateBitmap()
    hbmp.CreateCompatibleBitmap(hdc, 32, 32)
    hdc_mem = hdc.CreateCompatibleDC()
    hdc_mem.SelectObject(hbmp)
    win32gui.DrawIconEx(hdc_mem.GetSafeHdc(), 0, 0, hicon, 32, 32, 0, None, win32con.DI_NORMAL)
    bmpinfo = hbmp.GetInfo()
    bmpstr = hbmp.GetBitmapBits(True)
    img = Image.frombuffer('RGB', (bmpinfo['bmWidth'], bmpinfo['bmHeight']), bmpstr, 'raw', 'BGRX', 0, 1)
    
    # Muunna kuva RGBA-muotoon
    img = img.convert("RGBA")
    datas = img.getdata()
    new_data = []
    threshold = 30  # Kynnysarvo, jolla määritetään mikä on "lähes musta"
    for item in datas:
        if item[0] < threshold and item[1] < threshold and item[2] < threshold:
            # Aseta läpinäkyväksi
            new_data.append((0, 0, 0, 0))
        else:
            new_data.append(item)
    img.putdata(new_data)
    
    # Luo erillinen alfa-maski ja pehmennä sitä
    alpha = img.split()[3]
    alpha = alpha.filter(ImageFilter.GaussianBlur(radius=1.0))
    img.putalpha(alpha)

    
    output = io.BytesIO()
    img.save(output, format="PNG")
    png_data = output.getvalue()
    return base64.b64encode(png_data).decode("utf-8")





def get_icon_for_process(process_exe: str) -> str:
    try:
        large, small = win32gui.ExtractIconEx(process_exe, 0)
        if large:
            hicon = large[0]
        elif small:
            hicon = small[0]
        else:
            return ""
        icon_base64 = convert_hicon_to_base64(hicon)
        win32gui.DestroyIcon(hicon)
        return icon_base64
    except Exception as e:
        print("Error extracting icon:", e)
        return ""

def monitor_active_window():
    global active_session, sessions
    last_hwnd = None
    while True:
        hwnd, process_name, window_title, process_exe = get_active_window_info()
        now = datetime.datetime.now()
        if hwnd is None:
            time.sleep(1)
            continue
        if last_hwnd != hwnd:
            if active_session is not None:
                active_session["end_time"] = now.isoformat()
                start_time = datetime.datetime.fromisoformat(active_session["start_time"])
                duration = (now - start_time).total_seconds()
                active_session["duration_seconds"] = duration
                sessions.append(active_session)
                print("Session ended:", active_session)
                active_session = None
            icon_base64 = get_icon_for_process(process_exe)
            active_session = {
                "process": process_name,
                "window": window_title,
                "start_time": now.isoformat(),
                "end_time": None,
                "duration_seconds": None,
                "icon_url": "data:image/png;base64," + icon_base64 if icon_base64 else ""
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
