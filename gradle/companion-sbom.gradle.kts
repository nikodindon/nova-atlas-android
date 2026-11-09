import groovy.json.JsonOutput
import java.security.MessageDigest
import java.util.zip.ZipFile
import java.io.InputStream
import java.time.Instant

fun sha256(input: InputStream): String = input.use { stream ->
    val digest=MessageDigest.getInstance("SHA-256")
    val buffer=ByteArray(8192)
    while(true) { val count=stream.read(buffer);if(count<0) break;digest.update(buffer,0,count) }
    digest.digest().joinToString("") { "%02x".format(it) }
}
tasks.register("companionSbom") {
    dependsOn("assembleDebug")
    doLast {
        val apk=layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        val artifacts=configurations.getByName("debugRuntimeClasspath").resolvedConfiguration.resolvedArtifacts
        val components=artifacts.sortedBy { it.moduleVersion.id.toString() }.map { artifact ->
            val id=artifact.moduleVersion.id
            mapOf("type" to "library","group" to id.group,"name" to id.name,"version" to id.version,
                "purl" to "pkg:maven/${id.group}/${id.name}@${id.version}",
                "hashes" to listOf(mapOf("alg" to "SHA-256","content" to sha256(artifact.file.inputStream()))))
        }
        val natives=ZipFile(apk).use { zip -> zip.entries().asSequence().filter { !it.isDirectory && it.name.startsWith("lib/") && it.name.endsWith(".so") }.sortedBy { it.name }.map {
            mapOf("path_in_apk" to it.name,"bytes" to it.size,"sha256" to sha256(zip.getInputStream(it)),"cve_identification" to "NOT_ESTABLISHED")
        }.toList() }
        val output=layout.buildDirectory.dir("reports/sbom").get().asFile;output.mkdirs()
        val bom=mapOf("bomFormat" to "CycloneDX","specVersion" to "1.6","version" to 1,
            "metadata" to mapOf("timestamp" to Instant.now().toString()),"components" to components)
        output.resolve("android.cdx.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(bom))+"\n")
        output.resolve("apk-inventory.json").writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mapOf(
            "schema_version" to 1,"commit" to (System.getenv("GITHUB_SHA") ?: "unrecorded"),
            "apk_sha256" to sha256(apk.inputStream()),"variant" to "debug","maven_components" to components.size,
            "native_libraries" to natives,"scope" to "resolved debug runtime artifacts, including packaged SDKs disabled at runtime; not a reachability or license grant")))+"\n")
    }
}
