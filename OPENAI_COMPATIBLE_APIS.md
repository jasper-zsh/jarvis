# OpenAI-Compatible API Configuration

This app supports various OpenAI-compatible APIs, making it easy to switch between different AI providers.

## How to Switch API Providers

To use a different OpenAI-compatible API, simply modify the `apiConfig` variable in `LLMServiceImpl.kt`:

```kotlin
// Change this line in LLMServiceImpl.kt
private val apiConfig = APIConfig.OPENAI  // <- Change this
```

## Available API Configurations

### 1. OpenAI (Default)
```kotlin
private val apiConfig = APIConfig.OPENAI
```
- **Base URL**: `https://api.openai.com/`
- **Default Model**: `gpt-3.5-turbo`
- **API Key Format**: `sk-...`

### 2. DeepSeek
```kotlin
private val apiConfig = APIConfig.DEEPSEEK
```
- **Base URL**: `https://api.deepseek.com/`
- **Default Model**: `deepseek-chat`
- **API Key Format**: Provided by DeepSeek

### 3. Local AI Server
```kotlin
private val apiConfig = APIConfig.LOCAL_AI
```
- **Base URL**: `http://localhost:8080/`
- **Default Model**: `llama2`
- **Requirements**: Local AI server running on port 8080
- **Examples**: Ollama, LocalAI, FastChat

### 4. Together.ai
```kotlin
private val apiConfig = APIConfig.TOGETHER_AI
```
- **Base URL**: `https://api.together.xyz/`
- **Default Model**: `meta-llama/Llama-2-7b-chat-hf`
- **API Key Format**: Provided by Together.ai

### 5. Groq
```kotlin
private val apiConfig = APIConfig.GROQ
```
- **Base URL**: `https://api.groq.com/`
- **Default Model**: `llama3-8b-8192`
- **API Key Format**: Provided by Groq

## Custom API Configuration

You can also create a custom configuration for any OpenAI-compatible API:

```kotlin
private val apiConfig = APIConfig(
    baseUrl = "https://your-custom-api.com/",
    defaultModel = "your-model-name",
    name = "Custom API"
)
```

## API Key Setup

Regardless of which API provider you choose, you'll need to set the appropriate API key in the app settings:

1. Open the app
2. Go to Settings
3. Enter your API key for the selected provider

## Supported API Format

The app expects APIs that follow the OpenAI chat completion format:

```http
POST /v1/chat/completions
Authorization: Bearer YOUR_API_KEY
Content-Type: application/json

{
  "model": "model-name",
  "messages": [
    {"role": "system", "content": "You are Jarvis..."},
    {"role": "user", "content": "Hello!"}
  ],
  "temperature": 0.7,
  "max_tokens": 1000
}
```

## Common OpenAI-Compatible Providers

- **Ollama**: Local models, use `APIConfig.LOCAL_AI` with port 11434
- **LM Studio**: Local models, use `APIConfig.LOCAL_AI` with port 1234
- **FastChat**: Local serving, use `APIConfig.LOCAL_AI`
- **vLLM**: Local serving, use `APIConfig.LOCAL_AI`
- **Together.ai**: Cloud-based open models
- **Groq**: Fast inference cloud service
- **DeepSeek**: Cost-effective Chinese AI provider
- **Mistral AI**: European AI provider (custom config needed)

## Notes

- All providers use the same request/response format for compatibility
- Model names and capabilities vary by provider
- Some providers may have different rate limits or pricing
- Local APIs require the server to be running when using the app