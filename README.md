# HanDomotic: a Gesture-Based Domotic Control System

HanDomotic is a gesture-based control system for home automation using Bluetooth Low Energy (BLE) beacons and wearable technology. The system integrates an Android smartphone and a WearOS smartwatch to recognize hand gestures within an indoor environment. BLE beacons are utilized for accurate room localization by analyzing the Received Signal Strength Indicator (RSSI), offering a low-energy alternative to GPS and Wi-Fi-based solutions.

The core functionality involves detecting gestures to control home appliances. A Support Vector Machine (SVM) classifier, trained on tri-axis accelerometer data, identifies gestures such as "Circle" and "Double Clap." The system achieves perfect accuracy for "Circle" and "No Gesture," and 88\% accuracy for "Double Clap," with a 12\% misclassification rate.

The hardware setup includes BLE beacons, an Android smartphone for configuration and communication, and a WearOS smartwatch for continuous monitoring and gesture recognition. This ensures a user-friendly setup and reliable performance.

HanDomotic advances mobile and social sensing systems by providing an efficient and accurate solution for gesture-based home automation. Future work will aim to improve classification accuracy for complex gestures and add the necessary logic to control smart domotic devices.

## Contributors:
- Niccolò Mulè
- Cristiano Corsi
- Marco Imbelli Cai
- Lorenzo Ceccanti
