package com.attendancehub.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<State, Effect>(
    initialState: State
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state

    private val _effect = MutableSharedFlow<Effect>()
    val effect: Flow<Effect> = _effect

    protected fun updateState(block: State.() -> State) {
        _state.value = _state.value.block()
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }

    protected fun <T> execute(
        onStart: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
        onSuccess: (T) -> Unit = {},
        block: suspend () -> T,
    ) {
        viewModelScope.launch {
            try {
                onStart()
                val result = block()
                onSuccess(result)
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }
}