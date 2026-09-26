package com.neverreader.app.add

import com.neverreader.backend.model.Bookmark
import com.neverreader.repository.BookmarkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

object AddItemFromIntentUtil {

	enum class ErrorStatus {
		ADD_INVALID_URL,
		ADD_ALREADY_IN,
	}

	fun interface Callback {
		fun result(bookmark: Bookmark?, status: ErrorStatus?)
	}
}

@Singleton
class AddUrlSaver @Inject constructor(
	private val bookmarks: BookmarkRepository,
) {

	fun add(intentItem: IntentItem?, scope: CoroutineScope, callback: AddItemFromIntentUtil.Callback) {
		val url = intentItem?.url
		if (url.isNullOrBlank()) {
			callback.result(null, AddItemFromIntentUtil.ErrorStatus.ADD_INVALID_URL)
			return
		}
		scope.launch {
			val existing = bookmarks.bookmarkByUrlOnce(url)
			if (existing != null) {
				withContext(Dispatchers.Main) { callback.result(existing, AddItemFromIntentUtil.ErrorStatus.ADD_ALREADY_IN) }
				return@launch
			}
			val result = runCatching { bookmarks.add(url, intentItem.title) }
			withContext(Dispatchers.Main) {
				result.fold(
					onSuccess = { callback.result(it, null) },
					onFailure = { callback.result(null, AddItemFromIntentUtil.ErrorStatus.ADD_INVALID_URL) },
				)
			}
		}
	}
}
