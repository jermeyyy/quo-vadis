package com.jermey.navplayground

import com.jermey.feature1.resultdemo.Feature1Module
import com.jermey.navplayground.demo.ExploreModule
import com.jermey.navplayground.demo.NavigationModule
import com.jermey.navplayground.demo.ProfileModule
import com.jermey.navplayground.demo.StateDrivenDemoModule
import com.jermey.navplayground.demo.TabsDemoModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(
    modules = [
        NavigationModule::class,
        StateDrivenDemoModule::class,
        TabsDemoModule::class,
        ProfileModule::class,
        ExploreModule::class,
        Feature1Module::class,
    ]
)
object NavPlaygroundKoinApp
