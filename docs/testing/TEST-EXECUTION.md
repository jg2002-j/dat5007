# Test Execution & Metrics Guide

**Document ID**: TEST-EXEC-001  
**Date**: 2026-04-27  
**Phase**: Phase 6 - Test Cycle Closure & Metrics

---

## Overview

This guide explains how to execute the complete test suite, collect coverage metrics, and generate reports for the Weekly Meal Planner backend project.

---

## Prerequisites

- Java 17 or later
- Maven Wrapper scripts (`mvnw`, `mvnw.cmd`) in the project root
- Docker Desktop (for PostgreSQL Dev Services)
- JaCoCo plugin (included in pom.xml)
- REST-Assured (for integration tests, included in pom.xml)

---

## Test Execution Commands

### 1. Run All Tests

```bash
# Execute all unit, integration, and system tests
./mvnw clean test

# Expected output:
# BUILD SUCCESS
# Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
```

### 2. Run Only Unit Tests

```bash
# Execute only unit tests (PlannerServiceTest, RecipeServiceTest)
./mvnw clean test -Dtest=*Service*Test
```

### 3. Run Only Integration Tests

```bash
# Execute integration tests (RecipeRepoIT, PlannerEndpointIT)
# Note: Requires Docker running for PostgreSQL Dev Services
./mvnw clean test -Dtest=*IT
```

### 4. Run Specific Test Class

```bash
# Run only PlannerServiceTest
./mvnw clean test -Dtest=PlannerServiceTest

# Run only PlannerEndpointIT
./mvnw clean test -Dtest=PlannerEndpointIT
```

### 5. Run Tests with Coverage Report

```bash
# Execute all tests AND generate JaCoCo coverage report
./mvnw clean test jacoco:report

# Report output: target/jacoco-report/index.html
```

### 6. Run with Verbose Logging

```bash
# Execute with detailed test output
./mvnw clean test -e -X

# -e: Show stacktraces on failures
# -X: Enable debug logging
```

---

## Coverage Report Generation

### Generate JaCoCo Report

```bash
# Step 1: Run tests with JaCoCo agent
./mvnw clean test jacoco:report

# Step 2: View report in browser
# Open: target/jacoco-report/index.html

# Step 3: Export as CSV (if needed)
# Report also generated at: target/jacoco-report/jacoco.csv
```

### Merge Coverage Data (Unit + Integration)

The pom.xml already includes the merge goal:

```bash
# This is automatically executed during verify phase
./mvnw clean verify jacoco:report

# Merges:
# - target/jacoco.exec (unit tests)
# - target/jacoco-quarkus.exec (integration tests)
# - Into: target/jacoco-merged.exec
```

### Coverage Metrics Interpretation

| Metric | Definition | Target | Good Range |
|--------|-----------|--------|-----------|
| **Line Coverage** | % of lines executed | 85% | 85-95% |
| **Statement Coverage** | % of statements executed | 85% | 85-95% |
| **Decision/Branch Coverage** | % of if/else branches taken | 80% | 80-90% |
| **Method Coverage** | % of methods called | 90% | 90%+ |

---

## Running Tests in CI/CD Pipeline

### GitHub Actions Example

```yaml
name: Test & Coverage

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_PASSWORD: planner
          POSTGRES_DB: meal_planner
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: 17
      
      - name: Run tests
        run: ./mvnw clean test jacoco:report
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v2
        with:
          files: ./target/jacoco-report/jacoco.xml
```

---

## Test Report Locations

### Generated Reports

| Report | Location | Format | Purpose |
|--------|----------|--------|---------|
| JaCoCo Coverage | `target/jacoco-report/index.html` | HTML | Visual coverage analysis |
| JaCoCo CSV | `target/jacoco-report/jacoco.csv` | CSV | Metrics export |
| Surefire Report | `target/surefire-reports/` | XML/TXT | Unit test results |
| Failsafe Report | `target/failsafe-reports/` | XML/TXT | Integration test results |

### Viewing the JaCoCo Report

```bash
# After running: ./mvnw clean test jacoco:report

# On Windows (PowerShell):
Start-Process "target\jacoco-report\index.html"

# On macOS:
open target/jacoco-report/index.html

# On Linux:
xdg-open target/jacoco-report/index.html

# Or open in browser:
# File > Open > target/jacoco-report/index.html
```

---

## Defect Logging Procedure

### Using the Defect Log CSV

**File**: `docs/defect-log/defect-log.csv`

**Columns**:
- **DefectID**: Unique identifier (DEF-001, DEF-002, etc.)
- **Severity**: Critical, High, Medium, Low
- **TestCaseID**: Which test case failed (TR-001, TR-002, etc.)
- **Title**: Short defect summary
- **Description**: Detailed description
- **PreCondition**: State before test
- **Steps**: Steps to reproduce
- **ExpectedResult**: What should happen
- **ActualResult**: What actually happened
- **RootCause**: Investigation findings
- **ResolutionStatus**: Open, In Progress, Resolved, Closed
- **AssignedTo**: Developer responsible
- **CreatedDate**: When discovered
- **ResolvedDate**: When fixed

### Creating a New Defect Entry

```csv
DEF-006,High,TR-035,Example defect title,Full description here,"Precondition state","1. Step 1 2. Step 2","Expected behavior","Actual behavior","Root cause analysis","Open","DeveloperName",2026-04-28,
```

### GitHub Issues Integration

