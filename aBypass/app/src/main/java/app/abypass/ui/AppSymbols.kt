package app.abypass.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import app.abypass.R

/** Bundled Material Symbols Rounded from google/material-design-icons, Apache 2.0. */
internal object AppSymbols {
    val Apps: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_grid_view)
    val Videocam: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_videocam)
    val Terminal: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_terminal)
    val Science: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_science)
    val AutoAwesome: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_auto_awesome)
    val Settings: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_settings)
    val VerifiedUser: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_verified_user)
    val Palette: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_palette)
    val DesktopWindows: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_desktop_windows)
    val Search: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_search)
    val FavoriteBorder: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_favorite)
    val Favorite: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.symbol_favorite_filled)
}
