# Proguard rules for ConkyDroid
-keepclassmembers class * extends androidx.compose.ui.node.LayoutNode {
    *** modifier;
}
-dontwarn org.jetbrains.annotations.**
