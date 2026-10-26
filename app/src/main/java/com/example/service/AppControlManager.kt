package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

data class AppTarget(
    val id: String,
    val displayName: String,
    val packageName: String,
    val webFallbackUrl: String,
    val keywords: List<String>
)

sealed interface SpecialCommand {
    data class CurrentTime(val englishText: String, val bengaliText: String) : SpecialCommand
    data class WeatherQuery(val englishText: String, val bengaliText: String) : SpecialCommand
    data object StopListening : SpecialCommand
    data class Greeting(val englishText: String, val bengaliText: String) : SpecialCommand
}

class AppControlManager(private val context: Context) {

    companion object {
        private const val TAG = "AppControlManager"

        val SUPPORTED_APPS = listOf(
            AppTarget(
                id = "youtube",
                displayName = "YouTube",
                packageName = "com.google.android.youtube",
                webFallbackUrl = "https://m.youtube.com",
                keywords = listOf("youtube", "ইউটিউব", "ইউটিউবে")
            ),
            AppTarget(
                id = "chrome",
                displayName = "Google Chrome",
                packageName = "com.android.chrome",
                webFallbackUrl = "https://www.google.com",
                keywords = listOf("chrome", "ক্রোম", "browser", "ব্রাউজার", "গুগল ক্রোম")
            ),
            AppTarget(
                id = "facebook",
                displayName = "Facebook",
                packageName = "com.facebook.katana",
                webFallbackUrl = "https://m.facebook.com",
                keywords = listOf("facebook", "ফেসবুক", "fb", "এফবি")
            ),
            AppTarget(
                id = "whatsapp",
                displayName = "WhatsApp",
                packageName = "com.whatsapp",
                webFallbackUrl = "https://web.whatsapp.com",
                keywords = listOf("whatsapp", "হোয়াটসঅ্যাপ", "হোয়াটস অ্যাপ")
            ),
            AppTarget(
                id = "instagram",
                displayName = "Instagram",
                packageName = "com.instagram.android",
                webFallbackUrl = "https://www.instagram.com",
                keywords = listOf("instagram", "ইনস্টাগ্রাম", "ইন্সটাগ্রাম", "insta")
            ),
            AppTarget(
                id = "gmail",
                displayName = "Gmail",
                packageName = "com.google.android.gm",
                webFallbackUrl = "https://mail.google.com",
                keywords = listOf("gmail", "জিমেইল", "email", "ইমেইল")
            ),
            AppTarget(
                id = "maps",
                displayName = "Google Maps",
                packageName = "com.google.android.apps.maps",
                webFallbackUrl = "https://maps.google.com",
                keywords = listOf("maps", "ম্যাপ", "google maps", "গুগল ম্যাপ", "নেভিগেশন")
            ),
            AppTarget(
                id = "playstore",
                displayName = "Google Play Store",
                packageName = "com.android.vending",
                webFallbackUrl = "https://play.google.com",
                keywords = listOf("play store", "প্লে স্টোর", "playstore", "গুগল প্লে")
            )
        )
    }

    /**
     * Checks whether an application package is installed on the device
     * using the Android PackageManager with API 30+ <queries> declarations.
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking package $packageName", e)
            false
        }
    }

    /**
     * Identifies whether the input voice or text is requesting to launch an app.
     */
    fun matchAppLaunchCommand(input: String): AppTarget? {
        val lower = input.lowercase().trim()
        val isLaunchIntent = lower.contains("open") ||
                lower.contains("launch") ||
                lower.contains("start") ||
                lower.contains("খোলো") ||
                lower.contains("খোল") ||
                lower.contains("চালু করো") ||
                lower.contains("চালু কর") ||
                lower.contains("রান করো") ||
                lower.contains("ওপেন") ||
                lower.contains("যাও")

        for (app in SUPPORTED_APPS) {
            for (kw in app.keywords) {
                if (lower.contains(kw)) {
                    if (isLaunchIntent || lower.equals(kw, ignoreCase = true) || lower.startsWith(kw) || lower.endsWith(kw)) {
                        return app
                    }
                }
            }
        }
        return null
    }

    /**
     * Detects built-in fast special commands like time, stop, or greetings.
     */
    fun detectSpecialCommand(input: String): SpecialCommand? {
        val lower = input.lowercase().trim()

        // Stop listening commands
        if (lower == "stop" || lower.startsWith("stop") || lower.contains("থামো") || lower.contains("চুপ") || lower.contains("বন্ধ করো")) {
            return SpecialCommand.StopListening
        }

        // Current time query
        if ((lower.contains("time") && (lower.contains("what") || lower.contains("current") || lower.contains("now"))) ||
            lower.contains("কয়টা") || lower.contains("কয়টা") || lower.contains("কটা বাজে") ||
            (lower.contains("সময়") && lower.contains("কত")) || (lower.contains("সময়") && lower.contains("কত"))
        ) {
            val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val dateStr = sdf.format(java.util.Date())
            return SpecialCommand.CurrentTime(
                englishText = "The current time is $dateStr.",
                bengaliText = "এখন সময় হলো $dateStr।"
            )
        }

        // Weather query
        if (lower.contains("weather") || lower.contains("temperature") ||
            lower.contains("আবহাওয়া") || lower.contains("আবহাওয়া") || lower.contains("আজকের আবহাওয়া")
        ) {
            return SpecialCommand.WeatherQuery(
                englishText = "To check exact live hyper-local weather conditions, I can open Google Weather or Maps for you.",
                bengaliText = "আপনার এলাকার সঠিক ও রিয়েলটাইম আবহাওয়া দেখতে আমি গুগল ওয়েদার বা ম্যাপস খুলে দিতে পারি।"
            )
        }

        // Greeting
        if (lower == "hello" || lower == "hi" || lower == "hello rifaul ai" ||
            lower == "হ্যালো" || lower == "হাই" || lower == "হ্যালো rifaul ai" ||
            lower.contains("কেমন আছো") || lower.contains("how are you")
        ) {
            return SpecialCommand.Greeting(
                englishText = "Hello! I am RIFAUL AI, your futuristic personal assistant. How can I assist you today?",
                bengaliText = "হ্যালো! আমি RIFAUL AI, আপনার পার্সোনাল এআই অ্যাসিস্ট্যান্ট। আজ আপনাকে কীভাবে সাহায্য করতে পারি?"
            )
        }

        return null
    }

    /**
     * Attempts to launch an installed application verified via Intent.
     * Never pretends an action succeeded unless launch is verified.
     */
    fun launchApplication(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                Log.w(TAG, "No launch intent found for $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
            false
        }
    }

    /**
     * Safely opens a fallback web link in the browser.
     */
    fun openWebUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open web url $url", e)
            false
        }
    }

    /**
     * Safely opens the Google Play Store for the requested app.
     */
    fun openPlayStoreForApp(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback to browser URL for Play Store
            openWebUrl("https://play.google.com/store/apps/details?id=$packageName")
        }
    }
}
