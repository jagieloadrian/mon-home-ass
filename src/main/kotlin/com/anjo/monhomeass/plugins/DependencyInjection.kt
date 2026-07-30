package com.anjo.monhomeass.plugins

import com.anjo.monhomeass.repository.EventRepo
import com.anjo.monhomeass.repository.MigrationRepo
import com.anjo.monhomeass.repository.RecorderRepo
import com.anjo.monhomeass.repository.SchemaChangesRepo
import com.anjo.monhomeass.repository.StatisticMetaRepo
import com.anjo.monhomeass.repository.StatisticRepo
import com.anjo.monhomeass.service.EventService
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.DependencyKey
import io.ktor.server.plugins.di.dependencies

fun Application.configureDI() {
    dependencies {
        provide<EventRepo>(EventRepo::class)
        provide<MigrationRepo>(MigrationRepo::class)
        provide<RecorderRepo>(RecorderRepo::class)
        provide<SchemaChangesRepo>(SchemaChangesRepo::class)
        provide<StatisticMetaRepo>(StatisticMetaRepo::class)
        provide<StatisticRepo>(StatisticRepo::class)
        provide { EventService(get(DependencyKey<EventRepo>())) }
    }
}