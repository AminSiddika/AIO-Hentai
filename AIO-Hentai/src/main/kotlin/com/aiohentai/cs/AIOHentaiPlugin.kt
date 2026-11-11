package com.aiohentai.cs

import android.content.Context
import com.aiohentai.cs.sites.akih.AkiHProvider
import com.aiohentai.cs.sites.animeidhentai.AnimeIdHentaiProvider
import com.aiohentai.cs.sites.animesex.AnimeSexProvider
import com.aiohentai.cs.sites.cartoonporn.CartoonPornProvider
import com.aiohentai.cs.sites.erotichentai.EroticHentaiProvider
import com.aiohentai.cs.sites.hanime.HAnimeProvider
import com.aiohentai.cs.sites.hanimehentai.HanimeHentaiProvider
import com.aiohentai.cs.sites.hanimered.HanimeRedProvider
import com.aiohentai.cs.sites.hanimetv.HanimeTvProvider
import com.aiohentai.cs.sites.hdhentai.HdHentaiProvider
import com.aiohentai.cs.sites.hentai0.Hentai0Provider
import com.aiohentai.cs.sites.hentai222.Hentai222Provider
import com.aiohentai.cs.sites.hentai2w.Hentai2wProvider
import com.aiohentai.cs.sites.hentai4k.Hentai4kProvider
import com.aiohentai.cs.sites.hentaiastra.HentaiAstraProvider
import com.aiohentai.cs.sites.hentaibros.HentaibrosProvider
import com.aiohentai.cs.sites.hentaicity.HentaiCityProvider
import com.aiohentai.cs.sites.hentaicloud.HentaiCloudProvider
import com.aiohentai.cs.sites.hentaifor.HentaiForProvider
import com.aiohentai.cs.sites.hentaifox.HentaiFoxProvider
import com.aiohentai.cs.sites.hentaifreak.HentaiFreakProvider
import com.aiohentai.cs.sites.hentaigasm.HentaigasmProvider
import com.aiohentai.cs.sites.hentaigem.HentaiGemProvider
import com.aiohentai.cs.sites.hentaihaven.HentaiHavenProvider
import com.aiohentai.cs.sites.hentaihavencom.HentaiHavenComProvider
import com.aiohentai.cs.sites.hentaihavenred.HentaiHavenRedProvider
import com.aiohentai.cs.sites.hentaihavenxxx.HentaiHavenXxxProvider
import com.aiohentai.cs.sites.hentaihd.HentaiHDProvider
import com.aiohentai.cs.sites.hentaihorizon.HentaiHorizonProvider
import com.aiohentai.cs.sites.hentaijp.HentaiJpProvider
import com.aiohentai.cs.sites.hentaiking.HentaiKingProvider
import com.aiohentai.cs.sites.hentaimama.HentaiMamaProvider
import com.aiohentai.cs.sites.hentaimoon.HentaiMoonProvider
import com.aiohentai.cs.sites.hentaiocean.HentaiOceanProvider
import com.aiohentai.cs.sites.hentaiplay.HentaiPlayProvider
import com.aiohentai.cs.sites.hentaisea.HentaiSeaProvider
import com.aiohentai.cs.sites.hentaismile.HentaiSmileProvider
import com.aiohentai.cs.sites.hentaistream.HentaiStreamProvider
import com.aiohentai.cs.sites.hentaistreamtv.HentaistreamTvProvider
import com.aiohentai.cs.sites.hentaistube.HentaisTubeProvider
import com.aiohentai.cs.sites.hentaitubeonline.HentaiTubeOnlineProvider
import com.aiohentai.cs.sites.hentaitv.HentaiTvProvider
import com.aiohentai.cs.sites.hentaivideos.HentaiVideosProvider
import com.aiohentai.cs.sites.hentaiyes.HentaiYesProvider
import com.aiohentai.cs.sites.hentaiyoga.HentaiYogaProvider
import com.aiohentai.cs.sites.hentaverse.HentaverseProvider
import com.aiohentai.cs.sites.hstream.HStreamProvider
import com.aiohentai.cs.sites.javguru.JavGuruProvider
import com.aiohentai.cs.sites.miohentai.MioHentaiProvider
import com.aiohentai.cs.sites.muchohentai.MuchoHentaiProvider
import com.aiohentai.cs.sites.mundohentai.MundoHentaiProvider
import com.aiohentai.cs.sites.naughtyhentai.NaughtyHentaiProvider
import com.aiohentai.cs.sites.nekopoi.NekopoiProvider
import com.aiohentai.cs.sites.nsfwr34.NsfwR34Provider
import com.aiohentai.cs.sites.ohentai.OhentaiProvider
import com.aiohentai.cs.sites.okioto.OkiotoProvider
import com.aiohentai.cs.sites.oppai.OppaiProvider
import com.aiohentai.cs.sites.r34tube.R34TubeProvider
import com.aiohentai.cs.sites.rule34paheal.Rule34PahealProvider
import com.aiohentai.cs.sites.rule34s.Rule34sProvider
import com.aiohentai.cs.sites.rule34video.Rule34VideoProvider
import com.aiohentai.cs.sites.thejoidatabase.TheJoiDatabaseProvider
import com.aiohentai.cs.sites.threehentai.ThreeDHentaiProvider
import com.aiohentai.cs.sites.uncensoredhentai.UncensoredHentaiProvider
import com.aiohentai.cs.sites.underhentai.UnderHentaiProvider
import com.aiohentai.cs.sites.watchanimehentai.WatchAnimeHentaiProvider
import com.aiohentai.cs.sites.watchhentai.WatchHentaiProvider
import com.aiohentai.cs.sites.xanimeporn.XAnimePornProvider
import com.aiohentai.cs.sites.zhentube.ZhenTubeProvider
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AIOHentaiPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(WatchHentaiProvider())
        registerMainAPI(HentaiCityProvider())
        registerMainAPI(MuchoHentaiProvider())
        registerMainAPI(OhentaiProvider())
        registerMainAPI(HentaiPlayProvider())
        registerMainAPI(NaughtyHentaiProvider())
        registerMainAPI(HentaiStreamProvider())
        registerMainAPI(HentaiTvProvider())
        registerMainAPI(AnimeIdHentaiProvider())
        registerMainAPI(NsfwR34Provider())
        registerMainAPI(HentaiMoonProvider())
        registerMainAPI(HentaiHavenProvider())
        registerMainAPI(UncensoredHentaiProvider())
        registerMainAPI(TheJoiDatabaseProvider())
        registerMainAPI(HentaiHavenComProvider())
        registerMainAPI(MioHentaiProvider())
        registerMainAPI(AkiHProvider())
        registerMainAPI(HentaiGemProvider())
        registerMainAPI(HanimeHentaiProvider())
        registerMainAPI(HentaiHDProvider())
        registerMainAPI(HentaisTubeProvider())
        registerMainAPI(HentaiTubeOnlineProvider())
        registerMainAPI(Hentai2wProvider())
        registerMainAPI(Hentai0Provider())
        registerMainAPI(ZhenTubeProvider())
        registerMainAPI(HentaiFreakProvider())
        registerMainAPI(HentaiHorizonProvider())
        registerMainAPI(HentaiHavenRedProvider())
        registerMainAPI(HentaiVideosProvider())
        registerMainAPI(HentaiForProvider())
        registerMainAPI(HentaiJpProvider())
        registerMainAPI(HentaiCloudProvider())
        registerMainAPI(HStreamProvider())
        registerMainAPI(HentaiKingProvider())
        registerMainAPI(HentaiYesProvider())
        registerMainAPI(HentaiFoxProvider())
        registerMainAPI(HentaibrosProvider())
        registerMainAPI(EroticHentaiProvider())
        registerMainAPI(HAnimeProvider())
        registerMainAPI(HentaverseProvider())
        registerMainAPI(OppaiProvider())
        registerMainAPI(NekopoiProvider())
        registerMainAPI(MundoHentaiProvider())
        registerMainAPI(HanimeRedProvider())
        registerMainAPI(Rule34VideoProvider())
        registerMainAPI(HentaiHavenXxxProvider())
        registerMainAPI(AnimeSexProvider())
        registerMainAPI(ThreeDHentaiProvider())
        registerMainAPI(Rule34sProvider())
        registerMainAPI(HentaiSmileProvider())
        registerMainAPI(HentaiOceanProvider())
        registerMainAPI(HentaiYogaProvider())
        registerMainAPI(HentaiSeaProvider())
        registerMainAPI(HdHentaiProvider())
        registerMainAPI(HentaiMamaProvider())
        registerMainAPI(R34TubeProvider())
        registerMainAPI(WatchAnimeHentaiProvider())
        registerMainAPI(XAnimePornProvider())
        registerMainAPI(OkiotoProvider())
        registerMainAPI(UnderHentaiProvider())
        registerMainAPI(HentaiAstraProvider())
        registerMainAPI(HanimeTvProvider())
        registerMainAPI(HentaigasmProvider())
        registerMainAPI(Hentai4kProvider())
        registerMainAPI(HentaistreamTvProvider())
        registerMainAPI(Rule34PahealProvider())
        registerMainAPI(Hentai222Provider())
        registerMainAPI(CartoonPornProvider())
        registerMainAPI(JavGuruProvider())
    }
}
