package app.revanced.patches.nicomanga

import app.revanced.patcher.*
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import org.w3c.dom.Node

private const val MAIN_APPLICATION = "Lcom/lovehug/MainApplication;"
private const val EXTENSION =
    "Lapp/revanced/extension/nicomanga/NicomangaRevanced;"
private const val NETWORK_OBSERVER =
    "Lapp/revanced/extension/nicomanga/NetworkObserver;"
private const val OKHTTP_BUILDER = "Lokhttp3/OkHttpClient\$Builder;"
private const val FABRIC_UI_MANAGER = "Lcom/facebook/react/fabric/FabricUIManager;"

private val adClassPrefixes = listOf(
    "Lcom/applovin/",
    "Lcom/facebook/ads/",
    "Lcom/google/android/gms/ads/",
    "Lcom/tradplus/",
    "Lcom/vungle/",
    "Lcom/bytedance/sdk/openadsdk/",
    "Lcom/mbridge/msdk/",
    "Lcom/ironsource/",
    "Lcom/fyber/",
    "Lcom/chartboost/",
    "Lcom/unity3d/ads/",
    "Lcom/unity3d/services/",
    "Lcom/amazon/device/ads/",
    "Lsg/bigo/ads/",
    "Lexpo/modules/tradplusad/",
)

private val blockedAdMethods = setOf(
    "init",
    "initSdk",
    "initialize",
    "initializeSdk",
    "load",
    "loadAd",
    "loadBanner",
    "loadInterstitial",
    "loadRewarded",
    "loadRewardedAd",
    "loadRewardedVideo",
    "requestAd",
    "run",
    "saveEvent",
    "sendEvent",
    "dispatch",
    "track",
    "report",
    "upload",
    "show",
    "showAd",
    "showBanner",
    "showInterstitial",
    "showRewarded",
    "showRewardedAd",
    "showRewardedVideo",
    "showSplash",
)

private val adManifestPrefixes = listOf(
    "com.applovin.",
    "com.facebook.ads.",
    "com.google.android.gms.ads.",
    "com.tradplus.",
    "com.vungle.",
    "com.bytedance.sdk.openadsdk.",
    "com.mbridge.msdk.",
    "com.ironsource.",
    "com.fyber.",
    "com.chartboost.",
    "com.unity3d.ads.",
    "com.unity3d.services.",
    "com.amazon.device.ads.",
    "sg.bigo.ads.",
)

private val adPermissions = setOf(
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "android.permission.ACCESS_ADSERVICES_TOPICS",
    "com.applovin.array.apphub.permission.BIND_APPHUB_SERVICE",
)

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val TEST_PACKAGE_NAME = "app.revanced.nicomanga.test"

private val removeAdComponentsPatch = resourcePatch {
    apply {
        document("AndroidManifest.xml").use { document ->
            val root = document.documentElement
            val nodesToRemove = mutableListOf<Node>()

            fun androidName(element: Element): String =
                element.getAttributeNS(ANDROID_NAMESPACE, "name").ifEmpty {
                    element.getAttribute("android:name")
                }

            fun isAdName(name: String) =
                adManifestPrefixes.any(name::startsWith) ||
                    name.contains("admob", ignoreCase = true) ||
                    name.contains("mobileads", ignoreCase = true)

            fun inspect(node: Node) {
                if (node is Element) {
                    val name = androidName(node)
                    val remove = when (node.tagName) {
                        "uses-permission", "uses-permission-sdk-23" -> name in adPermissions
                        "activity", "activity-alias", "provider", "receiver", "service" ->
                            isAdName(name)
                        "meta-data", "property" -> isAdName(name)
                        "action" -> isAdName(name)
                        "uses-library", "uses-sdk-library" ->
                            isAdName(name) || name.contains("adservices", ignoreCase = true)
                        else -> false
                    }
                    if (remove) {
                        nodesToRemove += if (node.tagName == "action" && node.parentNode != null) {
                            node.parentNode
                        } else {
                            node
                        }
                    }
                }

                val children = node.childNodes
                for (index in 0 until children.length) inspect(children.item(index))
            }

            inspect(root)
            nodesToRemove.distinct().forEach { node -> node.parentNode?.removeChild(node) }
        }
    }
}

