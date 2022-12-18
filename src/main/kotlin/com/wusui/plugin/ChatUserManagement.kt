package com.wusui.plugin

import com.wusui.data.FriendItem
import com.wusui.data.VerifyInfo
import com.wusui.data.VerifyResp
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jetbrains.annotations.TestOnly
import java.sql.DriverManager
import java.sql.Statement
import kotlin.coroutines.CoroutineContext

object ChatUserManagement {
    private const val DB = "jdbc:sqlite:d:/AnkiChat/user.db"

    init {
        Class.forName("org.sqlite.JDBC")
        createTable()
    }


    /**
     * 更改密码
     */
    fun updatePwd(content: String) = flowByIO {
        val strList = content.decodeBase64String().split("||")
        if (strList.size != 3) {
            return@flowByIO false
        }
        val oldContent = strList
            .joinToString("||")
            .substringBeforeLast("||")
            .encodeBase64()
        val newPwd = strList[3]
        //用户存在才可进行更新密码
        if (getUserExist(oldContent)) {
            val chatUser = convertContentToUser(oldContent) ?: return@flowByIO false
            return@flowByIO useDataBasePool {
                it.executeUpdate(
                    """
                update User set Pwd='${newPwd.encodeBase64()}' 
                where qq='${chatUser.qq}'
                and Pwd ='${chatUser.pwd.encodeBase64()}'
            """.trimIndent()
                )
            }
        }
        return@flowByIO false
    }

    /**
     * 用户登录
     */
    fun userLogin(content: String, c: Boolean = userLogin(content)) = flowByIO { c }


