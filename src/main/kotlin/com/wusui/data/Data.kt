package com.wusui.data

import kotlinx.serialization.Serializable

@Serializable
data class SignInfo(
    val content: String
)

data class RegUser(
    val uName: String,
    val qq: String,
    val pwd: String,
)

data class LogUser(
    val qq: String,
    val pwd: String
)

@Serializable
data class FriendItem(
    val qq: String,
    val uName: String
)

@Serializable
data class ResFriendList(
    val code: Int,
    val success: Boolean,
    val list: MutableList<FriendItem>
)

@Serializable
data class VerifyInfo(
    val from: String,
    val to: String,
    val verifyMsg: String,
    val submitTime: String
)
@Serializable
data class VerifyResp(
    val verifyInfo: VerifyInfo,
    val admit: Boolean
)

@Serializable
data class VerifyList(
    val code: Int,
    val success: Boolean,
    val list: MutableList<VerifyInfo>
)

@Serializable
open class ResInfo(
    val code: Int, val success: Boolean, val msg: String
) {
    object SignSuccess : ResInfo(code = 0, success = true, msg = "注册成功！")
    object SignFailure : ResInfo(code = -1, success = false, msg = "注册失败！")
    object LoginSuccess : ResInfo(code = 0, success = true, msg = "登录成功！")
    object ModSuccess : ResInfo(code = 0, success = true, msg = "修改成功！")
    object LoginFailure : ResInfo(code = -1, success = false, msg = "登录失败！")
    object ModFailure : ResInfo(code = -1, success = false, msg = "修改失败！")
}