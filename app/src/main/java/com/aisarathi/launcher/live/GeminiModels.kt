package com.aisarathi.launcher.live

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// Setup packet sent to Gemini Live
data class GeminiLiveSetupRequest(
    @SerializedName("setup") val setup: SetupPayload
)

data class SetupPayload(
    @SerializedName("model") val model: String,
    @SerializedName("generationConfig") val generationConfig: GenerationConfig,
    @SerializedName("systemInstruction") val systemInstruction: SystemInstruction?,
    @SerializedName("tools") val tools: List<ToolDefinition>?
)

data class GenerationConfig(
    @SerializedName("responseModalities") val responseModalities: List<String> = listOf("AUDIO"),
    @SerializedName("speechConfig") val speechConfig: SpeechConfig? = null
)

data class SpeechConfig(
    @SerializedName("voiceConfig") val voiceConfig: VoiceConfig
)

data class VoiceConfig(
    @SerializedName("prebuiltVoiceConfig") val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

data class PrebuiltVoiceConfig(
    @SerializedName("voiceName") val voiceName: String
)

data class SystemInstruction(
    @SerializedName("parts") val parts: List<TextPart>
)

data class TextPart(
    @SerializedName("text") val text: String
)

data class ToolDefinition(
    @SerializedName("functionDeclarations") val functionDeclarations: List<FunctionDeclaration>
)

data class FunctionDeclaration(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: FunctionParameters
)

data class FunctionParameters(
    @SerializedName("type") val type: String = "OBJECT",
    @SerializedName("properties") val properties: Map<String, PropertySchema>,
    @SerializedName("required") val required: List<String>
)

data class PropertySchema(
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String
)

// Realtime input (Audio PCM from mic)
data class RealtimeInputMessage(
    @SerializedName("realtimeInput") val realtimeInput: RealtimeInputPayload
)

data class RealtimeInputPayload(
    @SerializedName("mediaChunks") val mediaChunks: List<MediaChunk>
)

data class MediaChunk(
    @SerializedName("mimeType") val mimeType: String = "audio/pcm;rate=16000",
    @SerializedName("data") val data: String
)

// Client Content for text turns
data class ClientContentMessage(
    @SerializedName("clientContent") val clientContent: ClientContentPayload
)

data class ClientContentPayload(
    @SerializedName("turns") val turns: List<ContentTurn>,
    @SerializedName("turnComplete") val turnComplete: Boolean = true
)

data class ContentTurn(
    @SerializedName("role") val role: String = "user",
    @SerializedName("parts") val parts: List<TextPart>
)

// Tool response back to model
data class ToolResponseMessage(
    @SerializedName("toolResponse") val toolResponse: ToolResponsePayload
)

data class ToolResponsePayload(
    @SerializedName("functionResponses") val functionResponses: List<FunctionResponse>
)

data class FunctionResponse(
    @SerializedName("response") val response: Map<String, Any>,
    @SerializedName("id") val id: String
)

// Incoming Server Response Models
data class ServerResponse(
    @SerializedName("serverContent") val serverContent: ServerContent?,
    @SerializedName("toolCall") val toolCall: ToolCallPayload?
)

data class ServerContent(
    @SerializedName("modelTurn") val modelTurn: ModelTurn?,
    @SerializedName("turnComplete") val turnComplete: Boolean?,
    @SerializedName("interrupted") val interrupted: Boolean?
)

data class ModelTurn(
    @SerializedName("parts") val parts: List<ModelPart>?
)

data class ModelPart(
    @SerializedName("text") val text: String?,
    @SerializedName("inlineData") val inlineData: InlineData?
)

data class InlineData(
    @SerializedName("mimeType") val mimeType: String?,
    @SerializedName("data") val data: String?
)

data class ToolCallPayload(
    @SerializedName("functionCalls") val functionCalls: List<FunctionCall>?
)

data class FunctionCall(
    @SerializedName("name") val name: String,
    @SerializedName("args") val args: Map<String, JsonElement>,
    @SerializedName("id") val id: String
)
