package com.aiohentai.cs.sites.xhamster

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class XhamsterProvider : AniyomiProvider(Xhamster()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
