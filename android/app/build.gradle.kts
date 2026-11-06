plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "gt.edu.umg.sigelab.mobile"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "gt.edu.umg.sigelab.mobile"

        minSdk = 24
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true

                packageScope = setOf(
                    "androidx.*",
                    "kotlin.",
                    "kotlinx.*"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // Componentes de Android y diseño.
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Lector de códigos QR.
    implementation(
        "com.journeyapps:zxing-android-embedded:4.3.0"
    )

    // Cámara frontal y captura de imágenes.
    implementation(
        "androidx.camera:camera-core:1.4.2"
    )

    implementation(
        "androidx.camera:camera-camera2:1.4.2"
    )

    implementation(
        "androidx.camera:camera-lifecycle:1.4.2"
    )

    implementation(
        "androidx.camera:camera-view:1.4.2"
    )

    // Pruebas.
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}