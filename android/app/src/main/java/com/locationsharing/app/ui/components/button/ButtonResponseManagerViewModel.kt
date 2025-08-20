package com.locationsharing.app.ui.components.button

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel wrapper for ButtonResponseManager to enable Hilt injection in Composables
 */
@HiltViewModel
class ButtonResponseManagerViewModel @Inject constructor(
    val buttonResponseManager: ButtonResponseManager
) : ViewModel()