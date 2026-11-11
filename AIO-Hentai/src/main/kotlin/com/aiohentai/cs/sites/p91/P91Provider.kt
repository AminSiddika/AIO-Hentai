package com.aiohentai.cs.sites.p91

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class P91Provider : AniyomiProvider(P91()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
