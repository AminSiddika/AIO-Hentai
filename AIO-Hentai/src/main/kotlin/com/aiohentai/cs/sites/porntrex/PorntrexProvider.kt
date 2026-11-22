package com.aiohentai.cs.sites.porntrex

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class PorntrexProvider : AniyomiProvider(Porntrex()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
