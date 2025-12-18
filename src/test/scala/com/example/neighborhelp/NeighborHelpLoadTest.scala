// src/test/scala/com/example/neighborhelp/NeighborHelpLoadTest.scala

package com.example.neighborhelp

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.language.postfixOps

class NeighborHelpLoadTest extends Simulation {

  // HTTP Configuration
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test")

  // Headers comunes
  val commonHeaders = Map(
    "Content-Type" -> "application/json",
    "Accept" -> "application/json"
  )

  // Feeder para datos dinámicos
  val userFeeder = Iterator.continually(Map(
    "userId" -> java.util.UUID.randomUUID().toString.take(8),
    "email" -> s"user_${java.util.UUID.randomUUID().toString.take(8)}@test.com",
    "username" -> s"user_${java.util.UUID.randomUUID().toString.take(8)}"
  ))

  // ========== SCENARIOS BÁSICOS ==========

  // Scenario 1: Browse Public Resources (No auth required)
  val browseResourcesScenario = scenario("Browse Public Resources")
    .exec(http("Get All Resources")
      .get("/api/resources")
      .headers(commonHeaders)
      .check(status.is(200))
      .check(jsonPath("$.content").exists)
    )
    .pause(1.second)
    .exec(http("Get Categories")
      .get("/api/categories")
      .headers(commonHeaders)
      .check(status.is(200))
    )
    .pause(1.second)
    .exec(http("Get Cities")
      .get("/api/locations/cities")
      .headers(commonHeaders)
      .check(status.is(200))
    )

  // Scenario 2: User Registration & Login
  val authScenario = scenario("User Authentication")
    .feed(userFeeder)
    .exec(http("Register User")
      .post("/api/auth/register")
      .headers(commonHeaders)
      .body(StringBody("""{
        "username": "${username}",
        "email": "${email}",
        "password": "TestPass123!",
        "firstName": "Test",
        "lastName": "User",
        "type": "INDIVIDUAL"
      }"""))
      .asJson
      .check(status.is(200))
      .check(jsonPath("$.id").saveAs("userId"))
    )
    .pause(2.seconds)
    .exec(http("Login User")
      .post("/api/auth/login")
      .headers(commonHeaders)
      .body(StringBody("""{
        "email": "${email}",
        "password": "TestPass123!"
      }"""))
      .asJson
      .check(status.is(200))
      .check(jsonPath("$.accessToken").saveAs("authToken"))
    )

  // Scenario 3: Create Resource (Requires auth)
  val createResourceScenario = scenario("Create Resource")
    .feed(userFeeder)
    .exec(authScenario)
    .pause(3.seconds)
    .exec(http("Create New Resource")
      .post("/api/resources")
      .header("Authorization", "Bearer ${authToken}")
      .headers(commonHeaders)
      .body(StringBody("""{
        "title": "Test Resource ${userId}",
        "description": "This is a test resource created during load testing",
        "category": "Food",
        "city": "Madrid",
        "street": "Test Street 123",
        "postalCode": "28001",
        "contactInfo": "${email}",
        "availability": "Monday to Friday 9AM-6PM",
        "cost": "FREE",
        "requirements": "None",
        "capacity": 10,
        "wheelchairAccessible": true,
        "languages": "English,Spanish",
        "targetAudience": "Everyone"
      }"""))
      .asJson
      .check(status.in(201, 200))
      .check(jsonPath("$.id").saveAs("resourceId"))
    )

  // Scenario 4: Search and Filter Resources
  val searchScenario = scenario("Search Resources")
    .exec(http("Search by City")
      .get("/api/resources?city=Madrid&size=10")
      .headers(commonHeaders)
      .check(status.is(200))
    )
    .pause(1.second)
    .exec(http("Search by Category")
      .get("/api/resources?categoryId=1&size=5")
      .headers(commonHeaders)
      .check(status.is(200))
    )
    .pause(1.second)
    .exec(http("Search with Text")
      .get("/api/resources?search=food&size=8")
      .headers(commonHeaders)
      .check(status.is(200))
    )

  // Scenario 5: Full User Journey
  val fullUserJourneyScenario = scenario("Full User Journey")
    .feed(userFeeder)
    .exec(authScenario)
    .pause(2.seconds)
    .exec(browseResourcesScenario)
    .pause(3.seconds)
    .exec(createResourceScenario)
    .pause(2.seconds)
    .exec(http("Get User Profile")
      .get("/api/users/me")
      .header("Authorization", "Bearer ${authToken}")
      .headers(commonHeaders)
      .check(status.is(200))
    )
    .exec(http("Get My Resources")
      .get("/api/resources/my-resources")
      .header("Authorization", "Bearer ${authToken}")
      .headers(commonHeaders)
      .check(status.is(200))
    )

  // ========== LOAD TEST SETUPS ==========

  // Test 1: Light Load - Public browsing (100 users over 2 minutes)
  /*
  setUp(
    browseResourcesScenario.inject(
      rampUsers(100).during(2.minutes)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.percentile3.lt(1000), // 95% < 1s
    global.successfulRequests.percent.gt(99)
  )
  */

  // Test 2: Medium Load - Mixed traffic (500 users over 5 minutes)
  /*
  setUp(
    browseResourcesScenario.inject(rampUsers(300).during(3.minutes)),
    searchScenario.inject(rampUsers(150).during(3.minutes)),
    authScenario.inject(rampUsers(50).during(3.minutes))
  ).protocols(httpProtocol)
  */

  // Test 3: Heavy Load - User journeys (1000 users over 10 minutes)

  setUp(
    fullUserJourneyScenario.inject(
      rampUsers(1000).during(10.minutes)
    ).protocols(httpProtocol)
  ).assertions(
    global.responseTime.percentile3.lt(2000),
    global.responseTime.max.lt(5000),
    global.successfulRequests.percent.gt(98)
  )

  // Test 4: Spike Test - Sudden 200 concurrent users
  /*
  setUp(
    browseResourcesScenario.inject(
      nothingFor(30.seconds),
      atOnceUsers(200)
    ).protocols(httpProtocol)
  )
  */
}