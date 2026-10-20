package com.example.practice.domain.usecase

import com.example.practice.domain.model.Track
import com.example.practice.domain.repository.TrackRepository

class GetTrackDetailsUseCase(private val repository: TrackRepository) {
    suspend operator fun invoke(id: Long): Track? =
        if (id > 0) repository.getTrackById(id) else null
}
