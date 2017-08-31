package com.example.haolu.realmcontentprovider

import io.realm.RealmObject
import io.realm.annotations.Required

// open class for SongRealmProxy to inherit
open class Song(var rank: Int = 0, @Required var title: String = "", @Required var artist: String = "") : RealmObject()