@Suppress("unused")
val useTestPackageNamePatch = resourcePatch(
    name = "検証用パッケージ名を使用",
    description = "既存アプリのデータを消さずに実機検証できるよう、別パッケージとしてインストールします。",
    use = false,
) {
    compatibleWith("com.lovehug")

    apply {
        document("AndroidManifest.xml").use { document ->
            val root = document.documentElement
            root.setAttribute("package", TEST_PACKAGE_NAME)
            (root.getElementsByTagName("application").item(0) as? Element)
                ?.setAttribute("android:debuggable", "true")

            fun rewrite(node: Node) {
                if (node is Element) {
                    val authorities = node.getAttributeNS(ANDROID_NAMESPACE, "authorities").ifEmpty {
                        node.getAttribute("android:authorities")
                    }
                    if (authorities.contains("com.lovehug")) {
                        node.setAttribute("android:authorities", authorities.replace("com.lovehug", TEST_PACKAGE_NAME))
                    }

                    if (node.tagName in setOf("permission", "uses-permission", "uses-permission-sdk-23")) {
                        val name = node.getAttributeNS(ANDROID_NAMESPACE, "name").ifEmpty {
                            node.getAttribute("android:name")
                        }
                        if (name.startsWith("com.lovehug.")) {
                            node.setAttribute("android:name", name.replace("com.lovehug", TEST_PACKAGE_NAME))
                        }
                    }
                }
                val children = node.childNodes
                for (index in 0 until children.length) rewrite(children.item(index))
            }

            rewrite(root)
        }
    }
}

@Suppress("unused")
val nicomangaRevancedPatch = bytecodePatch(
    name = "Nicomanga ReVanced",
    description = "Nicomanga拡張の基盤を追加し、広告SDKの通信・読込・表示を遮断します。",
) {
    compatibleWith("com.lovehug")
    extendWith("extensions/nicomanga.rve")
    dependsOn(removeAdComponentsPatch)

    apply {
        val onCreate = firstMethodDeclaratively {
            name("onCreate")
            definingClass(MAIN_APPLICATION)
            returnType("V")
            parameterTypes()
        }
        onCreate.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION->initializeApplication(Landroid/app/Application;)V",
        )

        classDefs.find { classDef -> classDef.type == OKHTTP_BUILDER }
            ?.let { classDef ->
                classDef.methods.find { method ->
                    method.name == "build" &&
                        method.parameterTypes.isEmpty() &&
                        method.returnType == "Lokhttp3/OkHttpClient;" &&
                        method.implementation != null
                }?.let { method ->
                    classDefs.getOrReplaceMutable(classDef).firstMethod(method).addInstruction(
                        0,
                        "invoke-static { p0 }, $NETWORK_OBSERVER->installOkHttpInterceptor(Ljava/lang/Object;)V",
                    )
                }
            }

        classDefs.find { classDef -> classDef.type == FABRIC_UI_MANAGER }
            ?.let { classDef ->
                val receiveEvents = classDef.methods.filter { method ->
                    method.name == "receiveEvent" &&
                        method.returnType == "V" &&
                        method.implementation != null &&
                        method.parameterTypes.count { it == "Ljava/lang/String;" } == 1 &&
                        method.parameterTypes.count { it == "Lcom/facebook/react/bridge/WritableMap;" } == 1
                }
                val mutableClass = classDefs.getOrReplaceMutable(classDef)
                receiveEvents.forEach { method ->
                    val parameters = method.parameterTypes
                    val tagParameter = if (parameters.size == 3) 0 else 1
                    val nameParameter = parameters.indexOf("Ljava/lang/String;")
                    val dataParameter = parameters.indexOf("Lcom/facebook/react/bridge/WritableMap;")
                    mutableClass.firstMethod(method).addInstruction(
                        0,
                        "invoke-static { p0, p${tagParameter + 1}, p${nameParameter + 1}, p${dataParameter + 1} }, " +
                            "$NETWORK_OBSERVER->onFabricEvent(Ljava/lang/Object;ILjava/lang/String;Ljava/lang/Object;)V",
                    )
                }
            }

        classDefs
            .filter { classDef -> adClassPrefixes.any(classDef.type::startsWith) }
            .forEach { classDef ->
                val targets = classDef.methods.filter { method ->
                    method.implementation != null &&
                        method.returnType == "V" &&
                        method.name in blockedAdMethods &&
                        method.name != "<init>" &&
                        method.name != "<clinit>"
                }
                if (targets.isEmpty()) return@forEach

                val mutableClass = classDefs.getOrReplaceMutable(classDef)
                targets.forEach { method ->
                    mutableClass.firstMethod(method).addInstruction(0, "return-void")
                }
            }
    }
}