    /**
     * 查询用户是否存在
     */
    private fun getUserExist(content: String): Boolean {
        try {
            var i = 0
            val chatUser = convertContentToUser(content) ?: return false
            useDataBasePool {
                val res = it.executeQuery(
                    """
                select * from User 
                where qq='${chatUser.qq}'
                    
            """.trimIndent()
                )
                while (res.next()) {
                    i++
                }
            }
            return i == 1
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 用户登录
     */
    private fun userLogin(content: String): Boolean {
        val logUser = convertContentToLogin(content) ?: return false
        val pwd = logUser.pwd
        val qq = logUser.qq
        var i = 0
        useDataBasePool {
            val set = it.executeQuery(
                """
                select * from User where qq ='$qq' and Pwd='${pwd.encodeBase64()}';
            """.trimIndent()
            )
            while (set.next()) {
                i++
            }
        }
        return i == 1
    }

    /**
     * 插入用户
     */
    fun signIn(content: String) = flowByIO {
        if (getUserExist(content)) {
            return@flowByIO false
        }
        try {
            val chatUser = convertContentToUser(content) ?: return@flowByIO false
            val uName = chatUser.uName
            val pwd = chatUser.pwd
            val qq = chatUser.qq
            if (!getUserExist(content)) {
                useDataBasePool {
                    it.executeUpdate("insert into User values ('$uName','${pwd.encodeBase64()}','$qq',null)")
                }
                useDataBasePool {
                    it.execute(
                        """
                        create table main.'cl_${qq}'
                        (
                            f_qq   text,
                            f_name text
                        );
                    """.trimIndent()
                    )
                }
            } else {
                false
            }
        } catch (e: Exception) {
            println(e.message)
            false
        }
    }

    /**
     * 查询用户好友
     */
    fun queryUserFriends(qq: String) = flowByIO {
        val fList = mutableListOf<FriendItem>()
        useDataBasePool {
            val qqSet = it.executeQuery(
                """
                select * from User where qq='$qq';
            """.trimIndent()
            )
            var qqInt = 0
            while (qqSet.next()) {
                qqInt++
            }
            if (qqInt == 1) {
                val set = it.executeQuery(
                    """
                    select * from main.'cl_${qq}';
                """.trimIndent()
                )

                while (set.next()) {
                    fList.add(
                        FriendItem(
                            qq = set.getString(1),
                            uName = set.getString(2)
                        )
                    )
                }
            }

        }

        fList
    }

    /**
     * 获取加好友信息
     */
    fun queryVerifyMsg(qq: String) = flowByIO {
        val list = mutableListOf<VerifyInfo>()
        useDataBasePool {
            val set = it.executeQuery(
                """
                select * from main.verify_list where to_u='$qq'; 
                """.trimIndent()
            )
            while (set.next()) {
                list.add(
                    VerifyInfo(
                        from = set.getString(1),
                        to = set.getString(2),
                        verifyMsg = set.getString(3),
                        submitTime = set.getString(4)
                    )
                )
            }
        }
        list
    }

    /**
     * 对验证消息进行回应，
     */
    @TestOnly
    fun respVerifyMsg(verifyResp: VerifyResp) = flowByIO {
        val verifyInfo = verifyResp.verifyInfo

        if (verifyResp.admit) {//如果接受
            useDataBasePool {
                it.executeUpdate(
                    """
                   insert  into  'cl_${verifyInfo.from}'(f_qq,f_name)
                    select qq,User.uName from User where qq='${verifyInfo.to}';
                """.trimIndent()
                )
                println("第一步完成")
            }
            useDataBasePool {
                it.execute(
                    """
                   insert  into  'cl_${verifyInfo.to}'(f_qq,f_name)
                    select qq,User.uName from User where qq='${verifyInfo.from}';
                """.trimIndent()
                )
                println("第二步完成")
            }
            useDataBasePool {
                //存入数据库之后，将验证消息删除
                it.execute(
                    """
                    delete 
                    from verify_list
                    where from_u = '${verifyInfo.from}'
                      and to_u = '${verifyInfo.to}';
                """.trimIndent()
                )
                println("第三步完成")
            }
        } else {
            //如果不接受该请求，则直接从消息中删除验证消息
            useDataBasePool {
                //存入数据库之后，将验证消息删除
                it.executeUpdate(
                    """
                    DELETE
                    FROM verify_list
                    WHERE from_u = '${verifyInfo.from}'
                      AND to_u = '${verifyInfo.to}'
                      AND verify_msg = '${verifyInfo.verifyMsg}'
                      AND verify_t = '${verifyInfo.submitTime}';
                """.trimIndent()
                )
            }
        }
    }

    /**
     * 通过QQ模糊查询好友列表
     */
    fun querySearchList(qq: String) = flowByIO {
        val list = mutableListOf<FriendItem>()
        if (qq.isEmpty()){
            return@flowByIO list
        }
        useDataBasePool {
            val set = it.executeQuery(
                """
                select qq,uName from User where qq like '%$qq%' or uName like '%$qq%';
            """.trimIndent()
            )
            while (set.next()) {
                with(list) {
                    add(
                        FriendItem(
                            set.getString(1),
                            set.getString(2)
                        )
                    )
                }
            }
        }
        list
    }

    /**
     * 添加好友，需要 发->收 两者的QQ，以及时间
     */
    fun addVerifyInfo(verifyInfo: VerifyInfo) = flowByIO {
        //首先检查有没有相同的验证消息
        var i = 0
        useDataBasePool {
            val res = it.executeQuery(
                """
                select * from main.verify_list where from_u='${verifyInfo.from}' and to_u ='${verifyInfo.to}';
                """.trimIndent()
            )
            while (res.next()) i++
        }
        //没有在数据库中找到相同的数据
        if (i == 0) {
            //进行数据插入
            useDataBasePool {
                it.executeUpdate(
                    """
                    insert into main.verify_list 
                    values ('${verifyInfo.from}',
                        '${verifyInfo.to}',
                        '${verifyInfo.verifyMsg}',
                        '${verifyInfo.submitTime}'
                        );
                    """.trimIndent()
                )
            }
        } else {
            false
        }
    }

    /**
     * 创建数据库表
     *
     */
    private fun createTable() {
        useDataBasePool {
            it.execute(
                """
            create table main.User(
                uName TEXT,
                Pwd TEXT,
                qq TEXT,
                uid integer primary key AUTOINCREMENT
            );
        """.trimIndent()
            )
            it.execute(
                """
                create table main.verify_list
                (
                    from_u text,
                    to_u   text,
                    verify_msg text,
                    verify_t text
                );
            """.trimIndent()
            )
        }
    }

    /**
     * Kotlin 数据库 DSL 统一打开关闭数据库连接
     */
    private fun useDataBasePool(content: (statement: Statement) -> Unit): Boolean {
        val driverManager = DriverManager.getConnection(DB)
        driverManager.autoCommit = false
        val stmt = driverManager.createStatement()
        return try {
            stmt.apply(content)
            driverManager.commit()
            stmt.close()
            driverManager.close()
            true
        } catch (e: Exception) {
            println(e.message)
            false
        }
    }

    //获取QQ
    fun requestQQ(content: String): String {
        val chatUser = convertContentToLogin(content) ?: return ""
        return chatUser.qq
    }

    //获取用户名
    fun requestUserName(content: String): String {
        val chatUser = convertContentToLogin(content) ?: return ""
        val qq = chatUser.qq
        var uname = ""
        useDataBasePool {
            val res = it.executeQuery(
                """
                select uName from User where qq='$qq';
                """.trimIndent()
            )
            while (res.next()) {
                uname = res.getString(1)
            }
        }
        return uname
    }

    /**
     * 泛型`<T>` flow
     */
    private fun <T> flowByIO(coroutineContext: CoroutineContext = Dispatchers.IO, content: suspend () -> T) =
        flow {
            emit(content())
        }.distinctUntilChanged().flowOn(coroutineContext)


}