package com.quranplus.app.features.chatbot.data

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.ResponseCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Session
import com.google.ai.edge.litertlm.SessionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

interface LlmRunner {
    fun generate(prompt: String): Flow<String>
    fun isAvailable(): Boolean
}

class LiteRtLmRunner(
    private val context: Context,
    private val modelRepository: ModelRepository
) : LlmRunner {

    private var engine: Engine? = null
    private var activeSession: Session? = null

    private val sessionConfig = SessionConfig(
        SamplerConfig(
            topK = 40,
            topP = 0.9,
            temperature = 0.7,
            seed = 0
        )
    )

    @Synchronized
    private fun getOrInitEngine(): Engine {
        engine?.let { return it }

        val modelFile = modelRepository.getActiveModelFile()
        if (!modelFile.exists()) {
            throw IllegalStateException("Model file not found at ${modelFile.absolutePath}")
        }

        Log.i(TAG, "Initializing LiteRT-LM Engine with model: ${modelFile.absolutePath}")
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            cacheDir = context.cacheDir.absolutePath
        )
        val newEngine = Engine(config)
        newEngine.initialize()
        engine = newEngine
        return newEngine
    }

    override fun isAvailable(): Boolean {
        return modelRepository.isAnyModelReady()
    }

    override fun generate(prompt: String): Flow<String> = callbackFlow {
        if (!modelRepository.isAnyModelReady()) {
            close(IllegalStateException("Model AI belum diunduh. Silakan unduh model melalui ModelGate."))
            return@callbackFlow
        }

        try {
            val eng = getOrInitEngine()
            activeSession?.cancelProcess()
            activeSession?.close()

            val session = eng.createSession(sessionConfig)
            activeSession = session

            session.generateContentStream(
                listOf(InputData.Text(prompt)),
                object : ResponseCallback {
                    override fun onNext(response: String) {
                        trySend(response)
                    }

                    override fun onDone() {
                        close()
                    }

                    override fun onError(throwable: Throwable) {
                        Log.e(TAG, "LiteRT-LM inference error", throwable)
                        close(throwable)
                    }
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to create session or start generation", t)
            close(t)
        }

        awaitClose {
            try {
                activeSession?.cancelProcess()
                activeSession?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing active session", e)
            } finally {
                activeSession = null
            }
        }
    }.flowOn(Dispatchers.IO)

    fun close() {
        try {
            activeSession?.close()
            engine?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during LiteRtLmRunner shutdown", e)
        } finally {
            activeSession = null
            engine = null
        }
    }

    companion object {
        private const val TAG = "LiteRtLmRunner"
    }
}
