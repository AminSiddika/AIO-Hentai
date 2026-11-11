package com.aiohentai.cs.sites.missav

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

/**
 * MissAV is JAV, not anime. The NSFW type keeps it in the adult section instead of
 * mixing it into Home -> Anime.
 */
class MissavProvider : AniyomiProvider(Missav()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
