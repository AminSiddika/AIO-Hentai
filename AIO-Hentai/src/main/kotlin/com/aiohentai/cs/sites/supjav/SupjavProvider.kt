package com.aiohentai.cs.sites.supjav

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class SupjavProvider : AniyomiProvider(Supjav()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
