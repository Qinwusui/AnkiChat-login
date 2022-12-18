package com.wusui.plugin

import com.wusui.data.LogUser
import com.wusui.data.RegUser
import io.ktor.util.*

internal fun convertContentToUser(content: String): RegUser? {
    try {
        val strList = content.decodeBase64String().split("||")
        if (strList.size != 3) {
            return null
        }
        val uName = strList[0]
        val qq = strList[1]
        val pwd = strList[2]
        return RegUser(uName, qq, pwd)
    } catch (e: Exception) {
        println(e.message)
        return null
    }
}

internal fun convertContentToLogin(content: String): LogUser? {
    try {
        val strList = content.decodeBase64String().split("||")
        if (strList.size != 2) {
            return null
        }
        val qq = strList[0]
        val pwd = strList[1]
        return LogUser(qq, pwd)
    } catch (e: Exception) {
        return null
    }
}