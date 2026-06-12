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
            ?: "jdbc:postgresql://localhost:5432/opencalc_chat"

        val databaseUser = config.propertyOrNull("database.user")?.getString()
            ?: System.getenv("DATABASE_USER")
            ?: "opencalc"

        val databasePassword = config.propertyOrNull("database.password")?.getString()
            ?: System.getenv("DATABASE_PASSWORD")
            ?: "opencalc_secret"

        val dataSource = HikariDataSource(HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = databaseUrl
            username = databaseUser
            password = databasePassword
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        })

        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(RoomsTable, MessagesTable, OfflineMessageQueueTable)
        }
    }
}
