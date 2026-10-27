package com.example.practice.domain.model

enum class TrackError { CONNECTION, TIMEOUT, SERVER, RATE_LIMIT, INVALID_RESPONSE, UNKNOWN }

/** Keeps HTTP and serialization dependencies out of the domain and presentation layers. */
class TrackException(val error: TrackError, cause: Throwable? = null) : Exception(cause)
