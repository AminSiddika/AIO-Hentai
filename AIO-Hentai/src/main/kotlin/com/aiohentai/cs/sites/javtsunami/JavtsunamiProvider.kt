package com.aiohentai.cs.sites.javtsunami

import com.aiohentai.cs.compat.AniyomiProvider
import com.lagradost.cloudstream3.TvType

class JavtsunamiProvider : AniyomiProvider(Javtsunami()) {
    override val supportedTypes = setOf(TvType.NSFW, TvType.Movie)
    override val preferredType = TvType.NSFW
}
