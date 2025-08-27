plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    // تم حذف إضافة Compose لأننا لا نستخدمها
}

android {
    namespace = "com.karmakids.karmalocktouch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.karmakids.karmalocktouch"
        minSdk = 24
        targetSdk = 35
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
    // تم حذف ميزة Compose لأننا لا نستخدمها
    buildFeatures {
        viewBinding = true // استخدام ViewBinding أفضل من findViewById
    }
}

// ✅ هذا هو قسم التبعيات الوحيد والصحيح
dependencies {

    // المكتبات الأساسية لوظائف أندرويد الحديثة وواجهة المستخدم
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // مكتبة Material Design لمكونات واجهة المستخدم العصرية
    implementation("com.google.android.material:material:1.11.0")

    // مكتبة ConstraintLayout التي احتجناها مسبقاً
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // مكتبة Activity KTX (مهمة جداً لحل خطأ registerForActivityResult)
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation(libs.firebase.ai)

    // مكتبات الاختبار القياسية
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}