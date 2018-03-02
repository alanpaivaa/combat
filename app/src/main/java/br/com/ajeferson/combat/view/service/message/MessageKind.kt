package br.com.ajeferson.combat.view.service.message

/**
 * Created by ajeferson on 02/03/2018.
 */
enum class MessageKind {

    WAIT_OPPONENT,
    OPPONENT_GIVE_UP,
    PLACE_PIECES,
    CHAT;

    val message get() = Message(kind = this)
    val isChat get() = this == CHAT
    val isPlacePiece get() = this == PLACE_PIECES

}