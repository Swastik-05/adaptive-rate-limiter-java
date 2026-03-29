# 🚀 Adaptive Multi-threaded Rate Limiter (Java)

## 📌 Overview

This project implements a **multi-threaded rate limiter** using the **Token Bucket algorithm** with per-user concurrency control. It simulates real-world backend behavior under varying load conditions and demonstrates **system stability, controlled throughput, and backpressure handling**.

---

## ⚙️ Features

* Per-user rate limiting using **ConcurrentHashMap**
* **Token Bucket algorithm** for request throttling
* Multi-threaded request simulation using **ExecutorService**
* Real-time metrics tracking:

  * Throughput (req/s)
  * Average latency (ms)
  * Acceptance & rejection rate
* CLI-based configuration (dynamic inputs at runtime)

---

## 🧠 System Design

```
Client Requests
      ↓
Thread Pool (ExecutorService)
      ↓
RateLimiter
      ↓
ConcurrentHashMap<userId, TokenBucket>
      ↓
TokenBucket (synchronized)
      ↓
MetricsCollector
```

---

## 🧪 Experimental Results

### ✅ Test 1 — Low Load

* Requests: 20
* Allowed: 100%
* Rejected: 0%
* Latency: ~50ms
* Behavior: System underutilized

---

### ⚖️ Test 2 — Medium Load

* Requests: 100
* Allowed: 15%
* Rejected: 85%
* Latency: ~51ms
* Behavior: Rate limiting active, throughput stabilized

---

### 🔴 Test 3 — High Load

* Requests: 100
* Allowed: 15%
* Rejected: 85%
* Latency: ~51ms
* Behavior: Throughput capped, system protected

---

### 🔥 Test 4 — Extreme Load

* Requests: 200
* Allowed: 3%
* Rejected: 97%
* Latency: ~52ms
* Behavior: Strong backpressure, stable latency

---

## 📊 Key Insights

* System maintains **stable latency (~50ms)** even under heavy load
* Throughput reaches a **controlled upper bound**
* Rejection rate increases with load → **graceful degradation**
* Prevents system overload by rejecting excess requests

---

## 💡 Engineering Highlights

* Used `ConcurrentHashMap` for thread-safe per-user bucket management
* Applied `synchronized` in TokenBucket to prevent race conditions
* Implemented sliding window metrics without locking (high performance)
* Designed system for **controlled degradation instead of failure**

---

## ▶️ How to Run

```bash
javac (Get-ChildItem -Recurse -Filter *.java | % { $_.FullName })
java Main
```

---

## 🧾 Example Output

```
Total Requests   : 100
Allowed Requests : 15
Rejected Requests: 85
Throughput       : 10.00 req/s
Avg Latency      : 50.80 ms
Acceptance Rate  : 15.00 %
Rejection Rate   : 85.00 %
```

---

## 🎯 Conclusion

This project demonstrates how backend systems handle high traffic using **rate limiting and concurrency control**, ensuring **system stability, predictable performance, and graceful degradation under load**.

---

## 🛠️ Tech Stack

* Java (Core)
* Multithreading (ExecutorService)
* Concurrent Data Structures

---

## 👤 Author

Swastik Sharma
[LinkedIn](https://www.linkedin.com/in/swastik-sharma-814778244/)
