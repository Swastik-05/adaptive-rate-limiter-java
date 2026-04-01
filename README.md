# 🚀 Adaptive Multi-threaded Rate Limiter (Java)

## 📌 Overview

This project implements a **multi-threaded rate limiter** using the **Token Bucket algorithm** with per-user concurrency control. It simulates real-world backend behavior under load and demonstrates **system stability, controlled throughput, and backpressure handling**.

---

## ⚙️ Features

* Per-user rate limiting using **ConcurrentHashMap**
* **Token Bucket algorithm** for request throttling
* Multi-threaded request simulation using **ExecutorService**
* **Sequential vs Multithreaded benchmarking**
* Real-time metrics tracking:

  * Throughput (req/s)
  * Average latency (ms)
  * Acceptance & rejection rate
* CLI-based configuration (runtime inputs)

---

## 🧠 System Design

```
Request → Thread Pool → RateLimiter → TokenBucket → Metrics
```

---

## 🧪 Experimental Results

### 🔥 High Load Test

* Requests: 100
* Threads: 20
* Allowed: ~15%
* Rejected: ~85%
* Latency: ~50ms

---

## ⚡ Performance Comparison

| Execution Type | Time    |
| -------------- | ------- |
| Sequential     | ~935 ms |
| Multithreaded  | ~66 ms  |

👉 **~14x faster using multithreading**

---

## 📊 Key Insights

* System maintains **stable latency (~50ms)** even under heavy load
* Throughput reaches a **controlled upper bound**
* Rejection rate increases with load → **graceful degradation**
* Multithreading significantly improves performance without affecting correctness

---

## 💡 Engineering Highlights

* Used `ConcurrentHashMap` for thread-safe per-user isolation
* Applied `synchronized` in TokenBucket to ensure atomic operations
* Used `ExecutorService` for controlled concurrency
* Benchmarked system against sequential execution to validate performance gains

---
## 🔄 Recent Enhancements

* Added a **SequentialProcessor** module to simulate request handling without concurrency (baseline system).
* Integrated **performance benchmarking** in `Main.java` to compare:

  * Sequential execution
  * Multithreaded execution
* Measured total execution time and calculated **speed improvement (~14x faster)**.
* Demonstrated how **parallel processing improves throughput** while maintaining consistent rate limiting behavior.

## ▶️ How to Run

```bash
javac (Get-ChildItem -Recurse -Filter *.java | % { $_.FullName })
java Main
```

---

## 🧾 Example Output

```
Sequential Time: ~935 ms
Multithreaded Time: ~66 ms
Speed Improvement: ~14x faster

Allowed Requests : 15
Rejected Requests: 85
Avg Latency      : ~50 ms
```

---

## 🎯 Conclusion

This project demonstrates how backend systems handle high traffic using **rate limiting + concurrency**, ensuring **stable performance and controlled degradation under load**.

---

## 🛠️ Tech Stack

* Java (Core)
* Multithreading (ExecutorService)
* Concurrent Data Structures

---

## 👤 Author

Swastik Sharma
[LinkedIn](https://www.linkedin.com/in/swastik-sharma-814778244/)
