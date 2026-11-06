package com.myexperience.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myexperience.app.ui.components.FloatingIcon
import com.myexperience.app.ui.components.bounceClick
import com.myexperience.app.ui.screens.*
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.Forest
import com.myexperience.app.ui.theme.MyExperienceTheme
import com.myexperience.app.ui.theme.Sea
import com.myexperience.app.update.AppUpdater
import com.myexperience.app.update.UpdateDialog
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyExperienceTheme { AppRoot() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Mondo", Icons.Rounded.Public),
    Tab("trips", "Viaggi", Icons.Rounded.Luggage),
    Tab("profile", "Profilo", Icons.Rounded.Person),
)

@Composable
fun AppRoot() {
    val vm: MainViewModel = viewModel()
    val nav = rememberNavController()
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        delay(1700)
        showSplash = false
    }
    LaunchedEffect(Unit) { AppUpdater.check(ctx.applicationContext, manual = false) }

    // I viaggi conclusi diventano album: controllo a ogni ritorno nell'app
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.completeEndedTrips() }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTab = tabs.any { it.route == route }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppNavHost(nav, vm)

        AnimatedVisibility(
            visible = onTab,
            enter = slideInVertically(spring(dampingRatio = 0.75f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomBar(
                current = route,
                onSelect = { r ->
                    nav.navigate(r) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAdd = { nav.navigate("place_edit/0") },
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(tween(600)) + scaleOut(tween(600), targetScale = 1.15f),
        ) { Splash() }

        if (!showSplash) UpdateDialog()
    }
}

/** Curva "emphasized" di Material 3: parte decisa, arriva morbida. */
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val NAV_MS = 450

/** Direzione tra le schede della barra: +1 verso destra, -1 verso sinistra, 0 se non sono schede. */
private fun tabDirection(from: String?, to: String?): Int {
    val a = tabs.indexOfFirst { it.route == from }
    val b = tabs.indexOfFirst { it.route == to }
    return if (a < 0 || b < 0) 0 else (b - a).coerceIn(-1, 1)
}

// Le pagine entrano scorrendo per intero sopra la precedente, che arretra in parallasse.
// Si usa lo scorrimento (non solo la dissolvenza) perché il globo è una SurfaceView e ignora l'alpha.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.navEnter(): EnterTransition {
    val dir = tabDirection(initialState.destination.route, targetState.destination.route)
    return if (dir != 0) slideInHorizontally(tween(NAV_MS, easing = Emphasized)) { it * dir / 4 } + fadeIn(tween(NAV_MS / 2))
    else slideInHorizontally(tween(NAV_MS, easing = Emphasized)) { it } + fadeIn(tween(NAV_MS / 3))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.navExit(): ExitTransition {
    val dir = tabDirection(initialState.destination.route, targetState.destination.route)
    return if (dir != 0) slideOutHorizontally(tween(NAV_MS, easing = Emphasized)) { -it * dir / 4 } + fadeOut(tween(NAV_MS / 2))
    else slideOutHorizontally(tween(NAV_MS, easing = Emphasized)) { -it / 4 } +
        scaleOut(tween(NAV_MS, easing = Emphasized), targetScale = 0.94f)
}

private fun navPopEnter(): EnterTransition =
    slideInHorizontally(tween(NAV_MS, easing = Emphasized)) { -it / 4 } +
        scaleIn(tween(NAV_MS, easing = Emphasized), initialScale = 0.94f)

private fun navPopExit(): ExitTransition =
    slideOutHorizontally(tween(NAV_MS, easing = Emphasized)) { it } + fadeOut(tween(NAV_MS, delayMillis = NAV_MS / 2))

@Composable
private fun AppNavHost(nav: NavHostController, vm: MainViewModel) {
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { navEnter() },
        exitTransition = { navExit() },
        popEnterTransition = { navPopEnter() },
        popExitTransition = { navPopExit() },
    ) {
        composable("home") {
            Page {
                HomeScreen(
                    vm = vm,
                    onOpenPlace = { nav.navigate("place/$it") },
                    onOpenTrip = { nav.navigate("trip/$it") },
                    onAddAt = { loc ->
                        vm.setPicked(loc)
                        nav.navigate("place_edit/0")
                    },
                    onOpenUnesco = { nav.navigate("unesco") },
                )
            }
        }
        composable("unesco") {
            Page {
                UnescoScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }
        composable("trips") {
            Page {
                TripsScreen(
                    vm = vm,
                    onOpenTrip = { nav.navigate("trip/$it") },
                    onNewTrip = { nav.navigate("trip_edit/0") },
                )
            }
        }
        composable("profile") {
            Page {
                ProfileScreen(
                    vm = vm,
                    onOpenPlace = { nav.navigate("place/$it") },
                    onAdd = { nav.navigate("place_edit/0") },
                    onOpenUnesco = { nav.navigate("unesco") },
                )
            }
        }
        composable("place/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            Page {
                val id = e.arguments?.getLong("id") ?: 0L
                PlaceDetailScreen(
                    vm = vm,
                    placeId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("place_edit/$id") },
                    onOpenMedia = { index -> nav.navigate("viewer/$id/$index") },
                )
            }
        }
        composable("place_edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            Page {
                val id = e.arguments?.getLong("id") ?: 0L
                PlaceEditScreen(
                    vm = vm,
                    placeId = id,
                    onBack = { nav.popBackStack() },
                    onPickLocation = { nav.navigate("picker/place") },
                    onSaved = { savedId ->
                        nav.popBackStack()
                        if (id == 0L) nav.navigate("place/$savedId")
                    },
                )
            }
        }
        composable(
            "viewer/{id}/{index}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("index") { type = NavType.IntType },
            ),
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(NAV_MS, easing = Emphasized), initialScale = 0.9f) },
            popExitTransition = { fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.9f) },
        ) { e ->
            Page {
                MediaViewerScreen(
                    vm = vm,
                    placeId = e.arguments?.getLong("id") ?: 0L,
                    startIndex = e.arguments?.getInt("index") ?: 0,
                    onBack = { nav.popBackStack() },
                )
            }
        }
        composable(
            "picker/{target}",
            enterTransition = { slideInVertically(tween(NAV_MS, easing = Emphasized)) { it } },
            exitTransition = { navExit() },
            popExitTransition = { slideOutVertically(tween(320, easing = Emphasized)) { it } },
        ) { e ->
            Page {
                LocationPickerScreen(
                    vm = vm,
                    target = e.arguments?.getString("target") ?: "place",
                    onDone = { nav.popBackStack() },
                )
            }
        }
        composable("trip/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            Page {
                val id = e.arguments?.getLong("id") ?: 0L
                TripDetailScreen(
                    vm = vm,
                    tripId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("trip_edit/$id") },
                    onAddDestination = { nav.navigate("picker/$id") },
                    onCompleted = { albumId ->
                        nav.popBackStack()
                        nav.navigate("place/$albumId")
                    },
                )
            }
        }
        composable("trip_edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            Page {
                val id = e.arguments?.getLong("id") ?: 0L
                TripEditScreen(
                    vm = vm,
                    tripId = id,
                    onBack = { nav.popBackStack() },
                    onSaved = { savedId ->
                        nav.popBackStack()
                        if (id == 0L) nav.navigate("trip/$savedId")
                    },
                )
            }
        }
    }
}

