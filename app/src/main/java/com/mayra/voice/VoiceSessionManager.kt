package com.mayra.voice

class VoiceSessionManager {
    enum class State { IDLE, LISTENING, PROCESSING, SPEAKING }
    private var state = State.IDLE
    fun startListening(){ state = State.LISTENING }
    fun startProcessing(){ state = State.PROCESSING }
    fun startSpeaking(){ state = State.SPEAKING }
    fun stop(){ state = State.IDLE }
    fun currentState(): State = state
}
