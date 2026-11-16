package com.example.practice

import com.example.practice.domain.model.Track
import com.example.practice.domain.repository.TrackRepository

/** Local study data; dates and prices are illustrative, not an API response. */
class FakeTrackRepository : TrackRepository {
    private val tracks = listOf(
        track(1, "One More Time", "Daft Punk", "Discovery", "Dance", "2000-11-30", 320357),
        track(2, "Bohemian Rhapsody", "Queen", "A Night at the Opera", "Rock", "1975-10-31", 354000),
        track(3, "Smells Like Teen Spirit", "Nirvana", "Nevermind", "Alternative", "1991-09-10", 301000),
        track(4, "Numb", "Linkin Park", "Meteora", "Alternative", "2003-03-25", 185000),
        track(5, "Blinding Lights", "The Weeknd", "After Hours", "Pop", "2019-11-29", 200000),
        track(6, "Viva La Vida", "Coldplay", "Viva La Vida or Death and All His Friends", "Alternative", "2008-06-12", 242000),
        track(7, "Rolling in the Deep", "Adele", "21", "Pop", "2010-11-29", 228000),
        track(8, "Believer", "Imagine Dragons", "Evolve", "Alternative", "2017-02-01", 204000),
        track(9, "Do I Wanna Know?", "Arctic Monkeys", "AM", "Alternative", "2013-06-19", 272000),
        track(10, "Feel Good Inc.", "Gorillaz", "Demon Days", "Alternative", "2005-05-09", 222000),
        track(11, "Uprising", "Muse", "The Resistance", "Alternative", "2009-08-03", 304000),
        track(12, "Creep", "Radiohead", "Pablo Honey", "Alternative", "1992-09-21", 238000, price = null),
        track(13, "Mr. Brightside", "The Killers", "Hot Fuss", "Alternative", "2004-06-07", 222000),
        track(14, "Enter Sandman", "Metallica", "Metallica", "Metal", "1991-07-29", 331000),
        track(15, "Californication", "Red Hot Chili Peppers", "Californication", "Rock", "1999-06-08", 329000),
        track(16, "Get Lucky", "Daft Punk", "Random Access Memories", "Dance", "2013-04-19", 369000),
        track(17, "Take on Me", "a-ha", "Hunting High and Low", "Pop", "1985-06-01", 225000, price = 0.99),
        track(18, "Enjoy the Silence", "Depeche Mode", "Violator", "Electronic", "1990-02-05", 372000, isStreamable = false)
    )

    override suspend fun searchTracks(query: String, limit: Int): List<Track> = tracks.toList()

    override suspend fun getTrackById(id: Long): Track? = tracks.firstOrNull { it.id == id }

    private fun track(
        id: Long,
        name: String,
        artist: String,
        album: String,
        genre: String,
        releaseDate: String,
        durationMillis: Long,
        price: Double? = 1.29,
        isStreamable: Boolean = true
    ) = Track(
        id = id,
        trackName = name,
        artistName = artist,
        collectionName = album,
        genre = genre,
        releaseDate = "${releaseDate}T08:00:00Z",
        trackTimeMillis = durationMillis,
        trackPrice = price,
        currency = "USD",
        country = "USA",
        isStreamable = isStreamable
    )
}