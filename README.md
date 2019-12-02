# Revolut Backend Test

The service allows to perform transfers between two accounts in the bank. It stores
transfer history separately from the balance.
The API specification is [documented in Open API 3.0 Specification format](./specs/account-funds-service.oas3.yml)
 

## Assumptions made

- Account identification (such as by email or by IBAN etc.) is performed by the client of the API. 
  This API only accepts the IDs of the accounts
- The service uses its own database. Additional details, such as account owner, type of account (is
  it a private or business account) etc. are supposed to be stored in other services
- The service's scope is limited to transferring from one account to another within the same service.
  External transfers are done in a separate service that integrates with a payment system such as SWIFT.
- Some important functions, such as transfer history, transfer audit, overdraft fee calculation etc. 
  are also assumed to be implemented in separate services. A possible way of sharing the transfers is using 
  change data capture (CDC) - based event sourcing.
- The API is a generic transfer API that can operate on any accounts in the bank.
  A frontent-specific API (web, mobile, ...) will use this API and will expose its own set of endpoints
  to restrict a particular user from accessing other users' accounts.
- additional means of validation (such as daily transfer limit) are client's responsibility.
- A client of the API should be able to perform retries and the API should process retries idempotently
  For that `operationId` request field is introduced. Any repeated requests with the same `operationId`
  will result in the same response as the first request.
- The service implementation will use an embedded database and thus in its current shape it will not 
  be able to scale horizontally without using a database shared between instances. Nevertheless,
  this implementation will ensure correctness in the presence of concurrent request as much as possible.
- No currency conversion is performed. Both accounts must use the same currency.
  If currency conversion is required, it should be done by the client.
- All API amount values have 2 digits after the decimal point, representing the cents.
- The only reason for transfer rejection in this service if the sender account doesn't have
  enough funds.
- Transfer size should be positive and smaller than 10 trillion (9 999 999 999 999.99).
- The maximum account balance is not defined.

## Chosen technologies

- Programming language: Java 11
- Build tool: Gradle 5
- Web framework: Micronaut
- In-memory database: H2
- Persistence: JDBC
- Test framework: JUnit 5

## Implementation notes
- This project follows API-first approach:  API requests and responses are [defined as a set of JSON schema files](./specs/schemas).
  `jsonschema2pojo` plugin transforms the JSON schemas into Java classes
  
- In the most cases it's assumed that validation is performed by the Java validation API
  and/or not necessary due to the use of JSR-305 annotations
  and therefore no additional argument validation is needed. Such cases aren't tested.
  
- Some of repository methods are implemented for test purposes only.
  They are marked as such by the use of comments and aren't explicitly covered by tests.

- Overdraft support is not implemented as a feature but the data model and the code base
  make adding it a straightforward task.
  
- A testing controller is added to create AccountFunds (a representation of account that holds balance).
  While it's used to create an account funds entry in the app, the endpoints it exposes are not part of the 'official'
  application API. Personally for me, a more preferred way of creating those entries would be through asynchronous
  messaging such as handling an `AccountCreated` event.
  
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

### to create the initial balance entries
This is not a part of the specification but is necessary for testing this implementation.
```bash
curl --request POST \
  --url http://localhost:8080/api/v1/account-funds \
  --header 'content-type: application/json' \
  --data '{
	"accountId": "a27fa283-f638-49d1-b150-8adf065c80e2",
	"balance": "0.00",
	"currency": "EUR"
}'

curl --request POST \
  --url http://localhost:8080/api/v1/account-funds \
  --header 'content-type: application/json' \
  --data '{
	"accountId": "48e3d142-e5d6-442a-bf61-42c3e5673700",
	"balance": "100.00",
	"currency": "EUR"
}'
```

### to perform a transfer
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

### to check updated balance

```bash
curl --request GET \
  --url http://localhost:8080/api/v1/account-funds/a27fa283-f638-49d1-b150-8adf065c80e2

curl --request GET \
  --url http://localhost:8080/api/v1/account-funds/48e3d142-e5d6-442a-bf61-42c3e5673700
```
