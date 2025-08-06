import time
import random
import json
from azure.iot.device import IoTHubDeviceClient, Message

# Replace with your actual device connection string from Azure Portal
CONNECTION_STRING = "HostName=industrialIoTHub.azure-devices.net;DeviceId=edge-device;SharedAccessKey=HVhFxaRHt0iP2QD8/Kz46pERWAWC58PP2n6DU+ZWvvg="

# Initialize IoT Hub client
device_client = IoTHubDeviceClient.create_from_connection_string(CONNECTION_STRING)

# ========= Simulated Industrial Pipeline =========

def generate_sensor_data():
    return {
        "PRESSURE": round(random.uniform(200.0, 300.0), 2),
        "VIBRATION": round(random.uniform(0.1, 2.0), 2),
        "FLOW_RATE": round(random.uniform(10.0, 50.0), 2),
        "TORQUE": round(random.uniform(50.0, 150.0), 2),
        "MOTOR_SPEED": round(random.uniform(1000.0, 3000.0), 2),
    }

def preprocess_data(raw_data):
    return {key: round(val * 1.01, 2) for key, val in raw_data.items()}

def process_data(processed_data):
    results = []
    for sensor, value in processed_data.items():
        delay = round(random.uniform(250.0, 270.0), 2)
        results.append({
            "sensor": sensor,
            "value": value,
            "cpu_delay_ms": delay,
            "anomaly": value > get_threshold(sensor)
        })
    return results

def get_threshold(sensor):
    thresholds = {
        "PRESSURE": 280.0,
        "VIBRATION": 1.8,
        "FLOW_RATE": 45.0,
        "TORQUE": 140.0,
        "MOTOR_SPEED": 2800.0,
    }
    return thresholds.get(sensor, float('inf'))

# ========= Azure IoT Integration =========

def send_startup_properties():
    properties = {
        "device": "edge-device",
        "location": "Factory-Floor-3",
        "type": "Industrial-Edge-Node"
    }
    msg = Message(json.dumps(properties))
    msg.custom_properties["startup"] = "true"
    device_client.send_message(msg)
    print("✅ Sent device startup metadata to Azure IoT Hub.")

def receive_twin_updates():
    def twin_patch_handler(patch):
        print(f"🧠 Desired properties updated from cloud: {patch}")
    device_client.on_twin_desired_properties_patch_received = twin_patch_handler

def on_c2d_message_received(message):
    print(f"📩 Cloud-to-Device message received: {message.data.decode()}")

# ========= Main Telemetry Loop =========

def send_telemetry():
    print("📡 Sending industrial telemetry to Azure IoT Hub...")
    while True:
        raw_data = generate_sensor_data()
        print(f"\n🟦 Raw Sensor Data: {raw_data}")

        preprocessed = preprocess_data(raw_data)
        print(f"🟩 Preprocessed: {preprocessed}")

        final_output = process_data(preprocessed)
        for item in final_output:
            msg = Message(json.dumps(item))
            msg.custom_properties["sensor_type"] = item["sensor"]
            msg.custom_properties["anomaly"] = str(item["anomaly"])
            device_client.send_message(msg)
            print(f"📤 Sent: {item}")

        time.sleep(5)

# ========= Main =========

if __name__ == "__main__":
    try:
        print("🚀 Industrial Automation Edge Device starting up...")

        send_startup_properties()
        receive_twin_updates()
        device_client.on_message_received = on_c2d_message_received

        send_telemetry()

    except KeyboardInterrupt:
        print("🛑 Simulation stopped by user.")
    finally:
        device_client.shutdown()
        print("🔌 Device disconnected from Azure IoT Hub.")
