package com.jermey.navplayground

import com.jermey.feature1.resultdemo.Feature1Module
import com.jermey.navplayground.demo.BackHandlerDemoModule
import com.jermey.navplayground.demo.ContainerDemoModule
import com.jermey.navplayground.demo.NavigationModule
import com.jermey.navplayground.demo.StateDrivenDemoModule
import com.jermey.navplayground.demo.TabsDemoModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(
    modules = [
        NavigationModule::class,
        StateDrivenDemoModule::class,
        TabsDemoModule::class,
        Feature1Module::class,
        BackHandlerDemoModule::class,
        ContainerDemoModule::class,
    ]
)
object NavPlaygroundKoinApp
