package com.aiohentai.cs.sites.okxxx

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class OkxxxProvider : AniyomiProvider(Okxxx()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
