# MANUAL-KAFKA.md

---

## Table of Contents

1. [Introduction](#1-introduction)  
2. [Prerequisites](#2-prerequisites)  
3. [Environment Overview](#3-environment-overview)  
4. [File Structure](#4-file-structure)  
5. [Environment Variables](#5-environment-variables)  
6. [Docker Compose Configuration](#6-docker-compose-configuration)  
   - 6.1. [Listeners and Ports](#61-listeners-and-ports)  
   - 6.2. [Advertised Listeners (Dynamic Host IP)](#62-advertised-listeners-dynamic-host-ip)  
   - 6.3. [Inter-Broker and Controller Listeners](#63-inter-broker-and-controller-listener-names)  
   - 6.4. [KRaft Mode Essentials](#64-kraft-mode-essentials)  
   - 6.5. [Internal Topics Configuration](#65-internal-topics-configuration)  
   - 6.6. [Broker Defaults and Development Conveniences](#66-broker-defaults-and-development-conveniences)  
7. [Step-by-Step Deployment](#7-step-by-step-deployment)  
   - 7.1. [Setting the Host IP Environment Variable](#71-setting-the-host-ip-environment-variable)  
   - 7.2. [Bringing Up the Kafka Service](#72-bringing-up-the-kafka-service)  
   - 7.3. [Verifying Broker Startup](#73-verifying-broker-startup)  
8. [Spring Boot Client Configuration](#8-spring-boot-client-configuration)  
9. [Troubleshooting](#9-troubleshooting)  
   - 9.1. [“UnknownHostException: host.docker.internal”](#91-unknownhostexception-hostdockerinternal)  
   - 9.2. [Repeated “Auto-Creation Request for __consumer_offsets” Messages](#92-repeated-auto-creation-request-for-__consumer_offsets-messages)  
   - 9.3. [Connectivity Tests from the Mac](#93-connectivity-tests-from-the-mac)  
   - 9.4. [Verifying Internal Topic Settings](#94-verifying-internal-topic-settings)  
10. [Common Pitfalls and Recommendations](#10-common-pitfalls-and-recommendations)  
11. [Appendix](#11-appendix)  
   - A. [Generating a Static Cluster ID](#a-generating-a-static-cluster-id)  
   - B. [Removing Old Volumes](#b-removing-old-volumes)  
   - C. [Sample `.env` File](#c-sample-env-file)  

---

## 1. Introduction

This document serves as a comprehensive manual for setting up and operating a single-node Apache Kafka 4.x (KRaft mode) broker inside Docker, hosted on a “side” machine (e.g., a Windows/WSL2 host), with a development client (e.g., a Spring Boot application) running on a macOS laptop. It consolidates configuration guidelines, best practices, environment variable usage, troubleshooting steps, and verification procedures based on recent experiences.

By the end of this manual, you will know how to:
- Configure Kafka in KRaft mode (no ZooKeeper).
- Dynamically advertise the broker’s host IP without hard-coding.
- Ensure that internal topics (especially `__consumer_offsets`) are created successfully in a single-broker environment.
- Deploy Kafka via Docker Compose.
- Connect a Spring Boot consumer/producer on macOS to the remote broker.
- Troubleshoot common connectivity and configuration errors.

---

## 2. Prerequisites

Before proceeding with the instructions below, ensure that you have the following:
1. **Docker Engine** and **Docker Compose** installed on the side machine (Windows or WSL2).  
2. **macOS** with a Java 11+ (or Java 17/21) JDK, Spring Boot and Kafka client libraries on your development machine.  
3. Basic familiarity with:
   - Docker Compose syntax (`version`, `services`, `environment`, `volumes`, etc.).  
   - Kafka fundamentals (brokers, topics, listeners, partitions, replication).  
   - Spring Boot application configuration (`application.yml` or `application.properties`).  
4. A static or dynamically assigned LAN IP for the side machine, e.g., `192.168.1.113`. You must be able to determine (and update) this IP whenever it changes.
5. (Optional) A UUID generator (e.g., `uuidgen`) to create a stable `CLUSTER_ID`.

---

## 3. Environment Overview

- **Side Machine** (Windows/WSL2):  
  - Hosts Docker daemon.  
  - Runs Kafka broker in KRaft mode via Docker Compose.  
  - Exposes ports 9092 (client) and 9093 (controller) to the LAN.  

- **macOS Development Machine**:  
  - Runs a Spring Boot application (e.g., `LoanWorkerApplication`) that acts as a Kafka consumer/producer.  
  - Connects to the Kafka broker using the side machine’s LAN IP (e.g., `192.168.1.113:9092`).  

- **Networking**:  
  - The side machine’s LAN IP may change (DHCP, network switch, etc.).  
  - Use an environment variable (`KAFKA_HOST_IP`) to inject the current IP into the broker’s `KAFKA_ADVERTISED_LISTENERS`.  
  - On macOS, verify connectivity via `telnet <HOST_IP> 9092` or `nc -vz <HOST_IP> 9092`.  

---

## 4. File Structure

```
project-root/
├── docker-compose.kafka.yaml
├── MANUAL-KAFKA.md   ← (this document)
├── .env              ← contains KAFKA_HOST_IP
└── spring-boot-app/
    └── src/main/resources/
        └── application.yml
```

- **`docker-compose.kafka.yaml`**  
  The Docker Compose file that defines the Kafka broker service and associated volumes.  

- **`MANUAL-KAFKA.md`**  
  This manual—intended to be placed at the root of your version-controlled repository for easy reference.  

- **`.env`**  
  Stores the environment variable `KAFKA_HOST_IP=<current LAN IP>` so Docker Compose will interpolate it at runtime.  

- **`spring-boot-app/src/main/resources/application.yml`**  
  Contains the Spring Boot consumer/producer configuration referencing the broker’s address.  

---

## 5. Environment Variables

### 5.1 `.env` File

Create a file named `.env` in the same directory as `docker-compose.kafka.yaml` with exactly one line:

```dotenv
# .env
KAFKA_HOST_IP=192.168.1.113
```

- **`KAFKA_HOST_IP`**  
  - Must be set to the side machine’s current IPv4 address on the LAN.  
  - Docker Compose reads this variable at startup and substitutes it wherever `${KAFKA_HOST_IP}` appears in the YAML.  
  - Whenever the side machine reboots or its IP changes, update this value and re-run the Compose command.

### 5.2 PowerShell / WSL Exports

Optionally, you can set it directly in your shell session before invoking Docker Compose:

**PowerShell (Windows)**  
```powershell
# Determine current IPv4 for a named interface (e.g., “Ethernet”)
$ip = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "Ethernet" | Where-Object { $_.PrefixOrigin -ne "WellKnown" }).IPAddress
$env:KAFKA_HOST_IP = $ip
Write-Host "Kafka will advertise at $env:KAFKA_HOST_IP"
```

**WSL2 (Linux Shell)**  
```bash
# Assuming the WSL network interface is eth0
export KAFKA_HOST_IP=$(ip addr show eth0 | grep 'inet ' | awk '{print $2}' | cut -d'/' -f1)
echo "Kafka will advertise at $KAFKA_HOST_IP"
```

After exporting, simply run:
```bash
docker-compose -f docker-compose.kafka.yaml up -d
```

> **Note:** If you set `KAFKA_HOST_IP` via `.env`, you do not need to re-export in shell. Docker Compose will load the `.env` file automatically.

---

## 6. Docker Compose Configuration

Below is the complete, annotated `docker-compose.kafka.yaml` file. Copy or reference this exactly, then place it alongside your `.env` file.

```yaml
# ── docker-compose.kafka.yaml ──
#
# Single-node Apache Kafka 4.x (KRaft mode) broker. 
# Exposes PLAINTEXT on 9092 for clients and CONTROLLER on 9093 for Kafka’s internal controller.

version: "3.9"

services:
  kafka:
    image: apache/kafka:4.0.0
    container_name: kafka
    hostname: kafka

    # ── Exposed Ports ──────────────────────────────────────────
    # 9092: Client connections (e.g., Spring Boot app on macOS)
    # 9093: Controller listener (internal to KRaft)
    ports:
      - "9092:9092"
      - "9093:9093"

    environment:
      # ── 1. Listeners                                 ─────────────────
      # Inside the container, Kafka binds to 0.0.0.0:9092 for PLAINTEXT 
      # and 0.0.0.0:9093 for the CONTROLLER listener.
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093

      # ── 2. Advertised Listeners (Dynamic Host IP)   ─────────────────
      # Uses environment variable from `.env` or shell export.
      # The actual hostname/IP that clients (macOS) will connect to.
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://${KAFKA_HOST_IP}:9092

      # ── 3. Inter-Broker & Controller Listener Names ─────────────────
      # Specifies which listener name is used for inter-broker traffic 
      # and which is used for controller communication.
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER

      # ── 4. KRaft Mode Essentials (No ZooKeeper)    ─────────────────
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093

      # Use a static cluster ID you generate once (see Appendix A).
      CLUSTER_ID: "demo-cluster-id-replace-with-real"

      # ── 5. Internal Topics Configuration             ─────────────────
      # In a single-node cluster, internal topics like __consumer_offsets 
      # default to replication factor 3, which cannot be satisfied. 
      # Override to replicate only to this one broker.
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS: 1

      # ── 6. Broker Defaults & Dev Conveniences        ─────────────────
      # Number of partitions for any new topic by default.
      KAFKA_NUM_PARTITIONS: 1

      # Where Kafka stores its log data inside the container.
      KAFKA_LOG_DIRS: "/var/lib/kafka/data"

      # Allow automatic topic creation (including internal topics) 
      # and topic deletion for convenience in development.
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_DELETE_TOPIC_ENABLE: "true"

    volumes:
      # Persist Kafka data between container restarts. 
      - kafka_data:/var/lib/kafka/data

    restart: unless-stopped

volumes:
  kafka_data:
```

### 6.1. Listeners and Ports

- **`KAFKA_LISTENERS`**  
  - `PLAINTEXT://0.0.0.0:9092` → Binds to all container interfaces on port 9092 for client traffic (producers/consumers).  
  - `CONTROLLER://0.0.0.0:9093` → Binds to all interfaces on port 9093 to handle KRaft’s internal controller communication.

- **`ports`** mapping  
  - `9092:9092` → Exposes container’s 9092 as host’s 9092.  
  - `9093:9093` → Exposes container’s 9093 as host’s 9093 (mainly for internal mechanics; clients normally only need 9092).

### 6.2. Advertised Listeners (Dynamic Host IP)

- **`KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://${KAFKA_HOST_IP}:9092`**  
  - Ensures that the broker advertises the correct, externally reachable IP/port.  
  - Avoids using `host.docker.internal` because the broker is not running on Docker Desktop; it is on a separate side machine.  
  - `${KAFKA_HOST_IP}` is read from either the `.env` file or your shell environment.

### 6.3. Inter-Broker and Controller Listener Names

- **`KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT`**  
  Defines which listener (by name) is used when brokers communicate with each other. In a single-node setup, this is effectively the same as the client listener.  

- **`KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER`**  
  In KRaft mode, the controller (which manages metadata, topic creation, partition assignments) also needs a listener. We designate port 9093 for that purpose.

### 6.4. KRaft Mode Essentials

- **`KAFKA_PROCESS_ROLES: broker,controller`**  
  Runs this node as both a broker (to handle producer/consumer traffic) and a controller (to manage cluster metadata).  

- **`KAFKA_NODE_ID: 1`**  
  The unique identifier for this broker/controller instance.  

- **`KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093`**  
  Tells Kafka which nodes participate in the controller quorum. Format is `<nodeId>@<hostname>:<controllerPort>`. Here, the hostname is `kafka` (the container’s hostname), and port is 9093.  

- **`CLUSTER_ID: "demo-cluster-id-replace-with-real"`**  
  A unique identifier for the Kafka cluster. Generate once, store in `.env` or your Compose file, and never change it—changing it resets all internal metadata.

### 6.5. Internal Topics Configuration

- **`KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1`**  
  By default, Kafka attempts to create `__consumer_offsets` with a replication factor of 3. In a single-node cluster, that cannot succeed (not enough replicas). Setting it to 1 ensures the topic is created exactly once on this broker.

- **`KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS: 1`**  
  One partition is sufficient for dev or single-node environments. Kafka will no longer retry creating additional partitions.

### 6.6. Broker Defaults and Development Conveniences

- **`KAFKA_NUM_PARTITIONS: 1`**  
  Default partition count for new topics created without specifying partitions.  

- **`KAFKA_LOG_DIRS: "/var/lib/kafka/data"`**  
  Directory inside the container where Kafka stores logs. Mapped to a Docker volume to persist data.  

- **`KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"`**  
  Allows Kafka to automatically create any topic when a producer or consumer attempts to use it. This includes internal topics.  

- **`KAFKA_DELETE_TOPIC_ENABLE: "true"`**  
  Permits topic deletion via `kafka-topics --delete ...`. Useful in development but not recommended for production without careful controls.

---

## 7. Step-by-Step Deployment

Below is the recommended workflow for deploying and verifying the Kafka broker. Whenever the side machine’s IP changes, follow steps 7.1–7.3 again.

### 7.1. Setting the Host IP Environment Variable

1. **Locate your `.env` file** (same directory as `docker-compose.kafka.yaml`).  
2. **Update** the file so that it contains exactly:

   ```dotenv
   KAFKA_HOST_IP=<current_LAN_IP_of_side_machine>
   ```

   For example, if your Windows machine’s current IPv4 is `192.168.1.113`:

   ```dotenv
   KAFKA_HOST_IP=192.168.1.113
   ```

3. Ensure that the `.env` file is saved. Docker Compose automatically loads environment variables from this file.

> **Alternative (PowerShell/WSL):** If you prefer not to use a `.env` file, export the variable directly in the shell before running Compose:
> 
> - **PowerShell**  
>   ```powershell
>   $env:KAFKA_HOST_IP = "192.168.1.113"
>   docker-compose -f docker-compose.kafka.yaml up -d
>   ```
> - **WSL2 (bash/zsh)**  
>   ```bash
>   export KAFKA_HOST_IP=192.168.1.113
>   docker-compose -f docker-compose.kafka.yaml up -d
>   ```

### 7.2. Bringing Up the Kafka Service

From the same directory as `docker-compose.kafka.yaml`, run:

```bash
docker-compose up -d
```

- The `-d` flag runs the container in detached mode.  
- Docker Compose will read `KAFKA_HOST_IP` either from your environment or from the `.env` file and substitute it into `KAFKA_ADVERTISED_LISTENERS`.  
- The broker container (`kafka`) will start and attempt to create internal topics.

### 7.3. Verifying Broker Startup

1. **View container logs** to confirm that Kafka has successfully created `__consumer_offsets`:

   ```bash
   docker logs -f kafka
   ```

   Look for these key sequences:
   -  
     ```
     [RaftManager id=1] Completed transition to Leader(…)
     [BrokerServer id=1] Successfully registered broker 1 with broker epoch …
     ```
     These lines indicate that the broker has become the cluster leader and registered itself successfully.
   -  
     ```
     [DefaultAutoTopicCreationManager] Created topic __consumer_offsets (replication=1, partitions=1)
     ```
     This single “Created topic __consumer_offsets…” line indicates success. You should **not** see repeated “Sent auto-creation request …” messages afterward.

2. **Verify connectivity from macOS**:

   On your Mac Terminal:

   ```bash
   nc -vz 192.168.1.113 9092
   ```

   - If you see “succeeded” or “open,” then port 9092 is reachable.  
   - If it fails, check for:  
     - Firewall on Windows blocking port 9092.  
     - Docker port mapping errors (ensure no other service is binding to host port 9092).  
   - Double-check that `KAFKA_ADVERTISED_LISTENERS` uses the same IP that your Mac is testing.

3. **Inspect the internal topics** (using a Kafka client tool if available):

   ```bash
   kafka-topics --bootstrap-server 192.168.1.113:9092      --describe --topic __consumer_offsets
   ```

   Expected output:

   ```
   Topic: __consumer_offsets  PartitionCount:1  ReplicationFactor:1  ...
     Partition: 0  Leader: 1  Replicas: 1  Isr: 1
   ```

   This confirms that the internal offsets topic exists and is configured correctly.

---

## 8. Spring Boot Client Configuration

On your macOS development machine, configure your Spring Boot application to point at the remote broker. In `src/main/resources/application.yml`, use:

```yaml
spring:
  kafka:
    bootstrap-servers: 192.168.1.113:9092
    consumer:
      group-id: loan-worker-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

  # JPA settings, logging, etc. can follow...
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
    org.springframework.boot.context.config: DEBUG
    org:
      springframework:
        kafka: ERROR
      apache:
        kafka: ERROR
```

- **`bootstrap-servers: 192.168.1.113:9092`**  
  Must match the IP and port that the broker advertised via `KAFKA_ADVERTISED_LISTENERS`.  
- Adjust any other logging or JPA properties as needed.  

---

## 9. Troubleshooting

Despite following the above instructions, you may still encounter certain errors or misconfigurations. This section addresses the most common scenarios.

### 9.1. “UnknownHostException: host.docker.internal”

**Symptom**  
```
WARN  … Error connecting to node host.docker.internal:9092 (id: 1 rack: null)
java.net.UnknownHostException: host.docker.internal
```

**Cause**  
- `host.docker.internal` is a special hostname that resolves to your host machine’s IP **only when Docker Desktop is running locally on macOS/Windows**.  
- In our setup, Kafka is running in Docker on a separate side machine, not Docker Desktop. Therefore, `host.docker.internal` is not resolvable in the broker container’s DNS.

**Solution**  
- Use the actual LAN IP address of the side machine (injected via `${KAFKA_HOST_IP}`) in `KAFKA_ADVERTISED_LISTENERS`.  
- Remove any references to `host.docker.internal` from your Compose file. After updating, redeploy and confirm the broker logs advertise the correct IP.

### 9.2. Repeated “Auto-Creation Request for `__consumer_offsets`” Messages

**Symptom**  
```
[2025-06-03 …] INFO Sent auto-creation request for Set(__consumer_offsets) to the active controller.
[2025-06-03 …] INFO Sent auto-creation request for Set(__consumer_offsets) to the active controller.
…
(repeated many times)
```

**Cause**  
- By default, Kafka tries to create `__consumer_offsets` with a replication factor of 3. In a single-node cluster, that cannot be satisfied—no other brokers exist—so the controller keeps retrying.

**Solution**  
1. In `docker-compose.kafka.yaml`, ensure the following environment variables are set:
   ```yaml
   KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
   KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS: 1
   ```
2. Tear down any existing volume that may contain a partially created offsets topic:
   ```bash
   docker-compose down -v
   ```
3. Bring the broker back up:
   ```bash
   docker-compose up -d
   ```
4. Confirm that you now see exactly one “Created topic __consumer_offsets…” log entry and no further retries.

### 9.3. Connectivity Tests from the Mac

After the broker is up, verify network-level connectivity:

```bash
telnet 192.168.1.113 9092
# or
nc -vz 192.168.1.113 9092
```

- **If it fails**:  
  - Verify Windows (or WSL2) firewall is not blocking port 9092.  
  - Ensure that Docker’s `ports` mapping (`9092:9092`) is correct and not overridden by another service.  
  - Double-check that `KAFKA_ADVERTISED_LISTENERS` uses the same IP that your Mac is testing.  

### 9.4. Verifying Internal Topic Settings

Use the Kafka CLI to confirm replication and partition settings:

```bash
# Assuming kafka-topics.sh is on your path (or installed via Homebrew/SDKMAN)
kafka-topics --bootstrap-server 192.168.1.113:9092   --describe --topic __consumer_offsets
```

- You should see:
  ```
  Topic: __consumer_offsets  PartitionCount:1  ReplicationFactor:1  ...
    Partition: 0  Leader: 1  Replicas: 1  Isr: 1
  ```
- **If you see a replication factor > 1 or multiple partitions**, it indicates your environment variables were not applied. Repeat steps in Section 6 to correct them, then rebuild the broker.

---

## 10. Common Pitfalls and Recommendations

1. **Hard-coding the broker IP** in the Compose file is fragile. Use `${KAFKA_HOST_IP}` and update it whenever the host’s IP changes.  
2. **Never rely on `host.docker.internal`** when the broker runs on a separate machine’s Docker engine. That hostname only works with Docker Desktop on the same host.  
3. **Remove and recreate volumes** (`docker-compose down -v`) whenever you change an internal-topic replication factor. Old data may have a stuck, partially created topic.  
4. **Single-broker KRaft defaults assume replication > 1.** Always override replication-only-for-single-node for any internal topics (`__consumer_offsets`, transaction logs, etc.)  
5. **Use a static `CLUSTER_ID`.** Generate once, store in `.env` or your Compose file, and never change it—changing it resets all internal metadata.  
6. **Monitor the controller’s readiness** in the logs. A few “auto-creation request” retries during startup are normal. If they persist beyond 5–10 seconds, replication settings are misconfigured.  
7. **If you need to expose multiple brokers later**, adjust `KAFKA_CONTROLLER_QUORUM_VOTERS` to join multiple nodes, increase replication factors accordingly, and ensure each broker has a unique `KAFKA_NODE_ID`.  

---

## 11. Appendix

### A. Generating a Static Cluster ID

Kafka KRaft mode requires a `CLUSTER_ID`. In production, you might store this in a key-value store or a secure vault. In development, you can generate one and hard-code it:

```bash
# On Linux / WSL2 / macOS:
uuidgen | tr -d "-"
# Example output: 8f5e8a3e2c764d9084fbc11d092ec691

# Use that value in docker-compose.kafka.yaml:
CLUSTER_ID: "8f5e8a3e2c764d9084fbc11d092ec691"
```

Once chosen, do not change it. If you do, Kafka will treat your cluster as brand new and attempt to reinitialize all internal topics (potentially failing if volumes still hold the old data).

### B. Removing Old Volumes

Whenever you change replication settings for internal topics (e.g., `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR`), you must remove existing data volumes before restarting. Otherwise, Kafka may be stuck with mismatched metadata.

```bash
docker-compose down -v
docker-compose up -d
```

- `down -v` → Stops containers and removes volumes defined in your Compose file (in this case, `kafka_data`).  
- `up -d` → Recreates the container and volume fresh.

### C. Sample `.env` File

```dotenv
# .env (place next to docker-compose.kafka.yaml)
KAFKA_HOST_IP=192.168.1.113
```

- Keep this file out of version control if you share the repository with others who will have different LAN IPs.  
- Alternatively, add a template `.env.example` with a placeholder:

  ```dotenv
  # .env.example
  KAFKA_HOST_IP=<your_side_machine_IP_here>
  ```

---

**End of Manual**  
2025-06-03 (last updated)
