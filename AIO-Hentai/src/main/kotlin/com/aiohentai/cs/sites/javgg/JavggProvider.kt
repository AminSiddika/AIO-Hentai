package com.aiohentai.cs.sites.javgg

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class JavggProvider : AniyomiProvider(Javgg()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
