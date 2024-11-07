package biz.itonline.trackrecord.support

data class UserData(
    var userId: String? = null,
    var userNick: String? = null,
    var userToken: String? = null,
    var userEmail: String? = null,
){
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "userNick" to userNick,
            "userToken" to userToken,
            "userEmail" to userEmail,
        )
    }

    fun clearUser(){
        userId = null
        userNick = null
        userToken = null
        userEmail = null
    }
}
