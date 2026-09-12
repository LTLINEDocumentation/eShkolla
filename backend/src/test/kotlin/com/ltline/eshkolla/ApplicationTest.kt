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
        val login = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("{\"username\":\"admin\",\"password\":\"123456\"}")
        }
        assertEquals(HttpStatusCode.OK, login.status)
        val token = Regex("\\\"token\\\":\\\"([^\\\"]+)").find(login.bodyAsText())!!.groupValues[1]

        val students = client.get("/api/v1/students") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, students.status)
        assertTrue(students.bodyAsText().contains("Ardit Krasniqi"))
    }
}
