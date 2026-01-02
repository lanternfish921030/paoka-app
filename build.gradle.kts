// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.3" apply false // 8/15
}

//subprojects {
//    configurations.configureEach {
//        resolutionStrategy.eachDependency {
//            if (requested.group == "androidx.media3") {
//                useVersion("1.8.1")
//                because("Keep all Media3 artifacts aligned on 1.8.1")
//            }
//        }
//    }
//}
