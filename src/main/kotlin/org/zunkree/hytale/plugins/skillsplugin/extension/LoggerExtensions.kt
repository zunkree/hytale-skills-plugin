package org.zunkree.hytale.plugins.skillsplugin.extension

import com.hypixel.hytale.logger.HytaleLogger
import java.util.logging.Level

inline fun HytaleLogger.debug(message: () -> String) {
    at(Level.FINE).log("%s", message())
}

inline fun HytaleLogger.info(message: () -> String) {
    at(Level.INFO).log("%s", message())
}

inline fun HytaleLogger.error(message: () -> String) {
    at(Level.SEVERE).log("%s", message())
}
