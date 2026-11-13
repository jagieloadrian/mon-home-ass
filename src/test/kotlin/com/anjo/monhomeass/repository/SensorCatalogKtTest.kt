package com.anjo.monhomeass.repository

import com.anjo.monhomeass.model.SensorSource
import com.anjo.monhomeass.model.dto.HaRegistries
import com.anjo.monhomeass.model.dto.SensorDto
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun source(
    entityId: String,
    friendlyName: String? = null,
    deviceClass: String? = null,
) = SensorSource(entityId, friendlyName, deviceClass, unit = null, metaName = null, metaUnit = null, unitClass = null)

private fun registries(vararg entities: HaRegistries.Entity) =
    HaRegistries(
        entities = entities.toList(),
        devices =
            listOf(
                HaRegistries.Device(id = "aqara", nameByUser = null, areaId = "kitchen"),
                HaRegistries.Device(id = "plug", nameByUser = "Desk plug", areaId = "office"),
            ),
        areas =
            listOf(
                HaRegistries.Area(areaId = "kitchen", name = "Kitchen", floorId = "ground"),
                HaRegistries.Area(areaId = "office", name = "Office", floorId = null),
            ),
        floors = listOf(HaRegistries.Floor(floorId = "ground", name = "Ground floor")),
    )

class SensorCatalogKtTest :
    StringSpec({

        "should take the area from the device and the floor from the area when the entity has no area of its own" {
            // given
            val input = listOf(source("sensor.0x00158d0001a2b3c4_temperature", "0x00158d0001a2b3c4 temperature", "temperature"))
            val entity = HaRegistries.Entity("sensor.0x00158d0001a2b3c4_temperature", deviceId = "aqara")

            // when
            val result = sensorCatalog(input, registries(entity))

            // then
            result shouldBe
                listOf(
                    SensorDto("sensor.0x00158d0001a2b3c4_temperature", "Kitchen temperature", "Kitchen", "Ground floor", "climate", null),
                )
        }

        "should prefer the entity area over the device area when both are set" {
            // given
            val input = listOf(source("sensor.plug_power", deviceClass = "power"))
            val entity = HaRegistries.Entity("sensor.plug_power", deviceId = "aqara", areaId = "office")

            // when
            val result = sensorCatalog(input, registries(entity))

            // then
            result.single().area shouldBe "Office"
            result.single().floor shouldBe null
        }

        "should name the sensor after the entity override when the user renamed the entity" {
            // given
            val input = listOf(source("sensor.plug_power", "Plug power", "power"))
            val entity = HaRegistries.Entity("sensor.plug_power", name = "Desk power", deviceId = "plug")

            // when
            val result = sensorCatalog(input, registries(entity))

            // then
            result.single().name shouldBe "Desk power"
        }

        "should name the sensor after the user device name and the entity name when the entity has an entity name" {
            // given
            val input = listOf(source("sensor.plug_power", "Plug power", "power"), source("sensor.plug_state", "Plug"))
            val entities =
                arrayOf(
                    HaRegistries.Entity("sensor.plug_power", originalName = "Power", hasEntityName = true, deviceId = "plug"),
                    HaRegistries.Entity("sensor.plug_state", originalName = "Power", hasEntityName = false, deviceId = "plug"),
                )

            // when
            val result = sensorCatalog(input, registries(*entities))

            // then
            result.map { it.name } shouldBe listOf("Desk plug Power", "Desk plug")
        }

        "should name the sensor after the area and the device class when no user name is set" {
            // given
            val input = listOf(source("sensor.aqara_co2", "Aqara co2", "carbon_dioxide"), source("sensor.aqara_x", "Aqara x"))
            val entities =
                arrayOf(
                    HaRegistries.Entity("sensor.aqara_co2", originalName = "CO2", deviceId = "aqara"),
                    HaRegistries.Entity("sensor.aqara_x", originalName = "Uptime", deviceId = "aqara"),
                )

            // when
            val result = sensorCatalog(input, registries(*entities))

            // then
            result.map { it.name } shouldBe listOf("Kitchen carbon dioxide", "Kitchen Uptime")
        }

        "should keep the recorder name when the entity has neither a user name nor an area" {
            // given
            val input = listOf(source("sensor.outdoor_temperature", "Outdoor temperature", "temperature"))
            val entity = HaRegistries.Entity("sensor.outdoor_temperature", originalName = "Temperature")

            // when
            val result = sensorCatalog(input, registries(entity))

            // then
            result shouldBe listOf(SensorDto("sensor.outdoor_temperature", "Outdoor temperature", null, null, "climate", null))
        }

        "should suffix repeated names in entity id order when two sensors resolve to the same name" {
            // given
            val input = listOf(source("sensor.b", deviceClass = "temperature"), source("sensor.a", deviceClass = "temperature"))
            val entities = arrayOf(HaRegistries.Entity("sensor.a", deviceId = "aqara"), HaRegistries.Entity("sensor.b", deviceId = "aqara"))

            // when
            val result = sensorCatalog(input, registries(*entities))

            // then
            result.map { it.entityId to it.name } shouldBe
                listOf("sensor.a" to "Kitchen temperature", "sensor.b" to "Kitchen temperature 2")
        }

        "should put the sensor into the diagnostic category when its entity category is diagnostic or config" {
            // given
            val input = listOf(source("sensor.a", deviceClass = "temperature"), source("sensor.b", deviceClass = "battery"))
            val entities =
                arrayOf(
                    HaRegistries.Entity("sensor.a", entityCategory = "diagnostic"),
                    HaRegistries.Entity("sensor.b", entityCategory = "config"),
                )

            // when
            val result = sensorCatalog(input, registries(*entities))

            // then
            result.map { it.category } shouldBe listOf("diagnostic", "diagnostic")
        }

        "should keep the recorder-only sensor when the entity is missing from the registry" {
            // given
            val input = listOf(source("opower:elec_cost"))

            // when
            val result = sensorCatalog(input, registries())

            // then
            result shouldBe listOf(SensorDto("opower:elec_cost", "Elec cost", null, null, "other", null))
        }
    })
