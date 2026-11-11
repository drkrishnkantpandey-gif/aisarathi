package com.aisarathi.launcher.model

enum class ConversationMode(val displayName: String, val wakeDescription: String) {
    COMMAND("Command", "Short direct answers for car controls"),
    TALK("Talk", "Long deep conversations with opinions and follow-ups"),
    CASUAL_CHAT("Casual Chat", "Light humorous Hinglish banter and stories");

    companion object {
        fun fromString(value: String): ConversationMode {
            return when (value.lowercase().trim()) {
                "talk", "talk mode", "baat", "baat karte hain" -> TALK
                "casual chat", "casual", "chat", "gup-shup", "gupshup", "gappe", "gappe maarte hain" -> CASUAL_CHAT
                else -> COMMAND
            }
        }
    }
}
