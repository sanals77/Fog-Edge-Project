# Fog-Edge-Project


## Overview

This project is a proof-of-concept implementation of a real-time Industrial IoT (IIoT) monitoring system using a hierarchical Fog and Edge Computing architecture. Inspired by the research work of Jamil et al. (2024), it demonstrates service distribution across the Edge-Fog-Cloud continuum for latency-sensitive industrial scenarios.

The system includes:
* A **Python-based Edge Device Simulator** that detects anomalies locally and sends telemetry data to the cloud.
* A **performance simulation using iFogSim** to model the full three-tier system and evaluate metrics like latency, energy consumption, and network usage.


## Architecture Layers

* **Edge Layer**: Simulated industrial sensors and real-time anomaly detection (Python script).
* **Fog Layer**: Intermediate aggregation and processing (modeled in iFogSim).
* **Cloud Layer**: Centralized storage, control, and analytics (Microsoft Azure IoT Hub).


## Project Files

* industry_iot_simulator.py – Python script simulating an intelligent edge device.
* IndustrialAutomationIoTSimulationSanal.java` – iFogSim simulation for performance evaluation.
* requirements.txt` – Required Python dependencies.
* README.md` – This file.


## Running the Edge Device Simulator

### Prerequisites

Make sure you have:

* Python 3.8+
* Azure IoT Hub (with a registered device)
* Your device connection string from Azure IoT Hub

Install required packages:

bash
pip install -r requirements.txt

### Setup

Open industry_iot_simulator.py and replace the placeholder connection string:

python
CONNECTION_STRING = "Your Azure IoT device connection string here"


with your actual Azure device connection string.

###  Run the Script

python industry_iot_simulator.py


You will observe:

* Simulated sensor data generation (pressure, vibration, etc.)
* Local anomaly detection and processing
* Telemetry sent to Azure IoT Hub


##  iFogSim Simulation

The Java simulation models a full three-tier Edge-Fog-Cloud deployment.

###  How to Run

1. Set up iFogSim in Eclipse or NetBeans.
2. Import and run IndustrialAutomationIoTSimulationSanal.java.
3. Observe the console logs for simulated:

   * Latency
   * Energy usage
   * Network performance


## Features Demonstrated

* Edge-local anomaly detection and data reduction
* Azure IoT Hub communication (device-to-cloud and cloud-to-device)
* Quantitative evaluation of:

  * Latency
  * Energy consumption (edge vs. cloud)
  * Network bandwidth usage
* Realistic IIoT system simulation in iFogSim
