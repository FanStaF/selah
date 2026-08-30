package com.fanstaf.selah

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.fanstaf.selah.service.UnlockService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fanstaf.selah.data.DisplayMode
import com.fanstaf.selah.ui.MainViewModel
import com.fanstaf.selah.ui.SettingsScreen
import com.fanstaf.selah.ui.TodayScreen
import com.fanstaf.selah.ui.VersesScreen
import com.fanstaf.selah.ui.overlay.OverlayController
import com.fanstaf.selah.ui.theme.SelahTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private var overlayGranted by mutableStateOf(false)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(this)
        enableEdgeToEdge()
        refreshOverlayGranted()
        maybeRequestNotifications()
        // Self-heal: if the feature is on but the service isn't running (after an app update or a
        // background kill), restart it when the app is opened.
        lifecycleScope.launch {
            if (AppGraph.settings.settings.first().enabled && Settings.canDrawOverlays(this@MainActivity)) {
                UnlockService.start(this@MainActivity)
            }
        }

        setContent {
            SelahTheme {
                MainScaffold(
                    vm = vm,
                    overlayGranted = overlayGranted,
                    onRequestOverlay = ::requestOverlayPermission,
                    onEnableToggle = ::onEnableToggle,
                    onPreview = ::previewVerse,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshOverlayGranted()
    }

    private fun refreshOverlayGranted() {
        overlayGranted = Settings.canDrawOverlays(this)
    }

    private fun onEnableToggle(enable: Boolean) {
        if (enable && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        vm.setEnabled(enable)
    }

    private fun requestOverlayPermission() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun maybeRequestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Show the current verse right now, so the look/timing can be checked without unlocking. */
    private fun previewVerse() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        val s = vm.settings.value
        val active = vm.verses.value.filter { it.active }
        val verse = when {
            active.isEmpty() -> return
            s.singleVerseId >= 0 -> active.firstOrNull { it.id == s.singleVerseId } ?: active.first()
            else -> active.first()
        }
        OverlayController(applicationContext).show(verse, s.mode, s.durationSeconds, s.fontScale)
    }
}

private enum class Tab(val label: String) { Today("Today"), Verses("Verses"), Settings("Settings") }

@Composable
private fun MainScaffold(
    vm: MainViewModel,
    overlayGranted: Boolean,
    onRequestOverlay: () -> Unit,
    onEnableToggle: (Boolean) -> Unit,
    onPreview: () -> Unit,
) {
    var tab by remember { mutableStateOf(Tab.Today) }
    val settings by vm.settings.collectAsState()
    val verses by vm.verses.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Today,
                    onClick = { tab = Tab.Today },
                    icon = { Icon(Icons.Outlined.WbTwilight, contentDescription = null) },
                    label = { Text(Tab.Today.label) },
                )
                NavigationBarItem(
                    selected = tab == Tab.Verses,
                    onClick = { tab = Tab.Verses },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text(Tab.Verses.label) },
                )
                NavigationBarItem(
                    selected = tab == Tab.Settings,
                    onClick = { tab = Tab.Settings },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(Tab.Settings.label) },
                )
            }
        },
    ) { inner ->
        val contentModifier = Modifier.padding(inner)
        when (tab) {
            Tab.Today -> TodayScreen(
                modifier = contentModifier,
                settings = settings,
                overlayGranted = overlayGranted,
                activeCount = verses.count { it.active },
                onRequestOverlay = onRequestOverlay,
                onEnableToggle = onEnableToggle,
                onPreview = onPreview,
            )
            Tab.Verses -> VersesScreen(
                modifier = contentModifier,
                verses = verses,
                singleVerseId = settings.singleVerseId,
                selectionIsSingle = settings.selection == com.fanstaf.selah.data.SelectionStrategy.SINGLE,
                onAdd = vm::addVerse,
                onUpdate = vm::updateVerse,
                onDelete = vm::deleteVerse,
                onSetActive = vm::setActive,
                onSetSingle = vm::setSingleVerse,
            )
            Tab.Settings -> SettingsScreen(
                modifier = contentModifier,
                settings = settings,
                onDuration = vm::setDuration,
                onMode = vm::setMode,
                onSelection = vm::setSelection,
                onMinInterval = vm::setMinInterval,
                onFontScale = vm::setFontScale,
            )
        }
    }
}
