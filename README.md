

# Telemetry Lab

## Overview

This Android app demonstrates a simple compute-intensive background task with real-time UI updates using **Jetpack Compose**. It simulates an "edge inference" pipeline, performing CPU-bound calculations off the main thread and updating the UI with latency metrics.

The app uses a **Foreground Service (FGS)** to run long-running compute tasks, ensuring smooth background execution while respecting Android 14 system policies. The user interface provides a **Start/Stop toggle**, a **Compute Load slider**, and a **real-time dashboard showing frame latency**.

---

## Features

* **Start/Stop Service:** Begin or stop telemetry computation with a single button.
* **Compute Load Slider:** Adjust the CPU workload (1–5).
* **Real-Time Latency Dashboard:** Shows latest frame latency and moving average (~20 Hz).
* **Power Awareness:** Automatically reduces compute load if **Battery Saver** is enabled, showing "Power-save mode".
* **Off-Main-Thread Execution:** CPU-intensive tasks run on **Dispatchers.Default**.
* **Foreground Service:** Uses `foregroundServiceType="dataSync"` with a visible notification.

---

## Architecture & Data Flow

```
[TelemetryService (Foreground Service)]
            |
   produces frameTimeMs
            v
      [TelemetryViewModel]
   - StateFlow for uiState
   - Updates compute load and latency
            |
            v
[Compose UI - TelemetryScreen]
   - Observes uiState via collectAsStateWithLifecycle
   - Displays latestFrameMs, avgFrameMs, compute load
   - Start/Stop button and slider controls
```

**Explanation:**

* The **service** generates frame timing data continuously while running.
* The **ViewModel** receives these updates via a callback and exposes a **StateFlow** for the Compose UI.
* The **Compose UI** observes the StateFlow and updates reactively, avoiding unnecessary recompositions.

---

## Implementation Details

Here is the attached file of Working https://drive.google.com/file/d/1KjrlChB3tXNX82fF2Bau_4oYmgGRTR1-/view?usp=sharing

### Threading & Backpressure

* Computation runs in a **single coroutine** on `Dispatchers.Default`.
* UI updates are **state-driven** via `StateFlow` to reduce recomposition overhead.

### Foreground Service Choice

* FGS ensures **continuous frame-level computation** with a visible notification.
* WorkManager is more suitable for batch/deferred tasks; it cannot guarantee 20 Hz updates.

### Compute Simulation

* Each "frame" performs CPU-bound math simulating edge inference.
* The workload scales with the **Compute Load slider (1–5)**.
* Ensures **off-main-thread execution**, keeping UI smooth.

### UI Implementation

* Jetpack Compose provides a reactive UI with minimal boilerplate.
* Start/Stop button and slider update the service via ViewModel.
* Dashboard shows **latest frame latency**, **average latency**, and **power-save mode** status.

---

## How to Run

1. Open in Android Studio.
2. Run on a device or emulator.
3. Press **Start** to begin telemetry computation.
4. Adjust **Compute Load** with the slider.
5. Observe real-time latency metrics.
6. Press **Stop** to halt the service.

---

## Notes

* No network or database needed; all computation is local.
* Demonstrates **off-main-thread compute**, proper **foreground service usage**, and **power-awareness**.
* Focuses on **performance and responsiveness** rather than UI polish.