/** Fondo opaco per ogni pagina: durante lo scorrimento copre quella sotto (globo compreso). */
@Composable
private fun Page(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
}

@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = Sea, spotColor = Sea)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TabItem(tabs[0], current == tabs[0].route) { onSelect(tabs[0].route) }
            TabItem(tabs[1], current == tabs[1].route) { onSelect(tabs[1].route) }
            AddButton(onAdd)
            TabItem(tabs[2], current == tabs[2].route) { onSelect(tabs[2].route) }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val bgAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(300), label = "bg")
    val tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .graphicsLayer { }
            .background(Brush.linearGradient(listOf(Sea.copy(alpha = bgAlpha), Forest.copy(alpha = bgAlpha))))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .animateContentSize(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, tab.label, tint = tint)
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Text(tab.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "add")
    val glow by t.animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g",
    )
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .size(54.dp)
            .shadow((14 * glow).dp, CircleShape, ambientColor = Sea, spotColor = Forest)
            .clip(CircleShape)
            .background(BrandBrush)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, "Nuova esperienza", tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun Splash() {
    val t = rememberInfiniteTransition(label = "splash")
    val shift by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "s",
    )
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0B1416), Sea.copy(alpha = 0.7f + 0.2f * shift), Forest, Color(0xFFC9A46A)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = 0.6f + 0.4f * enter.value
                scaleY = 0.6f + 0.4f * enter.value
                alpha = enter.value.coerceIn(0f, 1f)
            },
        ) {
            FloatingIcon(Icons.Rounded.Public, 84.dp, Color.White)
            Spacer(Modifier.height(16.dp))
            Text("MyExperience", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                "Ogni viaggio, una storia",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
