import java.net.URI

// Runs during configuration, independently of keystore presence. No network requests.
val endpointEnvironment=providers.gradleProperty("ATLAS_ENV").getOrElse("isolated")
if (endpointEnvironment == "release") {
    fun productionEndpoint(value: String): Boolean = try {
        val uri=URI(value)
        val host=uri.host?.lowercase()?.trimEnd('.') ?: ""
        val reserved=listOf("invalid","test","example","localhost","example.com","example.org","example.net")
        uri.scheme == "https" && host.isNotBlank() && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            reserved.none { host == it || host.endsWith(".$it") }
    } catch (_: Exception) { false }
    val api=providers.gradleProperty("ATLAS_ORIGIN").getOrElse("https://example.invalid/")
    val radio=providers.gradleProperty("ATLAS_RADIO").getOrElse("https://example.invalid/stream/nova-android")
    require(productionEndpoint(api) && productionEndpoint(radio)) { "Release requires non-placeholder HTTPS API and radio endpoints" }
}
