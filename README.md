# Revolut Backend Test

This project follows API-first approach:
- The API if [defined in Open API 3.0 Specification format](./src/main/resources/api-definition/account-funds-service.oas3.yml)
- the API requests and responses are [defined as a set of JSON schema files](./src/main/resources/api-definition/schemas)
- `jsonschema2pojo` plugin transforms the JSON schemas into Java classes

## Assumptions made

- Account identification (such as by email or by IBAN etc.) is performed by the client of the API. 
  This API only accepts the IDs of the accounts
- The service uses its own database. Additional details, such as account owner, type of account (is
  it a private or business account) etc. are supposed to be stored in other services
- The service's scope is limited to transferring from one account to another within the same service.
  External transfers are done in a separate service that integrates with a payment system such as SWIFT.
- The API is a generic transfer API that can operate on any accounts in the bank.
  A frontent-specific API (web, mobile, ...) will use this API and will expose its own set of endpoints
  to restrict a particular user from accessing other users' accounts.
- A client of the API should be able to perform retries and the API should process retries idempotently
  For that `operationId` request field is introduced. Any repeated requests with the same `operationId`
  will result in the same response as the first request.
- The service implementation will use an embedded database and thus in its current shape it will not 
  be able to scale horizontally without using a database shared between instances. Nevertheless,
  this implementation will ensure correctness in the presence of concurrent request as much as possible.
- No currency conversion is performed. Both accounts must use the same currency.

## Chosen technologies

- Programming language: Java 11
- Build tool: Gradle 5
- Web framework: Micronaut (+ Micronaut Data JDBC)
- Test framework: JUnit 5


## Requirements for running the application

Gradle `5.X.X` and `JDK 11` 

## Building the executable Jar file

```bash
./gradlew --full-stacktrace --info clean test integrationTest shadowJar
```

## Running the executable Jar

```bash
java -jar build/libs/revolut-test-0.0.1-all.jar
```

## Building a docker image

```bash
docker build -t revolut-test .
```

## Running the docker image
```bash
docker run -p 8080:8080 revolut-test
```

## Accessing the API

The application exposes the API on port `8080`.
An example of using `curl` for invoking the API:
```bash
curl --request POST \
  --url http://localhost:8080/api/v1/transfer \
  --header 'content-type: application/json' \
  --data '{
	"operationId": "b19d837f-2b85-4ec6-8c57-3a83cae34139 ",
	"accounts": {
		"from": {
			"id": "48e3d142-e5d6-442a-bf61-42c3e5673700"
		},
		"to": {
			"id": "a27fa283-f638-49d1-b150-8adf065c80e2"
		}
	},
	"amount": {
		"value": "90.05",
		"currency": "EUR"
	},
	"message": "Test transfer"
}'
```