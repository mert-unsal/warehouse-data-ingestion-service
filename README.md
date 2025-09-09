# Warehouse Data Ingestion Service

A Spring Boot edge (ingestion) microservice for accepting bulk warehouse data files (JSON) and publishing normalized inventory & product update events to Kafka. It provides only file upload endpoints (multipart/form-data); no CRUD or query APIs. Designed to decouple external data acquisition from downstream consumers (e.g. data consumer, command/query services).

## Architecture Overview
Flow:
1. Client uploads a JSON file (inventory or products) via REST multipart endpoint.
2. Service streams & deserializes payload into wrapper DTOs (`InventoryData`, `ProductsData`).
3. Records are mapped to event records (`InventoryUpdateEvent`, `ProductUpdateEvent`).
4. Batch publish to Kafka topics (one message per record) using `KafkaProducerService`.
5. Basic validation & error handling return early on malformed/empty files.
6. Downstream services consume and persist/transform as needed.

Design Highlights:
- Simple, append-only ingestion layer (stateless aside from transient processing).
- Separation of transport (file) vs internal event model (Kafka records).
- Structured logging & OpenTelemetry hooks (exporters disabled by default; ready for enablement).
- Resilient batch send with completion coordination; individual send failures raise `KafkaProduceFailedException`.

## Features
- Upload & parse inventory JSON file.
- Upload & parse products JSON file.
- Publish each parsed row as an individual Kafka message (keyed by article id or product name).
- Centralized error handling (validation, file, Kafka failures).
- Actuator health and OpenAPI documentation.
- Java 21 + preview flags (for consistency across modules).

## Data File Formats (Input)
Inventory file (example `inventory.json`):
```json
{
  "inventory": [
    { "art_id": "1", "name": "table leg", "stock": "50" },
    { "art_id": "2", "name": "screw", "stock": "500" }
  ]
}
```
Products file (example `products.json`):
```json
{
  "products": [
    {
      "name": "Dining Table",
      "contain_articles": [
        { "art_id": "1", "amount_of": "4" },
        { "art_id": "2", "amount_of": "12" }
      ]
    },
    {
      "name": "Coffee Table",
      "contain_articles": [
        { "art_id": "1", "amount_of": "4" }
      ]
    }
  ]
}
```
Notes:
- Numeric quantities (`stock`, `amount_of`) are strings in the ingestion DTOs & events. Downstream consumers currently coerce them to numeric types. Consider normalizing to numeric before publishing if strong typing is preferred.

## Published Kafka Events (Output)
InventoryUpdateEvent (current schema emitted):
```json
{
  "artId": "1",
  "name": "table leg",
  "stock": "50",
  "fileCreatedAt": "2025-09-05T12:00:00Z"
}
```
ProductUpdateEvent:
```json
{
  "name": "Dining Table",
  "containArticles": [
    { "art_id": "1", "amount_of": "4" },
    { "art_id": "2", "amount_of": "12" }
  ],
  "fileCreatedAt": "2025-09-05T12:00:00Z"
}
```
Field Mapping Notes:
- Input snake_case arrays (`contain_articles`, `art_id`, `amount_of`) preserved inside nested structures.
- `fileCreatedAt` is assigned server-side at upload time (Instant.now()).

## REST Endpoints
Base Port: 8081 (override with `PORT`).

Inventory Upload:
- `POST /api/v1/inventory/upload`
  - Multipart form field: `file` (JSON file as described above)
  - Success: 200 text/plain (`Inventory uploaded successfully`).

Products Upload:
- `POST /api/v1/products/upload`
  - Multipart form field: `file`
  - Success: 200 text/plain (`Products uploaded successfully`).

OpenAPI UI: `http://localhost:8081/swagger-ui.html`
OpenAPI JSON: `http://localhost:8081/api-docs`
Health: `GET /actuator/health`

## Sample cURL
```bash
# Upload inventory file
curl -X POST http://localhost:8081/api/v1/inventory/upload \
  -F "file=@inventory.json;type=application/json"

# Upload products file
curl -X POST http://localhost:8081/api/v1/products/upload \
  -F "file=@products.json;type=application/json"
```

## Kafka Configuration
Properties (application-kafka.yaml):
- Bootstrap servers: `${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`
- Producer: JSON serialization, idempotence enabled, linger 500ms for small batching.
- Topics:
  - Inventory: `${KAFKA_TOPIC_INVENTORY:ikea.warehouse.inventory.update.topic}` (referenced as `app.kafka.topics.inventory`)
  - Product: `${KAFKA_TOPIC_PRODUCT:ikea.warehouse.product.update.topic}` (referenced as `app.kafka.topics.product`)

Batch Send Behavior:
- Each record is sent with key (article id or product name) to allow partition affinity.
- Fail-fast on any future completion error (throws `KafkaProduceFailedException`).

## Configuration & Environment
Active profiles (default): `default,logging,management,kafka`
Key YAML files:
- `application.yaml` – core + multipart limits (10MB file size, 50MB request)
- `application-kafka.yaml` – producer & topic settings
- `application-logging.yaml` – log levels
- `application-management.yaml` – actuator endpoints

Environment Variables:
```
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_PRODUCT=ikea.warehouse.product.update.topic
KAFKA_TOPIC_INVENTORY=ikea.warehouse.inventory.update.topic
PORT=8081
OTEL_SERVICE_NAME=warehouse-data-ingestion-service
```
OpenTelemetry exporters are disabled (set to `none`) by default; supply OTEL_EXPORTER_* vars to enable.

## Validation & Error Handling
Validation Scope:
- Rejects empty/missing file.
- Relies on JSON structure matching wrapper DTOs. (No advanced schema validation yet.)

Common Exceptions:
- `FileProcessingException` – malformed/empty file -> 400
- `KafkaProduceFailedException` – Kafka send failure
- Generic exceptions -> 500

(If a GlobalExceptionHandler class exists it maps to structured JSON; extend README once error payload schema is finalized.)

## Observability & Logging
- Log levels tuned via `application-logging.yaml`.
- Structured JSON logging supported through logstash encoder (add appenders as needed).
- OpenTelemetry instrumentation present (trace context propagated to Kafka if downstream consumers also instrumented).

## Running Locally
With Maven:
```bash
mvn clean spring-boot:run -Dspring-boot.run.profiles=default,logging,management,kafka \
  -Dspring-boot.run.jvmArguments="--enable-preview"
```
Build JAR:
```bash
mvn clean package
java --enable-preview -jar target/warehouse-data-ingestion-service-0.0.1-SNAPSHOT.jar
```
Docker (service only):
```bash
docker build -t ikea/warehouse-data-ingestion-service .
```
Docker Compose (from monorepo root):
```bash
docker compose up -d --build warehouse-data-ingestion-service
```
Ensure Kafka broker reachable at `kafka:29092` (inside compose network) or override `KAFKA_BOOTSTRAP_SERVERS` for standalone runs.

## Processing Pipeline Summary
1. Accept multipart upload.
2. Deserialize to wrapper DTO (Jackson).
3. Map each element to builder-based event record adding `fileCreatedAt` timestamp.
4. Build key->event map.
5. Parallel async sends; await all futures.
6. On success: 200 response; on first failure: throw and surface error.

## Future Enhancements
- Stream parsing (Jackson `JsonParser`) to handle very large files.
- Add checksum + idempotency keys to prevent duplicate replays.
- Introduce schema validation (JSON Schema or Avro + schema registry) for stronger contracts.
- Retry/backoff on transient Kafka failures (currently fail-fast on send completion).
- Optional dead-letter topic for rejected/malformed entries.
