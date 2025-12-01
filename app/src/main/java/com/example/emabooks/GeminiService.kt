package com.example.emabooks

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {

    private const val TAG = "GeminiService"

    // Modelo básico – pode trocar para "gemini-1.5-pro" se quiser
    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    /**
     * Faz a chamada ao Gemini com um "pre prompt" de bibliotecário jovem e entusiasmado.
     */
    suspend fun askBibliotecario(pergunta: String): String = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            Log.e(TAG, "API KEY do Gemini está vazia. Verifique o local.properties / build.gradle.")
            return@withContext "Não consegui falar com o bibliotecário agora (configuração da chave da API está incorreta)."
        }

        return@withContext try {
            val prompt = """
                Você é um bibliotecário jovem, educado e entusiasmado chamado Ema.
                Você responde de forma clara, amigável e objetiva, ajudando o usuário
                com dúvidas sobre:
                - livros e recomendações de leitura
                - funcionamento da biblioteca
                - empréstimos, reservas e multas
                - uso do app EmaBooks (apenas de forma geral, sem detalhes técnicos demais)

                Sempre responda em português do Brasil, em tom acolhedor.

                Pergunta do usuário:
                "$pergunta"
            """.trimIndent()

            val response: GenerateContentResponse = model.generateContent(prompt)

            val texto = response.text?.trim()
            if (texto.isNullOrEmpty()) {
                Log.e(TAG, "Resposta vazia do modelo para a pergunta: $pergunta")
                "Não consegui formular uma resposta agora. Tenta perguntar de outro jeito?"
            } else {
                texto
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao chamar o Gemini: ${e.message}", e)

            // Para debug: expõe parte da mensagem de erro vinda do SDK / rede
            val erroDetalhe = e.localizedMessage ?: "erro desconhecido"

            // Você pode deixar mais amigável depois; por enquanto, ajuda a entender o problema real
            "Tive um problema técnico ao falar com o serviço do Gemini:\n$erroDetalhe"
        }
    }
}