package com.tittu.commandcenter

enum class CommandRisk {
    SAFE_READ_ONLY,
    REQUIRES_APPROVAL
}

/**
 * Conservative command policy: only a small allow-list of read-only commands
 * can run without a confirmation dialog. Everything else requires approval.
 */
object CommandRiskPolicy {
    private val safeExact = setOf(
        "pwd",
        "whoami",
        "date",
        "uname -a",
        "ls",
        "ls -la",
        "ls -l",
        "id",
        "df -h",
        "free -h",
        "opencode --version",
        "node --version",
        "npm --version",
        "python --version",
        "ffmpeg -version",
        "git status",
        "git diff",
        "git log -5 --oneline",
        "git branch --show-current"
    )

    fun classify(command: String): CommandRisk {
        val normalized = command.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) return CommandRisk.REQUIRES_APPROVAL

        // Shell operators, substitutions and redirections always need review.
        if (Regex("[;&|<>$]").containsMatchIn(normalized) ||
            normalized.contains(String(charArrayOf(96)))) {
            return CommandRisk.REQUIRES_APPROVAL
        }

        return if (normalized in safeExact) {
            CommandRisk.SAFE_READ_ONLY
        } else {
            CommandRisk.REQUIRES_APPROVAL
        }
    }
}
