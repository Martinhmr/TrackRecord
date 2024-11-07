package biz.itonline.trackrecord.exceptions

sealed class HZException(val task: String? = null, msg: String? = null) : Exception(msg)  {

    class NoNetworkConnectionException(task: String? = null,msg: String? = null) : HZException(msg)

    class InvalidUserException(task: String? = null,msg: String? = null) : HZException(msg)

    class InvalidParamException(task: String? = null,msg: String? = null) : HZException(msg)

    class ApiAccessNotAllowed(task: String? = null,msg: String? = null) : HZException(msg)

}