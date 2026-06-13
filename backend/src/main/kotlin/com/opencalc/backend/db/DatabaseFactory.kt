package com.opencalc.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: ApplicationConfig) {
        val databaseUrl = config.propertyOrNull("database.url")?.getString()
            ?: System.getenv("DATABASE_URL")
            ?: "jdbc:sqlite:./opencalc.db"

        val databaseUser = config.propertyOrNull("database.user")?.getString()
            ?: System.getenv("DATABASE_USER")
            ?: ""

        val databasePassword = config.propertyOrNull("database.password")?.getString()
            ?: System.getenv("DATABASE_PASSWORD")
            ?: ""

        val driverClass = when {
            databaseUrl.startsWith("jdbc:postgresql") -> "org.postgresql.Driver"
            databaseUrl.startsWith("jdbc:sqlite") -> "org.sqlite.JDBC"
            else -> "org.sqlite.JDBC"
        }

        val dataSource = HikariDataSource(HikariConfig().apply {
            driverClassName = driverClass
            jdbcUrl = databaseUrl
            if (databaseUser.isNotEmpty()) username = databaseUser
            if (databasePassword.isNotEmpty()) password = databasePassword
            maximumPoolSize = 10
            isAutoCommit = false
            if (driverClass == "org.postgresql.Driver") {
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            }
            validate()
        })

        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(RoomsTable, MessagesTable, OfflineMessageQueueTable)
        }
    }
}
