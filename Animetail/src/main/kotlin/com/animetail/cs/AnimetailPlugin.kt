package com.animetail.cs

import android.content.Context
import com.animetail.cs.sites.akih.AkiHProvider
import com.animetail.cs.sites.animeidhentai.AnimeIdHentaiProvider
import com.animetail.cs.sites.animesex.AnimeSexProvider
import com.animetail.cs.sites.cartoonporn.CartoonPornProvider
import com.animetail.cs.sites.erotichentai.EroticHentaiProvider
import com.animetail.cs.sites.hanime.HAnimeProvider
import com.animetail.cs.sites.hanimehentai.HanimeHentaiProvider
import com.animetail.cs.sites.hanimered.HanimeRedProvider
import com.animetail.cs.sites.hanimetv.HanimeTvProvider
import com.animetail.cs.sites.hdhentai.HdHentaiProvider
import com.animetail.cs.sites.hentai0.Hentai0Provider
import com.animetail.cs.sites.hentai222.Hentai222Provider
import com.animetail.cs.sites.hentai2w.Hentai2wProvider
import com.animetail.cs.sites.hentai4k.Hentai4kProvider
import com.animetail.cs.sites.hentaiastra.HentaiAstraProvider
import com.animetail.cs.sites.hentaibros.HentaibrosProvider
import com.animetail.cs.sites.hentaicity.HentaiCityProvider
import com.animetail.cs.sites.hentaicloud.HentaiCloudProvider
import com.animetail.cs.sites.hentaifor.HentaiForProvider
import com.animetail.cs.sites.hentaifox.HentaiFoxProvider
import com.animetail.cs.sites.hentaifreak.HentaiFreakProvider
import com.animetail.cs.sites.hentaigasm.HentaigasmProvider
import com.animetail.cs.sites.hentaigem.HentaiGemProvider
import com.animetail.cs.sites.hentaihaven.HentaiHavenProvider
import com.animetail.cs.sites.hentaihavencom.HentaiHavenComProvider
import com.animetail.cs.sites.hentaihavenred.HentaiHavenRedProvider
import com.animetail.cs.sites.hentaihavenxxx.HentaiHavenXxxProvider
import com.animetail.cs.sites.hentaihd.HentaiHDProvider
import com.animetail.cs.sites.hentaihorizon.HentaiHorizonProvider
import com.animetail.cs.sites.hentaijp.HentaiJpProvider
import com.animetail.cs.sites.hentaiking.HentaiKingProvider
import com.animetail.cs.sites.hentaimama.HentaiMamaProvider
import com.animetail.cs.sites.hentaimoon.HentaiMoonProvider
import com.animetail.cs.sites.hentaiocean.HentaiOceanProvider
import com.animetail.cs.sites.hentaiplay.HentaiPlayProvider
import com.animetail.cs.sites.hentaisea.HentaiSeaProvider
import com.animetail.cs.sites.hentaismile.HentaiSmileProvider
import com.animetail.cs.sites.hentaistream.HentaiStreamProvider
import com.animetail.cs.sites.hentaistreamtv.HentaistreamTvProvider
import com.animetail.cs.sites.hentaistube.HentaisTubeProvider
import com.animetail.cs.sites.hentaitubeonline.HentaiTubeOnlineProvider
import com.animetail.cs.sites.hentaitv.HentaiTvProvider
import com.animetail.cs.sites.hentaivideos.HentaiVideosProvider
import com.animetail.cs.sites.hentaiyes.HentaiYesProvider
import com.animetail.cs.sites.hentaiyoga.HentaiYogaProvider
import com.animetail.cs.sites.hentaverse.HentaverseProvider
import com.animetail.cs.sites.hstream.HStreamProvider
import com.animetail.cs.sites.javguru.JavGuruProvider
import com.animetail.cs.sites.miohentai.MioHentaiProvider
import com.animetail.cs.sites.muchohentai.MuchoHentaiProvider
import com.animetail.cs.sites.mundohentai.MundoHentaiProvider
import com.animetail.cs.sites.naughtyhentai.NaughtyHentaiProvider
import com.animetail.cs.sites.nekopoi.NekopoiProvider
import com.animetail.cs.sites.nsfwr34.NsfwR34Provider
import com.animetail.cs.sites.ohentai.OhentaiProvider
import com.animetail.cs.sites.okioto.OkiotoProvider
import com.animetail.cs.sites.oppai.OppaiProvider
import com.animetail.cs.sites.r34tube.R34TubeProvider
import com.animetail.cs.sites.rule34paheal.Rule34PahealProvider
import com.animetail.cs.sites.rule34s.Rule34sProvider
import com.animetail.cs.sites.rule34video.Rule34VideoProvider
import com.animetail.cs.sites.thejoidatabase.TheJoiDatabaseProvider
import com.animetail.cs.sites.threehentai.ThreeDHentaiProvider
import com.animetail.cs.sites.uncensoredhentai.UncensoredHentaiProvider
import com.animetail.cs.sites.underhentai.UnderHentaiProvider
import com.animetail.cs.sites.watchanimehentai.WatchAnimeHentaiProvider
import com.animetail.cs.sites.watchhentai.WatchHentaiProvider
import com.animetail.cs.sites.xanimeporn.XAnimePornProvider
import com.animetail.cs.sites.zhentube.ZhenTubeProvider
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AnimetailPlugin : Plugin() {
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
