package br.com.ajeferson.combat.view.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import br.com.ajeferson.combat.view.extension.value
import br.com.ajeferson.combat.view.service.connection.ConnectionManager
import br.com.ajeferson.combat.view.service.model.ChatMessage
import br.com.ajeferson.combat.view.service.repository.ChatRepository
import br.com.ajeferson.combat.view.service.connection.ConnectionManager.ConnectionStatus
import br.com.ajeferson.combat.view.viewmodel.GameViewModel.Status.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by ajeferson on 20/02/2018.
 */
class GameViewModel(private val connectionManager: ConnectionManager,
                    private val chatRepository: ChatRepository): ViewModel() {

    val messages = MutableLiveData<ChatMessage>()
    val status = ObservableField(Status.DISCONNECTED)
    val liveStatus = MutableLiveData<Status>()

    private fun setStatus(status: Status) {
        liveStatus.value = status
        this.status.value = status
    }

    fun onCreate() {
        subscribeToConnectionStatus()
        subscribeToChatMessages()
        connectionManager.connect()
    }

    fun onStart() {
    }

    fun onResume() {

    }

    fun onDestroy() {

    }

    private fun subscribeToChatMessages() {
        chatRepository
                .messages
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    messages.value = it
                }
    }

    private fun subscribeToConnectionStatus() {
        connectionManager
                .status
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    setStatus(when(it) {
                        ConnectionStatus.CONNECTING -> CONNECTING
                        ConnectionStatus.CONNECTED -> CONNECTED
                        else -> DISCONNECTED
                    })
                }
    }


    enum class Status {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        NONE,
        PLAYING
    }

}