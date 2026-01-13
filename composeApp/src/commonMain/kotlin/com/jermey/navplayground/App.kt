package com.jermey.navplayground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jermey.navplayground.demo.DemoApp
import com.jermey.navplayground.demo.navigationModule
import com.jermey.navplayground.demo.profileModule
import com.jermey.navplayground.demo.ui.theme.NavPlaygroundTheme
import com.jermey.navplayground.demo.ui.theme.rememberThemeManager
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import org.koin.ksp.generated.com_jermey_feature1_resultdemo_Feature1Module
import org.koin.ksp.generated.com_jermey_navplayground_demo_ExploreModule
import org.koin.ksp.generated.com_jermey_navplayground_demo_ProfileModule
import org.koin.ksp.generated.com_jermey_navplayground_demo_StateDrivenDemoModule
import org.koin.ksp.generated.com_jermey_navplayground_demo_TabsDemoModule

@Composable
fun App() {
    val themeManager = rememberThemeManager()
    val themeMode by themeManager.themeMode.collectAsState()
    val koinConfiguration = koinConfiguration {
        modules(
            navigationModule,
            profileModule,
            com_jermey_navplayground_demo_ProfileModule,
            com_jermey_navplayground_demo_TabsDemoModule,
            com_jermey_navplayground_demo_StateDrivenDemoModule,
            com_jermey_navplayground_demo_ExploreModule,
            com_jermey_feature1_resultdemo_Feature1Module,
        )
    }
    KoinApplication(configuration = koinConfiguration) {
        NavPlaygroundTheme(themeMode = themeMode) {
            DemoApp()
        }
    }
}
