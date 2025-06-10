plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.looom.virtualstream"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.looom.virtualstream"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        /**
         *  启用 BuildConfig 生成, 让xposed能获取到当前包名,
         *  该功能于AGP 8.0弃用, 于AGP 9.0彻底删除
         *  参考 https://stackoverflow.com/questions/74634321/fixing-the-build-type-contains-custom-buildconfig-fields-but-the-feature-is-di
         *  参考 https://cs.android.com/android-studio/platform/tools/base/+/0bc1c23297760643b03e8cfd8acc52c007a99cd6
         */
        buildConfig = true // 这目前是个无效的选项
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    /**
     * 加入三方库
     */
    implementation(libs.haze) // 组件模糊库
    implementation(libs.gson) // Gson
    compileOnly(libs.api) // Xposed API
}