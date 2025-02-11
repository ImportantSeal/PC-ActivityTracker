import socket
import threading
from flask import Flask, jsonify
import datetime

app = Flask(__name__)

@app.route('/status', methods=['GET'])
def get_status():
    return jsonify({
        "status": "PC is Online",
        "timestamp": str(datetime.datetime.now())
    })

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

# start UDP broadcaster in a separate thread
broadcast_thread = threading.Thread(target=broadcast_ip, daemon=True)
broadcast_thread.start()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
