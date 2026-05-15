# Quickstart: Calculator UI

**Branch**: `001-build-application-perform`  
**Date**: 2026-05-14

---

## Prerequisites

| Tool | Minimum version | Check |
|---|---|---|
| Java JDK | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| A modern browser | — | Chrome / Firefox / Safari / Edge (current stable) |

---

## 1. Generate the Spring Boot project skeleton

Use Spring Initializr to generate a Maven project, then place it at the repository root.

**Via browser** (https://start.spring.io):
- Project: Maven
- Language: Java
- Spring Boot: 3.4.x (latest stable)
- Group: `com.example`, Artifact: `calculator`
- Java: 21
- Dependencies: **Spring Web**, **Validation**

Download the ZIP, extract, and copy the contents to the repository root (so `pom.xml` is at the repository root).

**Or via `curl`** (one-liner):
```bash
curl -s "https://start.spring.io/starter.tgz" \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.4.5 \
  -d baseDir=calculator \
  -d groupId=com.example \
  -d artifactId=calculator \
  -d name=calculator \
  -d packageName=com.example.calculator \
  -d javaVersion=21 \
  -d dependencies=web,validation \
  | tar -xzvf -
# Then move the generated files to the repo root:
mv calculator/* . && rmdir calculator
```

---

## 2. Create the source files

Refer to [plan.md](plan.md) for the full directory layout. Create each file in the path shown:

```
src/main/java/com/example/calculator/
  CalculatorApplication.java
  controller/CalculatorController.java
  dto/CalculationRequest.java
  dto/CalculationResponse.java
  service/CalculatorService.java

src/main/resources/static/
  index.html

src/test/java/com/example/calculator/
  controller/CalculatorControllerTest.java
  service/CalculatorServiceTest.java
```

---

## 3. Run the tests

```bash
./mvnw test
```

Expected: all tests pass (green).

---

## 4. Start the application

```bash
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**.

---

## 5. Open the UI

Open **http://localhost:8080** in a browser.

You should see:
- Two numeric input fields labelled "First number" and "Second number"
- A dropdown labelled "Operation" with options: Add, Subtract, Multiply, Divide
- A "Calculate" button
- A result area (initially empty)

---

## 6. Validate acceptance scenarios

Manually verify the following (or run integration tests):

| # | Input A | Input B | Operation | Expected |
|---|---|---|---|---|
| AC-1 | 2 | 3 | Add | Result: 5 |
| AC-2 | 10 | 4 | Subtract | Result: 6 |
| AC-3 | 6 | 7 | Multiply | Result: 42 |
| AC-4 | 8 | 2 | Divide | Result: 4 |
| AC-5 | (empty) | 3 | Add | Validation error shown |
| AC-6 | 8 | 0 | Divide | Division-by-zero error shown |

---

## 7. Validate keyboard accessibility (SC-003)

1. Open **http://localhost:8080**
2. Press **Tab** to move focus to "First number"
3. Type a value, **Tab** to "Second number", type a value
4. **Tab** to the operation dropdown; use arrow keys to select an operation
5. **Tab** to "Calculate"; press **Enter**
6. Verify the result appears — mouse was never required

---

## 8. Build the executable JAR

```bash
./mvnw package -DskipTests
java -jar target/calculator-0.0.1-SNAPSHOT.jar
```

Confirm the app starts and the UI is accessible at **http://localhost:8080**.
