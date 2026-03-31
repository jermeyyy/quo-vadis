package com.jermey.navplayground.demo.ui.screens.containerdemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.navigation.ContainerDemoDestination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.flowmvi.rememberSharedContainer
import org.koin.core.qualifier.qualifier
import pro.respawn.flowmvi.compose.dsl.subscribe

@Screen(ContainerDemoDestination.Info::class)
@Composable
fun ContainerDemoInfoScreen() {
    val store =
        rememberSharedContainer<ContainerDemoContainer, ContainerDemoState, ContainerDemoIntent, ContainerDemoAction>(
            qualifier = qualifier<ContainerDemoContainer>()
        )
    val state by store.subscribe()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Info Tab", style = MaterialTheme.typography.headlineMedium)
        Text("Item ID: ${state.itemId}")
    }
}

@Screen(ContainerDemoDestination.Stats::class)
@Composable
fun ContainerDemoStatsScreen() {
    val store =
        rememberSharedContainer<ContainerDemoContainer, ContainerDemoState, ContainerDemoIntent, ContainerDemoAction>(
            qualifier = qualifier<ContainerDemoContainer>()
        )
    val state by store.subscribe()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Stats Tab", style = MaterialTheme.typography.headlineMedium)
        Text("Item ID: ${state.itemId}")
    }
}
