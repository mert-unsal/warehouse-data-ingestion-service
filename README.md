# Warehouse Data Ingestion Service

A Spring Boot microservice for ingesting, processing, and managing warehouse data for IKEA. The service supports scalable data ingestion via Kafka, exposes RESTful APIs for warehouse operations, and provides robust logging and management endpoints.

## Features
- Kafka-based data ingestion for real-time warehouse events
- RESTful API endpoints for product and inventory management
- Centralized logging configuration
- Management endpoints for health and metrics
- Exception handling and validation
- OpenAPI (Swagger) documentation

## Technologies Used
- Java 21
- Spring Boot
- Apache Kafka
- Maven

## Prerequisites
- Java 21
- Maven 3.6+
- Kafka broker (for data ingestion)

## Setup Instructions

### 1. Clone the repository
```sh
git clone git@github.com:YOUR_ORG/warehouse-data-ingestion-service.git
cd warehouse-data-ingestion-service
```

### 2. Configure application properties
Edit the YAML files in `src/main/resources/` as needed:
- `application.yaml`: main application config
- `application-kafka.yaml`: Kafka settings
- `application-logging.yaml`: logging config
- `application-management.yaml`: management endpoints

### 3. Build the project
```sh
./mvnw clean package
```

### 4. Run the service
```sh
./mvnw spring-boot:run
```

### 5. Run tests
```sh
./mvnw test
```

## Usage

### API Endpoints
The service exposes RESTful endpoints for warehouse data operations, including:
- `/api/products`: Manage products
- `/api/inventory`: Manage inventory

See the OpenAPI documentation at [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html) when the service is running for full details and example requests.

### Kafka Integration
Configure Kafka settings in `application-kafka.yaml` to enable real-time data ingestion from warehouse event streams.

## Contributing
Contributions are welcome! Please open issues or submit pull requests. See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License
This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

## Contact
For support or questions, please contact the maintainers via GitHub Issues.
