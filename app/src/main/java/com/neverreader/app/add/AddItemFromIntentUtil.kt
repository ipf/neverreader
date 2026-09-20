package com.neverreader.app.add

import com.neverreader.app.AppGraph
import com.neverreader.sdk.api.generated.enums.ItemStatus
import com.neverreader.sdk.api.generated.thing.Item
import com.neverreader.sdk.api.thing.ItemUtil
import com.neverreader.sdk.api.value.UrlString
import com.neverreader.sdk2.analytics.context.Interaction
import com.neverreader.sync.source.result.SyncException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Helper for shared logic between the different forms of saving from an intent.
 */
object AddItemFromIntentUtil {
	@JvmStatic fun add(intentItem: IntentItem?, app: AppGraph, it: Interaction, callback: Callback) {
		val url = intentItem?.url
		if (url == null) {
			callback.result(null, ErrorStatus.ADD_INVALID_URL)
			return
		}

		val pocket = app.pocket()
		val add = pocket.spec().actions().add()
			.url(UrlString(url))
			.context(it.context)
			.time(it.time)

		if (!intentItem.title.isNullOrBlank()) {
			add.title(intentItem.title)
		}

		val item = ItemUtil.create(url, pocket.spec())

		val isAlreadySaved = AtomicBoolean(false)
		pocket.syncLocal(item)
			.onSuccess { found: Item? -> isAlreadySaved.set(found != null && found.status === ItemStatus.UNREAD) }
			.onComplete {
				pocket.sync(item, add.build()).onSuccess { i: Item? ->
					if (isAlreadySaved.get()) {
						callback.result(i, ErrorStatus.ADD_ALREADY_IN)
					} else {
						callback.result(i, null)
					}
				}
					.onFailure { e: SyncException? ->
						callback.result(
							null,
							ErrorStatus.ADD_INVALID_URL
						)
					}
			}
	}

	enum class ErrorStatus {
		ADD_INVALID_URL,
		ADD_ALREADY_IN,
	}

	interface Callback {
		fun result(item: Item?, status: ErrorStatus?)
	}
}
