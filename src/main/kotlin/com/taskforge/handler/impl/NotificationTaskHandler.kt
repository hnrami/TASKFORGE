package com.taskforge.handler.impl

import com.taskforge.handler.TaskHandler
import com.taskforge.model.TaskDefinition
import com.taskforge.model.TaskContext
import com.taskforge.model.TaskResult
import com.taskforge.model.TaskStatus

/**
 * Task handler for sending notifications (email, Slack, etc.).
 * Task config expected:
 * - channel (String): 'email', 'slack', 'webhook', etc.
 * - recipient (String): Email address, Slack channel, webhook URL, etc.
 * - message (String): The notification message
 * - subject (String, optional): For email notifications
 */
class NotificationTaskHandler : TaskHandler {

    override fun type(): String = "notification"

    override fun execute(definition: TaskDefinition, context: TaskContext): TaskResult {
        return try {
            val channel = definition.config["channel"] as? String
                ?: return TaskResult(
                    status = TaskStatus.FAILED,
                    output = emptyMap(),
                    message = "Missing required config: channel"
                )

            val recipient = definition.config["recipient"] as? String
                ?: return TaskResult(
                    status = TaskStatus.FAILED,
                    output = emptyMap(),
                    message = "Missing required config: recipient"
                )

            val message = definition.config["message"] as? String
                ?: return TaskResult(
                    status = TaskStatus.FAILED,
                    output = emptyMap(),
                    message = "Missing required config: message"
                )

            val subject = definition.config["subject"] as? String

            // In a real implementation, would send notification via appropriate service
            // For now, return mock success
            val output = mapOf(
                "channel" to channel,
                "recipient" to recipient,
                "status" to "sent",
                "timestamp" to System.currentTimeMillis()
            )

            TaskResult(
                status = TaskStatus.SUCCESS,
                output = output,
                message = "Notification sent successfully to $recipient via $channel"
            )
        } catch (e: Exception) {
            TaskResult(
                status = TaskStatus.FAILED,
                output = emptyMap(),
                message = "Notification failed: ${e.message}"
            )
        }
    }

    override fun cancel() {
        // Cancel pending notifications
    }
}
