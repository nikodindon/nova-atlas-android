package com.niko.novaatlas

import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.serialization.decodeFromString
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class CompanionTest {
    private fun fixture(name: String) = javaClass.classLoader!!.getResource("contracts/$name.json")!!.readText()
    @Test fun explicitEnvironmentAndTimeouts() { assertEquals("https",java.net.URI(BuildConfig.API_ORIGIN).scheme);assertFalse(BuildConfig.ADS_ENABLED);assertFalse(BuildConfig.PREMIUM_ENABLED);val client=ApiClient.client();assertEquals(20000,client.callTimeoutMillis);assertFalse(client.followRedirects);assertFalse(client.retryOnConnectionFailure) }
    @Test fun fixtureFingerprints() { val expected=ApiClient.json.decodeFromString<Map<String,String>>(fixture("sha256"));expected.forEach { (name,hash) -> val bytes=javaClass.classLoader!!.getResource("contracts/$name")!!.readBytes();assertEquals(hash,java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }) } }
    @Test fun customCategoryOrdering() { val data=ApiClient.json.decodeFromString<SiteResponse>("""{"status":"ok","streams":{},"categories":[{"id":"日本語","name":"日本の技術","order":0},{"id":"local","name":"Locales","order":1}]}""");assertEquals(listOf("日本語","local"),data.categories.map{it.id}) }
    @Test fun legacyContract() { val r=ApiClient.json.decodeFromString<ArticlesResponse>(fixture("legacy")); assertEquals("ok",r.status); assertTrue(r.articles.isNotEmpty()) }
    @Test fun publicContract() { val r=ApiClient.json.decodeFromString<PublicArticles>(fixture("articles")); assertEquals("ok",r.status); assertTrue(r.articles.isNotEmpty()); assertNotNull(r.articles[0].legacy()) }
    @Test fun approvedImageContract() { val r=ApiClient.json.decodeFromString<PublicArticles>(fixture("approved-articles")); val image=r.articles[0].image!!;assertFalse(image.fallback);assertEquals("CC BY 4.0",image.attribution!!.license);assertNotNull(SafeLinks.image(image));assertTrue(image.attribution.author.isNotBlank()) }
    @Test fun categoryContract() { val r=ApiClient.json.decodeFromString<SiteResponse>(fixture("site")); assertTrue(r.categories.isNotEmpty()); assertEquals("/stream/nova-android",r.streams["android"]) }
    @Test fun eventsContract() { val r=ApiClient.json.decodeFromString<EventsResponse>(fixture("events")); assertTrue(r.index_available); assertEquals(2,r.events[0].article_count); assertEquals(2,r.events[0].articles.size) }
    @Test fun eventContract() { assertEquals(2,ApiClient.json.decodeFromString<EventResponse>(fixture("event")).event.article_count) }
    @Test fun subscriptionNotPurchaseProof() { val r=ApiClient.json.decodeFromString<SubscriptionStatusResponse>(fixture("subscription")); assertFalse(r.is_premium) }
    @Test fun optionalUnicode() { val r=ApiClient.json.decodeFromString<Article>("""{"hash":"h","timestamp":"","category":"日本語","title":"العربية 🎙️ <script>"}"""); assertEquals("日本語",r.category); assertNull(r.image) }
    @Test fun ambiguousDate() { assertNull(reliableInstant("2026-10-25T02:30:00")); assertNull(reliableInstant("invalid")); assertNotNull(reliableInstant("2026-10-25T02:30:00+02:00")) }
    @Test fun datesLeap() { assertNotNull(reliableInstant("2024-02-29T23:00:00Z")); assertNull(reliableInstant("2025-02-29T23:00:00Z")) }
    @Test fun dangerousLinks() { listOf("javascript:alert(1)","data:image/svg+xml,x","file:///etc/passwd","https://u:p@example.invalid").forEach { assertNull(SafeLinks.external(it)) } }
    @Test fun mediaPaths() { listOf("https://evil.invalid/media/a","//evil.invalid/media/a","/media/../a","/media/%2e/a","/media/a?x=1").forEach { assertNull(SafeLinks.local(it,"/media/")) }; assertEquals("/media/images/x/320.jpg",SafeLinks.local("/media/images/x/320.jpg","/media/")) }
    @Test fun revokedAndUnknownImages() { assertNull(SafeLinks.image(PublicImage("/media/images/x/320.jpg",true))); assertNull(SafeLinks.image(PublicImage("/media/images/x/320.jpg",false))); assertNotNull(SafeLinks.image(PublicImage("/media/images/x/320.jpg",false,attribution=Attribution(license="CC BY 4.0")))) }
    @Test fun retryAfterValues() { assertEquals(120L,retryAfter("120",0)); assertNull(retryAfter("bad",0)); assertEquals(60L,retryAfter("Thu, 1 Jan 1970 00:01:00 GMT",0)) }
    @Test fun radioEmbargoBlocksManualPlay() { var now=0L;val embargo=RadioEmbargo{now};embargo.record("600");assertFalse(embargo.allowed());now=599999;assertFalse(embargo.allowed());now=600000;assertTrue(embargo.allowed()) }
    @Test fun eofRecoveryBounded() { val p=RadioEndRecovery();assertEquals(listOf(3000L,6000L,12000L,24000L),(1..4).map{p.nextDelay()});assertNull(p.nextDelay());p.reset();assertEquals(3000L,p.nextDelay()) }
    @Test fun radioBounded() { assertEquals(3000L,radioRetryDelay(1)); assertEquals(24000L,radioRetryDelay(4)); assertEquals(androidx.media3.common.C.TIME_UNSET,radioRetryDelay(5)) }
    @Test fun radioFatal() { listOf(401,403,404,410).forEach { assertEquals(androidx.media3.common.C.TIME_UNSET,radioRetryDelay(1,it)) }; assertEquals(androidx.media3.common.C.TIME_UNSET,radioRetryDelay(1,tls=true)) }
    @Test fun radioEmbargo() { assertEquals(30000L,radioRetryDelay(1,429,"30")); assertEquals(androidx.media3.common.C.TIME_UNSET,radioRetryDelay(1,429,"600")) }

    private class Fake: NovaAtlasApi {
        var active=0; var peak=0; var calls=0; var failure: Exception?=null; var wait=0L
        override suspend fun articles(size: Int): PublicArticles { calls++;active++;peak=maxOf(peak,active);try { delay(wait);failure?.let { throw it };return PublicArticles("ok","20261108",0,emptyList()) } finally { active-- } }
        override suspend fun site()=SiteResponse("ok",listOf(SiteCategory("日本語","日本語")),mapOf("android" to "/stream/nova-android"))
        override suspend fun getArticles(date:String?,category:String?,limit:Int)=ArticlesResponse("ok","20261108",0,emptyList())
        override suspend fun getSubscriptionStatus(deviceId:String):SubscriptionStatusResponse=error("not requested")
        override suspend fun events(size:Int)=EventsResponse("ok",emptyList(),false)
        override suspend fun event(id:String):EventResponse=error("not requested")
    }
    @Test fun slowSingleFlight()=runTest { val f=Fake().apply {wait=7000};val repo=NewsRepository(f);val jobs=(1..8).map { launch {repo.refresh()} };jobs.joinAll();assertEquals(1,f.peak);assertEquals(1,f.calls) }
    @Test fun cancelledAndResumed()=runTest { val f=Fake().apply {wait=10000};val r=NewsRepository(f);val j=launch {r.refresh()};runCurrent();j.cancelAndJoin();assertFalse(r.state.value.refreshing);f.wait=0;r.refresh();assertEquals(2,f.calls) }
    @Test fun staleDataPreserved()=runTest { var now=0L;val f=Fake();val r=NewsRepository(f){now};r.refresh();val old=r.state.value.observedAt;now=60000;f.failure=IOException("sensitive private URL");r.refresh();assertEquals(old,r.state.value.observedAt);assertEquals("network",r.state.value.error);assertFalse(r.state.value.error!!.contains("sensitive")) }
    @Test fun emptyNotError()=runTest { val r=NewsRepository(Fake());r.refresh();assertTrue(r.state.value.articles.isEmpty());assertNull(r.state.value.error);assertNotNull(r.state.value.observedAt) }
    @Test fun lifecycleLoopCancelled()=runTest { val f=Fake();val r=NewsRepository(f){testScheduler.currentTime};val j=launch {r.observe()};runCurrent();advanceTimeBy(120001);j.cancelAndJoin();val count=f.calls;advanceTimeBy(300000);assertEquals(count,f.calls);assertEquals(3,count) }
    @Test fun backoffNoBusyLoop()=runTest { val f=Fake().apply {failure=IOException()};val r=NewsRepository(f){0};repeat(10){r.refresh()};assertEquals(1,f.calls) }
    @Test fun invalidInterval()=runTest { try {NewsRepository(Fake()).observe(2000);fail()} catch (_:IllegalArgumentException){} }
    @Test fun noIndex()=runTest { assertFalse(Fake().events(20).index_available) }

    private fun http(code:Int,body:String="{}")=MockResponse().setResponseCode(code).setHeader("Content-Type","application/json").setBody(body)
    @Test fun mockV2AndCategories()=runTest { MockWebServer().use { server -> server.start();server.enqueue(http(200,fixture("articles")));server.enqueue(http(200,fixture("site")));val r=NewsRepository(ApiClient.create(server.url("/").toString()));r.refresh();assertNull(r.state.value.error);assertEquals(2,server.requestCount) } }
    @Test fun legacyFallbackOnlyMissing()=runTest { MockWebServer().use {s->s.start();s.enqueue(http(404));s.enqueue(http(200,fixture("legacy")));val r=NewsRepository(ApiClient.create(s.url("/").toString()));r.refresh();assertTrue(r.state.value.legacy);assertEquals(2,s.requestCount)} }
    @Test fun httpFailuresNoFallback()=runTest { for (code in listOf(401,403,429,500,304)) { MockWebServer().use {s->s.start();s.enqueue(http(code).setHeader("Retry-After","600"));val r=NewsRepository(ApiClient.create(s.url("/").toString()));r.refresh();assertNotNull(r.state.value.error);assertEquals(1,s.requestCount)} } }
    @Test fun malformedNotEmptySuccess()=runTest { MockWebServer().use {s->s.start();s.enqueue(http(200,"not json"));val r=NewsRepository(ApiClient.create(s.url("/").toString()));r.refresh();assertEquals("format",r.state.value.error);assertNull(r.state.value.observedAt)} }
    @Test fun noRedirect()=runTest { MockWebServer().use {s->s.start();s.enqueue(http(302).setHeader("Location","http://127.0.0.1/private"));val r=NewsRepository(ApiClient.create(s.url("/").toString()));r.refresh();assertNotNull(r.state.value.error);assertEquals(1,s.requestCount)} }
    @Test fun excessiveJsonDepth()=runTest { MockWebServer().use {s->s.start();s.enqueue(http(200,"[".repeat(200)+"]".repeat(200)));val r=NewsRepository(ApiClient.create(s.url("/").toString()));r.refresh();assertEquals("format",r.state.value.error)} }
    @Test fun quotedBracketsAreText() { validateJsonDepth(ApiClient.json.encodeToString(kotlinx.serialization.serializer<String>(),"[".repeat(500))) }
    @Test fun oversized()=runTest { MockWebServer().use {s->s.start();s.enqueue(http(200,"x".repeat(2*1024*1024+1)));val r=NewsRepository(ApiClient.create(s.url("/").toString()));r.refresh();assertEquals("network",r.state.value.error)} }
    @Test fun timeout()=runTest { MockWebServer().use {s->s.start();s.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));val c=ApiClient.client().newBuilder().callTimeout(100,java.util.concurrent.TimeUnit.MILLISECONDS).build();val r=NewsRepository(ApiClient.create(s.url("/").toString(),c));r.refresh();assertEquals("network",r.state.value.error)} }
}
