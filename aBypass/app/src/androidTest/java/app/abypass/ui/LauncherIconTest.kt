package app.abypass.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import app.abypass.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
    @SdkSuppress(minSdkVersion = 33)
    @Test fun launcherMarkIsCenteredInsideTheSafeZoneAndSupportsThemedIcons() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val foreground = requireNotNull(context.getDrawable(R.drawable.ic_foreground))
        val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
        foreground.setBounds(0, 0, 108, 108)
        foreground.draw(Canvas(bitmap))
        var left = 108; var top = 108; var right = 0; var bottom = 0
        for (y in 0 until 108) for (x in 0 until 108) {
            if (Color.alpha(bitmap.getPixel(x, y)) > 32) {
                assertTrue("Mark must fit the circular adaptive-icon safe zone", (x + 0.5 - 54) * (x + 0.5 - 54) + (y + 0.5 - 54) * (y + 0.5 - 54) <= 33 * 33)
                left = minOf(left, x); right = maxOf(right, x)
                top = minOf(top, y); bottom = maxOf(bottom, y)
            }
        }
        assertTrue(right > left && bottom > top)
        assertEquals(54f, (left + right + 1) / 2f, 1f)
        assertEquals(54f, (top + bottom + 1) / 2f, 1f)
        val icon = context.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable
        assertNotNull(icon.monochrome)
        val preview = Bitmap.createBitmap(216, 216, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 216, 216)
        icon.draw(Canvas(preview))
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "screenshots").apply { mkdirs() }
        File(directory, "launcher-icon.png").outputStream().use { preview.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
