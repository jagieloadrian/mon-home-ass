package com.anjo.monhomeass.testutils

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction

fun prepareDatabase() {
    Database.connect(
            url = "jdbc:h2:mem:home_assistant;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
            user = "home_ass_monitor",
            password = "testpassword"
    )
}


fun addRecords(insertFunc: () -> InsertStatement<Number>) {
    transaction {
        insertFunc()
    }
}