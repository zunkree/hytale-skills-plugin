package org.zunkree.hytale.plugins.skillsplugin

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import org.zunkree.hytale.plugins.skillsplugin.bootstrap.PluginApplication
import org.zunkree.hytale.plugins.skillsplugin.config.SkillsConfigCodec

class SkillsPlugin(
    init: JavaPluginInit,
) : JavaPlugin(init) {
    val configRef = withConfig("config", SkillsConfigCodec.CODEC)

    private lateinit var app: PluginApplication

    override fun setup() {
        super.setup()
        app = PluginApplication(this)
        app.setup()
    }

    override fun start() {
        super.start()
        app.start()
    }

    override fun shutdown() {
        if (::app.isInitialized) {
            app.shutdown()
        }
        super.shutdown()
    }
}
