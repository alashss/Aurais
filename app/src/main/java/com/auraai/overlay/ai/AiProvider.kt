package com.auraai.overlay.ai

enum class AiProvider(val id: String, val displayName: String, val baseUrl: String) {
    AUTO("auto", "Auto (Cascade)", ""),
    GEMINI("gemini", "Gemini Flash", "https://generativelanguage.googleapis.com"),
    GROQ("groq", "Groq (Llama3)", "https://api.groq.com/openai/v1"),
    MISTRAL("mistral", "Mistral (Free)", "https://api.mistral.ai/v1"),
    COHERE("cohere", "Cohere (Trial)", "https://api.cohere.ai/v1"),
    TOGETHER("together", "Together AI", "https://api.together.xyz/v1"),
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1"),
    POLLINATIONS("pollinations", "Pollinations (Free/No Key)", "https://text.pollinations.ai"),
}
