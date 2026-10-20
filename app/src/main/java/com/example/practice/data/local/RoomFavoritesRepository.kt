package com.example.practice.data.local

import com.example.practice.domain.model.Track
import com.example.practice.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.map

class RoomFavoritesRepository(private val dao: FavoriteTrackDao) : FavoritesRepository {
    override fun observeTracks() = dao.observeTracks().map { tracks -> tracks.map { it.toTrack() } }
    override suspend fun getTrack(id: Long): Track? = dao.getTrack(id)?.toTrack()
    override suspend fun add(track: Track) = dao.insert(FavoriteTrackEntity.from(track))
    override suspend fun remove(id: Long) = dao.delete(id)
}