Alternatively, use GitHub Issues with labels:

```bash
# Create a test failure issue
Title: [TEST-FAILURE] TR-001 - Description of failure
Labels: [test-failure], [defect], [critical]
Body:
  Test Case: TR-001
  Severity: Critical
  Steps to Reproduce:
    1. ...
  Expected: ...
  Actual: ...
```

---

## Continuous Monitoring

### Monthly Coverage Trend

Track coverage over time:

```bash
# Extract coverage percentage
./mvnw clean test jacoco:report -q

# Parse CSV for metrics
grep "TOTAL" target/jacoco-report/jacoco.csv | \
  awk -F, '{print "Line Coverage: " $5 "%, Branch: " $6 "%"}'

# Log to file for trend tracking
date +"%Y-%m-%d" >> coverage-trends.log
grep "TOTAL" target/jacoco-report/jacoco.csv | \
  awk -F, '{print $5}' >> coverage-trends.log
```

### Quality Gates

Establish minimum coverage thresholds:

```xml
<!-- In pom.xml: Set minimum coverage requirements -->
<execution>
  <id>report</id>
  <phase>test</phase>
  <goals>
    <goal>report</goal>
  </goals>
</execution>
<execution>
  <id>check</id>
  <phase>test</phase>
  <goals>
    <goal>check</goal>
  </goals>
  <configuration>
    <rules>
      <rule>
        <element>PACKAGE</element>
        <excludes>
          <exclude>*Test</exclude>
        </excludes>
        <limits>
          <limit>
            <counter>LINE</counter>
            <value>COVEREDRATIO</value>
            <minimum>0.80</minimum>
          </limit>
        </limits>
      </rule>
    </rules>
  </configuration>
</execution>
```

---

## Performance Baselines

### Recording Performance Data

From test execution, observe response times:

```
Save operation took: 234ms
View operation took: 156ms
Recipe retrieval took: 89ms
DB query took: 45ms
```

### Setting Performance Goals

| Operation | Current | Target | Threshold |
|-----------|---------|--------|-----------|
| POST /planner/save/day | 234ms | < 500ms | < 2000ms |
| GET /planner/view/day/{date} | 156ms | < 300ms | < 1000ms |
| GET /planner/recipes | ~API latency | < 3000ms | < 5000ms |

---

## Test Summary Report

### Phase Completion Checklist

- [ ] **Phase 1: Static Testing**
  - [ ] Code review completed
  - [ ] RTM created (docs/testing/RTM.md)
  - [ ] Architecture review documented

- [ ] **Phase 2: Unit Testing**
  - [ ] PlannerServiceTest: 7 tests written
  - [ ] RecipeServiceTest: 5 tests written
  - [ ] Statement coverage > 85%
  - [ ] Decision coverage > 80%

- [ ] **Phase 3: Integration Testing**
  - [ ] RecipeRepoIT: 9 tests written
  - [ ] Database transactions verified
  - [ ] Data persistence validated

- [ ] **Phase 4: System Testing**
  - [ ] PlannerEndpointIT: 12 tests written
  - [ ] End-to-end workflows tested
  - [ ] Black-box EP/BVA applied

- [ ] **Phase 5: Performance & Security**
  - [ ] Performance baseline established
  - [ ] Security posture documented
  - [ ] Load testing recommendations

- [ ] **Phase 6: Closure & Metrics**
  - [ ] JaCoCo report generated
  - [ ] Defect log created (docs/defect-log/defect-log.csv)
  - [ ] RTM traceability verified
  - [ ] Test metrics documented

---

## Troubleshooting

### Issue: "Database connection refused"

**Solution**: Ensure Docker is running
```bash
docker ps  # Should show postgres container
docker compose up -d  # If not running
```

### Issue: "Test hangs or timeout"

**Solution**: Increase test timeout in pom.xml
```xml
<maven.surefire.version>3.5.3</maven.surefire.version>
<!-- Configuration -->
<configuration>
  <forkedProcessTimeoutInSeconds>300</forkedProcessTimeoutInSeconds>
</configuration>
```

### Issue: "JaCoCo report not generated"

**Solution**: Ensure verify phase is run
```bash
# Use verify instead of test
./mvnw clean verify jacoco:report
```

### Issue: "REST-Assured tests fail with 404"

**Solution**: Ensure application started in test mode
- Check @QuarkusTest annotation present
- Verify DEV services enabled in test properties
- Check endpoint paths are correct

---

## Best Practices

1. **Run tests frequently** - Execute before each commit
2. **Monitor coverage trends** - Track over sprints
3. **Maintain test data** - Keep fixtures up-to-date
4. **Document defects** - Use standard template
5. **Review reports** - Examine coverage gaps
6. **Update RTM** - Keep traceability current
7. **Automate execution** - Use CI/CD pipelines
8. **Archive reports** - Keep historical data

---

## Additional Resources

- **JaCoCo Documentation**: https://www.eclemma.org/jacoco/
- **JUnit 5 Guide**: https://junit.org/junit5/docs/current/user-guide/
- **REST-Assured**: https://rest-assured.io/
- **Quarkus Testing**: https://quarkus.io/guides/getting-started-testing
- **Maven**: https://maven.apache.org/guides/

---

## Contact & Support

For questions about test execution or metrics:
- **Test Lead**: [contact info]
- **QA Team**: [contact info]
- **Documentation**: docs/testing/

