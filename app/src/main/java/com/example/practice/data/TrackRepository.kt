package com.example.practice.data

import com.example.practice.model.Track

interface TrackRepository {
    fun getTracks(): List<Track>
    fun getTrackById(id: Long): Track?
}
