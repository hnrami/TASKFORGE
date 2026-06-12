package com.taskforge.handler

import com.sun.net.httpserver.HttpServer
import com.taskforge.handler.impl.HttpTaskHandler
import com.taskforge.handler.impl.ScriptTaskHandler
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskStatus
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RealHandlerTest {
    @Test
    fun `http handler classifies success server error and client error`() {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/success") { exchange ->
            exchange.sendResponseHeaders(201, 2)
            exchange.responseBody.use { it.write("ok".toByteArray()) }
        }
        server.createContext("/server-error") { exchange -> exchange.sendResponseHeaders(503, -1); exchange.close() }
        server.createContext("/client-error") { exchange -> exchange.sendResponseHeaders(400, -1); exchange.close() }
        server.start()
        try {
            val handler = HttpTaskHandler()
            val base = "http://localhost:${server.address.port}"
            val success = handler.execute(TaskDefinition("a", "http", config = mapOf("url" to "$base/success")), TaskContext())
            val serverError = handler.execute(TaskDefinition("b", "http", config = mapOf("url" to "$base/server-error")), TaskContext())
            val clientError = handler.execute(TaskDefinition("c", "http", config = mapOf("url" to "$base/client-error")), TaskContext())
            assertEquals(TaskStatus.SUCCESS, success.status)
            assertEquals(201, success.output["statusCode"])
            assertEquals("ok", success.output["responseBody"])
            assertTrue(serverError.retryable)
            assertTrue(!clientError.retryable)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `script handler uses exit code and captures both streams`() {
        val handler = ScriptTaskHandler()
        val successCommand = if (isWindows()) "Write-Output out; [Console]::Error.WriteLine('warn'); exit 0"
        else "echo out; echo warn >&2; exit 0"
        val failureCommand = if (isWindows()) "exit 7" else "exit 7"

        val success = handler.execute(TaskDefinition("a", "script", config = mapOf("command" to successCommand)), TaskContext())
        val failure = handler.execute(TaskDefinition("b", "script", config = mapOf("command" to failureCommand)), TaskContext())

        assertEquals(TaskStatus.SUCCESS, success.status)
        assertTrue(success.output["stdout"].toString().contains("out"))
        assertTrue(success.output["stderr"].toString().contains("warn"))
        assertEquals(0, success.output["exitCode"])
        assertEquals(TaskStatus.FAILED, failure.status)
        assertEquals(7, failure.output["exitCode"])
    }

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("windows")
}
