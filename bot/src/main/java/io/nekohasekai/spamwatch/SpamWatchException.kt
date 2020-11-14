package io.nekohasekai.spamwatch

class SpamWatchException : Exception {

    var statusCode = 0

    constructor()
    constructor(statusCode: Int, message: String) : super(message) {
        this.statusCode = statusCode
    }

    constructor(message: String, cause: Exception) : super(message, cause)
    constructor(cause: Exception) : super(cause)

    override fun toString(): String {

        return if (statusCode != 0) "$statusCode: $message"
        else message ?: (cause ?: this).javaClass.simpleName

    }

}