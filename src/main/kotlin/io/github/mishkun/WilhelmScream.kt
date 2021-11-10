package io.github.mishkun

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import java.io.BufferedInputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

const val WILHELM_SCREAM_PATH = "wilhelm.wav"
val ktFileExtensionRegex = "kts?$".toRegex()

fun invokeLaterOnEDT(block: () -> Unit) =
    ApplicationManager.getApplication().invokeAndWait(block, ModalityState.NON_MODAL)

fun show(
    message: String,
    title: String = "",
    notificationType: NotificationType = NotificationType.INFORMATION,
    groupDisplayId: String = "",
    notificationListener: NotificationListener? = null
) {
    invokeLaterOnEDT {
        val notification = Notification(
            groupDisplayId,
            title,
            // this is because Notification doesn't accept empty messages
            message.takeUnless { it.isBlank() } ?: "[ empty ]",
            notificationType,
            notificationListener)
        ApplicationManager.getApplication().messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
    }
}

class WilhelmScreamListener : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        super.documentChanged(event)

        if (event.isWholeTextReplaced) return

        val file = FileDocumentManager.getInstance().getFile(event.document)
        if (file?.extension?.matches(ktFileExtensionRegex) != true) return

        if (event.newFragment.contains("!!") ||
            event.withPreviousChars(1) == "!!") {
            playSound(WILHELM_SCREAM_PATH)
        }

        if (event.newFragment.contains("lateinit") ||
            event.withPreviousChars(7).contains("lateinit")) {
            playSound(WILHELM_SCREAM_PATH)
        }
    }

    private fun DocumentEvent.withPreviousChars(number: Int): String =
        document.getText(TextRange((offset - number).coerceAtLeast(0), offset + number))


    @Synchronized
    fun playSound(url: String) = Thread {
        try {
            val clip = AudioSystem.getClip()
            val resourceStream = javaClass.getResourceAsStream("/sounds/$url")
            resourceStream?.let {
                val bufferedInputStream = BufferedInputStream(resourceStream)
                val inputStream: AudioInputStream = AudioSystem.getAudioInputStream(
                    bufferedInputStream
                )
                clip.open(inputStream)
                clip.start()
            }
        } catch (e: Exception) {
            show(e.message.toString(), notificationType = NotificationType.ERROR)
        }
    }.start()
}

