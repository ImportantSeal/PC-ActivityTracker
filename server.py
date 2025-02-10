from flask import Flask, jsonify
import datetime

app = Flask(__name__)

@app.route('/status', methods=['GET'])
def get_status():
    return jsonify({
        "status": "PC is Online",
        "timestamp": str(datetime.datetime.now())
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
