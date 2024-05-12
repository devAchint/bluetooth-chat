package com.techuntried.bluetooth.domain

sealed interface ConnectionResult{
    object ConnectionEstablished:ConnectionResult
    data class Error(val message:String):ConnectionResult
}