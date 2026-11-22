package com.aiohentai.cs.sites.tnaflix

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class TnaflixProvider : AniyomiProvider(Tnaflix()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
