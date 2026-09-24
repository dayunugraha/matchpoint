package com.matchpoint.app.audio

data class SoundEffect(val title: String, val filename: String)

/** Every reaction sound the live match screen offers — ported from LiveScoringView.soundEffects.
 * Shared by the on-phone chips and the watch's Sound FX picker so both show the same list. */
val SoundEffects = listOf(
    SoundEffect("Second Serve", "second_serve"),
    SoundEffect("Double Fault", "double_fault"),
    SoundEffect("Great Point", "great_point"),
    SoundEffect("GGWP", "ggwp"),
    SoundEffect("What a Point!", "what_a_point"),
    SoundEffect("What a Match!", "what_a_match"),
    SoundEffect("Saya Lawan", "saya_lawan"),
    SoundEffect("Hidup Jokowi", "hidup_jokowi"),
    SoundEffect("Hey Antek", "hey_antek"),
    SoundEffect("Kaget", "kaget"),
    SoundEffect("Fart", "fart"),
    SoundEffect("Sad Violin", "sad_violin"),
    SoundEffect("Yay", "yay"),
    SoundEffect("Wow", "wow"),
    SoundEffect("Salah", "salah"),
    SoundEffect("Faaah", "faaah"),
    SoundEffect("End", "end"),
    SoundEffect("Asiap", "asiap"),
    SoundEffect("Akh", "akh")
)
