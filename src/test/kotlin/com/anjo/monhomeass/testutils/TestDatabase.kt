package com.anjo.monhomeass.testutils

import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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