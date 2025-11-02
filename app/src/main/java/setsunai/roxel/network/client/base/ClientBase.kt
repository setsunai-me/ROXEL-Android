package setsunai.roxel.network.client.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import setsunai.roxel.network.data.ServerCredentials

open class ClientBase : ViewModel() {
    protected val credentialsFlow = MutableStateFlow<ServerCredentials?>(null)
    protected var job: Job? = null

    open suspend fun onLaunch(): Job? {
        return null
    }
    open fun onClose() {}

    fun updateCredentials(credentials: ServerCredentials) {
        viewModelScope.launch(Dispatchers.IO) {
            this@ClientBase.credentialsFlow.emit(credentials)
        }
    }

    fun launch() {
        if (job?.isActive == true) return
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                onLaunch()
            } catch (_: Throwable) {
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        onClose()
    }

    override fun onCleared() {
        super.onCleared()
        cancel()
    }
}