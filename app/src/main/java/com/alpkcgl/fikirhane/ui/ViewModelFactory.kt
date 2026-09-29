package com.alpkcgl.fikirhane.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.alpkcgl.fikirhane.AppContainer
import com.alpkcgl.fikirhane.FikirhaneApp

/** `viewModelFactory { initializer { … } }` içinde AppContainer'a erişim. */
fun CreationExtras.appContainer(): AppContainer = (this[APPLICATION_KEY] as FikirhaneApp).container
