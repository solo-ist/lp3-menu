plugins { id("com.android.application") }

android {
    namespace = "ist.solo.menu"
    compileSdk = 34

    defaultConfig {
        applicationId = "ist.solo.menu"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // Private signing identity. The default debug key is public and forgeable,
    // and Menu is the only route to every app hidden from the toolbox — an
    // update signed by anyone else would take the whole shelf with it. The
    // keystore lives outside the repo; the password comes from the
    // environment at build time via `op read`, never from a file.
    signingConfigs {
        create("soloist") {
            storeFile = file(
                System.getenv("MENU_SIGNING_STORE")
                    ?: "${System.getProperty("user.home")}/.android-keys/soloist-menu.jks",
            )
            storePassword = System.getenv("MENU_SIGNING_PASSWORD").orEmpty()
            keyAlias = "soloist-menu"
            keyPassword = System.getenv("MENU_SIGNING_PASSWORD").orEmpty()
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("soloist")
        }
        release {
            // 21 KB of platform-only code; R8 buys nothing and risks stripping
            // the manifest-referenced receiver.
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("soloist")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
