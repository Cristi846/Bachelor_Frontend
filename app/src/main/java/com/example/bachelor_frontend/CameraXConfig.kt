package com.example.bachelor_frontend

import androidx.camera.core.CameraXConfig
import androidx.camera.camera2.Camera2Config

class CameraXConfig : CameraXConfig.Provider {
    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .build()
    }
}