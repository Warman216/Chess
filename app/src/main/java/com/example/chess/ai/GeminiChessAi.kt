package com.example.chess.ai

import android.util.Log
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.GameState
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiChessAi(
    private val heuristicAi: HeuristicChessAi = HeuristicChessAi()
) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun getAiMove(state: GameState, difficulty: AiDifficulty): AiMoveResult {
        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) {
            throw IllegalStateException("No legal moves available")
        }

        // If not Grandmaster difficulty or no API key, use the heuristic engine
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (difficulty != AiDifficulty.GRANDMASTER || !hasValidKey) {
            return heuristicAi.selectMove(state, difficulty)
        }

        return try {
            queryGeminiForMove(state, legalMoves, apiKey)
        } catch (e: Exception) {
            Log.w("GeminiChessAi", "Gemini API query failed, falling back to heuristic engine: ${e.message}")
            heuristicAi.selectMove(state, difficulty)
        }
    }

    private suspend fun queryGeminiForMove(
        state: GameState,
        legalMoves: List<Move>,
        apiKey: String
    ): AiMoveResult = withContext(Dispatchers.IO) {
        val fen = ChessEngine.toFen(state)
        val aiColor = state.turn
        val legalMovesUci = legalMoves.joinToString(", ") { it.uci }
        val recentMoves = state.moveHistory.takeLast(6).joinToString(" ") { it.san }

        val prompt = """
            You are a World Chess Grandmaster playing as ${aiColor.name}.
            Current position in FEN: $fen
            Recent moves: $recentMoves
            List of strictly legal moves in UCI format: [$legalMovesUci]
            
            Choose the single best strategic or tactical move from the legal moves list.
            Provide your response strictly in JSON format with two keys:
            "move": "the chosen UCI move string exactly from the list",
            "commentary": "1-2 brief sentences of grandmaster insight explaining your reasoning in an engaging, educational tone"
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            throw RuntimeException("Gemini API HTTP ${response.code}: $errBody")
        }

        val respStr = response.body?.string() ?: throw RuntimeException("Empty response")
        val respJson = JSONObject(respStr)
        val text = respJson
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val resultObj = JSONObject(text)
        val chosenUci = resultObj.optString("move").trim().lowercase()
        val commentary = resultObj.optString("commentary", "Calculated grandmaster continuation.")

        val matchedMove = legalMoves.firstOrNull { it.uci.lowercase() == chosenUci }
            ?: legalMoves.firstOrNull { it.uci.startsWith(chosenUci.take(4)) }
            ?: heuristicAi.selectMove(state, AiDifficulty.GRANDMASTER).move

        AiMoveResult(
            move = matchedMove,
            commentary = commentary,
            evaluationScore = heuristicAi.evaluatePosition(state)
        )
    }

    suspend fun getCoachHint(state: GameState, playerColor: PieceColor): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        val legalMoves = ChessEngine.getLegalMoves(state)
        if (legalMoves.isEmpty()) return@withContext "No moves available in this position."

        val bestMove = heuristicAi.selectMove(state, AiDifficulty.CLUB).move
        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (!hasValidKey) {
            val srcPiece = state.getPiece(bestMove.from)
            return@withContext "Coach advice: Look at active squares for your ${srcPiece?.type?.name?.lowercase() ?: "piece"} towards ${bestMove.to.algebraic} to create tactical pressure or secure piece coordination."
        }

        try {
            val fen = ChessEngine.toFen(state)
            val prompt = """
                You are an encouraging Chess Grandmaster Coach helping a student playing as ${playerColor.name}.
                Current position in FEN: $fen
                A strong candidate move is: ${bestMove.uci} (${state.getPiece(bestMove.from)?.type?.name} to ${bestMove.to.algebraic}).
                
                Provide a short 1-2 sentence hint or guiding question that helps the player spot the key tactical motif (e.g. piece activity, fork, king safety, open file, controlling the center) without giving away the entire move outright.
            """.trimIndent()

            val jsonRequest = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                respJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
            } else {
                "Coach advice: Focus on controlling central squares and improving the mobility of your ${state.getPiece(bestMove.from)?.type?.name?.lowercase()}."
            }
        } catch (e: Exception) {
            "Coach advice: Focus on controlling central squares and improving the mobility of your ${state.getPiece(bestMove.from)?.type?.name?.lowercase()}."
        }
    }
}
