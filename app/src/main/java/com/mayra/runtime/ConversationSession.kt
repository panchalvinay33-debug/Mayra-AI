package com.mayra.runtime

data class ConversationSession(
    val id:String,
    val userInput:String,
    val createdAt:Long,
    val active:Boolean=true
)
