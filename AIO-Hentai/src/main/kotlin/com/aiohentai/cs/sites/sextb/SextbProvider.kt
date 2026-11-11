package com.aiohentai.cs.sites.sextb

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class SextbProvider : AniyomiProvider(Sextb()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
