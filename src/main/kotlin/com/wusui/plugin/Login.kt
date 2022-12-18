package com.wusui.plugin

import com.wusui.data.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.login() {
    install(ContentNegotiation) {
        gson {
        }
    }
    route("/user") {
        //注册接口
        post("/sign") {
            try {
                val signInfo = call.receive<SignInfo>()
                ChatUserManagement.signIn(signInfo.content).collect {
                    if (it) {
                        call.respond(
                            ResInfo.SignSuccess
                        )
                    } else {
                        call.respond(ResInfo.SignFailure)
                    }
                }
            } catch (e: Exception) {

                call.respond(ResInfo.LoginFailure)
            }

        }
        //登录接口
        post("/login") {
            val signInfo = call.receive<SignInfo>()
            ChatUserManagement.userLogin(signInfo.content).collect {
                if (it) {
                    call.respond(ResInfo.LoginSuccess)
                } else {
                    call.respond(ResInfo.LoginFailure)
                }

            }
        }
        //获取QQ号
        post("/requestQQ") {
            val signInfo=call.receive<SignInfo>()
            call.respond(ResInfo(code = 0,success = true, msg = ChatUserManagement.requestQQ(signInfo.content)))
        }
        //获取用户名
        post("/uname") {
            val signInfo=call.receive<SignInfo>()
            call.respond(ResInfo(code = 0,success = true, msg = ChatUserManagement.requestUserName(signInfo.content)))
        }
        //获取好友列表
        post("/queryFriends") {
            val qq = call.receiveText()
            ChatUserManagement.queryUserFriends(qq).collect{
                call.respond(ResFriendList(
                    code = 0,
                    success = true,
                    list = it
                ))
            }
        }
        //模糊查询用户
        post("/querySearchFriends") {
            val qq =call.receiveText()
            ChatUserManagement.querySearchList(qq).collect{
                call.respond(ResFriendList(
                    code = 0,
                    success = true,
                    list=it
                ))
            }
        }
        //获取验证信息
        post("/queryVerifyMsg"){
            val qq=call.receiveText()
            ChatUserManagement.queryVerifyMsg(qq).collect{
                call.respond(VerifyList(
                    code = 0,
                    success = true,
                    list = it
                ))
            }
        }
        //增加验证信息
        post("/addVerifyMsg") {
            val verifyInfo=call.receive<VerifyInfo>()
            ChatUserManagement.addVerifyInfo(verifyInfo).collect{
                call.respond(ResInfo(
                    code = 0,
                    success = it,
                    msg = ""
                ))
            }
        }
        //回应验证信息
        post("/respVerifyMsg") {
            val verifyInfo=call.receive<VerifyResp>()
            ChatUserManagement.respVerifyMsg(verifyInfo).collect{
                call.respond(ResInfo(
                    code = 0,
                    success = it,
                    msg = ""
                ))
            }
        }
        //修改密码
        post("/mod") {
            val signInfo = call.receive<SignInfo>()
            ChatUserManagement.updatePwd(signInfo.content).collect {
                if (it) {
                    call.respond(ResInfo.ModSuccess)
                } else {
                    call.respond(ResInfo.ModFailure)
                }
            }
        }
    }
}