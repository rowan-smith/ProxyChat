package dev.rono.proxychat.core

import dev.rono.proxychat.core.utils.ToggleUtils
import org.koin.dsl.module

val proxyChatModule = module {
    single { ToggleUtils() }
}
