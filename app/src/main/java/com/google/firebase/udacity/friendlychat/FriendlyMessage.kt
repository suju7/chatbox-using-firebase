package com.google.firebase.udacity.friendlychat

    class FriendlyMessage(text: String?, name: String?, photoUrl: String?) {
    var text: String? = null
    var name: String? = null
    var photoUrl: String? = null

     init{
        this.text = text
        this.name = name
        this.photoUrl = photoUrl
    }

    constructor() : this(null, null, null)

}