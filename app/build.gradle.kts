plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.taskmate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.taskmate"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    // Ajout du bloc packagingOptions pour résoudre les conflits de fichiers
    packagingOptions {
        exclude("**/META-INF/LICENSE")
        exclude("**/META-INF/LICENSE.txt")
        exclude("**/META-INF/NOTICE")
        exclude("**/META-INF/NOTICE.txt")
        exclude("**/META-INF/DEPENDENCIES")
        exclude("**/META-INF/DEPENDENCIES.txt")
        exclude("**/META-INF/NOTICE.md")
        exclude("**/META-INF/LICENSE.md")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.auth)
    implementation ("com.google.firebase:firebase-auth:23.1.0")
    implementation ("com.google.firebase:firebase-firestore:24.8.2")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation("com.sendgrid:sendgrid-java:4.7.0")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}