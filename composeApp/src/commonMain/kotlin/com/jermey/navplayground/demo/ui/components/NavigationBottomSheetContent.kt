package com.jermey.navplayground.demo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssistantDirection
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.DeepLinkDestination
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.destinations.DemoTabs
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.quo.vadis.core.navigation.core.Destination

@Composable
fun NavigationBottomSheetContent(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Navigation Patterns",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        HorizontalDivider()

        BottomSheetNavigationItem(
            icon = Icons.Default.Home,
            label = "Home",
            description = "Main dashboard",
            selected = currentRoute == "home",
            onClick = { onNavigate(MainTabs.HomeTab) }
        )

        BottomSheetNavigationItem(
            icon = Icons.AutoMirrored.Filled.List,
            label = "Master-Detail",
            description = "List with detail view pattern",
            selected = currentRoute?.startsWith("master_detail") == true,
            onClick = { onNavigate(MasterDetailDestination.List) }
        )

        BottomSheetNavigationItem(
            icon = Icons.Default.Dashboard,
            label = "Tabs Example",
            description = "Nested tabs navigation",
            selected = currentRoute?.startsWith("tabs") == true,
            onClick = { onNavigate(DemoTabs.MusicTab.List) }
        )

        BottomSheetNavigationItem(
            icon = Icons.AutoMirrored.Filled.AssistantDirection,
            label = "Process Flow",
            description = "Multi-step wizard",
            selected = currentRoute?.startsWith("process") == true,
            onClick = { onNavigate(ProcessDestination.Start) }
        )

        BottomSheetNavigationItem(
            icon = Icons.Default.Link,
            label = "Deep Link Demo",
            description = "Deep linking examples",
            selected = currentRoute == "deeplink_demo",
            onClick = { onNavigate(DeepLinkDestination.Demo) }
        )

        BottomSheetNavigationItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            description = "App settings",
            selected = currentRoute == "settings",
            onClick = { onNavigate(MainTabs.SettingsTab.Main) }
        )
    }
}
