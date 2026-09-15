package com.ltline.eshkolla

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApplicationTest {
    @Test
    fun healthEndpointWorks() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("eShkolla API"))
    }

    @Test
    fun loginAndStudentsEndpointWork() = testApplication {
        application { module() }
        val username = System.getenv("BOOTSTRAP_ADMIN_USERNAME") ?: "admin"
        val password = System.getenv("BOOTSTRAP_ADMIN_PASSWORD") ?: "test-admin-password-123"
        val login = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("{\"username\":\"$username\",\"password\":\"$password\"}")
        }
        assertEquals(HttpStatusCode.OK, login.status)
        val token = Json.parseToJsonElement(login.bodyAsText()).jsonObject["token"]?.jsonPrimitive?.content
        require(!token.isNullOrBlank()) { "Login nuk ktheu token." }

        val students = client.get("/api/v1/students") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, students.status)
        assertTrue(students.bodyAsText().contains("items"))
        assertTrue(students.bodyAsText().contains("total"))
    }
}
