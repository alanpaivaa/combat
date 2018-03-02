package br.com.ajeferson.combat.view.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import br.com.ajeferson.combat.view.extension.value
import br.com.ajeferson.combat.view.service.connection.GameService
import br.com.ajeferson.combat.view.service.message.MessageKind
import br.com.ajeferson.combat.view.service.model.*
import br.com.ajeferson.combat.view.view.enumeration.BoardItemKind
import br.com.ajeferson.combat.view.view.enumeration.BoardItemKind.*
import br.com.ajeferson.combat.view.view.enumeration.GameStatus
import br.com.ajeferson.combat.view.view.enumeration.GameStatus.*
import br.com.ajeferson.combat.view.view.enumeration.PieceKind
import br.com.ajeferson.combat.view.view.enumeration.PieceKind.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by ajeferson on 20/02/2018.
 */
class GameViewModel(private val gameService: GameService): ViewModel() {

    val messages = MutableLiveData<ChatMessage>()
    val status = ObservableField(DISCONNECTED)
    val liveStatus = MutableLiveData<GameStatus>()
    val error = MutableLiveData<Error>()
    var move = MutableLiveData<Move>()

    val placedCoordinates = MutableLiveData<Coordinates>()
    val placedPiece = MutableLiveData<PieceCoordinatesDto>()

    lateinit var pieces: MutableList<MutableList<Piece?>>

    var moveCoordinate: Coordinates? = null

    private val initialAvailablePieces = mapOf(
//            SOLDIER to 8,
//            BOMB to 6,
//            GUNNER to 5,
//            SERGEANT to 4,
//            TENANT to 4,
//            CAPTAIN to 4,
//            MAJOR to 3,
//            COLONEL to 2,
//            GENERAL to 1,
//            MARSHAL to 1,
//            SPY to 1,
            PRISONER to 1
    )

    var availablePieces = mutableMapOf<PieceKind, Int>()
    val availablePiecesCount get() = availablePieces
            .map { it.value }
            .reduce { a, b -> a + b }

    private fun setStatus(status: GameStatus) {
        liveStatus.value = status
        this.status.value = status
    }



    /**
     * Life Cycle
     * */

    fun onCreate() {
        resetGame()
        subscribeToStatus()
        subscribeToChats()
        subscribeToPlacedPieces()
        subscribeToMoves()
    }

    fun onStart() {
    }

    fun onResume() {

    }

    fun onDestroy() {

    }





    /**
     * Subscriptions
     * */

    private fun subscribeToChats() {
    }

    private fun subscribeToStatus() {
        gameService
                .status
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                    when(it) {
                        DISCONNECTED -> {
                            resetGame()
                        }
                        else -> Unit
                    }

                    setStatus(it)

                }, {
                    // TODO Handle
                })
    }

    private fun subscribeToPlacedPieces() {
        gameService
                .placedPieces
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    placePiece(it)
                }
    }

    private fun subscribeToMoves() {
        gameService
                .moves
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    move.value = it
                }
    }



    /**
     * Pieces Placing
     * */

    fun selectPiece(pieceKind: PieceKind, coordinates: Coordinates) {

        // Build the message
        val message = MessageKind
                .PLACE_PIECE
                .message
                .apply { addValues(pieceKind.toString()) } // Add the kind
                .apply { addValues(coordinates.row) }
                .apply { addValues(coordinates.column) }

        // Send the message
        gameService.sendMessage(message)

    }

    private fun placePiece(dto: PieceCoordinatesDto) {

        // Place the piece
        val (row, column) = dto.coordinates
        pieces[row][column] = dto.piece.copy()
        placedPiece.value = dto

        // Should not decrease amount if it belongs to the opponent
        if(dto.piece.belongsToOpponent) {
            return
        }

        // Reduce the current amount
        val oldAmount = availablePieces[dto.piece.kind] ?: return
        availablePieces[dto.piece.kind] = oldAmount - 1

        // Ready to play
        if(availablePiecesCount == 0) {
            setStatus(READY)
            gameService.sendMessage(MessageKind.READY.message)
        }

    }




    /**
     * View Actions
     * */

    fun onConnectTouched() {

        if(status.value == CONNECTING) return

        if(status.value != DISCONNECTED) {
            error.value = Error.ALREADY_CONNECTED
            return
        }

        gameService.connect()

    }

    fun onGiveUpTouched() {

        if(status.value == null || status.value == DISCONNECTED) {
            error.value = Error.ALREADY_DISCONNECTED
            return
        }

        gameService.disconnect()

    }

    val didClickPiece: (Int, Int) -> Unit = click@ { row, column ->

        val status = this.status.value ?: return@click

        val coordinates = Coordinates.newInstance(row, column)

        if(status.isPlacingPieces) {
            if(row < 6 || pieces[row][column] != null) {
                error.value = Error.PLACE_PIECE_INVALID_COORDINATES
                return@click
            }
            placedCoordinates.value = coordinates
        }

        // TODO Validate moveCoordinate
        if(status.isTurn) {
            if(moveCoordinate == null) { // Select piece to moveCoordinate
                moveCoordinate = coordinates
            } else {
                gameService.sendMove(moveCoordinate!!, coordinates)
                moveCoordinate = null
            }
        }

    }

    private fun resetGame() {
        pieces = (0 until 10).map { arrayOfNulls<Piece>(10).toMutableList() }.toMutableList()
        initialAvailablePieces.forEach { availablePieces[it.key] = it.value }
    }

    val board: List<List<BoardItemKind>> = listOf(
            listOf(LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND),
            listOf(LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND),
            listOf(LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND),
            listOf(LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND),
            listOf(LAND, LAND, WATER, WATER, LAND, LAND, WATER, WATER, LAND, LAND),
            listOf(LAND, LAND, WATER, WATER, LAND, LAND, WATER, WATER, LAND, LAND),
            listOf(LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND),
            listOf(LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND),
            listOf(LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND),
            listOf(LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND, LAND)
    )

    enum class Error {
        PLACE_PIECE_INVALID_COORDINATES,
        ALREADY_CONNECTED,
        ALREADY_DISCONNECTED
    }

}