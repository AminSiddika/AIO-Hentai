package com.aiohentai.cs.sites.javbangers

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class JavbangersProvider : AniyomiProvider(Javbangers()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
