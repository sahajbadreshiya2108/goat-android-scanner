package privacy.scanner

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.regex.Pattern

object DocumentScanner {

    private val EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}")
    private val PHONE_PATTERN = Pattern.compile("(\\+?[0-9]{1,3}[- ]?)?\\(?[0-9]{3}\\)?[- ]?[0-9]{3}[- ]?[0-9]{4}")
    private val AWS_PATTERN = Pattern.compile("AKIA[0-9A-Z]{16}")

    fun scanTextOrDocUri(context: Context, uri: Uri, fileName: String, scanId: String): List<MobileFinding> {
        val findings = mutableListOf<MobileFinding>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var lineNum = 1
                var line: String?

                while (reader.readLine().also { line = it } != null && lineNum <= 2000) {
                    val currentLine = line ?: break

                    // Email Check
                    val emailMatcher = EMAIL_PATTERN.matcher(currentLine)
                    while (emailMatcher.find()) {
                        val email = emailMatcher.group()
                        findings.add(
                            MobileFinding(
                                id = UUID.randomUUID().toString(),
                                scanId = scanId,
                                category = "PERSONAL_INFO",
                                findingType = "Email Address Found",
                                severity = "LOW",
                                confidence = 0.90f,
                                sourceItem = fileName,
                                location = "Line $lineNum",
                                rawValue = email,
                                maskedValue = maskEmail(email),
                                explanation = "Document contains exposed personal or corporate email.",
                                risk = "Spam collection and phishing target.",
                                recommendedAction = "Sanitize email address before public distribution.",
                                timestamp = System.currentTimeMillis().toString()
                            )
                        )
                    }

                    // AWS Key Check
                    val awsMatcher = AWS_PATTERN.matcher(currentLine)
                    if (awsMatcher.find()) {
                        val key = awsMatcher.group()
                        findings.add(
                            MobileFinding(
                                id = UUID.randomUUID().toString(),
                                scanId = scanId,
                                category = "SECRET",
                                findingType = "AWS Cloud Access Key",
                                severity = "CRITICAL",
                                confidence = 0.99f,
                                sourceItem = fileName,
                                location = "Line $lineNum",
                                rawValue = key,
                                maskedValue = key.take(4) + "************" + key.takeLast(3),
                                explanation = "Hardcoded Amazon Web Services Access Key.",
                                risk = "Direct compromise of cloud infrastructure.",
                                recommendedAction = "Revoke AWS key from IAM console immediately.",
                                timestamp = System.currentTimeMillis().toString()
                            )
                        )
                    }

                    lineNum++
                }
            }
        } catch (e: Exception) {}

        return findings
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"
        val user = parts[0]
        val maskedUser = if (user.length <= 2) user.take(1) + "*" else user.take(1) + "***" + user.takeLast(1)
        return "$maskedUser@${parts[1]}"
    }
}
