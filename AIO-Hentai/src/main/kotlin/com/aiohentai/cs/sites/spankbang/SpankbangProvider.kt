package com.aiohentai.cs.sites.spankbang

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class SpankbangProvider : AniyomiProvider(Spankbang()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
