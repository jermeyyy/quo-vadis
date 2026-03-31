package com.jermey.navplayground.demo.ui.screens.containerdemo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.jermey.navplayground.navigation.ContainerDemoDestination
import com.jermey.quo.vadis.annotations.TabsContainer
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
import com.jermey.quo.vadis.flowmvi.rememberSharedContainer
import org.koin.core.qualifier.qualifier
import pro.respawn.flowmvi.compose.dsl.subscribe

@OptIn(ExperimentalMaterial3Api::class)
@TabsContainer(ContainerDemoDestination::class)
@Composable
fun ContainerDemoWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    val store =
        rememberSharedContainer<ContainerDemoContainer, ContainerDemoState, ContainerDemoIntent, ContainerDemoAction>(
            qualifier = qualifier<ContainerDemoContainer>()
        )
    val state by store.subscribe()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Item: ${state.itemId}") },
                navigationIcon = {
                    IconButton(onClick = { scope.navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val tabsList = scope.tabs.toList()
            TabRow(
                selectedTabIndex = tabsList.indexOf(scope.activeTab).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth()
            ) {
                tabsList.forEach { tab ->
                    val (label, icon) = when (tab) {
                        is ContainerDemoDestination.Info -> "Info" to Icons.Default.Info
                        is ContainerDemoDestination.Stats -> "Stats" to Icons.Default.QueryStats
                        else -> "Tab" to Icons.Default.Info
                    }
                    Tab(
                        selected = scope.activeTab == tab,
                        onClick = { scope.switchTab(tab) },
                        enabled = !scope.isTransitioning,
                        text = { Text(label) },
                        icon = { Icon(icon, contentDescription = label) }
                    )
                }
            }

            content()
        }
    }
}
