package org.khanacademy.satzendeskbot

import com.ullink.slack.simpleslackapi.SlackAttachment
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted
import com.ullink.slack.simpleslackapi.impl.SlackChatConfiguration
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import java.io.File

fun readToken(): String {
    val filename = System.getProperty("user.home") + "/.sat-zendesk-bot-token"
    return File(filename).readText().trim()
}

fun connect(): SlackSession {
    val session = SlackSessionFactory.createWebSocketSlackSession(readToken())
    addListener(session)
    session.connect()
    return session
}

val satExp =  Regex("\\bsat\\b", RegexOption.IGNORE_CASE)
val cbExp = Regex("college\\s?board", RegexOption.IGNORE_CASE)

fun isSATMessage(event: SlackMessagePosted): Boolean {
    return event.attachments.any { att ->
        val fields = listOf(
                att.text,
                att.pretext,
                att.fallback,
                att.title
        )
        return fields.any { f ->
            satExp.containsMatchIn(f) || cbExp.containsMatchIn(f)
        }
    } || satExp.containsMatchIn(event.messageContent) || cbExp.containsMatchIn(event.messageContent)
}

fun isInZendeskTickets(event: SlackMessagePosted, session: SlackSession): Boolean =
    event.channel.id == session.findChannelByName("zendesk-tickets").id


fun listener(event: SlackMessagePosted, session: SlackSession): Unit {
    if (isSATMessage(event) && isInZendeskTickets(event, session)) {

        val msgBuilder = SlackPreparedMessage.Builder()
            .withMessage(event.messageContent)
            .withUnfurl(false)
        event.attachments.forEach { msgBuilder.addAttachment(it) }

        val config = SlackChatConfiguration.getConfiguration()
            .withName("Zendesk Zebra")

        session.sendMessage(
                session.findChannelByName("sat-monitoring"),
                msgBuilder.build(),
                config)
    }
}

fun addListener(session: SlackSession): Unit {
    session.addMessagePostedListener(::listener)
}

fun main(args: Array<String>): Unit {
    connect()
}

